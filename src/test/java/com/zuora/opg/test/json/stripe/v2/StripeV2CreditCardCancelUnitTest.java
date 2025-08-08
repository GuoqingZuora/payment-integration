package com.zuora.opg.test.json.stripe.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.zuora.base.Decimal;
import com.zuora.billing.opg.test.common.OpgJsonBaseTest;
import com.zuora.billing.opg.test.json.stripe.StripeTestHelper;
import com.zuora.billing.opg.test.support.common.BillingAccountBuilder;
import com.zuora.billing.opg.test.support.common.CurrencyBuilder;
import com.zuora.billing.opg.test.support.common.HashMapBuilder;
import com.zuora.billing.opg.test.support.common.PaymentBuilder;
import com.zuora.billing.opg.test.support.engine.OpgRequestMapHelper;
import com.zuora.billing.opg.test.verify.ConnectorHeadersVerifierBuilder;
import com.zuora.zbilling.account.model.BillingAccount;
import com.zuora.zbilling.payment.model.Payment;
import com.zuora.zbilling.paymentmethod.model.PaymentMethod;
import com.zuora.zbilling.setting.gateway.model.PaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.OpenPaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.connector.HttpConnectorCommonUtil;
import com.zuora.zpayment.openpaymentgateway.engine.constants.OpenPaymentGatewayConstants;
import com.zuora.zpayment.openpaymentgateway.engine.templateengine.ZUtility;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Map;

public class StripeV2CreditCardCancelUnitTest extends OpgJsonBaseTest {

    private static final ZUtility zUtility = new ZUtility();


    @Test
    public void case_01_ItShouldPassPaymentIntentIdStoredInReferenceIdInDBAndSecondReferenceIdisNull() throws Exception {
        final String case_01_response = "/com/zuora/opg/test/json/stripe_2/creditcard/cancel/case_01/payment_cancel_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        final String idempotencyKey = zUtility.getUUID(64);
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("2.22"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructCancelCallRequestMap(payment);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("ReferenceId", "pi_3MyVz7SHKlqiaBI9021dV2fZ");
        requestMap.put("SecondPaymentReferenceId", null);


        // Validation
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3MyVz7SHKlqiaBI9021dV2fZ/cancel"),
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
        assertEquals("pi_3MyVz7SHKlqiaBI9021dV2fZ", responseMap.get("GatewayReferenceId"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(9)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3MyVz7SHKlqiaBI9021dV2fZ/cancel"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build());
    }

    @Test
    public void case_02_ItShouldPassPaymentIntentIdStoredInSecondReferenceIdInDB() throws Exception {
        final String case_01_response = "/com/zuora/opg/test/json/stripe_2/creditcard/cancel/case_01/payment_cancel_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        final String idempotencyKey = zUtility.getUUID(64);
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("2.22"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructCancelCallRequestMap(payment);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("ReferenceId", "ch_3MyrXmSHKlqiaBI91561aMgW");
        requestMap.put("SecondPaymentReferenceId", "pi_3MyVz7SHKlqiaBI9021dV2fZ");


        // Validation
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3MyVz7SHKlqiaBI9021dV2fZ/cancel"),
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
        assertEquals("pi_3MyVz7SHKlqiaBI9021dV2fZ", responseMap.get("GatewayReferenceId"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(9)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3MyVz7SHKlqiaBI9021dV2fZ/cancel"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build());
    }

    @Test
    public void case_03_ItShouldThrowErrorWhenPaymentIntentIdIsNotPresentInDBAndDifferentIdIsStoredInReferenceIdAndSecondReferenceId() throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        final String idempotencyKey = zUtility.getUUID(64);
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("2.22"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructCancelCallRequestMap(payment);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("ReferenceId", "ch_3MyrXmSHKlqiaBI91561aMgW");
        requestMap.put("SecondPaymentReferenceId", "ch_3MyrXmSHKlqiaBI91561aMgW");

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertEquals("ZuoraValidationError", responseMap.get("GatewayResponseCode"));
        assertEquals("{isValidPaymentIntentId = false, ReferenceId = ch_3MyrXmSHKlqiaBI91561aMgW, SecondPaymentReferenceId = ch_3MyrXmSHKlqiaBI91561aMgW}",  responseMap.get("GatewayRequestString"));
        assertEquals("Cancellation request not sent to gateway as payment intent id is not present", responseMap.get("GatewayResponseMessage"));
    }

    @Test
    public void case_04_ItShouldThrowErrorWhenPaymentIntentIdIsNotPresentInDB() throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        final String idempotencyKey = zUtility.getUUID(64);
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("2.22"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructCancelCallRequestMap(payment);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("ReferenceId", null);
        requestMap.put("SecondPaymentReferenceId", null);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Failed", responseMap.get("ZuoraResponseCode"));
        assertEquals("ZuoraValidationError", responseMap.get("GatewayResponseCode"));
        assertEquals("{isValidPaymentIntentId = false, ReferenceId = , SecondPaymentReferenceId = }",  responseMap.get("GatewayRequestString"));
        assertEquals("Cancellation request not sent to gateway as payment intent id is not present", responseMap.get("GatewayResponseMessage"));
    }

    @Test
    public void case_04_ReturnUnknownZuoraResponseCodeWhenGatewayReturns5XXHttpStatus() throws Exception {
        final String case_04_response = "/com/zuora/opg/test/json/stripe_2/creditcard/cancel/case_04/payment_cancel_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        final String idempotencyKey = zUtility.getUUID(64);
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("2.22"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructCancelCallRequestMap(payment);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put("ReferenceId", "pi_3MyVz7SHKlqiaBI9021dV2fZ");
        requestMap.put("SecondPaymentReferenceId", null);

        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3MyVz7SHKlqiaBI9021dV2fZ/cancel"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION)
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "529")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_04_response))
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
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents/pi_3MyVz7SHKlqiaBI9021dV2fZ/cancel"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .build());
    }


}
