package com.zuora.opg.test.json.stripe.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.zuora.base.Decimal;
import com.zuora.billing.opg.test.common.OpgJsonBaseTest;
import com.zuora.billing.opg.test.json.stripe.StripeRequestPayloadExtractor;
import com.zuora.billing.opg.test.json.stripe.StripeTestHelper;
import com.zuora.billing.opg.test.support.common.BillingAccountBuilder;
import com.zuora.billing.opg.test.support.common.CurrencyBuilder;
import com.zuora.billing.opg.test.support.common.HashMapBuilder;
import com.zuora.billing.opg.test.support.common.PaymentBuilder;
import com.zuora.billing.opg.test.support.common.PaymentGatewayBuilder;
import com.zuora.billing.opg.test.support.engine.OpgRequestMapHelper;
import com.zuora.billing.opg.test.verify.ConnectorHeadersVerifierBuilder;
import com.zuora.zbilling.account.model.BillingAccount;
import com.zuora.zbilling.payment.model.Payment;
import com.zuora.zbilling.paymentgateway.model.PaymentGatewaySimpleType;
import com.zuora.zbilling.paymentmethod.model.PaymentMethod;
import com.zuora.zbilling.setting.gateway.model.PaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.OpenPaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.connector.HttpConnectorCommonUtil;
import com.zuora.zpayment.openpaymentgateway.engine.constants.OpenPaymentGatewayConstants;
import com.zuora.zpayment.openpaymentgateway.engine.templateengine.ZUtility;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StripeV2CreditCardPaymentUnitTest extends OpgJsonBaseTest {
    private static final ZUtility zUtility = new ZUtility();

    @Test
    public void case_01_ItShouldSucceedForNormalRecurringPayment() throws Exception {
        final String case_01_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_01/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
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
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3")
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
        assertEquals("ch_3MojCtGT0RtLbTJ23BbZ3kcJ", responseMap.get("GatewayReferenceId"));
        assertEquals("695148101731176", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("pi_3MojCtGT0RtLbTJ23ymvvxiL", responseMap.get("GatewaySecondReferenceId"));

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
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5") // verify RadarSessionId
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_02_ItShouldSucceedForMotoPayment() throws Exception {
        final String case_02_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_02/payment_response_for_moto.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("2.26"))
                .withPaymentNumber("P-00031423")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);

        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put(OpenPaymentGatewayConstants.REQUEST_ZUORA_ECOMMERCE_INDICATOR, OpenPaymentGatewayConstants.ECOMMERCE_INDICATOR_MOTO);
        requestMap.put(OpenPaymentGatewayConstants.REQUEST_MIT_EXTRACT_SCP, OpenPaymentGatewayConstants.FLAG_YES);

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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3MonYqGT0RtLbTJ21lqGh4Pa", responseMap.get("GatewayReferenceId"));
        assertEquals("695148101731176", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("pi_3MonYqGT0RtLbTJ21YSOxVgu", responseMap.get("GatewaySecondReferenceId"));


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
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("payment_method_options[card][moto]", "true"),
                                        Matchers.hasEntry("amount", "226"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031423")
                                ),
                                "check request payload")
                        .build());
    }

    /*
          In order to execute 'CreateAndConfirmPaymentIntentWithoutL3' transaction, need to follow the below intermediate transactions (in order):
                  'CheckExistingPayment' ---ERROR---> 'CreateAndConfirmPaymentIntent'
                  ---ERROR(due to L3 params)---> 'CheckL3Error' ---SUCCESS---> 'CreateAndConfirmPaymentIntentWithoutL3'
     */
    @Test
    public void case_03_ItShouldSucceed_WithCreateAndConfirmPaymentIntentWithoutL3TransactionFlow() throws Exception {
        final String case_03_createAndConfirmPaymentIntent_error_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_03/payment_create&confirmpaymentintent_l3param_error_response.json";
        final String case_03_createAndConfirmPaymentIntentWithoutL3_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_03/payment_create&confirmpaymentintent_without_l3_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardWithGatewayOptionsForTest();

        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("40"))
                .withPaymentNumber("P-00000017")
                .withPaymentMethod(paymentMethod)
                .withInvoiceNumber("INV00000008")
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
                                .put("EnableL3", "on")
                                .put("L3Downgrade", "on")
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_ITEM_LIST, "TestNameValueList");

        // pass RadarSessionId via gateway options
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "RadarSessionId", "rse_1N9KQO4ZWiZesCzm3BqRc4ti");

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
        assertEquals("ch_3N9KSP4ZWiZesCzm1J1OsQmn", responseMap.get("GatewayReferenceId"));
        assertEquals("121819797114103", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("pi_3N9KSP4ZWiZesCzm1O45jRT0", responseMap.get("GatewaySecondReferenceId"));

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
                                        Matchers.hasEntry("payment_method_data[card][number]", "4242424242424242"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2028"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "5"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "Fremont"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "CA"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        // Sample Level3 data
                                        Matchers.hasEntry("level3[merchant_reference]", "P-00000017"),
                                        Matchers.hasEntry("level3[customer_reference]", "A-99999988"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "4000"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000017"),

                                        Matchers.hasEntry("radar_options[session]", "rse_1N9KQO4ZWiZesCzm3BqRc4ti") // verify RadarSessionId
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
                                        Matchers.hasEntry("payment_method_data[card][number]", "4242424242424242"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2028"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "5"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "Fremont"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "CA"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),

                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "4000"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000017"),

                                        Matchers.hasEntry("radar_options[session]", "rse_1N9KQO4ZWiZesCzm3BqRc4ti"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all") // verify skip_rules
                                ),
                                "check CreateAndConfirmPaymentIntentWithoutL3 transaction request payload")
                        .build());
    }

    @Test
    public void case_04_ItShouldSucceedForNormalRecurringPaymentwithMITfields() throws Exception {
        final String case_04_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_04/payment_response_case4.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        String networkTransactionId = "901151114810210";
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
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
        OpgRequestMapHelper.addMITParameterIntoRequestMap(requestMap,
                "Recurring", "Merchant", paymentMethod.getCreditCardType().name(), networkTransactionId);


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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_04_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3MojCtGT0RtLbTJ23BbZ3kcJ", responseMap.get("GatewayReferenceId"));
        assertEquals("695148101731176", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("pi_3MojCtGT0RtLbTJ23ymvvxiL", responseMap.get("GatewaySecondReferenceId"));

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
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("payment_method_options[card][mit_exemption][network_transaction_id]", networkTransactionId),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_05_ItShouldSucceedForNormalRecurringPayment_WhenStripeRadarSkipRulesParamIsPassed() throws Exception {
        final String case_05_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_05/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = PaymentGatewayBuilder.aPaymentGateway()
                .withId("pgw#2643251431")
                .withTenantId("68672831")
                .withPaymentGatewayType(new PaymentGatewaySimpleType("Stripe", "2"))
                .withGatewayName("Stripe Json Test")
                .withAuthDefaultAmount(Decimal.valueOf("1"))
                .withIsTest(true)
                .build();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
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
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules' setting
                                .build()));

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
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_05_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3NTLgqI3dKT3t0iK1uU8CD4V", responseMap.get("GatewayReferenceId"));
        assertEquals("651031155273537", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("pi_3NTLgqI3dKT3t0iK1jEzh4C2", responseMap.get("GatewaySecondReferenceId"));

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
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467"),
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all") // verify skip_rules
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_06_ItShouldSucceed_WithCreateAndConfirmPaymentIntentWithL3AndAccountNumberTrimmedWithInvoiceListCountLessThan200() throws Exception {
        final String case_06_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_06/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();

        //Account number length greater than 25
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A00000007A00000007A00000007")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("40"))
                .withPaymentNumber("P-00000017")
                .withPaymentMethod(paymentMethod)
                .withInvoiceNumber("INV00000008")
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
                                .put("EnableL3", "on")
                                .put("L3Downgrade", "on")
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_ITEM_LIST, "TestNameValueList");

        // pass RadarSessionId via gateway options
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "RadarSessionId", "rse_1N9KQO4ZWiZesCzm3BqRc4ti");

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
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_06_response))
                        .build()
        );

        //Invoice item list count 1(less than 200)
        List<Map<String, String>> nameValuePairsList = new ArrayList<>();
        nameValuePairsList.add(StripeTestHelper.buildInvoiceListItem());

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway, nameValuePairsList);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3NTLgqI3dKT3t0iK1uU8CD4V", responseMap.get("GatewayReferenceId"));
        assertEquals("651031155273537", responseMap.get("MITReceivedTXID"));
        assertEquals("pi_3NTLgqI3dKT3t0iK1jEzh4C2", responseMap.get("GatewaySecondReferenceId"));

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
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "4000"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000017"),
                                        Matchers.hasEntry("level3[customer_reference]", "A00000007A00000007A000000") // Trimmed account number
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_07_ItShouldSucceed_WithCreateAndConfirmPaymentIntentWithL3AndAccountNumberTrimmedWithInvoiceListCountMoreThan200() throws Exception {
        final String case_07_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_07/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();

        //Account number length greater than 25
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A00000007A00000007A00000007")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("40"))
                .withPaymentNumber("P-00000017")
                .withPaymentMethod(paymentMethod)
                .withInvoiceNumber("INV00000008")
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
                                .put("EnableL3", "on")
                                .put("L3Downgrade", "on")
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_ITEM_LIST, "TestNameValueList");

        // pass RadarSessionId via gateway options
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "RadarSessionId", "rse_1N9KQO4ZWiZesCzm3BqRc4ti");

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
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_07_response))
                        .build()
        );

        //Invoice item list count 205(greater than 200)
        List<Map<String, String>> nameValuePairsList = new ArrayList<>();
        int i = 1;
        while(i <= 205){
            nameValuePairsList.add(StripeTestHelper.buildInvoiceListItem());
            i++;
        }

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway, nameValuePairsList);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3NTLgqI3dKT3t0iK1uU8CD4V", responseMap.get("GatewayReferenceId"));
        assertEquals("651031155273537", responseMap.get("MITReceivedTXID"));
        assertEquals("pi_3NTLgqI3dKT3t0iK1jEzh4C2", responseMap.get("GatewaySecondReferenceId"));

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
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "4000"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000017"),
                                        Matchers.hasEntry("level3[customer_reference]", "A00000007A00000007A000000") // Trimmed account number
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_08_NormalRecurringPaymentReturnUnknownZuoraResponseCodeWhenGatewayReturns5XXHttpStatus() throws Exception {
        final String case_08_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_08/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
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
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "529")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_08_response))
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
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5") // verify RadarSessionId
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_09_CreateAndConfirmPaymentIntentWithoutL3ReturnUnknownZuoraResponseCodeWhenGatewayReturns5XXHttpStatus() throws Exception {
        final String case_09_createAndConfirmPaymentIntent_error_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_09/l3param_error_response.json";
        final String case_09_createAndConfirmPaymentIntentWithoutL3_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_09/paymentintent_without_l3_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardWithGatewayOptionsForTest();

        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("40"))
                .withPaymentNumber("P-00000017")
                .withPaymentMethod(paymentMethod)
                .withInvoiceNumber("INV00000008")
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
                                .put("EnableL3", "on")
                                .put("L3Downgrade", "on")
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_ITEM_LIST, "TestNameValueList");

        // pass RadarSessionId via gateway options
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "RadarSessionId", "rse_1N9KQO4ZWiZesCzm3BqRc4ti");

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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_09_createAndConfirmPaymentIntent_error_response))
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
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "529")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_09_createAndConfirmPaymentIntentWithoutL3_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Unknown", responseMap.get("ZuoraResponseCode"));
        assertEquals("529", responseMap.get("GatewayResponseCode"));
        assertEquals("529", responseMap.get("lastHttpStatusCode"));
        assertNotNull(responseMap.get("GatewayRequestString"));
        assertNotNull(responseMap.get("GatewayResponseString"));

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
                                        Matchers.hasEntry("payment_method_data[card][number]", "4242424242424242"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2028"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "5"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "Fremont"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "CA"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        // Sample Level3 data
                                        Matchers.hasEntry("level3[merchant_reference]", "P-00000017"),
                                        Matchers.hasEntry("level3[customer_reference]", "A-99999988"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "4000"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000017"),

                                        Matchers.hasEntry("radar_options[session]", "rse_1N9KQO4ZWiZesCzm3BqRc4ti") // verify RadarSessionId
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
                                        Matchers.hasEntry("payment_method_data[card][number]", "4242424242424242"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2028"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "5"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "Fremont"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "CA"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),

                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "4000"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000017"),

                                        Matchers.hasEntry("radar_options[session]", "rse_1N9KQO4ZWiZesCzm3BqRc4ti"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all") // verify skip_rules
                                ),
                                "check CreateAndConfirmPaymentIntentWithoutL3 transaction request payload")
                        .build());
    }

    @Test
    public void case_10_NormalRecurringPaymentReturnFailedZuoraResponseCodeWhenGatewayReturns5XXHttpStatusWithErroCodeLengthGreaterThan3() throws Exception {
        final String case_10_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_10/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
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
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "5299")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_10_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertEquals("5299", responseMap.get("GatewayResponseCode"));
        assertEquals("5299", responseMap.get("lastHttpStatusCode"));
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
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5") // verify RadarSessionId
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_11_ItShouldSendMandateIdInRequestIfTheCondtionsAreSatisfiedAndSettingIsNotEnabled() throws Exception {
        final String case_10_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_10/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        paymentMethod.setMandateId("mandate_xxxxx");
        paymentMethod.setCcRefTxnPnrefID("pm_xxxxx");
        paymentMethod.setSecondTokenId("cus_xxxxx");
        paymentMethod.setMitGatewayToken1("");
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("Indian Rupee", "INR", "356"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("15000"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("PaymentSource", "PaymentRun");

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
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "5299")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_10_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertEquals("5299", responseMap.get("GatewayResponseCode"));
        assertEquals("5299", responseMap.get("lastHttpStatusCode"));
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
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("mandate", "mandate_xxxxx"),
                                        Matchers.hasEntry("payment_method", "pm_xxxxx"),
                                        Matchers.hasEntry("customer", "cus_xxxxx"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "1500000"),
                                        Matchers.hasEntry("currency", "INR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467") // verify RadarSessionId
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_12_ItShouldNotSendMandateIdInRequestIfTheCondtionsAreSatisfiedAndSettingIsEnabled() throws Exception {
        final String case_10_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_10/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "Feature-IgnoreMandateIdInPaymentRunAboveINR15K-Enabled=true;")
                        .build()));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        paymentMethod.setMandateId("mandate_xxxxx");
        paymentMethod.setCcRefTxnPnrefID("pm_xxxxx");
        paymentMethod.setSecondTokenId("cus_xxxxx");
        paymentMethod.setMitGatewayToken1("");
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("Indian Rupee", "INR", "356"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("15000"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("PaymentSource", "PaymentRun");

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
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "5299")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_10_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertEquals("5299", responseMap.get("GatewayResponseCode"));
        assertEquals("5299", responseMap.get("lastHttpStatusCode"));
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
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.not(Matchers.hasKey("mandate")),
                                        Matchers.hasEntry("payment_method", "pm_xxxxx"),
                                        Matchers.hasEntry("customer", "cus_xxxxx"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "1500000"),
                                        Matchers.hasEntry("currency", "INR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467") // verify RadarSessionId
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_13_ItShouldSucceedWithDescriptionInRequestWhenDescriptionIsSetInGatewayConfiguration() throws Exception {
        final String case_13_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_13/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
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
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("PaymentDescription", "Test Payment Description")
                                .build()));

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
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_13_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3MojCtGT0RtLbTJ23BbZ3kcJ", responseMap.get("GatewayReferenceId"));
        assertEquals("695148101731176", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("pi_3MojCtGT0RtLbTJ23ymvvxiL", responseMap.get("GatewaySecondReferenceId"));

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
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5"), // verify RadarSessionId
                                        Matchers.hasEntry("description", "Test Payment Description")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_14_ItShouldSucceedWithDescriptionNotInRequestWhenDescriptionIsBlankInGatewayConfiguration() throws Exception {
        final String case_14_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_14/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
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
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("PaymentDescription", "")
                                .build()));

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
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_14_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3MojCtGT0RtLbTJ23BbZ3kcJ", responseMap.get("GatewayReferenceId"));
        assertEquals("695148101731176", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("pi_3MojCtGT0RtLbTJ23ymvvxiL", responseMap.get("GatewaySecondReferenceId"));

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
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5") // verify RadarSessionId
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_15_ItShouldSucceedWithDescriptionNotInRequestWhenDescriptionIsSpecialCharactersInGatewayConfiguration() throws Exception {
        final String case_15_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_15/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "Feature-ShouldUpdateMetadataField-Enabled=true;Feature-CustomInvoiceField-Value=test_pokemon;")
                        .build()));

        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
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
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("PaymentDescription", "~!@#%^&*()_+-=[]{}|;:',.<>?/`~!@#%^&*()_+-=[]{}|;:',.<>?/`~!@#%^&*()_+-=[]{}|;:',.<>?/`~!@#%^&*()_+-=[]")
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + OpenPaymentGatewayConstants.INVOICE_NUM, "INV-00000002");

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
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_15_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3MojCtGT0RtLbTJ23BbZ3kcJ", responseMap.get("GatewayReferenceId"));
        assertEquals("695148101731176", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("pi_3MojCtGT0RtLbTJ23ymvvxiL", responseMap.get("GatewaySecondReferenceId"));

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
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467"),
                                        Matchers.hasEntry("metadata[test_pokemon]", "INV-00000002"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5"), // verify RadarSessionId
                                        Matchers.hasEntry("description", "~!@#%^&*()_+-=[]{}|;:',.<>?/`~!@#%^&*()_+-=[]{}|;:',.<>?/`~!@#%^&*()_+-=[]{}|;:',.<>?/`~!@#%^&*()_+-=[]")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_16_ItShouldFailWithDescriptionInRequestWhenDescriptionIsSetInGatewayConfigurationAndLengthGreaterThan1000() throws Exception {
        final String case_16_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_16/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
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

        String paymentDescription = "KT(]s!ig8}^TF5)bsI):EbdEp!#fGj_xWeqEXOKA}v@5_0k#oNd.bNmD#v!IZ1QFe8Mg]*R3Wi)1u4pn!" +
                "!Gu5({7Tk)+Gpqt@4$]Y$RfQnH3Y@ED{A-+w0~MDFeYw77!TWYf;R5P1Dj[1R|Z*P%g_mfMcLkxv[EsL%Xo97)(wEhU9L3&OC[XO-" +
                "wkDzYX.4|F_DgTYIqafMCn1*GV3OY8#RU@3o1Zl^L%M-cEdN8Qn!V|iA|]pRMhgb5jD]5.rUl}bdu85US`oY`$ArvDBm~7#B3aEN~D|" +
                "WVPTMcbXbLZ-x5X{k+U?]e.qK9?17ayEQB0}k|fbX!IjOD)&G5HYZT4jHq)v^5P&U~H#*N5cFo~}~06OhlbN1)Spzd6vZTSxW=|" +
                "OhjNP_P%k$Kz3wYdEE)UKLY4TZ]!S7gY94%EmCVB+a;_Hf3g4.yk;=fh-U@t]N28oV!OaelK6t!Pr.dJZ!*Vmbre#EZSB{x}a+4" +
                "WDVGTzU6mnsA-t*bZm17yDdY6O3U1Js|F]3`PSHxrJ]5Z8{1QtGp8a@pl8zO0aErZ5jFStjI~kT5A_rUJ-!a#KD=Xz@jWbOQKv8]3~" +
                "czoT4o4KBX8f{ft0&Y#_9btL[=e[@M{htg~CLlZ3jeP#3V_&{`1K89J1yE=Z&NwVE#?IMnlO.joU(MTDP*V7#-jNz5WXxTsIEb%NeO" +
                "*MRc5O2E@6gZAs{^jZn0}D8BD30C&U>SYD&(3d)~3T]SII83k}bMQUWf)bIqL|DCAqSyVQ-9tkmczme1hUu&vUJf7NG6~r2YIa?WZ" +
                "AZy_^y%ZK{F$T50I#A-NjW07[4t}%2n#&?D)LMM6Ocrg^*~HqHg.xIz_-o|XB$!)3CHH9~t^?z$@WJukA{-h#U6!JMr44d~TR`ztqz" +
                "Sz;jG7gqL;R-=1K8U]x[P7ZToVXj[W@`Jz]y?YeS2L(2SB)d0J;hSBJX$ln*FP;FO1VNoK0}V}+dLj{E$P9Ag*{9tdmrObR7w1XYVe~{rp}p]-a";

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("PaymentDescription", paymentDescription)
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        // pass RadarSessionId via gateway options
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "RadarSessionId", "rse_1N7fvKSDx60UgxZnDB5Hb4v5");

        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + OpenPaymentGatewayConstants.INVOICE_NUM, "INV-00000002");

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
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "400")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_16_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertEquals("400", responseMap.get("GatewayResponseCode"));
        assertEquals("400", responseMap.get("lastHttpStatusCode"));
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
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "222"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.not(Matchers.hasKey("metadata[invoice_number]")),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5"), // verify RadarSessionId
                                        Matchers.hasEntry("description", paymentDescription)
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_17_ItShouldSucceed_WithCreateAndConfirmPaymentIntentWithoutL3TransactionFlowWithPaymentDescription() throws Exception {
        final String case_17_createAndConfirmPaymentIntent_error_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_17/payment_create&confirmpaymentintent_l3param_error_response.json";
        final String case_17_createAndConfirmPaymentIntentWithoutL3_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_17/payment_create&confirmpaymentintent_without_l3_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "Feature-ShouldUpdateMetadataField-Enabled=true;")
                        .build()));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardWithGatewayOptionsForTest();

        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("40"))
                .withPaymentNumber("P-00000017")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
                                .put("EnableL3", "on")
                                .put("L3Downgrade", "on")
                                .put("PaymentDescription", "Test Payment Description")
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + OpenPaymentGatewayConstants.INVOICE_NUM, "INV-00000002");

        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_ITEM_LIST, "TestNameValueList");

        // pass RadarSessionId via gateway options
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "RadarSessionId", "rse_1N9KQO4ZWiZesCzm3BqRc4ti");

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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_17_createAndConfirmPaymentIntent_error_response))
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_17_createAndConfirmPaymentIntentWithoutL3_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("[L3 Data Downgraded] Approved", responseMap.get("GatewayResponseMessage")); // this indicates L2/L3 data downgrade flow
        assertEquals("ch_3N9KSP4ZWiZesCzm1J1OsQmn", responseMap.get("GatewayReferenceId"));
        assertEquals("121819797114103", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("pi_3N9KSP4ZWiZesCzm1O45jRT0", responseMap.get("GatewaySecondReferenceId"));

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
                                        Matchers.hasEntry("payment_method_data[card][number]", "4242424242424242"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2028"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "5"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "Fremont"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "CA"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        // Sample Level3 data
                                        Matchers.hasEntry("level3[merchant_reference]", "P-00000017"),
                                        Matchers.hasEntry("level3[customer_reference]", "A-99999988"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "4000"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000017"),
                                        Matchers.hasEntry("metadata[invoice_number]", "INV-00000002"),
                                        Matchers.hasEntry("description", "Test Payment Description"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N9KQO4ZWiZesCzm3BqRc4ti") // verify RadarSessionId
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
                                        Matchers.hasEntry("payment_method_data[card][number]", "4242424242424242"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2028"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "5"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "Fremont"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "CA"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),

                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "4000"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000017"),
                                        Matchers.hasEntry("metadata[invoice_number]", "INV-00000002"),
                                        Matchers.hasEntry("description", "Test Payment Description"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N9KQO4ZWiZesCzm3BqRc4ti"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all") // verify skip_rules
                                ),
                                "check CreateAndConfirmPaymentIntentWithoutL3 transaction request payload")
                        .build());
    }

    @Test
    public void case_18_ItShouldSendSoftDescriptor() throws Exception {
        final String approved_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_01/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("2.22"))
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
        assertEquals("ch_3MojCtGT0RtLbTJ23BbZ3kcJ", responseMap.get("GatewayReferenceId"));
        assertEquals("695148101731176", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("pi_3MojCtGT0RtLbTJ23ymvvxiL", responseMap.get("GatewaySecondReferenceId"));

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
                                        Matchers.hasEntry("statement_descriptor_suffix", "test soft descriptor")
                                ),
                                "check request payload")
                        .build());
    }

}
