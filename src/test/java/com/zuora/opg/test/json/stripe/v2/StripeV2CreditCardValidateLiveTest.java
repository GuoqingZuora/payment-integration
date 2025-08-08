package com.zuora.opg.test.json.stripe.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.zuora.billing.opg.test.common.OpgJsonBaseTest;
import com.zuora.billing.opg.test.json.stripe.StripeTestHelper;
import com.zuora.billing.opg.test.support.engine.OpgRequestMapHelper;
import com.zuora.zbilling.paymentmethod.model.PaymentMethod;
import com.zuora.zbilling.setting.gateway.model.PaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.OpenPaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.constants.OpenPaymentGatewayConstants;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Map;

public class StripeV2CreditCardValidateLiveTest extends OpgJsonBaseTest {
    @Test
    public void validate_ItShouldReturnApprovedWhenEveryThingWorksWell() throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(true,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForLiveTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertTrue(StringUtils.startsWith(responseMap.get("GatewayReferenceId"), "seti_"));
        assertTrue(StringUtils.isNotBlank(responseMap.get("MITReceivedTXID")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
    }

    @Test
    public void validate_ItShouldReturnApprovedForMotoWhenEveryThingWorksWell() throws Exception {
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(true,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();

        // use gateway instance setting for live test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForLiveTest(paymentGateway));

        Map<String, String> requestMap = OpgRequestMapHelper.constructValidationCallRequestMap(paymentMethod, paymentGateway);
        requestMap.put(OpenPaymentGatewayConstants.REQUEST_ZUORA_ECOMMERCE_INDICATOR, OpenPaymentGatewayConstants.ECOMMERCE_INDICATOR_MOTO);
        requestMap.put(OpenPaymentGatewayConstants.REQUEST_MIT_EXTRACT_SCP, OpenPaymentGatewayConstants.FLAG_YES);

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway);
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertTrue(StringUtils.startsWith(responseMap.get("GatewayReferenceId"), "seti_"));
        assertTrue(StringUtils.isNotBlank(responseMap.get("MITReceivedTXID")));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
    }
}
