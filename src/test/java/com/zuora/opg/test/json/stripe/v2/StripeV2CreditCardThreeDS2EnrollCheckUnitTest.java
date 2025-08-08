package com.zuora.opg.test.json.stripe.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

public class StripeV2CreditCardThreeDS2EnrollCheckUnitTest extends OpgJsonBaseTest {
    private static final ZUtility zUtility = new ZUtility();

    @Test
    public void case_01_ThreeDs2EnrollCheckSuccessful() {
        final String case_01_threeds2enrollcheck_response = "/com/zuora/opg/test/json/stripe_2/creditcard/threeds2enrollcheck/case_01/threeds2enrollcheck_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructThreeDS2EnrollCheckCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/setup_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_threeds2enrollcheck_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("seti_1Nl6pVSC2hQ3y1eli8IArVIm", responseMap.get("GatewayReferenceId"));

        verifyConnectorRequest(httpsConnector,
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][email]", "test@gmail.com"),
                                        Matchers.hasEntry("payment_method_data[billing_details][phone]", "123-1234-123"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_02_ReturnUnknownZuoraResponseCodeWhenGatewayReturns5XXHttpStatus() {
        final String case_02_threeds2enrollcheck_response = "/com/zuora/opg/test/json/stripe_2/creditcard/threeds2enrollcheck/case_02/threeds2enrollcheck_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructThreeDS2EnrollCheckCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/setup_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "529")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_threeds2enrollcheck_response))
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][email]", "test@gmail.com"),
                                        Matchers.hasEntry("payment_method_data[billing_details][phone]", "123-1234-123"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ),
                                "check request payload")
                        .build());
    }
}
