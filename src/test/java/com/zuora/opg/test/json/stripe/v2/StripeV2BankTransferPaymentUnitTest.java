package com.zuora.opg.test.json.stripe.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.zuora.base.Decimal;
import com.zuora.billing.opg.test.common.OpgJsonBaseTest;
import com.zuora.billing.opg.test.json.stripe.StripeRequestPayloadExtractor;
import com.zuora.billing.opg.test.json.stripe.StripeTestHelper;
import com.zuora.billing.opg.test.support.common.BillingAccountBuilder;
import com.zuora.billing.opg.test.support.common.CurrencyBuilder;
import com.zuora.billing.opg.test.support.common.HashMapBuilder;
import com.zuora.billing.opg.test.support.common.PaymentBuilder;
import com.zuora.billing.opg.test.support.engine.OpgRequestMapHelper;
import com.zuora.billing.opg.test.verify.ConnectorHeadersVerifierBuilder;
import com.zuora.zbilling.account.model.BillingAccount;
import com.zuora.zbilling.payment.model.Payment;
import com.zuora.zbilling.paymentmethod.model.PaymentMethod;
import com.zuora.zbilling.setting.gateway.model.PaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.OpenPaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.connector.HttpConnectorCommonUtil;
import com.zuora.zpayment.openpaymentgateway.engine.constants.OpenPaymentGatewayConstants;
import com.zuora.zpayment.openpaymentgateway.engine.templateengine.ZUtility;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Map;

public class StripeV2BankTransferPaymentUnitTest extends OpgJsonBaseTest {
    private static final ZUtility zUtility = new ZUtility();

    @Test
    public void case_01_ItShouldSucceedForNormalPayment() throws Exception {
        final String case_01_response = "/com/zuora/opg/test/json/stripe_2/bankTransfer/payment/case_01/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodBankTransferPaymentMethodForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("2.22"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        // pass RadarSessionId via gateway options
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "RadarSessionId", "rse_1N7fvKSDx60UgxZnDB5Hb4v5");

        // Sale
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_paymentintent")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("pi_3MvJQpHo3zohLKRe1nK4BOTd", responseMap.get("GatewayReferenceId"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(9)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_paymentintent"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_types[]", "sepa_debit"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_02_ReturnUnknownZuoraResponseCodeWhenGatewayReturns5XXHttpStatus() {;
        final String case_02_payment_response = "/com/zuora/opg/test/json/stripe_2/bankTransfer/payment/case_02/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodBankTransferPaymentMethodForTest();

        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("2.22"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        // Sale
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_paymentintent")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "529")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_payment_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Unknown", responseMap.get("ZuoraResponseCode"));
        assertEquals("529", responseMap.get("GatewayResponseCode"));
        assertEquals("529", responseMap.get("lastHttpStatusCode"));
        assertNotNull(responseMap.get("GatewayRequestString"));
        assertNotNull(responseMap.get("GatewayResponseString"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(9)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_paymentintent"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_types[]", "sepa_debit"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_03_ItShouldSucceedForBacsNormalPaymentWithTokens() throws Exception {
        final String case_03_response = "/com/zuora/opg/test/json/stripe_2/bankTransfer/payment/case_03/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodBankTransferBACSPaymentMethodForTest();
        paymentMethod.setCcRefTxnPnrefID("cus_Oj3UpqsNzuqE29");
        paymentMethod.setSecondTokenId("pi_3MvJQpHo3zohLKRe1nK4BOTd");
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("2.22"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        // pass RadarSessionId via gateway options
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "RadarSessionId", "rse_1N7fvKSDx60UgxZnDB5Hb4v5");

        // Sale
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_paymentintent")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_03_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("pi_3NvwQy4ZWiZesCzm1wjH1fWW", responseMap.get("GatewayReferenceId"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(9)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_paymentintent"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_types[]", "bacs_debit"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467"),
                                        Matchers.hasEntry("customer", "cus_Oj3UpqsNzuqE29"),
                                        Matchers.hasEntry("payment_method", "pi_3MvJQpHo3zohLKRe1nK4BOTd")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_04_ItShouldSucceedForBacsNormalPaymentWithoutTokens() throws Exception {
        final String case_04_response = "/com/zuora/opg/test/json/stripe_2/bankTransfer/payment/case_04/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodBankTransferBACSPaymentMethodForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("2.22"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        // pass RadarSessionId via gateway options
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "RadarSessionId", "rse_1N7fvKSDx60UgxZnDB5Hb4v5");

        // Sale
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_paymentintent")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "400")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_04_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertEquals("400", responseMap.get("GatewayResponseCode"));
        assertEquals("[invalid_request_error/parameter_invalid_empty] You passed an empty string for 'payment_method'. We assume empty "
                + "values are an attempt to unset a parameter; however 'payment_method' cannot be unset. "
                + "You should remove 'payment_method' from your request or supply a non-empty value.", responseMap.get("GatewayResponseMessage"));
        assertEquals("", responseMap.get("GatewayReferenceId"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(9)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_paymentintent"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_types[]", "bacs_debit"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467"),
                                        Matchers.hasEntry("customer", ""),
                                        Matchers.hasEntry("payment_method", "")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_05_ItShouldSendStatementDescriptor() throws Exception {
        final String approved_response = "/com/zuora/opg/test/json/stripe_2/bankTransfer/payment/case_01/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodBankTransferPaymentMethodForTest();
        final Payment payment = PaymentBuilder.aPayment()
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withSoftDescriptor("test soft descriptor")
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);

        // Sale
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(approved_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("pi_3MvJQpHo3zohLKRe1nK4BOTd", responseMap.get("GatewayReferenceId"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(9)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("statement_descriptor", "test soft descriptor")
                                ),
                                "check request payload")
                        .build());
    }
}
