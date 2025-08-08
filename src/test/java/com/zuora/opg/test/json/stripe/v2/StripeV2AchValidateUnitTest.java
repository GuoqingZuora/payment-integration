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

public class StripeV2AchValidateUnitTest extends OpgJsonBaseTest {
    private static final ZUtility zUtility = new ZUtility();

    @Test
    public void case_01_ItShouldApproveGoodAchPaymentMethod() {
        final String case_01_get_create_or_update_account_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_01/create_or_update_account_response.json";
        final String case_01_create_source_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_01/create_source_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAchForTest();
        paymentMethod.setSecondTokenId(null);


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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_get_create_or_update_account_response))
                        .build()
        );

        // transaction: CreateSource
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_OYD7cD98gTt05A/sources"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_create_source_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("lastHttpStatusCode"));
        assertEquals("ba_1Nl6grSC2hQ3y1elSgrpU1qr", responseMap.get("GatewayReferenceId"));
        assertEquals("cus_OYD7cD98gTt05A", responseMap.get("GatewaySecondReferenceId"));
        assertEquals("ba_1Nl6grSC2hQ3y1elSgrpU1qr", responseMap.get("GatewayResponseToken1"));
        assertEquals("cus_OYD7cD98gTt05A", responseMap.get("GatewayResponseToken2"));

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
                                        Matchers.hasEntry("email", ""),
                                        Matchers.hasEntry("phone", ""),
                                        Matchers.hasEntry("address[country]", ""),
                                        Matchers.hasEntry("address[state]", ""),
                                        Matchers.hasEntry("address[city]", ""),
                                        Matchers.hasEntry("address[line1]", ""),
                                        Matchers.hasEntry("address[line2]", ""),
                                        Matchers.hasEntry("address[postal_code]", "")
                                ),
                                "check request payload")
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_OYD7cD98gTt05A/sources"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("source[object]", "bank_account"),
                                        Matchers.hasEntry("source[country]", "US"),
                                        Matchers.hasEntry("source[currency]", "USD"),
                                        Matchers.hasEntry("source[account_number]", "000123456789"),
                                        Matchers.hasEntry("source[routing_number]", "110000000"),
                                        Matchers.hasEntry("source[account_holder_type]", "individual")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_02_ReturnUnknownZuoraResponseCodeWhenGatewayReturns5XXHttpStatus() {
        final String case_02_get_create_or_update_account_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_02/create_or_update_account_response.json";
        final String case_02_create_source_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_02/create_source_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAchForTest();
        paymentMethod.setSecondTokenId(null);


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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_get_create_or_update_account_response))
                        .build()
        );

        // transaction: CreateSource
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_OYD7cD98gTt05A/sources"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "529")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_create_source_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Unknown", responseMap.get("ZuoraResponseCode"));
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
                                        Matchers.hasEntry("email", ""),
                                        Matchers.hasEntry("phone", ""),
                                        Matchers.hasEntry("address[country]", ""),
                                        Matchers.hasEntry("address[state]", ""),
                                        Matchers.hasEntry("address[city]", ""),
                                        Matchers.hasEntry("address[line1]", ""),
                                        Matchers.hasEntry("address[line2]", ""),
                                        Matchers.hasEntry("address[postal_code]", "")
                                ),
                                "check request payload")
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_OYD7cD98gTt05A/sources"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("source[object]", "bank_account"),
                                        Matchers.hasEntry("source[country]", "US"),
                                        Matchers.hasEntry("source[currency]", "USD"),
                                        Matchers.hasEntry("source[account_number]", "000123456789"),
                                        Matchers.hasEntry("source[routing_number]", "110000000"),
                                        Matchers.hasEntry("source[account_holder_type]", "individual")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_03_ItShouldApproveGoodAchPaymentMethodOfSourceTypeWhenWrongTokensIdFormat() {
        final String case_03_get_create_or_update_account_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_03/create_or_update_account_response.json";
        final String case_03_create_source_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_03/create_source_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAchForTest();
        paymentMethod.setCcRefTxnPnrefID("testTokenId");
        paymentMethod.setSecondTokenId(null);

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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_03_get_create_or_update_account_response))
                        .build()
        );

        // transaction: CreateSource
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_OYD7cD98gTt05A/sources"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_03_create_source_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("lastHttpStatusCode"));
        assertEquals("ba_1Nl6grSC2hQ3y1elSgrpU1qr", responseMap.get("GatewayReferenceId"));
        assertEquals("cus_OYD7cD98gTt05A", responseMap.get("GatewaySecondReferenceId"));
        assertEquals("ba_1Nl6grSC2hQ3y1elSgrpU1qr", responseMap.get("GatewayResponseToken1"));
        assertEquals("cus_OYD7cD98gTt05A", responseMap.get("GatewayResponseToken2"));

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
                                        Matchers.hasEntry("email", ""),
                                        Matchers.hasEntry("phone", ""),
                                        Matchers.hasEntry("address[country]", ""),
                                        Matchers.hasEntry("address[state]", ""),
                                        Matchers.hasEntry("address[city]", ""),
                                        Matchers.hasEntry("address[line1]", ""),
                                        Matchers.hasEntry("address[line2]", ""),
                                        Matchers.hasEntry("address[postal_code]", "")
                                ),
                                "check request payload")
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_OYD7cD98gTt05A/sources"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("source[object]", "bank_account"),
                                        Matchers.hasEntry("source[country]", "US"),
                                        Matchers.hasEntry("source[currency]", "USD"),
                                        Matchers.hasEntry("source[account_number]", "000123456789"),
                                        Matchers.hasEntry("source[routing_number]", "110000000"),
                                        Matchers.hasEntry("source[account_holder_type]", "individual")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_04_ItShouldApproveGoodAchPaymentMethodOfSourceTypeWhenWrongSecondTokensIdFormat() {
        final String case_04_get_customer_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_04/get_customer_response.json";
        final String case_04_get_create_or_update_account_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_04/create_or_update_account_response.json";
        final String case_04_create_source_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_04/create_source_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAchForTest();
        paymentMethod.setCcRefTxnPnrefID("pm_1NyKTV4ZWiZesCzmoZsPQRJN");
        paymentMethod.setSecondTokenId("testSecondTokenId");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap= OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        // transaction: GetCustomerById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/testSecondTokenId"),
                        Matchers.hasEntry("METHOD", "GET"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "404")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_04_get_customer_response))
                        .build()
        );

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
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_04_get_create_or_update_account_response))
                        .build()
        );

        // transaction: CreateSource
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_OYD7cD98gTt05A/sources"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_04_create_source_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("lastHttpStatusCode"));
        assertEquals("ba_1Nl6grSC2hQ3y1elSgrpU1qr", responseMap.get("GatewayReferenceId"));
        assertEquals("cus_OYD7cD98gTt05A", responseMap.get("GatewaySecondReferenceId"));
        assertEquals("ba_1Nl6grSC2hQ3y1elSgrpU1qr", responseMap.get("GatewayResponseToken1"));
        assertEquals("cus_OYD7cD98gTt05A", responseMap.get("GatewayResponseToken2"));


        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/testSecondTokenId"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build(),

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
                                        Matchers.hasEntry("email", ""),
                                        Matchers.hasEntry("phone", ""),
                                        Matchers.hasEntry("address[country]", ""),
                                        Matchers.hasEntry("address[state]", ""),
                                        Matchers.hasEntry("address[city]", ""),
                                        Matchers.hasEntry("address[line1]", ""),
                                        Matchers.hasEntry("address[line2]", ""),
                                        Matchers.hasEntry("address[postal_code]", "")
                                ),
                                "check request payload")
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(8)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_OYD7cD98gTt05A/sources"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("source[object]", "bank_account"),
                                        Matchers.hasEntry("source[country]", "US"),
                                        Matchers.hasEntry("source[currency]", "USD"),
                                        Matchers.hasEntry("source[account_number]", "000123456789"),
                                        Matchers.hasEntry("source[routing_number]", "110000000"),
                                        Matchers.hasEntry("source[account_holder_type]", "individual")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_05_ValidTokenFormatWithInvalidCustomerIdThrowErrorAndFeatureEnabled() {
        final String case_05_get_customer_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_05/get_customer_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "Feature-ACHTokenizationSupport-Enabled=true;")
                        .build()));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAchForTest();
        paymentMethod.setCcRefTxnPnrefID("pm_1NyKTV4ZWiZesCzmoZsPQRJN");
        paymentMethod.setSecondTokenId("cus_PkWVPTVWba9Z8v");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap= OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        // transaction: GetCustomerById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_PkWVPTVWba9Z8v"),
                        Matchers.hasEntry("METHOD", "GET"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "404")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_05_get_customer_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertEquals("404", responseMap.get("lastHttpStatusCode"));
        assertEquals("Customer Account is not found", responseMap.get("GatewayResponseMessage"));
        assertEquals("404", responseMap.get("GatewayResponseCode"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_PkWVPTVWba9Z8v"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build());
    }

    @Test
    public void case_06_ValidTokenFormatWithDeletedCustomerIdThrowErrorAndFeatureEnabled() {
        final String case_06_get_customer_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_06/get_customer_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "Feature-ACHTokenizationSupport-Enabled=true;")
                        .build()));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAchForTest();
        paymentMethod.setCcRefTxnPnrefID("pm_1NyKTV4ZWiZesCzmoZsPQRJN");
        paymentMethod.setSecondTokenId("cus_PkNeARGWTlaPEo");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap= OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        // transaction: GetCustomerById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_PkNeARGWTlaPEo"),
                        Matchers.hasEntry("METHOD", "GET"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_06_get_customer_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("lastHttpStatusCode"));
        assertEquals("Customer Account is not in active state", responseMap.get("GatewayResponseMessage"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_PkNeARGWTlaPEo"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build());
    }

    @Test
    public void case_07_ValidTokenFormatWithValidCustomerIdAndInvalidPMIdThrowErrorAndFeatureEnabled() {
        final String case_07_get_customer_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_07/get_customer_response.json";
        final String case_07_get_pm_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_07/get_pm_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "Feature-ACHTokenizationSupport-Enabled=true;")
                        .build()));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAchForTest();
        paymentMethod.setCcRefTxnPnrefID("pm_1NyKTV4ZWiZesCzmoZsPQRJN");
        paymentMethod.setSecondTokenId("cus_PkWVPTVWba9Z8u");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap= OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        // transaction: GetCustomerById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_PkWVPTVWba9Z8u"),
                        Matchers.hasEntry("METHOD", "GET"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_07_get_customer_response))
                        .build()
        );

        // transaction: GetPaymentMethodById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_PkWVPTVWba9Z8u/payment_methods/pm_1NyKTV4ZWiZesCzmoZsPQRJN"),
                        Matchers.hasEntry("METHOD", "GET"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "404")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_07_get_pm_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertEquals("404", responseMap.get("lastHttpStatusCode"));
        assertEquals("[invalid_request_error] Invalid request", responseMap.get("GatewayResponseMessage"));
        assertEquals("404", responseMap.get("GatewayResponseCode"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_PkWVPTVWba9Z8u"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_PkWVPTVWba9Z8u/payment_methods/pm_1NyKTV4ZWiZesCzmoZsPQRJN"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build());
    }

    @Test
    public void case_08_ValidTokenFormatWithValidCustomerIdAndInvalidPMTypeThrowErrorAndFeatureEnabled() {
        final String case_08_get_customer_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_08/get_customer_response.json";
        final String case_08_get_pm_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_08/get_pm_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "Feature-ACHTokenizationSupport-Enabled=true;")
                        .build()));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAchForTest();
        paymentMethod.setCcRefTxnPnrefID("pm_1LfimdGT0RtLbTJ2I4d89RC7");
        paymentMethod.setSecondTokenId("cus_MOVoWV0qwqVujF");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap= OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        // transaction: GetCustomerById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_MOVoWV0qwqVujF"),
                        Matchers.hasEntry("METHOD", "GET"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_08_get_customer_response))
                        .build()
        );

        // transaction: GetPaymentMethodById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_MOVoWV0qwqVujF/payment_methods/pm_1LfimdGT0RtLbTJ2I4d89RC7"),
                        Matchers.hasEntry("METHOD", "GET"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_08_get_pm_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("lastHttpStatusCode"));
        assertEquals("Invalid Payment Method Type", responseMap.get("GatewayResponseMessage"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_MOVoWV0qwqVujF"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_MOVoWV0qwqVujF/payment_methods/pm_1LfimdGT0RtLbTJ2I4d89RC7"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build());
    }


    @Test
    public void case_09_ValidTokenFormatWithValidTokensShouldApproveGoodAchPMAndFeatureEnabled() {
        final String case_09_get_customer_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_09/get_customer_response.json";
        final String case_09_get_pm_response = "/com/zuora/opg/test/json/stripe_2/ach/validate/case_09/get_pm_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "Feature-ACHTokenizationSupport-Enabled=true;")
                        .build()));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAchForTest();
        paymentMethod.setCcRefTxnPnrefID("pm_1Ovcpl4ZWiZesCzmXazlWoSz");
        paymentMethod.setSecondTokenId("cus_MOVoWV0qwqVujF");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap= OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        // transaction: GetCustomerById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_MOVoWV0qwqVujF"),
                        Matchers.hasEntry("METHOD", "GET"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_09_get_customer_response))
                        .build()
        );

        // transaction: GetPaymentMethodById
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_MOVoWV0qwqVujF/payment_methods/pm_1Ovcpl4ZWiZesCzmXazlWoSz"),
                        Matchers.hasEntry("METHOD", "GET"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_09_get_pm_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);

        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("lastHttpStatusCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("pm_1Ovcpl4ZWiZesCzmXazlWoSz", responseMap.get("GatewayReferenceId"));
        assertEquals("cus_MOVoWV0qwqVujF", responseMap.get("GatewaySecondReferenceId"));
        assertEquals("pm_1Ovcpl4ZWiZesCzmXazlWoSz", responseMap.get("GatewayResponseToken1"));
        assertEquals("cus_MOVoWV0qwqVujF", responseMap.get("GatewayResponseToken2"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_MOVoWV0qwqVujF"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build(),

                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(6)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers/cus_MOVoWV0qwqVujF/payment_methods/pm_1Ovcpl4ZWiZesCzmXazlWoSz"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "GET"), "Method is GET")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build());
    }
}
