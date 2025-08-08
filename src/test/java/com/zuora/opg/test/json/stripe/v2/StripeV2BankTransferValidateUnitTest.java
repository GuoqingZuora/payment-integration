package com.zuora.opg.test.json.stripe.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.zuora.billing.opg.test.common.OpgJsonBaseTest;
import com.zuora.billing.opg.test.json.stripe.StripeRequestPayloadExtractor;
import com.zuora.billing.opg.test.json.stripe.StripeTestHelper;
import com.zuora.billing.opg.test.support.common.HashMapBuilder;
import com.zuora.billing.opg.test.support.engine.OpgRequestMapHelper;
import com.zuora.billing.opg.test.verify.ConnectorHeadersVerifierBuilder;
import com.zuora.zbilling.paymentmethod.model.PaymentMethod;
import com.zuora.zbilling.setting.gateway.model.PaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.OpenPaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.connector.HttpConnectorCommonUtil;
import com.zuora.zpayment.openpaymentgateway.engine.constants.OpenPaymentGatewayConstants;
import com.zuora.zpayment.openpaymentgateway.engine.templateengine.ZUtility;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Map;

public class StripeV2BankTransferValidateUnitTest extends OpgJsonBaseTest {
    private static final ZUtility zUtility = new ZUtility();

    @Test
    public void case_01_ItShouldApproveGoodBankTRansferPaymentMethod() {
        final String case_01_create_customer_response = "/com/zuora/opg/test/json/stripe_2/bankTransfer/validate/case_01/create_customer_response.json";
        final String case_01_confirm_setup_intent_response = "/com/zuora/opg/test/json/stripe_2/bankTransfer/validate/case_01/confirm_setup_intent_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodBankTransferPaymentMethodForTest();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap= OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);


        // transaction: CreateOrUpdateAccount
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_create_customer_response))
                        .build()
        );

        // transaction: ConfirmSetupIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/setup_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_confirm_setup_intent_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("lastHttpStatusCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("email", "test@gmail.com")
                                ),
                                "check request payload")
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/setup_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_types[]", "sepa_debit"),
                                        Matchers.hasEntry("mandate_data[customer_acceptance][type]", "online"),
                                        Matchers.hasEntry("customer", "cus_OYHCcy7xvuK5R0"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("payment_method_data[type]", "sepa_debit"),
                                        Matchers.hasEntry("payment_method_data[sepa_debit][iban]", "DE09100100101234567893"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestZuora")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_02_ReturnUnknownZuoraResponseCodeWhenGatewayReturns5XXHttpStatus() {
        final String case_02_create_customer_response = "/com/zuora/opg/test/json/stripe_2/bankTransfer/validate/case_02/create_customer_response.json";
        final String case_02_confirm_setup_intent_response = "/com/zuora/opg/test/json/stripe_2/bankTransfer/validate/case_02/confirm_setup_intent_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodBankTransferPaymentMethodForTest();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap= OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);


        // transaction: CreateOrUpdateAccount
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_create_customer_response))
                        .build()
        );

        // transaction: ConfirmSetupIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/setup_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "529")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_confirm_setup_intent_response))
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
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("email", "test@gmail.com")
                                ),
                                "check request payload")
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/setup_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_types[]", "sepa_debit"),
                                        Matchers.hasEntry("mandate_data[customer_acceptance][type]", "online"),
                                        Matchers.hasEntry("customer", "cus_OYHCcy7xvuK5R0"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("payment_method_data[type]", "sepa_debit"),
                                        Matchers.hasEntry("payment_method_data[sepa_debit][iban]", "DE09100100101234567893"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestZuora")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_03_ValidateBacsPaymentMethodWithTokensWithValidFormatProvided() {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodBankTransferBACSPaymentMethodForTest();
        paymentMethod.setCcRefTxnPnrefID("cus_Oj3UpqsNzuqE29");
        paymentMethod.setSecondTokenId("pm_1NvbNf4ZWiZesCzmqxFL54bc");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap= OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);


        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertNull(responseMap.get("lastHttpStatusCode"));
        assertEquals("", responseMap.get("GatewayResponseMessage"));
        assertEquals("", responseMap.get("GatewayResponseCode"));
    }

    @Test
    public void case_04_ValidateBacsPaymentMethodWithTokensWithInvalidFormatProvided() {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodBankTransferBACSPaymentMethodForTest();
        paymentMethod.setCcRefTxnPnrefID("testTokenId");
        paymentMethod.setSecondTokenId("testSecondTokenId");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap= OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);


        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertNull(responseMap.get("lastHttpStatusCode"));
        assertEquals("Invalid payment method id", responseMap.get("GatewayResponseMessage"));
        assertEquals("400", responseMap.get("GatewayResponseCode"));
    }

    @Test
    public void case_05_ValidateBacsPaymentMethodWithNoTokensProvided() {
        final String case_05_create_customer_response = "/com/zuora/opg/test/json/stripe_2/bankTransfer/validate/case_05/create_customer_response.json";
        final String case_05_confirm_setup_intent_response = "/com/zuora/opg/test/json/stripe_2/bankTransfer/validate/case_05/confirm_setup_intent_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodBankTransferBACSPaymentMethodForTest();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap= OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);


        // transaction: CreateOrUpdateAccount
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_05_create_customer_response))
                        .build()
        );

        // transaction: ConfirmSetupIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/setup_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_05_confirm_setup_intent_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("lastHttpStatusCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("cus_OjPDx52Y6gedpQ", responseMap.get("GatewaySecondReferenceId"));
        assertEquals("pm_1NvwP24ZWiZesCzm10iDDGRI", responseMap.get("GatewayThirdReferenceId"));
        assertEquals("cus_OjPDx52Y6gedpQ", responseMap.get("GatewayResponseToken1"));
        assertEquals("pm_1NvwP24ZWiZesCzm10iDDGRI", responseMap.get("GatewayResponseToken2"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("email", "test@gmail.com")
                                ),
                                "check request payload")
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/setup_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_types[]", "bacs_debit"),
                                        Matchers.hasEntry("mandate_data[customer_acceptance][type]", "online"),
                                        Matchers.hasEntry("customer", "cus_OjPDx52Y6gedpQ"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("payment_method_data[type]", "bacs_debit"),
                                        Matchers.hasEntry("payment_method_data[bacs_debit][account_number]", "00012345"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestZuora")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_06_ItShouldApprovePADBankTransferPaymentMethodWithoutSkipVerification() {
        final String create_customer_response = "/com/zuora/opg/test/json/stripe_2/bankTransfer/validate/case_06/create_customer_response_06.json";
        final String confirm_setup_intent_response = "/com/zuora/opg/test/json/stripe_2/bankTransfer/validate/case_06/confirm_setup_intent_response_06.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildPADBankTransferPaymentMethodForTest();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap= OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("browser_IpAddress", "0:0:0:0:0:0:0:1");

        // transaction: CreateOrUpdateAccount
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(create_customer_response))
                        .build()
        );

        // transaction: ConfirmSetupIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/setup_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(confirm_setup_intent_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("lastHttpStatusCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("mandate_1O5kbeSDn2FrzZUFku5gFt1M", responseMap.get("GatewayReferenceId"));
        assertEquals("cus_OYHCcy7xvuK5R0", responseMap.get("GatewaySecondReferenceId"));
        assertEquals("pm_1O5kbcSDn2FrzZUFvldSxgsP", responseMap.get("GatewayThirdReferenceId"));
        assertEquals("cus_OYHCcy7xvuK5R0", responseMap.get("GatewayResponseToken1"));
        assertEquals("pm_1O5kbcSDn2FrzZUFvldSxgsP", responseMap.get("GatewayResponseToken2"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("email", "test@gmail.com")
                                ),
                                "check request payload")
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/setup_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("expand[]", "mandate"),
                                        Matchers.hasEntry("payment_method_types[]", "acss_debit"),
                                        Matchers.hasEntry("usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("mandate_data[customer_acceptance][type]", "online"),
                                        Matchers.hasEntry("customer", "cus_OYHCcy7xvuK5R0"),
                                        Matchers.hasEntry("mandate_data[customer_acceptance][online][ip_address]", "0:0:0:0:0:0:0:1"),
                                        Matchers.hasEntry("payment_method_data[type]", "acss_debit"),
                                        Matchers.hasEntry("payment_method_data[acss_debit][account_number]", "000123456789"),
                                        Matchers.hasEntry("payment_method_data[acss_debit][institution_number]", "000"),
                                        Matchers.hasEntry("payment_method_data[acss_debit][transit_number]", "11000"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestZuora"),
                                        Matchers.hasEntry("payment_method_data[billing_details][email]", "test@gmail.com"),
                                        Matchers.hasEntry("payment_method_options[acss_debit][mandate_options][payment_schedule]", "sporadic"),
                                        Matchers.hasEntry("payment_method_options[acss_debit][currency]", "cad"),
                                        Matchers.hasEntry("payment_method_options[acss_debit][mandate_options][transaction_type]", "personal")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_07_ItShouldApprovePADBankTransferPaymentMethodWithSkipVerification() {
        final String create_customer_response = "/com/zuora/opg/test/json/stripe_2/bankTransfer/validate/case_07/create_customer_response_07.json";
        final String confirm_setup_intent_response = "/com/zuora/opg/test/json/stripe_2/bankTransfer/validate/case_07/confirm_setup_intent_response_07.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "Feature-SkipVerificationMethod-Enabled=true;")
                        .build()));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildPADBankTransferPaymentMethodForTest();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap= OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("browser_IpAddress", "0:0:0:0:0:0:0:1");


        // transaction: CreateOrUpdateAccount
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(create_customer_response))
                        .build()
        );

        // transaction: ConfirmSetupIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/setup_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(confirm_setup_intent_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("lastHttpStatusCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("mandate_1O5k3BSDn2FrzZUFIpkxgfdw", responseMap.get("GatewayReferenceId"));
        assertEquals("cus_OYHCcy7xvuK5R0", responseMap.get("GatewaySecondReferenceId"));
        assertEquals("pm_1O5k39SDn2FrzZUFGFKUEJqy", responseMap.get("GatewayThirdReferenceId"));
        assertEquals("cus_OYHCcy7xvuK5R0", responseMap.get("GatewayResponseToken1"));
        assertEquals("pm_1O5k39SDn2FrzZUFGFKUEJqy", responseMap.get("GatewayResponseToken2"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("email", "test@gmail.com")
                                ),
                                "check request payload")
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/setup_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("expand[]", "mandate"),
                                        Matchers.hasEntry("payment_method_types[]", "acss_debit"),
                                        Matchers.hasEntry("usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("mandate_data[customer_acceptance][type]", "online"),
                                        Matchers.hasEntry("customer", "cus_OYHCcy7xvuK5R0"),
                                        Matchers.hasEntry("mandate_data[customer_acceptance][online][ip_address]", "0:0:0:0:0:0:0:1"),
                                        Matchers.hasEntry("payment_method_data[type]", "acss_debit"),
                                        Matchers.hasEntry("payment_method_data[acss_debit][account_number]", "000123456789"),
                                        Matchers.hasEntry("payment_method_data[acss_debit][institution_number]", "000"),
                                        Matchers.hasEntry("payment_method_data[acss_debit][transit_number]", "11000"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestZuora"),
                                        Matchers.hasEntry("payment_method_data[billing_details][email]", "test@gmail.com"),
                                        Matchers.hasEntry("payment_method_options[acss_debit][mandate_options][payment_schedule]", "sporadic"),
                                        Matchers.hasEntry("payment_method_options[acss_debit][currency]", "cad"),
                                        Matchers.hasEntry("payment_method_options[acss_debit][mandate_options][transaction_type]", "personal"),
                                        // Check verification_method = skip
                                        Matchers.hasEntry("payment_method_options[acss_debit][verification_method]", "skip")
                                ),
                                "check request payload")
                        .build());
    }
}
