package com.zuora.opg.test.json.stripe.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.zuora.billing.opg.test.common.OpgJsonBaseTest;
import com.zuora.billing.opg.test.json.stripe.StripeRequestPayloadExtractor;
import com.zuora.billing.opg.test.json.stripe.StripeTestHelper;
import com.zuora.billing.opg.test.support.common.HashMapBuilder;
import com.zuora.billing.opg.test.support.engine.OpgRequestMapHelper;
import com.zuora.billing.opg.test.verify.ConnectorHeadersVerifierBuilder;
import com.zuora.enums.PaymentMethodType;
import com.zuora.zbilling.paymentmethod.model.PaymentMethod;
import com.zuora.zbilling.setting.gateway.model.PaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.OpenPaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.connector.HttpConnectorCommonUtil;
import com.zuora.zpayment.openpaymentgateway.engine.constants.OpenPaymentGatewayConstants;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class StripeV2IdealThreeDs2EnrollCheckUnitTest extends OpgJsonBaseTest {

    private PaymentGateway paymentGateway;
    private PaymentMethod paymentMethod;
    private OpenPaymentGateway opg;

    @Before
    public void setupOPMType() {
        PaymentMethodType.addOpenPaymentMethodType(26, "Ideal", "iDEAL");

        opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        paymentMethod = StripeTestHelper.buildIdealPaymentMethodForTest();

        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));
    }

    @After
    public void clearOPMType() {
        PaymentMethodType.removeOpenPaymentMethodType("Ideal");
    }

    @Test // OperationId: 07c3cf20c7944ee3b95e787a37b4586b
    public void case_01_ThreeDs2EnrollCheckSuccessful_WhenOnSessionPaymentWithStoringPaymentMethod() {
        final String createCustomer_response = "/com/zuora/opg/test/json/stripe_2/ideal/threeds2enrollcheck/case_01/createCustomer_response.json";
        final String createPaymentIntent_response = "/com/zuora/opg/test/json/stripe_2/ideal/threeds2enrollcheck/case_01/createPaymentIntent_response.json";
        final String confirmPaymentIntent_response = "/com/zuora/opg/test/json/stripe_2/ideal/threeds2enrollcheck/case_01/confirmPaymentIntent_response.json";

        Map<String, String> requestMap = OpgRequestMapHelper.constructThreeDS2EnrollCheckCallRequestMap(paymentMethod, paymentGateway);
        putThreeDS2RequestMappingFields(requestMap);
        requestMap.putAll(getMandateRequestMappingFields());
        requestMap.put("AccountNumber", "A-000012345");

        expectStripeConnectorResponse("https://api.stripe.com/v1/payment_intents", "200", createPaymentIntent_response);
        expectStripeConnectorResponse("https://api.stripe.com/v1/payment_intents/pi_3Pxs1u4ZWiZesCzm1OjRSsaE/confirm", "200", confirmPaymentIntent_response);
        expectStripeConnectorResponse("https://api.stripe.com/v1/customers", "200", createCustomer_response);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertBasicSuccessResponseFields(responseMap);

        verifyConnectorRequest(httpsConnector,
                // transaction -> CreateCustomer
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers"), "Check Customer URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION_2020))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("description", "Auto customer by on session payment by 68672831_A-000012345")
                                ),
                                "check transaction CreateCustomer request payload")
                        .build(),
                // transaction -> CreatePaymentIntent
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION_2020))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("amount", "1000"),
                                        Matchers.hasEntry("currency", "EUR")
                                ),
                                "check transaction CreatePaymentIntent request payload")
                        .build(),
                // transaction -> ConfirmPaymentIntent
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3Pxs1u4ZWiZesCzm1OjRSsaE/confirm"), "Check Confirm URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION_2020))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("return_url",
                                                "http://localhost:8080/apps/PublicHostedPageLite.do?method=handleThreeDs2Callback&tenantId=9&threeDs2Ts=1726066825179"),
                                        Matchers.hasEntry("mandate_data[customer_acceptance][type]", "online"),
                                        Matchers.hasEntry("mandate_data[customer_acceptance][online][ip_address]", "0:0:0:0:0:0:0:1"),
                                        Matchers.hasKey("mandate_data[customer_acceptance][online][user_agent]"),
                                        Matchers.hasKey("mandate_data[customer_acceptance][accepted_at]"),
                                        Matchers.hasEntry("payment_method", "pm_1Pxs1W4ZWiZesCzmiLhpBHPZ")
                                ),
                                "check ConfirmPaymentIntent transaction request payload")
                        .build());
    }

    @Test // OperationId: 157bfe1c2d22429ea565ac8dcb2d990a
    public void case_02_ThreeDs2EnrollCheckSuccessful_WhenOnSessionPaymentWithoutStoringPaymentMethod() {
        final String createPaymentIntent_response = "/com/zuora/opg/test/json/stripe_2/ideal/threeds2enrollcheck/case_02/createPaymentIntent_response.json";
        final String confirmPaymentIntent_response = "/com/zuora/opg/test/json/stripe_2/ideal/threeds2enrollcheck/case_02/confirmPaymentIntent_response.json";

        Map<String, String> requestMap = OpgRequestMapHelper.constructThreeDS2EnrollCheckCallRequestMap(paymentMethod, paymentGateway);
        putThreeDS2RequestMappingFields(requestMap);
        requestMap.put("threeDs_storePaymentMethod", "false");

        expectStripeConnectorResponse("https://api.stripe.com/v1/payment_intents", "200", createPaymentIntent_response);
        expectStripeConnectorResponse("https://api.stripe.com/v1/payment_intents/pi_3Py4Ew4ZWiZesCzm113ONHEX/confirm", "200", confirmPaymentIntent_response);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertBasicSuccessResponseFields(responseMap);

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION_2020))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("amount", "1000"),
                                        Matchers.hasEntry("currency", "EUR")
                                ),
                                "check transaction CreatePaymentIntent request payload")
                        .build(),
                // transaction -> ConfirmPaymentIntent
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3Py4Ew4ZWiZesCzm113ONHEX/confirm"), "Check Confirm URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION_2020))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("return_url",
                                                "http://localhost:8080/apps/PublicHostedPageLite.do?method=handleThreeDs2Callback&tenantId=9&threeDs2Ts=1726066825179"),
                                        Matchers.hasEntry("payment_method", "pm_1Pxs1W4ZWiZesCzmiLhpBHPZ")
                                ),
                                "check ConfirmPaymentIntent transaction request payload")
                        .build());
    }

    @Test // OperationId: 000601e2fb1b477fb048e9fe7e7bdd8e
    public void case_03_ThreeDs2EnrollCheckShouldFail_WhenFieldsNotPassed() {
        final String createCustomer_response = "/com/zuora/opg/test/json/stripe_2/ideal/threeds2enrollcheck/case_03/createCustomer_response.json";
        final String createPaymentIntent_response = "/com/zuora/opg/test/json/stripe_2/ideal/threeds2enrollcheck/case_03/createPaymentIntent_response.json";
        final String confirmPaymentIntent_response = "/com/zuora/opg/test/json/stripe_2/ideal/threeds2enrollcheck/case_03/confirmPaymentIntent_response.json";

        Map<String, String> requestMap = OpgRequestMapHelper.constructThreeDS2EnrollCheckCallRequestMap(paymentMethod, paymentGateway);
        putThreeDS2RequestMappingFields(requestMap);
        requestMap.putAll(getMandateRequestMappingFields());
        requestMap.put("AccountNumber", "A-000012345");

        expectStripeConnectorResponse("https://api.stripe.com/v1/payment_intents", "200", createPaymentIntent_response);
        expectStripeConnectorResponse("https://api.stripe.com/v1/payment_intents/pi_3Py53Q4ZWiZesCzm0md83Sih/confirm", "400", confirmPaymentIntent_response);
        expectStripeConnectorResponse("https://api.stripe.com/v1/customers", "200", createCustomer_response);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertEquals("400", responseMap.get("GatewayResponseCode"));
        assertTrue(responseMap.get("GatewayResponseMessage")
                .contains(
                        "[invalid_request_error/parameter_missing] When confirming a PaymentIntent with a `setup_future_usage` value of `off_session` and a PaymentMethod of type `ideal`, the PaymentMethod must include `billing_details[name]` and `billing_details[email]`."));

        verifyConnectorRequest(httpsConnector,
                // transaction -> CreateCustomer
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers"), "Check Customer URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION_2020))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("description", "Auto customer by on session payment by 68672831_A-000012345")
                                ),
                                "check transaction CreateCustomer request payload")
                        .build(),
                // transaction -> CreatePaymentIntent
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION_2020))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("amount", "1000"),
                                        Matchers.hasEntry("currency", "EUR")
                                ),
                                "check transaction CreatePaymentIntent request payload")
                        .build(),
                // transaction -> ConfirmPaymentIntent
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3Py53Q4ZWiZesCzm0md83Sih/confirm"), "Check Confirm URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION_2020))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("return_url",
                                                "http://localhost:8080/apps/PublicHostedPageLite.do?method=handleThreeDs2Callback&tenantId=9&threeDs2Ts=1726066825179"),
                                        Matchers.hasEntry("mandate_data[customer_acceptance][type]", "online"),
                                        Matchers.hasEntry("mandate_data[customer_acceptance][online][ip_address]", "0:0:0:0:0:0:0:1"),
                                        Matchers.hasKey("mandate_data[customer_acceptance][online][user_agent]"),
                                        Matchers.hasKey("mandate_data[customer_acceptance][accepted_at]"),
                                        Matchers.hasEntry("payment_method", "pm_1Pxs1W4ZWiZesCzmiLhpBHPZ")
                                ),
                                "check ConfirmPaymentIntent transaction request payload")
                        .build());
    }

    private void putThreeDS2RequestMappingFields(Map<String, String> requestMap) {
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_3DS + "doPayment", "true");
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_3DS + "storePaymentMethod", "true");
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_3DS + "idealPaymentMethodId", "pm_1Pxs1W4ZWiZesCzmiLhpBHPZ");
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_3DS + "idealReturnUrl",
                "http://localhost:8080/apps/PublicHostedPageLite.do?method=handleThreeDs2Callback&tenantId=9&threeDs2Ts=1726066825179");
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_3DS + "authorizationAmount", "10");
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_3DS + "currency", "EUR");
    }

    private Map<String, String> getMandateRequestMappingFields() {
        return Map.of(
                OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "ipAddress", "0:0:0:0:0:0:0:1",
                OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "UserAgent", "Mozilla/5.0 Chrome/122.0.0.0 Safari/537.36"
        );
    }

    private void expectStripeConnectorResponse(String url, String statusCode, String mockResponse) {
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", url),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION_2020)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, statusCode)
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(mockResponse))
                        .build()
        );
    }

    private void assertBasicSuccessResponseFields(Map<String, String> responseMap) {
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertNotNull(responseMap.get("ThreeDS2ResponseData"));
    }
}
