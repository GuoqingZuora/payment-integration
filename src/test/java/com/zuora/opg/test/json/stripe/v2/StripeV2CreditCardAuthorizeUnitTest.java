package com.zuora.opg.test.json.stripe.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.zuora.billing.opg.test.common.OpgJsonBaseTest;
import com.zuora.billing.opg.test.json.stripe.StripeRequestPayloadExtractor;
import com.zuora.billing.opg.test.json.stripe.StripeTestHelper;
import com.zuora.billing.opg.test.support.common.BillingAccountBuilder;
import com.zuora.billing.opg.test.support.common.CurrencyBuilder;
import com.zuora.billing.opg.test.support.common.HashMapBuilder;
import com.zuora.billing.opg.test.support.engine.OpgRequestMapHelper;
import com.zuora.billing.opg.test.verify.ConnectorHeadersVerifierBuilder;
import com.zuora.zbilling.account.model.BillingAccount;
import com.zuora.zbilling.paymentmethod.model.PaymentMethod;
import com.zuora.zbilling.setting.gateway.model.PaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.OpenPaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.connector.HttpConnectorCommonUtil;
import com.zuora.zpayment.openpaymentgateway.engine.constants.OpenPaymentGatewayConstants;
import com.zuora.zpayment.openpaymentgateway.engine.templateengine.ZUtility;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Map;

public class StripeV2CreditCardAuthorizeUnitTest extends OpgJsonBaseTest {
    private static final ZUtility zUtility = new ZUtility();

    @Test
    public void case_01_AuthorizeSuccess() {
        final String case_01_authorize_response = "/com/zuora/opg/test/json/stripe_2/creditcard/authorize/case_01/authorize_response.json";
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

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructAuthorizeCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);


        // transaction: capture
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_authorize_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("pi_3M2ZHwSDIx8h6iU71LVetETQ", responseMap.get("GatewayReferenceId"));

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
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey))
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
                                        Matchers.hasEntry("amount", "100"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("capture_method", "manual")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_02_ReturnUnknownZuoraResponseCodeWhenGatewayReturns5XXHttpStatus() {
        final String case_02_authorize_response = "/com/zuora/opg/test/json/stripe_2/creditcard/authorize/case_02/authorize_response.json";
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

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructAuthorizeCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);


        // transaction: capture
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "529")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_authorize_response))
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
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey))
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
                                        Matchers.hasEntry("amount", "100"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("capture_method", "manual")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_03_AuthorizeStatus_requires_action() {
        final String case_03_authorize_response = "/com/zuora/opg/test/json/stripe_2/creditcard/authorize/case_03/authorize_response.json";
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

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructAuthorizeCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);


        // transaction: capture
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_03_authorize_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        String errorMessage = "[card_error/authentication_required/authentication_required] Your card was declined. This transaction requires authentication.";

        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertEquals("402", responseMap.get("GatewayResponseCode"));
        assertEquals(errorMessage, responseMap.get("GatewayResponseMessage"));
        assertEquals("200", responseMap.get("lastHttpStatusCode"));
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
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey))
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
                                        Matchers.hasEntry("amount", "100"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("capture_method", "manual")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_04_AuthorizeStatus_requires_capture() {
        final String case_04_authorize_response = "/com/zuora/opg/test/json/stripe_2/creditcard/authorize/case_04/authorize_response.json";
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

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructAuthorizeCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);


        // transaction: capture
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_04_authorize_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("200", responseMap.get("lastHttpStatusCode"));
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
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey))
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
                                        Matchers.hasEntry("amount", "100"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("capture_method", "manual")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_05_ItShouldSucceed_withPaymentDescription() {
        final String case_05_authorize_response = "/com/zuora/opg/test/json/stripe_2/creditcard/authorize/case_05/authorize_response.json";
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

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("PaymentDescription", "Test Payment Description")
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructAuthorizeCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);


        // transaction: capture
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_05_authorize_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("pi_3M2ZHwSDIx8h6iU71LVetETQ", responseMap.get("GatewayReferenceId"));

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
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey))
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
                                        Matchers.hasEntry("amount", "100"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("description", "Test Payment Description"),
                                        Matchers.hasEntry("capture_method", "manual")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_06_ItShouldSendSoftDescriptor() {
        final String case_01_authorize_response = "/com/zuora/opg/test/json/stripe_2/creditcard/authorize/case_01/authorize_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructAuthorizeCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.AUTHORIZATION_SOFTDESCRIPTOR, "test soft descriptor");


        // transaction: capture
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_authorize_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("pi_3M2ZHwSDIx8h6iU71LVetETQ", responseMap.get("GatewayReferenceId"));

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
