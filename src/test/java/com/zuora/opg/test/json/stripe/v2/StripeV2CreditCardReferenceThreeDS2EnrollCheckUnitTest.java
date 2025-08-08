package com.zuora.opg.test.json.stripe.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.zuora.billing.opg.test.common.OpgJsonBaseTest;
import com.zuora.billing.opg.test.json.stripe.StripeRequestPayloadExtractor;
import com.zuora.billing.opg.test.json.stripe.StripeTestHelper;
import com.zuora.billing.opg.test.support.common.CurrencyBuilder;
import com.zuora.billing.opg.test.support.common.HashMapBuilder;
import com.zuora.billing.opg.test.support.engine.OpgRequestMapHelper;
import com.zuora.billing.opg.test.verify.ConnectorHeadersVerifierBuilder;
import com.zuora.enums.PaymentMethodType;
import com.zuora.zbilling.account.model.Country;
import com.zuora.zbilling.paymentmethod.model.PaymentMethod;
import com.zuora.zbilling.setting.gateway.model.PaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.OpenPaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.connector.HttpConnectorCommonUtil;
import com.zuora.zpayment.openpaymentgateway.engine.constants.OpenPaymentGatewayConstants;
import com.zuora.zpayment.openpaymentgateway.engine.templateengine.ZUtility;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Map;

public class StripeV2CreditCardReferenceThreeDS2EnrollCheckUnitTest extends OpgJsonBaseTest {
    private static final ZUtility zUtility = new ZUtility();

    @Test
    public void case_01_ThreeDs2EnrollCheckSuccessful() {
        final String case_01_createcustomer_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/threeds2enrollcheck/case_01/createcustomer_response.json";
        final String case_01_threeds2enrollcheck_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/threeds2enrollcheck/case_01/threeds2enrollcheck_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCCRefForTest();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructThreeDS2EnrollCheckCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_createcustomer_response))
                        .build()
        );


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
                                        Matchers.hasEntry("description", "Auto customer by on session payment $paymentIntentId")
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][number]", ""),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", ""),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", ""),
                                        Matchers.hasEntry("customer", "cus_OYDFA8MQ2bLtBO"),
                                        Matchers.hasEntry("usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_02_ReturnUnknownZuoraResponseCodeWhenGatewayReturns5XXHttpStatus() {
        final String case_02_createcustomer_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/threeds2enrollcheck/case_02/createcustomer_response.json";
        final String case_02_threeds2enrollcheck_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/threeds2enrollcheck/case_02/threeds2enrollcheck_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCCRefForTest();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructThreeDS2EnrollCheckCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/customers"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_createcustomer_response))
                        .build()
        );


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
                                        Matchers.hasEntry("description", "Auto customer by on session payment $paymentIntentId")
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][number]", ""),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", ""),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", ""),
                                        Matchers.hasEntry("customer", "cus_OYDFA8MQ2bLtBO"),
                                        Matchers.hasEntry("usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_03_ThreeDs2EnrollCheckSuccessful_withNonINRCurrencyWhenFeatureStripeCustomerAddressEnabledFlagIsTrue() {
        final String case_01_createcustomer_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/threeds2enrollcheck/case_01/createcustomer_response.json";
        final String case_01_threeds2enrollcheck_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/threeds2enrollcheck/case_01/threeds2enrollcheck_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "Feature-StripeCustomerAddress-Enabled=true;")
                        .build()));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        paymentMethod.setMethodType(PaymentMethodType.CreditCardReferenceTransaction);
        paymentMethod.setCcRefTxnPnrefID("card_1JQZtl4ZWiZesCzmzXas7qd7");
        paymentMethod.setSecondTokenId("cus_K4jQ4dzKx97XMD");
        paymentMethod.setCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"));
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructThreeDS2EnrollCheckCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        setUpExpectingConnectorResponse("https://api.stripe.com/v1/customers",case_01_createcustomer_response);
        setUpExpectingConnectorResponse("https://api.stripe.com/v1/setup_intents",case_01_threeds2enrollcheck_response);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("seti_1Nl6pVSC2hQ3y1eli8IArVIm", responseMap.get("GatewayReferenceId"));
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
                                        Matchers.hasEntry("description", "Auto customer by on session payment $paymentIntentId"),
                                        Matchers.hasEntry("name", "TestName11 TestName22"),
                                        Matchers.hasEntry("address[line1]", "Add11"),
                                        Matchers.hasEntry("address[line2]", "Add22"),
                                        Matchers.hasEntry("address[postal_code]", "11111"),
                                        Matchers.hasEntry("address[city]", "TestCity11"),
                                        Matchers.hasEntry("address[state]", "Delaware"),
                                        Matchers.hasEntry("address[country]", "US")
                                ), "check /v1/customer payload")
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("customer", "cus_OYDFA8MQ2bLtBO"),
                                        Matchers.hasEntry("usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ), "check /v1/setup_intents payload")
                        .build());
    }

    @Test
    public void case_04_ThreeDs2EnrollCheckSuccessful_withNonINRCurrency_withBillToInfoWhenFeatureStripeCustomerAddressEnabledFlagIsTrue() {
        final String case_01_createcustomer_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/threeds2enrollcheck/case_01/createcustomer_response.json";
        final String case_01_threeds2enrollcheck_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/threeds2enrollcheck/case_01/threeds2enrollcheck_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "Feature-StripeCustomerAddress-Enabled=true;")
                        .build()));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        Country country = paymentMethod.getCreditCardCountry();
        paymentMethod.setCreditCardHolderName("");
        paymentMethod.setCreditCardAddress1("");
        paymentMethod.setCreditCardAddress2("");
        paymentMethod.setCreditCardPostalCode("");
        paymentMethod.setCreditCardCity("");
        paymentMethod.setCreditCardState("");
        paymentMethod.setCreditCardCountry(null);
        paymentMethod.setMethodType(PaymentMethodType.CreditCardReferenceTransaction);
        paymentMethod.setCcRefTxnPnrefID("card_1JQZtl4ZWiZesCzmzXas7qd7");
        paymentMethod.setSecondTokenId("cus_K4jQ4dzKx97XMD");
        paymentMethod.setCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"));
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructThreeDS2EnrollCheckCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put(OpenPaymentGatewayConstants.BILL_TO_CONTACT_FIRST_NAME, "billToFirstName");
        requestMap.put(OpenPaymentGatewayConstants.BILL_TO_CONTACT_ADDRESS_1, "billToAdd1");
        requestMap.put(OpenPaymentGatewayConstants.BILL_TO_CONTACT_ADDRESS_2, "billToAdd2");
        requestMap.put(OpenPaymentGatewayConstants.BILL_TO_CONTACT_ZIP, "2222");
        requestMap.put(OpenPaymentGatewayConstants.BILL_TO_CONTACT_CITY, "billToCity");
        requestMap.put(OpenPaymentGatewayConstants.BILL_TO_CONTACT_STATE, "billToState");
        requestMap.put(OpenPaymentGatewayConstants.BILL_TO_CONTACT_COUNTRY, String.valueOf(country.getThreeDigitISOCode()));
        setUpExpectingConnectorResponse("https://api.stripe.com/v1/customers",case_01_createcustomer_response);
        setUpExpectingConnectorResponse("https://api.stripe.com/v1/setup_intents",case_01_threeds2enrollcheck_response);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("seti_1Nl6pVSC2hQ3y1eli8IArVIm", responseMap.get("GatewayReferenceId"));
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
                                        Matchers.hasEntry("description", "Auto customer by on session payment $paymentIntentId"),
                                        Matchers.hasEntry("name", "billToFirstName"),
                                        Matchers.hasEntry("address[line1]", "billToAdd1"),
                                        Matchers.hasEntry("address[line2]", "billToAdd2"),
                                        Matchers.hasEntry("address[postal_code]", "2222"),
                                        Matchers.hasEntry("address[city]", "billToCity"),
                                        Matchers.hasEntry("address[state]", "billToState"),
                                        Matchers.hasEntry("address[country]", "US")
                                ), "check /v1/customer payload")
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("customer", "cus_OYDFA8MQ2bLtBO"),
                                        Matchers.hasEntry("usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ), "check /v1/setup_intents payload")
                        .build());
    }

    @Test
    public void case_05_ThreeDs2EnrollCheckSuccessful_withNonINRCurrency_withSoldToInfoWhenFeatureStripeCustomerAddressEnabledFlagIsTrue() {
        final String case_01_createcustomer_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/threeds2enrollcheck/case_01/createcustomer_response.json";
        final String case_01_threeds2enrollcheck_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/threeds2enrollcheck/case_01/threeds2enrollcheck_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "Feature-StripeCustomerAddress-Enabled=true;")
                        .build()));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        Country country = paymentMethod.getCreditCardCountry();
        paymentMethod.setCreditCardHolderName("");
        paymentMethod.setCreditCardAddress1("");
        paymentMethod.setCreditCardAddress2("");
        paymentMethod.setCreditCardPostalCode("");
        paymentMethod.setCreditCardCity("");
        paymentMethod.setCreditCardState("");
        paymentMethod.setCreditCardCountry(null);
        paymentMethod.setMethodType(PaymentMethodType.CreditCardReferenceTransaction);
        paymentMethod.setCcRefTxnPnrefID("card_1JQZtl4ZWiZesCzmzXas7qd7");
        paymentMethod.setSecondTokenId("cus_K4jQ4dzKx97XMD");
        paymentMethod.setCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"));
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructThreeDS2EnrollCheckCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put(OpenPaymentGatewayConstants.SOLD_TO_CONTACT_FIRST_NAME, "soldToFirstName");
        requestMap.put(OpenPaymentGatewayConstants.SOLD_TO_CONTACT_ADDRESS_1, "soldToAdd1");
        requestMap.put(OpenPaymentGatewayConstants.SOLD_TO_CONTACT_ADDRESS_2, "soldToAdd2");
        requestMap.put(OpenPaymentGatewayConstants.SOLD_TO_CONTACT_ZIP, "3333");
        requestMap.put(OpenPaymentGatewayConstants.SOLD_TO_CONTACT_CITY, "soldToCity");
        requestMap.put(OpenPaymentGatewayConstants.SOLD_TO_CONTACT_STATE, "soldToState");
        requestMap.put(OpenPaymentGatewayConstants.SOLD_TO_CONTACT_COUNTRY, String.valueOf(country.getThreeDigitISOCode()));
        setUpExpectingConnectorResponse("https://api.stripe.com/v1/customers",case_01_createcustomer_response);
        setUpExpectingConnectorResponse("https://api.stripe.com/v1/setup_intents",case_01_threeds2enrollcheck_response);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("seti_1Nl6pVSC2hQ3y1eli8IArVIm", responseMap.get("GatewayReferenceId"));
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
                                        Matchers.hasEntry("description", "Auto customer by on session payment $paymentIntentId"),
                                        Matchers.hasEntry("name", "soldToFirstName"),
                                        Matchers.hasEntry("address[line1]", "soldToAdd1"),
                                        Matchers.hasEntry("address[line2]", "soldToAdd2"),
                                        Matchers.hasEntry("address[postal_code]", "3333"),
                                        Matchers.hasEntry("address[city]", "soldToCity"),
                                        Matchers.hasEntry("address[state]", "soldToState"),
                                        Matchers.hasEntry("address[country]", "US")
                                ), "check /v1/customer payload")
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("customer", "cus_OYDFA8MQ2bLtBO"),
                                        Matchers.hasEntry("usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ), "check /v1/setup_intents payload")
                        .build());
    }

    @Test
    public void case_06_ThreeDs2EnrollCheckSuccessful_withNonINRCurrency_withOutAddressInfoWhenFeatureStripeCustomerAddressEnabledFlagIsFalse() {
        final String case_01_createcustomer_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/threeds2enrollcheck/case_01/createcustomer_response.json";
        final String case_01_threeds2enrollcheck_response = "/com/zuora/opg/test/json/stripe_2/creditcardreference/threeds2enrollcheck/case_01/threeds2enrollcheck_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "Feature-StripeCustomerAddress-Enabled=false;")
                        .build()));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        Country country = paymentMethod.getCreditCardCountry();
        paymentMethod.setCreditCardHolderName("");
        paymentMethod.setCreditCardAddress1("");
        paymentMethod.setCreditCardAddress2("");
        paymentMethod.setCreditCardPostalCode("");
        paymentMethod.setCreditCardCity("");
        paymentMethod.setCreditCardState("");
        paymentMethod.setCreditCardCountry(null);
        paymentMethod.setMethodType(PaymentMethodType.CreditCardReferenceTransaction);
        paymentMethod.setCcRefTxnPnrefID("card_1JQZtl4ZWiZesCzmzXas7qd7");
        paymentMethod.setSecondTokenId("cus_K4jQ4dzKx97XMD");
        paymentMethod.setCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"));
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructThreeDS2EnrollCheckCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put(OpenPaymentGatewayConstants.SOLD_TO_CONTACT_FIRST_NAME, "soldToFirstName");
        requestMap.put(OpenPaymentGatewayConstants.SOLD_TO_CONTACT_ADDRESS_1, "soldToAdd1");
        requestMap.put(OpenPaymentGatewayConstants.SOLD_TO_CONTACT_ADDRESS_2, "soldToAdd2");
        requestMap.put(OpenPaymentGatewayConstants.SOLD_TO_CONTACT_ZIP, "3333");
        requestMap.put(OpenPaymentGatewayConstants.SOLD_TO_CONTACT_CITY, "soldToCity");
        requestMap.put(OpenPaymentGatewayConstants.SOLD_TO_CONTACT_STATE, "soldToState");
        requestMap.put(OpenPaymentGatewayConstants.SOLD_TO_CONTACT_COUNTRY, String.valueOf(country.getThreeDigitISOCode()));
        setUpExpectingConnectorResponse("https://api.stripe.com/v1/customers",case_01_createcustomer_response);
        setUpExpectingConnectorResponse("https://api.stripe.com/v1/setup_intents",case_01_threeds2enrollcheck_response);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("seti_1Nl6pVSC2hQ3y1eli8IArVIm", responseMap.get("GatewayReferenceId"));
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
                                        Matchers.hasEntry("description", "Auto customer by on session payment $paymentIntentId"),
                                        Matchers.not("name"),
                                        Matchers.not("address[line1]"),
                                        Matchers.not("address[line2]"),
                                        Matchers.not("address[postal_code]"),
                                        Matchers.not("address[city]"),
                                        Matchers.not("address[state]"),
                                        Matchers.not("address[country]")
                                ), "check /v1/customer payload")
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
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("customer", "cus_OYDFA8MQ2bLtBO"),
                                        Matchers.hasEntry("usage", "off_session"),
                                        Matchers.hasEntry("confirm", "true")
                                ), "check /v1/setup_intents payload")
                        .build());
    }

    private void setUpExpectingConnectorResponse(String url, String response){
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", url),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(response))
                        .build()
        );
    }
}