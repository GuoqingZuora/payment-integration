package com.zuora.opg.test.json.stripe.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.zuora.base.Decimal;
import com.zuora.billing.opg.test.common.OpgJsonBaseTest;
import com.zuora.billing.opg.test.json.stripe.StripeTestHelper;
import com.zuora.billing.opg.test.support.common.BillingAccountBuilder;
import com.zuora.billing.opg.test.support.common.CurrencyBuilder;
import com.zuora.billing.opg.test.support.common.HashMapBuilder;
import com.zuora.billing.opg.test.support.common.PaymentBuilder;
import com.zuora.billing.opg.test.support.engine.OpgRequestMapHelper;
import com.zuora.zbilling.account.model.BillingAccount;
import com.zuora.zbilling.payment.model.Payment;
import com.zuora.zbilling.paymentmethod.model.PaymentMethod;
import com.zuora.zbilling.setting.gateway.model.PaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.OpenPaymentGateway;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Map;

public class StripeV2ApplePayCreditCardValidateLiveTest extends OpgJsonBaseTest {

    @Test
    public void validate_ItShouldReturnApprovedWhenEveryThingWorksWellForVisa() throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(true,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with Visa Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodVisaApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForLiveTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertTrue(StringUtils.startsWith(responseMap.get("GatewayReferenceId"), "pm_"));
        assertTrue(StringUtils.isNotBlank(responseMap.get("MITReceivedTXID")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
    }

    @Test
    public void validate_ItShouldReturnApprovedWhenEveryThingWorksWellForMasterCard() throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(true,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with MasterCard Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodMasterCardApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForLiveTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertTrue(StringUtils.startsWith(responseMap.get("GatewayReferenceId"), "pm_"));
        assertTrue(StringUtils.isNotBlank(responseMap.get("MITReceivedTXID")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
    }


    @Test
    public void validate_ItShouldReturnApprovedWhenEveryThingWorksWellForAmex() throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(true,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with American Express Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAmexApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForLiveTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertTrue(StringUtils.startsWith(responseMap.get("GatewayReferenceId"), "pm_"));
        assertTrue(StringUtils.isNotBlank(responseMap.get("MITReceivedTXID")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
    }


    @Test
    public void validate_ItShouldReturnApprovedWhenEveryThingWorksWellForDiscover() throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(true,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with Discover Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodDiscoverApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForLiveTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertTrue(StringUtils.startsWith(responseMap.get("GatewayReferenceId"), "pm_"));
        assertTrue(StringUtils.isNotBlank(responseMap.get("MITReceivedTXID")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
    }


    @Test
    public void validate_ItShouldReturnApprovedWhenEveryThingWorksWellForJCB() throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(true,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with JCB Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodJCBApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForLiveTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertTrue(StringUtils.startsWith(responseMap.get("GatewayReferenceId"), "pm_"));
        assertTrue(StringUtils.isNotBlank(responseMap.get("MITReceivedTXID")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
    }

    @Test
    public void validate_ItShouldReturnApprovedWhenEveryThingWorksWellForVisa_StandaloneAuthIsTrue()
            throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(true,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "StandaloneAuth=true;") //Default Auth Amount is 1
                        .build()));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with Visa Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodVisaApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForLiveTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertTrue(StringUtils.startsWith(responseMap.get("GatewayReferenceId"), "pm_"));
        assertTrue(StringUtils.isNotBlank(responseMap.get("MITReceivedTXID")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertTrue(StringUtils.isNotBlank(responseMap.get("AuthTransactionId")));

        String authorizationId = responseMap.get("AuthTransactionId");

        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("US Dollar", "USD", "840"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("1"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .withAuthTransactionId(authorizationId)
                .build();

        Map<String, String> captureCallRequestMap = OpgRequestMapHelper.constructCaptureCallRequestMap(payment, paymentGateway);

        Map<String, String> captureResponseMap = opg.performPaymentOperation(captureCallRequestMap, paymentGateway);
        assertEquals("Approved", captureResponseMap.get("GatewayResponseMessage"));
        assertEquals("200", captureResponseMap.get("GatewayResponseCode"));
        assertEquals(authorizationId, captureResponseMap.get("GatewayReferenceId"));
        assertTrue(StringUtils.isNotBlank(captureResponseMap.get("GatewayRequestString")));
        assertTrue(StringUtils.isNotBlank(captureResponseMap.get("GatewayResponseString")));
    }

    @Test
    public void validate_ItShouldReturnApprovedWhenEveryThingWorksWellForVisa_StandaloneAuthIsFalse()
            throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(true,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(HashMapBuilder.<String, String>builder()
                        .put("PaymentGateway.FeatureSettings", "StandaloneAuth=false;") //Default Auth Amount is 1
                        .build()));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with Visa Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodVisaApplePayCreditCardForTest();
        paymentMethod.setCardSecurityCode("A6Vc1bMADSDLcMV5rsB0MAACAAA");

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForLiveTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertTrue(StringUtils.startsWith(responseMap.get("GatewayReferenceId"), "pm_"));
        assertTrue(StringUtils.isNotBlank(responseMap.get("MITReceivedTXID")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertTrue(StringUtils.isBlank(responseMap.get("AuthTransactionId"))); //AuthTransactionId should be Blank
    }

}
