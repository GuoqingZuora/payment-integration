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
import com.zuora.billing.opg.test.support.common.PaymentMethodBuilder;
import com.zuora.billing.opg.test.support.engine.OpgRequestMapHelper;
import com.zuora.billing.opg.test.verify.ConnectorHeadersVerifierBuilder;
import com.zuora.enums.PaymentMethodType;
import com.zuora.enums.PaymentSourceType;
import com.zuora.zbilling.account.model.BillingAccount;
import com.zuora.zbilling.payment.model.Payment;
import com.zuora.zbilling.paymentmethod.model.PaymentMethod;
import com.zuora.zbilling.setting.gateway.model.PaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.OpenPaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.connector.HttpConnectorCommonUtil;
import com.zuora.zpayment.openpaymentgateway.engine.constants.OpenPaymentGatewayConstants;
import com.zuora.zpayment.openpaymentgateway.engine.templateengine.ZUtility;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StripeV2CreditCardReferenceTransactionPaymentUnitTest extends OpgJsonBaseTest {
    private static final ZUtility zUtility = new ZUtility();

    @Test
    public void case_01_ItShouldSucceed_WithCreateAndConfirmPaymentIntentTransactionFlow() throws Exception {
        final String case_01_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_01/payment_create&confirm_pi_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCCRefForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("Indian Rupee", "INR", "356"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("98"))
                .withPaymentNumber("P-00000069")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3N90b9SDx60UgxZn1ThJ0ayk", responseMap.get("GatewayReferenceId"));
        assertEquals("717597857811279", responseMap.get("MITReceivedTXID"));
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
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "9800"),
                                        Matchers.hasEntry("currency", "INR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("customer", "cus_K4jQ4dzKx97XMD"),
                                        Matchers.hasEntry("payment_method", "card_1JQZtl4ZWiZesCzmzXas7qd7"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000069"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all") // verify skip_rules
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
    public void case_02_ItShouldSucceed_WithCreateAndConfirmPaymentIntentWithoutL3TransactionFlow() throws Exception {
        final String case_02_createAndConfirmPaymentIntent_error_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_02/payment_create&confirmpaymentintent_l3param_error_response.json";
        final String case_02_createAndConfirmPaymentIntentWithoutL3_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_02/payment_create&confirmpaymentintent_without_l3_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = PaymentMethodBuilder.aPaymentMethod()
                .withMethodType(PaymentMethodType.CreditCardReferenceTransaction)
                .withCcRefTxnPnrefID("pm_1N9KZV4ZWiZesCzmoigmWs1Y")
                .withSecondTokenId("cus_NvAvgUB9pbcarf")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();

        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("8"))
                .withPaymentNumber("P-00000022")
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

        requestMap.put("PaymentNumber", "P-00000022");

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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_createAndConfirmPaymentIntent_error_response))
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_createAndConfirmPaymentIntentWithoutL3_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("[L3 Data Downgraded] Approved", responseMap.get("GatewayResponseMessage")); // this indicates L2/L3 data downgrade flow
        assertEquals("ch_3N9QuF4ZWiZesCzm1wbp9HBw", responseMap.get("GatewayReferenceId"));
        assertEquals("121819797114103", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertTrue(StringUtils.isBlank(responseMap.get("GatewaySecondReferenceId")));

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
                                        Matchers.hasEntry("customer", "cus_NvAvgUB9pbcarf"),
                                        Matchers.hasEntry("payment_method", "pm_1N9KZV4ZWiZesCzmoigmWs1Y"),
                                        // Sample Level3 data
                                        Matchers.hasEntry("level3[merchant_reference]", "P-00000022"),
                                        Matchers.hasEntry("level3[customer_reference]", "A-99999988"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "800"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000022"),

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
                                        Matchers.hasEntry("customer", "cus_NvAvgUB9pbcarf"),
                                        Matchers.hasEntry("payment_method", "pm_1N9KZV4ZWiZesCzmoigmWs1Y"),

                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "800"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000022"),

                                        Matchers.hasEntry("radar_options[session]", "rse_1N9KQO4ZWiZesCzm3BqRc4ti"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all") // verify skip_rules
                                ),
                                "check CreateAndConfirmPaymentIntentWithoutL3 transaction request payload")
                        .build());
    }

    @Test
    public void case_03_ItShouldSucceed_WithCreateAndConfirmPaymentIntentWithL3AndAccountNumberTrimmedWithInvoiceListCountLessThan200() throws Exception {
        final String case_03_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_03/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCCRefForTest();

        //Account number length greater than 25
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A00000007A00000007A00000007")
                .withCurrency(CurrencyBuilder.of("Indian Rupee", "INR", "356"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("98"))
                .withPaymentNumber("P-00000069")
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_03_response))
                        .build()
        );

        //Invoice item list count 1(less than 200)
        List<Map<String, String>> nameValuePairsList = new ArrayList<>();
        nameValuePairsList.add(StripeTestHelper.buildInvoiceListItem());

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway, nameValuePairsList);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3N90b9SDx60UgxZn1ThJ0ayk", responseMap.get("GatewayReferenceId"));
        assertEquals("717597857811279", responseMap.get("MITReceivedTXID"));
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
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "9800"),
                                        Matchers.hasEntry("currency", "INR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("customer", "cus_K4jQ4dzKx97XMD"),
                                        Matchers.hasEntry("payment_method", "card_1JQZtl4ZWiZesCzmzXas7qd7"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000069"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all"), // verify skip_rules
                                        Matchers.hasEntry("level3[customer_reference]", "A00000007A00000007A000000") // Trimmed account number
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_04_ItShouldSucceed_WithCreateAndConfirmPaymentIntentWithL3AndAccountNumberTrimmedWithInvoiceListCountMoreThan200() throws Exception {
        final String case_04_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_04/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCCRefForTest();

        //Account number length greater than 25
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A00000007A00000007A00000007")
                .withCurrency(CurrencyBuilder.of("Indian Rupee", "INR", "356"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("98"))
                .withPaymentNumber("P-00000069")
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_04_response))
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
        assertEquals("ch_3N90b9SDx60UgxZn1ThJ0ayk", responseMap.get("GatewayReferenceId"));
        assertEquals("717597857811279", responseMap.get("MITReceivedTXID"));
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
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "9800"),
                                        Matchers.hasEntry("currency", "INR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("customer", "cus_K4jQ4dzKx97XMD"),
                                        Matchers.hasEntry("payment_method", "card_1JQZtl4ZWiZesCzmzXas7qd7"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000069"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all"), // verify skip_rules
                                        Matchers.hasEntry("level3[customer_reference]", "A00000007A00000007A000000") // Trimmed account number
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_05_NormalRecurringPaymentReturnUnknownZuoraResponseCodeWhenGatewayReturns5XXHttpStatus() throws Exception {
        final String case_05_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_05/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCCRefForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("Indian Rupee", "INR", "356"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("98"))
                .withPaymentNumber("P-00000069")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
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
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "529")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_05_response))
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
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "9800"),
                                        Matchers.hasEntry("currency", "INR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("customer", "cus_K4jQ4dzKx97XMD"),
                                        Matchers.hasEntry("payment_method", "card_1JQZtl4ZWiZesCzmzXas7qd7"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000069"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all") // verify skip_rules
                                ),
                                "check request payload")
                        .build());
    }


    @Test
    public void case_06_CreateAndConfirmPaymentIntentWithoutL3ReturnUnknownZuoraResponseCodeWhenGatewayReturns5XXHttpStatus() throws Exception {
        final String case_06_createAndConfirmPaymentIntent_error_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_06/l3param_error_response.json";
        final String case_06_createAndConfirmPaymentIntentWithoutL3_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_06/paymentintent_without_l3_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = PaymentMethodBuilder.aPaymentMethod()
                .withMethodType(PaymentMethodType.CreditCardReferenceTransaction)
                .withCcRefTxnPnrefID("pm_1N9KZV4ZWiZesCzmoigmWs1Y")
                .withSecondTokenId("cus_NvAvgUB9pbcarf")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();

        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("8"))
                .withPaymentNumber("P-00000022")
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

        requestMap.put("PaymentNumber", "P-00000022");

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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_06_createAndConfirmPaymentIntent_error_response))
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_06_createAndConfirmPaymentIntentWithoutL3_response))
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
                                        Matchers.hasEntry("customer", "cus_NvAvgUB9pbcarf"),
                                        Matchers.hasEntry("payment_method", "pm_1N9KZV4ZWiZesCzmoigmWs1Y"),
                                        // Sample Level3 data
                                        Matchers.hasEntry("level3[merchant_reference]", "P-00000022"),
                                        Matchers.hasEntry("level3[customer_reference]", "A-99999988"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "800"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000022"),

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
                                        Matchers.hasEntry("customer", "cus_NvAvgUB9pbcarf"),
                                        Matchers.hasEntry("payment_method", "pm_1N9KZV4ZWiZesCzmoigmWs1Y"),

                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "800"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000022"),

                                        Matchers.hasEntry("radar_options[session]", "rse_1N9KQO4ZWiZesCzm3BqRc4ti"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all") // verify skip_rules
                                ),
                                "check CreateAndConfirmPaymentIntentWithoutL3 transaction request payload")
                        .build());
    }

    @Test
    public void case_07_ItShouldSucceed_WhenPaymentMethodTokensHasSpaces() throws Exception {
        final String case_01_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_07/response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCCRefForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("Indian Rupee", "INR", "356"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("98"))
                .withPaymentNumber("P-00000069")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        //TokenId and SecondTokenId contains spaces
        paymentMethod.setCcRefTxnPnrefID("      card_1OPfVJDWEdEWOKCaLrwG4jBX        ");
        paymentMethod.setSecondTokenId(" cus_PDNfVuNqbqqWhO           ");

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3OUL6UDWEdEWOKCa0LvDkN7a", responseMap.get("GatewayReferenceId"));
        assertEquals("113488768120655", responseMap.get("MITReceivedTXID"));
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
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "9800"),
                                        Matchers.hasEntry("currency", "INR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("customer", "cus_PDNfVuNqbqqWhO"), //should not contain any space
                                        Matchers.hasEntry("payment_method", "card_1OPfVJDWEdEWOKCaLrwG4jBX"), //should not contain any space
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000069"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all") // verify skip_rules
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_08_ItShouldNotSendMandateIdInRequestIfTheCondtionsAreSatisfiedAndSettingIsEnabled() {
        final String case_01_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_07/response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "Feature-IgnoreMandateIdInPaymentRunAboveINR15K-Enabled=true;")
                        .build()));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCCRefForTest();
        paymentMethod.setMandateId("mandate_xxxxxx");
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("Indian Rupee", "INR", "356"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("15000"))
                .withPaymentNumber("P-00000069")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("PaymentSource", "PaymentRun");

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
        assertEquals("ch_3OUL6UDWEdEWOKCa0LvDkN7a", responseMap.get("GatewayReferenceId"));
        assertEquals("113488768120655", responseMap.get("MITReceivedTXID"));
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
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "1500000"),
                                        Matchers.hasEntry("currency", "INR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("customer", "cus_K4jQ4dzKx97XMD"), //should not contain any space
                                        Matchers.hasEntry("payment_method", "card_1JQZtl4ZWiZesCzmzXas7qd7"), //should not contain any space
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000069"),
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all"), // verify skip_rules
                                        Matchers.not(Matchers.hasKey("mandate"))
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_09_ItShouldSendMandateIdInRequestIfTheCondtionsAreSatisfiedAndSettingIsNotEnabled() {
        final String case_01_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_07/response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCCRefForTest();
        paymentMethod.setMandateId("mandate_xxxxxx");
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("Indian Rupee", "INR", "356"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("15000"))
                .withPaymentNumber("P-00000069")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .withSourceType(PaymentSourceType.PaymentRun)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("PaymentSource", "PaymentRun");

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
        assertEquals("ch_3OUL6UDWEdEWOKCa0LvDkN7a", responseMap.get("GatewayReferenceId"));
        assertEquals("113488768120655", responseMap.get("MITReceivedTXID"));
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
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "1500000"),
                                        Matchers.hasEntry("currency", "INR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("customer", "cus_K4jQ4dzKx97XMD"), //should not contain any space
                                        Matchers.hasEntry("payment_method", "card_1JQZtl4ZWiZesCzmzXas7qd7"), //should not contain any space
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000069"),
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all"), // verify skip_rules
                                        Matchers.hasEntry("mandate", "mandate_xxxxxx")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_10_ItShouldSucceedWithDescriptionInRequestWhenDescriptionIsSetInGatewayConfiguration() throws Exception {
        final String case_10_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_10/payment_create&confirm_pi_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCCRefForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("Indian Rupee", "INR", "356"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("98"))
                .withPaymentNumber("P-00000069")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_10_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3N90b9SDx60UgxZn1ThJ0ayk", responseMap.get("GatewayReferenceId"));
        assertEquals("717597857811279", responseMap.get("MITReceivedTXID"));
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
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "9800"),
                                        Matchers.hasEntry("currency", "INR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("customer", "cus_K4jQ4dzKx97XMD"),
                                        Matchers.hasEntry("payment_method", "card_1JQZtl4ZWiZesCzmzXas7qd7"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000069"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all"), // verify skip_rules
                                        Matchers.hasEntry("description", "Test Payment Description")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_11_ItShouldSucceedWithDescriptionNotInRequestWhenDescriptionIsBlankInGatewayConfiguration() throws Exception {
        final String case_11_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_11/payment_create&confirm_pi_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCCRefForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("Indian Rupee", "INR", "356"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("98"))
                .withPaymentNumber("P-00000069")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_11_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3N90b9SDx60UgxZn1ThJ0ayk", responseMap.get("GatewayReferenceId"));
        assertEquals("717597857811279", responseMap.get("MITReceivedTXID"));
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
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "9800"),
                                        Matchers.hasEntry("currency", "INR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("customer", "cus_K4jQ4dzKx97XMD"),
                                        Matchers.hasEntry("payment_method", "card_1JQZtl4ZWiZesCzmzXas7qd7"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000069"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all") // verify skip_rules
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_12_ItShouldSucceedWithDescriptionNotInRequestWhenDescriptionIsSpecialCharactersInGatewayConfiguration() throws Exception {
        final String case_12_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_12/payment_create&confirm_pi_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCCRefForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("Indian Rupee", "INR", "356"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("98"))
                .withPaymentNumber("P-00000069")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
                                .put("PaymentDescription", "~!@#%^&*()_+-=[]{}|;:',.<>?/`~!@#%^&*()_+-=[]{}|;:',.<>?/`~!@#%^&*()_+-=[]{}|;:',.<>?/`~!@#%^&*()_+-=[]")
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_12_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3N90b9SDx60UgxZn1ThJ0ayk", responseMap.get("GatewayReferenceId"));
        assertEquals("717597857811279", responseMap.get("MITReceivedTXID"));
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
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "9800"),
                                        Matchers.hasEntry("currency", "INR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("customer", "cus_K4jQ4dzKx97XMD"),
                                        Matchers.hasEntry("payment_method", "card_1JQZtl4ZWiZesCzmzXas7qd7"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000069"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all"), // verify skip_rules
                                        Matchers.hasEntry("description", "~!@#%^&*()_+-=[]{}|;:',.<>?/`~!@#%^&*()_+-=[]{}|;:',.<>?/`~!@#%^&*()_+-=[]{}|;:',.<>?/`~!@#%^&*()_+-=[]")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_14_ItShouldFailWithDescriptionInRequestWhenDescriptionIsSetInGatewayConfigurationAndLengthGreaterThan1000() throws Exception {
        final String case_14_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_14/payment_create&confirm_pi_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCCRefForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("Indian Rupee", "INR", "356"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("98"))
                .withPaymentNumber("P-00000069")
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
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
                                .put("PaymentDescription", paymentDescription)
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
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "400")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_14_response))
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
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "9800"),
                                        Matchers.hasEntry("currency", "INR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("customer", "cus_K4jQ4dzKx97XMD"),
                                        Matchers.hasEntry("payment_method", "card_1JQZtl4ZWiZesCzmzXas7qd7"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000069"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all"), // verify skip_rules
                                        Matchers.hasEntry("description", paymentDescription)
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_15_ItShouldSucceed_WithCreateAndConfirmPaymentIntentWithoutL3TransactionFlowWithPaymentDescription() throws Exception {
        final String case_15_createAndConfirmPaymentIntent_error_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_15/payment_create&confirmpaymentintent_l3param_error_response.json";
        final String case_15_createAndConfirmPaymentIntentWithoutL3_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_15/payment_create&confirmpaymentintent_without_l3_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = PaymentMethodBuilder.aPaymentMethod()
                .withMethodType(PaymentMethodType.CreditCardReferenceTransaction)
                .withCcRefTxnPnrefID("pm_1N9KZV4ZWiZesCzmoigmWs1Y")
                .withSecondTokenId("cus_NvAvgUB9pbcarf")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();

        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("8"))
                .withPaymentNumber("P-00000022")
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
                                .put("PaymentDescription", "Test Payment Description")
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_ITEM_LIST, "TestNameValueList");

        // pass RadarSessionId via gateway options
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "RadarSessionId", "rse_1N9KQO4ZWiZesCzm3BqRc4ti");

        requestMap.put("PaymentNumber", "P-00000022");

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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_15_createAndConfirmPaymentIntent_error_response))
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_15_createAndConfirmPaymentIntentWithoutL3_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("[L3 Data Downgraded] Approved", responseMap.get("GatewayResponseMessage")); // this indicates L2/L3 data downgrade flow
        assertEquals("ch_3N9QuF4ZWiZesCzm1wbp9HBw", responseMap.get("GatewayReferenceId"));
        assertEquals("121819797114103", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertTrue(StringUtils.isBlank(responseMap.get("GatewaySecondReferenceId")));

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
                                        Matchers.hasEntry("customer", "cus_NvAvgUB9pbcarf"),
                                        Matchers.hasEntry("payment_method", "pm_1N9KZV4ZWiZesCzmoigmWs1Y"),
                                        // Sample Level3 data
                                        Matchers.hasEntry("level3[merchant_reference]", "P-00000022"),
                                        Matchers.hasEntry("level3[customer_reference]", "A-99999988"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "800"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000022"),
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
                                        Matchers.hasEntry("customer", "cus_NvAvgUB9pbcarf"),
                                        Matchers.hasEntry("payment_method", "pm_1N9KZV4ZWiZesCzmoigmWs1Y"),

                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "800"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000022"),
                                        Matchers.hasEntry("description", "Test Payment Description"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N9KQO4ZWiZesCzm3BqRc4ti"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all") // verify skip_rules
                                ),
                                "check CreateAndConfirmPaymentIntentWithoutL3 transaction request payload")
                        .build());
    }

    @Test
    public void case_16_ItShouldSucceedWithDescriptionInRequestWhenDescriptionIsSendOverGatewayOptions() throws Exception {
        final String case_10_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_10/payment_create&confirm_pi_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCCRefForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("Indian Rupee", "INR", "356"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("98"))
                .withPaymentNumber("P-00000069")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
                                .put("PaymentDescription", "Test Payment Description")
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "RadarSessionId", "rse_1N7fvKSDx60UgxZnDB5Hb4v5");
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "PaymentDescription", "Test Gateway Options PaymentDescription");

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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_10_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3N90b9SDx60UgxZn1ThJ0ayk", responseMap.get("GatewayReferenceId"));
        assertEquals("717597857811279", responseMap.get("MITReceivedTXID"));
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
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "9800"),
                                        Matchers.hasEntry("currency", "INR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("customer", "cus_K4jQ4dzKx97XMD"),
                                        Matchers.hasEntry("payment_method", "card_1JQZtl4ZWiZesCzmzXas7qd7"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00000069"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5"), // verify RadarSessionId
                                        Matchers.hasEntry("radar_options[skip_rules][]", "all"), // verify skip_rules
                                        Matchers.hasEntry("description", "Test Gateway Options PaymentDescription")
                                ), "check request payload")
                        .build());
    }

    @Test
    public void case_17_ItShouldSendStatementDescriptor() {
        final String approved_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/payment/case_01/payment_create&confirm_pi_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCCRefForTest();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentMethod(paymentMethod)
                .withSoftDescriptor("test soft descriptor")
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("DisableRadarRules", "on") // Enable 'skip_rules'
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);

        // pass RadarSessionId via gateway options
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "RadarSessionId", "rse_1N7fvKSDx60UgxZnDB5Hb4v5");

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
        assertEquals("ch_3N90b9SDx60UgxZn1ThJ0ayk", responseMap.get("GatewayReferenceId"));
        assertEquals("717597857811279", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));

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
