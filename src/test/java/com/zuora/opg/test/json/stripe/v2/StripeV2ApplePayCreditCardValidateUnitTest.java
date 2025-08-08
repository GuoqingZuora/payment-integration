package com.zuora.opg.test.json.stripe.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.zuora.billing.opg.test.common.OpgJsonBaseTest;
import com.zuora.billing.opg.test.json.stripe.StripeRequestPayloadExtractor;
import com.zuora.billing.opg.test.json.stripe.StripeTestHelper;
import com.zuora.billing.opg.test.support.common.HashMapBuilder;
import com.zuora.billing.opg.test.support.engine.OpgRequestMapHelper;
import com.zuora.billing.opg.test.verify.ConnectorHeadersVerifierBuilder;
import com.zuora.enums.CreditCardType;
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

public class StripeV2ApplePayCreditCardValidateUnitTest extends OpgJsonBaseTest {
    private static final ZUtility zUtility = new ZUtility();

    @Test
    public void case_01_ItShouldReturnApprovedWhenEveryThingWorksWellForVisa() throws Exception {
        final String paymentIntentResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_01/validate_paymentIntentResponse_visa.json";
        final String voidResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_01/validate_voidResponse_visa.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with Visa Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodVisaApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "electronicCommerceIndicator", "5");

        // CreateAndConfirmPaymentIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(paymentIntentResponse))
                        .build()
        );

        // Void
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3N3IuNI3dKT3t0iK04DuoL9N/cancel"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(voidResponse))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("pm_1N3IuNI3dKT3t0iKQTmqftLB", responseMap.get("GatewayReferenceId"));
        assertEquals("901151114810210", responseMap.get("MITReceivedTXID"));
        assertEquals("", responseMap.get("AuthTransactionId")); //AuthTransactionId should be null(we should not return AuthTransactionId after making void )
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "1111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "apple_pay"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][cryptogram]", "A6Vc1bMADSDLcMV5rsB0MAACAAA"),
                                        Matchers.hasEntry("amount", "100"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("capture_method", "manual"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][electronic_commerce_indicator]", "05"),
                                        Matchers.hasEntry("setup_future_usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ),
                                "check request payload")
                        .build(),
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3N3IuNI3dKT3t0iK04DuoL9N/cancel"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("REQUEST_BODY", ""))
                        .build());
    }

    @Test
    public void case_02_ItShouldReturnFailedWhenInvalidExpiryYear() throws Exception {
        final String case_02_paymentIntentResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_02/CreateAndConfirmPaymentIntent_error.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildCreditCardWithInvalidExpiryYearForTest();
        paymentMethod.setCreditCardType(CreditCardType.AppleVisa);
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "electronicCommerceIndicator", "5");

        // CreateAndConfirmPaymentIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "402")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_paymentIntentResponse))
                        .build()
        );


        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertEquals("402", responseMap.get("GatewayResponseCode"));
        assertEquals("[card_error/invalid_expiry_year] Your card's expiration year is invalid.", responseMap.get("GatewayResponseMessage"));
        assertTrue(StringUtils.isBlank(responseMap.get("GatewayReferenceId")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedTXID")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("", responseMap.get("AuthTransactionId")); //AuthTransactionId should be null(we should not return AuthTransactionId after making void )

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2022"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "1111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2022"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "apple_pay"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][cryptogram]", "A6Vc1bMADSDLcMV5rsB0MAACAAA"),
                                        Matchers.hasEntry("amount", "100"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("capture_method", "manual"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][electronic_commerce_indicator]", "05"),
                                        Matchers.hasEntry("setup_future_usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_03_ItShouldReturnApprovedWhenEveryThingWorksWellForMasterCard() throws Exception {
        final String paymentIntentResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_03/validate_paymentIntentResponse_masterCard.json";
        final String voidResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_03/validate_voidResponse_masterCard.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with MasterCard Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodMasterCardApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        // Check for ECI = 07
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "electronicCommerceIndicator", "7");

        // CreateAndConfirmPaymentIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(paymentIntentResponse))
                        .build()
        );

        // Void
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3NGfP4GT0RtLbTJ20rjMUGMf/cancel"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(voidResponse))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("pm_1NGfP4GT0RtLbTJ2V15A89FJ", responseMap.get("GatewayReferenceId"));
        assertEquals("MCCIYQ1MK0608", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("", responseMap.get("AuthTransactionId")); //AuthTransactionId should be null(we should not return AuthTransactionId after making void )

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "4444"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "5555555555554444"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "apple_pay"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][cryptogram]", "A6Vc1bMADSDLcMV5rsB0MAACAAA"),
                                        Matchers.hasEntry("amount", "100"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("capture_method", "manual"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][electronic_commerce_indicator]", "07"),
                                        Matchers.hasEntry("setup_future_usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ),
                                "check request payload")
                        .build(),
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3NGfP4GT0RtLbTJ20rjMUGMf/cancel"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("REQUEST_BODY", ""))
                        .build());
    }

    @Test
    public void case_04_ItShouldReturnApprovedWhenEveryThingWorksWellForAmex() throws Exception {
        final String paymentIntentResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_04/validate_paymentIntentResponse_amex.json";
        final String voidResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_04/validate_voidResponse_amex.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with American Express Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAmexApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "electronicCommerceIndicator", "5");

        // CreateAndConfirmPaymentIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(paymentIntentResponse))
                        .build()
        );

        // Void
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3NGgJkGT0RtLbTJ23c7hXbyC/cancel"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(voidResponse))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("pm_1NGgJkGT0RtLbTJ2LZlSCadr", responseMap.get("GatewayReferenceId"));
        assertEquals("656850841024977", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("", responseMap.get("AuthTransactionId")); //AuthTransactionId should be null(we should not return AuthTransactionId after making void )

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "0005"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "378282246310005"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "apple_pay"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][cryptogram]", "A6Vc1bMADSDLcMV5rsB0MAACAAA"),
                                        Matchers.hasEntry("amount", "100"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("capture_method", "manual"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][electronic_commerce_indicator]", "05"),
                                        Matchers.hasEntry("setup_future_usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ),
                                "check request payload")
                        .build(),
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3NGgJkGT0RtLbTJ23c7hXbyC/cancel"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("REQUEST_BODY", ""))
                        .build());
    }

    @Test
    public void case_05_ItShouldReturnApprovedWhenEveryThingWorksWellForDiscover() throws Exception {
        final String paymentIntentResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_05/validate_paymentIntentResponse_discover.json";
        final String voidResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_05/validate_voidResponse_discover.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with Discover Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodDiscoverApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "electronicCommerceIndicator", "5");

        // CreateAndConfirmPaymentIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(paymentIntentResponse))
                        .build()
        );

        // Void
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3NGgOXGT0RtLbTJ23GPCd9DZ/cancel"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(voidResponse))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("pm_1NGgOXGT0RtLbTJ29wlQNh8w", responseMap.get("GatewayReferenceId"));
        assertEquals("577254731141131", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("", responseMap.get("AuthTransactionId")); //AuthTransactionId should be null(we should not return AuthTransactionId after making void )

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "1117"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "6011111111111117"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "apple_pay"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][cryptogram]", "A6Vc1bMADSDLcMV5rsB0MAACAAA"),
                                        Matchers.hasEntry("amount", "100"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("capture_method", "manual"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][electronic_commerce_indicator]", "05"),
                                        Matchers.hasEntry("setup_future_usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ),
                                "check request payload")
                        .build(),
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3NGgOXGT0RtLbTJ23GPCd9DZ/cancel"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("REQUEST_BODY", ""))
                        .build());
    }

    @Test
    public void case_06_ItShouldReturnApprovedWhenEveryThingWorksWellForJCB() throws Exception {
        final String paymentIntentResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_06/validate_paymentIntentResponse_jcb.json";
        final String voidResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_06/validate_voidResponse_jcb.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with JCB Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodJCBApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "electronicCommerceIndicator", "5");

        // CreateAndConfirmPaymentIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(paymentIntentResponse))
                        .build()
        );

        // Void
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3NGgYdGT0RtLbTJ218sNDSgB/cancel"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(voidResponse))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("pm_1NGgYdGT0RtLbTJ26q8QXdNW", responseMap.get("GatewayReferenceId"));
        assertEquals("507780868675715", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("", responseMap.get("AuthTransactionId")); //AuthTransactionId should be null(we should not return AuthTransactionId after making void )

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "0505"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "3566002020360505"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "apple_pay"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][cryptogram]", "A6Vc1bMADSDLcMV5rsB0MAACAAA"),
                                        Matchers.hasEntry("amount", "100"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("capture_method", "manual"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][electronic_commerce_indicator]", "05"),
                                        Matchers.hasEntry("setup_future_usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ),
                                "check request payload")
                        .build(),
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3NGgYdGT0RtLbTJ218sNDSgB/cancel"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("REQUEST_BODY", ""))
                        .build());
    }

    @Test
    public void case_07_ItShouldReturnAuthTransactionIdWhenStandaloneAuthIsTrue() throws Exception {
        final String paymentIntentResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_07/validate_paymentIntentResponse_visa.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "StandaloneAuth=true;") //Default Auth Amount is 1
                        .build()));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        //Test with Visa Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodVisaApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "electronicCommerceIndicator", "5");

        // CreateAndConfirmPaymentIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(paymentIntentResponse))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("pm_1N3IuNI3dKT3t0iKQTmqftLB", responseMap.get("GatewayReferenceId"));
        assertEquals("901151114810210", responseMap.get("MITReceivedTXID"));
        assertEquals("pi_3N3IuNI3dKT3t0iK04DuoL9N", responseMap.get("AuthTransactionId")); //AuthTransactionId (we should always return AuthTransactionId when StandaloneAuth is true)
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .notMatches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3N3IuNI3dKT3t0iK04DuoL9N/cancel"),"we should not void the Auth ")
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
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "apple_pay"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][cryptogram]", "A6Vc1bMADSDLcMV5rsB0MAACAAA"),
                                        Matchers.hasEntry("amount", "100"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("capture_method", "manual"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][electronic_commerce_indicator]", "05"),
                                        Matchers.hasEntry("setup_future_usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_08_ItShouldReturnNotAuthTransactionIdWhenStandaloneAuthIsFalse() throws Exception {
        final String paymentIntentResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_10/validate_paymentIntentResponse_visa.json";
        final String voidResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_10/validate_voidResponse_visa.json";

        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "StandaloneAuth=false;") //Default Auth Amount is 1
                        .build()));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with Visa Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodVisaApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "electronicCommerceIndicator", "5");

        // CreateAndConfirmPaymentIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(paymentIntentResponse))
                        .build()
        );

        // Void
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3N3IuNI3dKT3t0iK04DuoL9N/cancel"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(voidResponse))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("pm_1N3IuNI3dKT3t0iKQTmqftLB", responseMap.get("GatewayReferenceId"));
        assertEquals("901151114810210", responseMap.get("MITReceivedTXID"));
        assertEquals("", responseMap.get("AuthTransactionId")); //AuthTransactionId should be null(we should not return AuthTransactionId after making void )
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "1111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "apple_pay"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][cryptogram]", "A6Vc1bMADSDLcMV5rsB0MAACAAA"),
                                        Matchers.hasEntry("amount", "100"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("capture_method", "manual"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][electronic_commerce_indicator]", "05"),
                                        Matchers.hasEntry("setup_future_usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ),
                                "check request payload")
                        .build(),
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3N3IuNI3dKT3t0iK04DuoL9N/cancel"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("REQUEST_BODY", ""))
                        .build());
    }
    @Test
    public void case_09_ItShouldReturnApprovedForVisaWithoutGatewayOptionEcommerceIndicator() throws Exception {
        final String paymentIntentResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_01/validate_paymentIntentResponse_visa.json";
        final String voidResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_01/validate_voidResponse_visa.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with Visa Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodVisaApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);

        // CreateAndConfirmPaymentIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(paymentIntentResponse))
                        .build()
        );

        // Void
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3N3IuNI3dKT3t0iK04DuoL9N/cancel"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(voidResponse))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("pm_1N3IuNI3dKT3t0iKQTmqftLB", responseMap.get("GatewayReferenceId"));
        assertEquals("901151114810210", responseMap.get("MITReceivedTXID"));
        assertEquals("", responseMap.get("AuthTransactionId")); //AuthTransactionId should be null(we should not return AuthTransactionId after making void )
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "1111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "apple_pay"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][cryptogram]", "A6Vc1bMADSDLcMV5rsB0MAACAAA"),
                                        Matchers.hasEntry("amount", "100"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("capture_method", "manual"),
                                        // Without gatewayOptions ElectronicCommerceIndicator
                                        Matchers.not(Matchers.hasKey("payment_method_options[card][network_token][electronic_commerce_indicator]")),
                                        Matchers.hasEntry("setup_future_usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ),
                                "check request payload")
                        .build(),
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3N3IuNI3dKT3t0iK04DuoL9N/cancel"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("REQUEST_BODY", ""))
                        .build());
    }

    @Test
    public void case_11_ItShouldReturnApprovedWhenEveryThingWorksWellWithPaymentDescription() throws Exception {
        final String paymentIntentResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_11/validate_paymentIntentResponse_visa.json";
        final String voidResponse = "/com/zuora/opg/test/json/stripe_2/applepaycreditcard/validate/case_11/validate_voidResponse_visa.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with Visa Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodVisaApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTestWithExtraParams(paymentGateway,
                        HashMapBuilder.<String, String>builder()
                                .put("PaymentDescription", "Test Payment Description")
                                .build()));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "electronicCommerceIndicator", "5");

        // CreateAndConfirmPaymentIntent
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(paymentIntentResponse))
                        .build()
        );

        // Void
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3N3IuNI3dKT3t0iK04DuoL9N/cancel"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(voidResponse))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("pm_1N3IuNI3dKT3t0iKQTmqftLB", responseMap.get("GatewayReferenceId"));
        assertEquals("901151114810210", responseMap.get("MITReceivedTXID"));
        assertEquals("", responseMap.get("AuthTransactionId")); //AuthTransactionId should be null(we should not return AuthTransactionId after making void )
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][last4]", "1111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][network_token][tokenization_method]", "apple_pay"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][cryptogram]", "A6Vc1bMADSDLcMV5rsB0MAACAAA"),
                                        Matchers.hasEntry("amount", "100"),
                                        Matchers.hasEntry("currency", "USD"),
                                        Matchers.hasEntry("capture_method", "manual"),
                                        Matchers.hasEntry("description", "Test Payment Description"),
                                        Matchers.hasEntry("payment_method_options[card][network_token][electronic_commerce_indicator]", "05"),
                                        Matchers.hasEntry("setup_future_usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ),
                                "check request payload")
                        .build(),
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3N3IuNI3dKT3t0iK04DuoL9N/cancel"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("REQUEST_BODY", ""))
                        .build());
    }
}
