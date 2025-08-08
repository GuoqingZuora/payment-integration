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
import com.zuora.zbilling.paymentmethod.model.PaymentMethod;
import com.zuora.zbilling.setting.gateway.model.PaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.OpenPaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.connector.HttpConnectorCommonUtil;
import com.zuora.zpayment.openpaymentgateway.engine.constants.OpenPaymentGatewayConstants;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Map;

public class StripeV2CreditCardValidateUnitTest extends OpgJsonBaseTest {


    @Test
    public void case_01_ItShouldReturnApprovedWhenEveryThingWorksWell() throws Exception {
        final String case_01_response = "/com/zuora/opg/test/json/stripe_2/creditcard/validate/case_01/validate_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        paymentMethod.setCardSecurityCode("234");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);

        // Validation
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("seti_1Moj00GT0RtLbTJ2mz11xGAY", responseMap.get("GatewayReferenceId"));
        assertEquals("695148101731176", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));

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
                                        Matchers.hasEntry("payment_method_data[card][cvc]", "234"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("expand[]", "latest_attempt"),
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
    public void case_02_ItShouldReturnNTIForMotoTransaction() throws Exception {
        final String case_02_response = "/com/zuora/opg/test/json/stripe_2/creditcard/validate/case_02/validate_response_for_moto.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        paymentMethod.setCardSecurityCode("234");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.REQUEST_ZUORA_ECOMMERCE_INDICATOR, OpenPaymentGatewayConstants.ECOMMERCE_INDICATOR_MOTO);
        requestMap.put(OpenPaymentGatewayConstants.REQUEST_MIT_EXTRACT_SCP, OpenPaymentGatewayConstants.FLAG_YES);

        // Validation
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_response))
                        .build()
        );


        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("seti_1Moj4gGT0RtLbTJ2bBddtIk9", responseMap.get("GatewayReferenceId"));
        assertEquals("695148101731176", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));


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
                                        Matchers.hasEntry("payment_method_data[card][cvc]", "234"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("expand[]", "latest_attempt"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("payment_method_options[card][moto]", "true"),
                                        Matchers.hasEntry("usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_03_ReturnUnknownZuoraResponseCodeWhenGatewayReturns5XXHttpStatus() throws Exception {
        final String case_03_response = "/com/zuora/opg/test/json/stripe_2/creditcard/validate/case_03/validate_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        paymentMethod.setCardSecurityCode("234");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);

        // Validation
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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_03_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Unknown", responseMap.get("ZuoraResponseCode"));
        assertEquals("529", responseMap.get("GatewayResponseCode"));
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
                                        Matchers.hasEntry("payment_method_data[card][cvc]", "234"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("expand[]", "latest_attempt"),
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
