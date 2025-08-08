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

public class StripeV2AchPaymentUnitTest extends OpgJsonBaseTest {
    private static final ZUtility zUtility = new ZUtility();

    @Test
    public void case_01_ItShouldSucceedForNormalPayment_withoutIP() {
        final String case_01_get_customer_status_response = "/com/zuora/opg/test/json/stripe_2/ach/payment/case_01/get_customer_status_response.json";
        final String case_01_get_bank_account_response = "/com/zuora/opg/test/json/stripe_2/ach/payment/case_01/get_bank_account_response.json";
        final String case_01_payment_response = "/com/zuora/opg/test/json/stripe_2/ach/payment/case_01/payment_response.json";
        final String tokenId = "ba_1MAcjnHo3zohLKRe4Quaahlk"; // default_source in the get_customer_status_response.json
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAchForTest();
        paymentMethod.setCcRefTxnPnrefID(tokenId);
        paymentMethod.setIpAddress("192.168.0.1");

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

        // transaction: GetCustomerStatusById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe"),
                        Matchers.hasEntry("METHOD", "GET"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_get_customer_status_response))
                        .build()
        );

        // transaction: GetBankAccountById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe/sources/" + tokenId),
                        Matchers.hasEntry("METHOD", "GET"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_get_bank_account_response))
                        .build()
        );


        // transaction: CreateAndConfirmPaymentIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_payment_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("py_3MvJQpHo3zohLKRe1d5xR8eZ", responseMap.get("GatewayReferenceId"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe/sources/" + tokenId), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(9)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_types[]", "ach_debit"),
                                        Matchers.hasEntry("source", tokenId),
                                        Matchers.hasEntry("customer", "tk_9321123ajafe"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467"),
                                        Matchers.not(Matchers.hasEntry("payment_method_data[ip]", "192.168.0.1"))
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_02_ReturnUnknownZuoraResponseCodeWhenGatewayReturns5XXHttpStatus() {
        final String case_02_get_customer_status_response = "/com/zuora/opg/test/json/stripe_2/ach/payment/case_02/get_customer_status_response.json";
        final String case_02_get_bank_account_response = "/com/zuora/opg/test/json/stripe_2/ach/payment/case_02/get_bank_account_response.json";
        final String case_02_payment_response = "/com/zuora/opg/test/json/stripe_2/ach/payment/case_02/payment_response.json";
        final String tokenId = "ba_1MAcjnHo3zohLKRe4Quaahlk"; // default_source in the get_customer_status_response.json
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAchForTest();
        paymentMethod.setCcRefTxnPnrefID(tokenId);
        paymentMethod.setIpAddress("192.168.0.1");

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

        // transaction: GetCustomerStatusById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe"),
                        Matchers.hasEntry("METHOD", "GET"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_get_customer_status_response))
                        .build()
        );

        // transaction: GetBankAccountById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe/sources/" + tokenId),
                        Matchers.hasEntry("METHOD", "GET"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_get_bank_account_response))
                        .build()
        );


        // transaction: CreateAndConfirmPaymentIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3")
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
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe/sources/" + tokenId), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(9)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_types[]", "ach_debit"),
                                        Matchers.hasEntry("source", tokenId),
                                        Matchers.hasEntry("customer", "tk_9321123ajafe"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467"),
                                        Matchers.not(Matchers.hasEntry("payment_method_data[ip]", "192.168.0.1"))
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_03_ItShouldSucceedForNormalPaymentWhenOldTokensFormat() {
        final String case_03_get_customer_status_response = "/com/zuora/opg/test/json/stripe_2/ach/payment/case_03/get_customer_status_response.json";
        final String case_03_get_bank_account_response = "/com/zuora/opg/test/json/stripe_2/ach/payment/case_03/get_bank_account_response.json";
        final String case_03_payment_response = "/com/zuora/opg/test/json/stripe_2/ach/payment/case_03/payment_response.json";
        final String tokenId = "ba_1MAcjnHo3zohLKRe4Quaahlk"; // default_source in the get_customer_status_response.json
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAchForTest();
        paymentMethod.setCcRefTxnPnrefID(tokenId);
        paymentMethod.setIpAddress("192.168.0.1");

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

        // transaction: GetCustomerStatusById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe"),
                        Matchers.hasEntry("METHOD", "GET"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_03_get_customer_status_response))
                        .build()
        );

        // transaction: GetBankAccountById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe/sources/" + tokenId),
                        Matchers.hasEntry("METHOD", "GET"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_03_get_bank_account_response))
                        .build()
        );


        // transaction: CreateAndConfirmPaymentIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_03_payment_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("py_3MvJQpHo3zohLKRe1d5xR8eZ", responseMap.get("GatewayReferenceId"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe/sources/" + tokenId), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(9)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_types[]", "ach_debit"),
                                        Matchers.hasEntry("source", tokenId),
                                        Matchers.hasEntry("customer", "tk_9321123ajafe"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467"),
                                        Matchers.not(Matchers.hasEntry("payment_method_data[ip]", "192.168.0.1"))
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_04_ItShouldSucceedForNormalPaymentWhenNewTokensFormatAndFeatureEnabled() {
        final String case_04_payment_response = "/com/zuora/opg/test/json/stripe_2/ach/payment/case_04/payment_response.json";
        final String tokenId = "pm_1Ovbiv4ZWiZesCzmKYzBYktN";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "Feature-ACHTokenizationSupport-Enabled=true;")
                        .build()));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAchForTest();
        paymentMethod.setCcRefTxnPnrefID(tokenId);
        paymentMethod.setSecondTokenId("cus_Pl7IhB2ujMBtNs");

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

        // transaction: CreateAndConfirmPaymentIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_04_payment_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("py_3Ovbiv4ZWiZesCzm070Bc0Oq", responseMap.get("GatewayReferenceId"));

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
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_types[]", "us_bank_account"),
                                        Matchers.hasEntry("payment_method", tokenId),
                                        Matchers.hasEntry("customer", "cus_Pl7IhB2ujMBtNs")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_05_ItShouldSendSoftDescriptorInPaymentIntent() {
        final String get_customer_status_response = "/com/zuora/opg/test/json/stripe_2/ach/payment/case_01/get_customer_status_response.json";
        final String get_bank_account_response = "/com/zuora/opg/test/json/stripe_2/ach/payment/case_01/get_bank_account_response.json";
        final String payment_response = "/com/zuora/opg/test/json/stripe_2/ach/payment/case_01/payment_response.json";
        final String tokenId = "ba_1MAcjnHo3zohLKRe4Quaahlk"; // default_source in the get_customer_status_response.json
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false, StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAchForTest();
        paymentMethod.setCcRefTxnPnrefID(tokenId);

        final Payment payment = PaymentBuilder.aPayment()
                .withPaymentMethod(paymentMethod)
                .withSoftDescriptor("test soft descriptor")
                .build();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);

        // transaction: GetCustomerStatusById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(get_customer_status_response))
                        .build()
        );

        // transaction: GetBankAccountById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe/sources/" + tokenId)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(get_bank_account_response))
                        .build()
        );


        // transaction: CreateAndConfirmPaymentIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(payment_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("py_3MvJQpHo3zohLKRe1d5xR8eZ", responseMap.get("GatewayReferenceId"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe"), "Check URL")
                        .build(),
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/tk_9321123ajafe/sources/" + tokenId), "Check URL")
                        .build(),
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(9)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("statement_descriptor", "test soft descriptor")
                                ),
                                "check request payload")
                        .build());
    }
}
