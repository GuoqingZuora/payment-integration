package com.zuora.opg.test.json.stripe.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Map;

public class StripeV2GooglePayCreditCardPaymentUnitTest extends OpgJsonBaseTest {
    private static final ZUtility zUtility = new ZUtility();

    @Test
    public void case_01_ItShouldSucceedForGooglePayRecurringPaymentForVisa() throws Exception {
        final String payment_response = "/com/zuora/opg/test/json/stripe_2/googlepaycreditcard/payment/case_01/payment_response.json";
        String networkTransactionId = "695148101731176";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with Visa Google Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodVisaGooglePayCreditCardForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("4"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        OpgRequestMapHelper.addMITParameterIntoRequestMap(requestMap,
                "Recurring", "Merchant", "GoogleVisa", networkTransactionId);
        requestMap.put("CreditCardType", "GoogleVisa");

        // Sale
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(payment_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3NNABlGT0RtLbTJ22d6tyR9M", responseMap.get("GatewayReferenceId"));
        assertEquals(networkTransactionId, responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));

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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "1111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "google_pay_dpan"),
                                        Matchers.hasEntry("payment_method_options[card][mit_exemption][network_transaction_id]", networkTransactionId),
                                        Matchers.hasEntry("off_session", "true"),
                                        Matchers.hasEntry("amount", "400"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_02_ItShouldSucceedForGooglePayRecurringPaymentWithL3Data() throws Exception {
        final String payment_response = "/com/zuora/opg/test/json/stripe_2/googlepaycreditcard/payment/case_02/payment_response_withL3.json";
        String networkTransactionId = "901151114810210";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodVisaGooglePayCreditCardForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("4"))
                .withPaymentNumber("P-00000017")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("EnableL3", "on")
                                .put("L3Downgrade", "on")
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_ITEM_LIST, "TestNameValueList");
        OpgRequestMapHelper.addMITParameterIntoRequestMap(requestMap,
                "Recurring", "Merchant", "GoogleVisa", networkTransactionId);
        requestMap.put("CreditCardType", "GoogleVisa");

        // Sale
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(payment_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3NFaYhI3dKT3t0iK1wqHhoKl", responseMap.get("GatewayReferenceId"));
        assertEquals("901151114810210", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));

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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "1111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "google_pay_dpan"),
                                        Matchers.hasEntry("payment_method_options[card][mit_exemption][network_transaction_id]", "901151114810210"),
                                        Matchers.hasEntry("off_session", "true"),
                                        Matchers.hasEntry("amount", "400"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000017"),
                                        // Sample Level3 data
                                        Matchers.hasEntry("level3[merchant_reference]", "P-00000017"),
                                        Matchers.hasEntry("level3[customer_reference]", "A-99999988")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_03_ItShouldSucceed_WithCreateAndConfirmPaymentIntentWithoutL3TransactionFlow() throws Exception {
        final String case_03_createAndConfirmPaymentIntent_error_response = "/com/zuora/opg/test/json/stripe_2/googlepaycreditcard/payment/case_03/payment_create&confirmpaymentintent_l3param_error_Gpay_response.json";
        final String case_03_createAndConfirmPaymentIntentWithoutL3_response = "/com/zuora/opg/test/json/stripe_2/googlepaycreditcard/payment/case_03/payment_create&confirmpaymentintent_without_l3_Gpay_response.json";
        String networkTransactionId = "901151114810210";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodVisaGooglePayCreditCardForTest();

        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("4"))
                .withPaymentNumber("P-00000017")
                .withPaymentMethod(paymentMethod)
                .withInvoiceNumber("INV00000008")
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("EnableL3", "on")
                                .put("L3Downgrade", "on")
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_ITEM_LIST, "TestNameValueList");
        OpgRequestMapHelper.addMITParameterIntoRequestMap(requestMap,
                "Recurring", "Merchant", "GoogleVisa", networkTransactionId);
        requestMap.put("CreditCardType", "GoogleVisa");

        requestMap.put("PaymentNumber", "P-00000017");

        // transaction -> CreateAndConfirmPaymentIntent
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
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "400")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_03_createAndConfirmPaymentIntent_error_response))
                        .build()
        );

        // transaction -> CreateAndConfirmPaymentIntentWithoutL3
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withoutl3")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_03_createAndConfirmPaymentIntentWithoutL3_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("[L3 Data Downgraded] Approved", responseMap.get("GatewayResponseMessage")); // this indicates L2/L3 data downgrade flow
        assertEquals("ch_3NFakcI3dKT3t0iK1VpRaYSb", responseMap.get("GatewayReferenceId"));
        assertEquals("901151114810210", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("pi_3NFakcI3dKT3t0iK1vssbeQG", responseMap.get("GatewaySecondReferenceId"));

        verifyConnectorRequest(httpsConnector,
                // transaction -> CreateAndConfirmPaymentIntent
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "1111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "google_pay_dpan"),
                                        Matchers.hasEntry("payment_method_options[card][mit_exemption][network_transaction_id]", "901151114810210"),
                                        Matchers.hasEntry("off_session", "true"),
                                        Matchers.hasEntry("amount", "400"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000017"),
                                        // Sample Level3 data
                                        Matchers.hasEntry("level3[merchant_reference]", "P-00000017"),
                                        Matchers.hasEntry("level3[customer_reference]", "A-99999988")
                                ),
                                "check transaction CreateAndConfirmPaymentIntent request payload")
                        .build(),
                // transaction -> CreateAndConfirmPaymentIntentWithoutL3
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(9)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withoutl3"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "1111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "google_pay_dpan"),
                                        Matchers.hasEntry("payment_method_options[card][mit_exemption][network_transaction_id]", "901151114810210"),
                                        Matchers.hasEntry("off_session", "true"),
                                        Matchers.hasEntry("amount", "400"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000017")
                                ),
                                "check CreateAndConfirmPaymentIntentWithoutL3 transaction request payload")
                        .build());
    }

    @Test
    public void case_04_ItShouldSucceedForGooglePayRecurringPaymentWithoutNTI() throws Exception {
        final String payment_response = "/com/zuora/opg/test/json/stripe_2/googlepaycreditcard/payment/case_04/payment_response_case4.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodVisaGooglePayCreditCardForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("4"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("CreditCardType", "GoogleVisa");

        // Sale
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(payment_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3NNCgEGT0RtLbTJ227DVFLL4", responseMap.get("GatewayReferenceId"));
        assertEquals("695148101731176", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));

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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "1111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "google_pay_dpan"),
                                        Matchers.hasEntry("off_session", "true"),
                                        Matchers.hasEntry("amount", "400"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_05_ItShouldSucceedForGooglePayRecurringPaymentForMasterCard() throws Exception {
        final String payment_response = "/com/zuora/opg/test/json/stripe_2/googlepaycreditcard/payment/case_05/payment_response_mastercard.json";
        String networkTransactionId = "MCCIYQ1MK0626";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with MasterCard Google Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodMasterCardGooglePayCreditCardForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("4"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("CreditCardType", "GoogleMasterCard");
        OpgRequestMapHelper.addMITParameterIntoRequestMap(requestMap,
                "Recurring", "Merchant", "GoogleMasterCard", networkTransactionId);

        // Sale
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(payment_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3NNCkbGT0RtLbTJ22C4ojnE5", responseMap.get("GatewayReferenceId"));
        assertEquals(networkTransactionId, responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));

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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "4444"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "5555555555554444"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "google_pay_dpan"),
                                        Matchers.hasEntry("payment_method_options[card][mit_exemption][network_transaction_id]", networkTransactionId),
                                        Matchers.hasEntry("off_session", "true"),
                                        Matchers.hasEntry("amount", "400"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_06_ItShouldSucceedForGooglePayRecurringPaymentForAmex() throws Exception {
        final String payment_response = "/com/zuora/opg/test/json/stripe_2/googlepaycreditcard/payment/case_06/payment_response_amex.json";
        String networkTransactionId = "656850841024977";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        //Test with American Express Google Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAmexGooglePayCreditCardForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("4"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("CreditCardType", "GoogleAmericanExpress");
        OpgRequestMapHelper.addMITParameterIntoRequestMap(requestMap,
                "Recurring", "Merchant", "GoogleAmericanExpress", networkTransactionId);

        // Sale
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(payment_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3NNCrQGT0RtLbTJ23YJqLQam", responseMap.get("GatewayReferenceId"));
        assertEquals(networkTransactionId, responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));

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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "0005"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "378282246310005"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "google_pay_dpan"),
                                        Matchers.hasEntry("payment_method_options[card][mit_exemption][network_transaction_id]", networkTransactionId),
                                        Matchers.hasEntry("off_session", "true"),
                                        Matchers.hasEntry("amount", "400"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_07_ItShouldSucceedForGooglePayRecurringPaymentForDiscover() throws Exception {
        final String payment_response = "/com/zuora/opg/test/json/stripe_2/googlepaycreditcard/payment/case_07/payment_response_discover.json";
        String networkTransactionId = "577254731141131";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with Discover Google Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodDiscoverGooglePayCreditCardForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("4"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("CreditCardType", "GoogleDiscover");
        OpgRequestMapHelper.addMITParameterIntoRequestMap(requestMap,
                "Recurring", "Merchant", "GoogleDiscover", networkTransactionId);

        // Sale
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(payment_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3NNCzbGT0RtLbTJ21BYyxRbK", responseMap.get("GatewayReferenceId"));
        assertEquals(networkTransactionId, responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));

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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "1117"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "6011111111111117"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "google_pay_dpan"),
                                        Matchers.hasEntry("payment_method_options[card][mit_exemption][network_transaction_id]", networkTransactionId),
                                        Matchers.hasEntry("off_session", "true"),
                                        Matchers.hasEntry("amount", "400"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_08_ItShouldSucceedForGooglePayRecurringPaymentForJCB() throws Exception {
        final String payment_response = "/com/zuora/opg/test/json/stripe_2/googlepaycreditcard/payment/case_08/payment_response_jcb.json";
        String networkTransactionId = "507780868675715";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with JCB Google Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodJCBGooglePayCreditCardForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("4"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("CreditCardType", "GoogleJCB");
        OpgRequestMapHelper.addMITParameterIntoRequestMap(requestMap,
                "Recurring", "Merchant", "GoogleJCB", networkTransactionId);

        // Sale
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(payment_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3NND1wGT0RtLbTJ21FWq2o7r", responseMap.get("GatewayReferenceId"));
        assertEquals(networkTransactionId, responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));

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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "0505"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "3566002020360505"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "google_pay_dpan"),
                                        Matchers.hasEntry("payment_method_options[card][mit_exemption][network_transaction_id]", networkTransactionId),
                                        Matchers.hasEntry("off_session", "true"),
                                        Matchers.hasEntry("amount", "400"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467")
                                ),
                                "check request payload")
                        .build());
    }
}
