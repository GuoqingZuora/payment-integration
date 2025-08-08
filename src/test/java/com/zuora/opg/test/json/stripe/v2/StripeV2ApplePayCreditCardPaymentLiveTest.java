package com.zuora.opg.test.json.stripe.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.zuora.base.Decimal;
import com.zuora.billing.opg.test.common.OpgJsonBaseTest;
import com.zuora.billing.opg.test.json.stripe.StripeTestHelper;
import com.zuora.billing.opg.test.support.common.BillingAccountBuilder;
import com.zuora.billing.opg.test.support.common.CurrencyBuilder;
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

public class StripeV2ApplePayCreditCardPaymentLiveTest extends OpgJsonBaseTest {
    @Test
    public void payment_ItShouldSucceedForNormalRecurringPaymentForVisaApplePay() throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(true,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        String networkTransactionId = "901151114810210";
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with Visa Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodVisaApplePayCreditCardForTest();
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
                StripeTestHelper.buildGatewayInstanceSettingForLiveTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        OpgRequestMapHelper.addMITParameterIntoRequestMap(requestMap,
                "Recurring", "Merchant", paymentMethod.getCreditCardType().name(), networkTransactionId);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertTrue(StringUtils.startsWith(responseMap.get("GatewayReferenceId"), "ch_"));
        assertTrue(StringUtils.isNotBlank(responseMap.get("MITReceivedTXID")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
    }

    @Test
    public void payment_ItShouldSucceedForNormalRecurringPaymentForMasterCardApplePay() throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(true,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        String networkTransactionId = "901151114810210";
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with MasterCard Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodMasterCardApplePayCreditCardForTest();
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
                StripeTestHelper.buildGatewayInstanceSettingForLiveTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        OpgRequestMapHelper.addMITParameterIntoRequestMap(requestMap,
                "Recurring", "Merchant", paymentMethod.getCreditCardType().name(), networkTransactionId);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertTrue(StringUtils.startsWith(responseMap.get("GatewayReferenceId"), "ch_"));
        assertTrue(StringUtils.isNotBlank(responseMap.get("MITReceivedTXID")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
    }

    @Test
    public void payment_ItShouldSucceedForNormalRecurringPaymentForAmexApplePay() throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(true,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        String networkTransactionId = "901151114810210";
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with American Express Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodAmexApplePayCreditCardForTest();
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
                StripeTestHelper.buildGatewayInstanceSettingForLiveTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        OpgRequestMapHelper.addMITParameterIntoRequestMap(requestMap,
                "Recurring", "Merchant", paymentMethod.getCreditCardType().name(), networkTransactionId);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertTrue(StringUtils.startsWith(responseMap.get("GatewayReferenceId"), "ch_"));
        assertTrue(StringUtils.isNotBlank(responseMap.get("MITReceivedTXID")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
    }

    @Test
    public void payment_ItShouldSucceedForNormalRecurringPaymentForDiscoverApplePay() throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(true,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        String networkTransactionId = "901151114810210";
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();

        //Test with Discover Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodDiscoverApplePayCreditCardForTest();
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
                StripeTestHelper.buildGatewayInstanceSettingForLiveTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        OpgRequestMapHelper.addMITParameterIntoRequestMap(requestMap,
                "Recurring", "Merchant", paymentMethod.getCreditCardType().name(), networkTransactionId);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertTrue(StringUtils.startsWith(responseMap.get("GatewayReferenceId"), "ch_"));
        assertTrue(StringUtils.isNotBlank(responseMap.get("MITReceivedTXID")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
    }

    @Test
    public void payment_ItShouldSucceedForNormalRecurringPaymentForJCBApplePay() throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(true,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        String networkTransactionId = "901151114810210";
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        //Test with JCB Apple Pay Credit Card
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodJCBApplePayCreditCardForTest();
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
                StripeTestHelper.buildGatewayInstanceSettingForLiveTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        OpgRequestMapHelper.addMITParameterIntoRequestMap(requestMap,
                "Recurring", "Merchant", paymentMethod.getCreditCardType().name(), networkTransactionId);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertTrue(StringUtils.startsWith(responseMap.get("GatewayReferenceId"), "ch_"));
        assertTrue(StringUtils.isNotBlank(responseMap.get("MITReceivedTXID")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
    }

}
