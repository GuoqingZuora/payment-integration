package com.zuora.opg.test.json.stripe.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

public class StripeV2IdealThreeDs2SubmitChallengeUnitTest extends OpgJsonBaseTest {

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
    public void case_01_ThreeDs2SubmitChallengeSuccessful() {
        final String retrievePaymentIntent_response = "/com/zuora/opg/test/json/stripe_2/ideal/threeds2submitchallenge/case_01/retrievePaymentIntent_response.json";

        Map<String, String> requestMap = OpgRequestMapHelper.constructThreeDS2SubmitChallengeResultCallRequestMap(paymentMethod, paymentGateway);
        putThreeDS2RequestMappingFields(requestMap);

        expectStripeConnectorResponse("https://api.stripe.com/v1/payment_intents/pi_3Pxs1u4ZWiZesCzm1OjRSsaE", "200", retrievePaymentIntent_response);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertBasicSuccessResponseFields(responseMap);

        verifyConnectorRequest(httpsConnector,
                // transaction -> RetrievePaymentIntent
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3Pxs1u4ZWiZesCzm1OjRSsaE"), "Check Confirm URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION_2020))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(),
                                "check RetrievePaymentIntent transaction request payload")
                        .build());
    }

    @Test
    public void case_02_ThreeDs2SubmitChallengeFail_WhenInvalidPaymentIntentIdIsPassed() {
        final String retrievePaymentIntent_response = "/com/zuora/opg/test/json/stripe_2/ideal/threeds2submitchallenge/case_02/retrievePaymentIntent_response.json";

        Map<String, String> requestMap = OpgRequestMapHelper.constructThreeDS2SubmitChallengeResultCallRequestMap(paymentMethod, paymentGateway);
        putThreeDS2RequestMappingFields(requestMap);
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_3DS + "paymentIntentId", "missing_id");

        expectStripeConnectorResponse("https://api.stripe.com/v1/payment_intents/missing_id", "404", retrievePaymentIntent_response);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertEquals("404", responseMap.get("GatewayResponseCode"));
        assertEquals("[invalid_request_error/resource_missing] No such payment_intent: 'missing_id'", responseMap.get("GatewayResponseMessage"));

        verifyConnectorRequest(httpsConnector,
                // transaction -> RetrievePaymentIntent
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/missing_id"), "Check Confirm URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION_2020))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(),
                                "check RetrievePaymentIntent transaction request payload")
                        .build());
    }

    private void putThreeDS2RequestMappingFields(Map<String, String> requestMap) {
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_3DS + "doPayment", "true");
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_3DS + "paymentIntentId", "pi_3Pxs1u4ZWiZesCzm1OjRSsaE");
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_3DS + "storePaymentMethod", "true");
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_3DS + "idealPaymentMethodId", "pm_1Pxs1W4ZWiZesCzmiLhpBHPZ");
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_3DS + "idealReturnUrl",
                "http://localhost:8080/apps/PublicHostedPageLite.do?method=handleThreeDs2Callback&tenantId=9&threeDs2Ts=1726066825179");
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_3DS + "authorizationAmount", "10");
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_3DS + "currency", "EUR");
    }

    private void expectStripeConnectorResponse(String url, String statusCode, String mockResponse) {
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", url),
                        Matchers.hasEntry("METHOD", "GET"),
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
