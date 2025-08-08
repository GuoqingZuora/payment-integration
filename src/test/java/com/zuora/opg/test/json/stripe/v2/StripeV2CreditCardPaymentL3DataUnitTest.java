package com.zuora.opg.test.json.stripe.v2;

import com.zuora.base.Decimal;
import com.zuora.billing.opg.test.common.OpgJsonBaseTest;
import com.zuora.billing.opg.test.json.stripe.StripeRequestPayloadExtractor;
import com.zuora.billing.opg.test.json.stripe.StripeTestHelper;
import com.zuora.billing.opg.test.support.common.*;
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
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class StripeV2CreditCardPaymentL3DataUnitTest extends OpgJsonBaseTest {
    private static final ZUtility zUtility = new ZUtility();

    @Test
    public void case_01_ItShouldProperlySendL3DataWhenAmountIsBiggerThanMaxInteger() throws Exception {
        final String case_01_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_01/payment_response.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("Rupiah", "IDR", "360"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("83830594.38"))
                .withPaymentNumber("P-00031467")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway, true));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);
        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);

        // pass RadarSessionId via gateway options
        requestMap.put(OpenPaymentGatewayConstants.CONTEXT_PREFIX_GW_OPTIONS + "RadarSessionId", "rse_1N7fvKSDx60UgxZnDB5Hb4v5");

        final String[][] l3Data = new String[][]{
                {"1151373912","8a12924591a270510191ad14b0261995","","SKU-00000020","Standard Biz Monthly","14","213499","Host","328788.46","0.00E+00","0.00E+00","2988986","8a128ab1918782ed0191ab5a103d5bd4","AvalaraTax"},
                {"1151373913","8a12924591a270510191ad14b0261996","","SKU-00000029","Webinar 1000 Monthly","1","5078580","Host","558643.8","0.00E+00","0.00E+00","5078580","8a128ab1918782ed0191ab5a0fc55b8a","AvalaraTax"},
                {"1151373914","8a12924591a270510191ad14b0261997","","SKU-00000029","Webinar 3000 Monthly","1","14787630","Host","1626639.3","0.00E+00","0.00E+00","14787630","8a128ab1918782ed0191ab5a10315b92","AvalaraTax"},
                {"1151373915","8a12924591a270510191ad14b0261998","","SKU-00000029","Webinar 5000 Monthly","1","37193130","Host","4091244.3","0.00E+00","0.00E+00","37193130","8a128ab1918782ed0191ab5a10355b97","AvalaraTax"},
                {"1151373916","8a12924591a270510191ad14b0261999","","SKU-00000029","Webinar 500 Monthly","4","1180023","Host","519210.12","0.00E+00","0.00E+00","4720092","8a128ab1918782ed0191ab5a10415bea","AvalaraTax"},
                {"1151373917","8a12924591a270510191ad14b026199a","","SKU-00000021","1000 Participants meeting Monthly","8","1344330","Host","1183010.4","0.00E+00","0.00E+00","10754640","8a128ab1918782ed0191ab5a10395ba2","AvalaraTax"},
        };
        List<Map<String, String>> invoiceItemList = OpgRequestMapHelper.constructInvoiceItemList(List.of(l3Data));

        // Sale
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_01_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway, invoiceItemList, new ArrayList<>());
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3MojCtGT0RtLbTJ23BbZ3kcJ", responseMap.get("GatewayReferenceId"));
        assertEquals("695148101731176", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("pi_3MojCtGT0RtLbTJ23ymvvxiL", responseMap.get("GatewaySecondReferenceId"));

        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(9)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("off_session", "recurring"),
                                        Matchers.hasEntry("amount", "8383059438"),
                                        Matchers.hasEntry("currency", "IDR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031467"),
                                        Matchers.hasEntry("radar_options[session]", "rse_1N7fvKSDx60UgxZnDB5Hb4v5"), // verify RadarSessionId
                                        Matchers.hasEntry("level3[customer_reference]", "A-99999988"),
                                        Matchers.hasEntry("level3[line_items][0][discount_amount]", "0"),
                                        Matchers.hasEntry("level3[line_items][0][product_code]", "SKU-00000020"),
                                        Matchers.hasEntry("level3[line_items][0][product_description]", "Standard Biz Monthly"),
                                        Matchers.hasEntry("level3[line_items][0][quantity]", "14"),
                                        Matchers.hasEntry("level3[line_items][0][tax_amount]", "32878846"),
                                        Matchers.hasEntry("level3[line_items][0][unit_cost]", "21349900"),
                                        Matchers.hasEntry("level3[line_items][1][discount_amount]", "0"),
                                        Matchers.hasEntry("level3[line_items][1][product_code]", "SKU-00000029"),
                                        Matchers.hasEntry("level3[line_items][1][product_description]", "Webinar 1000 Monthly"),
                                        Matchers.hasEntry("level3[line_items][1][quantity]", "1"),
                                        Matchers.hasEntry("level3[line_items][1][tax_amount]", "55864380"),
                                        Matchers.hasEntry("level3[line_items][1][unit_cost]", "507858000"),
                                        Matchers.hasEntry("level3[line_items][2][discount_amount]", "0"),
                                        Matchers.hasEntry("level3[line_items][2][product_code]", "SKU-00000029"),
                                        Matchers.hasEntry("level3[line_items][2][product_description]", "Webinar 3000 Monthly"),
                                        Matchers.hasEntry("level3[line_items][2][quantity]", "1"),
                                        Matchers.hasEntry("level3[line_items][2][tax_amount]", "162663930"),
                                        Matchers.hasEntry("level3[line_items][2][unit_cost]", "1478763000"),
                                        Matchers.hasEntry("level3[line_items][3][discount_amount]", "0"),
                                        Matchers.hasEntry("level3[line_items][3][product_code]", "SKU-00000029"),
                                        Matchers.hasEntry("level3[line_items][3][product_description]", "Webinar 5000 Monthly"),
                                        Matchers.hasEntry("level3[line_items][3][quantity]", "1"),
                                        Matchers.hasEntry("level3[line_items][3][tax_amount]", "409124430"),
                                        Matchers.hasEntry("level3[line_items][3][unit_cost]", "3719313000"),
                                        Matchers.hasEntry("level3[line_items][4][discount_amount]", "0"),
                                        Matchers.hasEntry("level3[line_items][4][product_code]", "SKU-00000029"),
                                        Matchers.hasEntry("level3[line_items][4][product_description]", "Webinar 500 Monthly"),
                                        Matchers.hasEntry("level3[line_items][4][quantity]", "4"),
                                        Matchers.hasEntry("level3[line_items][4][tax_amount]", "51921012"),
                                        Matchers.hasEntry("level3[line_items][4][unit_cost]", "118002300"),
                                        Matchers.hasEntry("level3[line_items][5][discount_amount]", "0"),
                                        Matchers.hasEntry("level3[line_items][5][product_code]", "SKU-00000021"),
                                        Matchers.hasEntry("level3[line_items][5][product_description]", "1000 Participants meeting"),
                                        Matchers.hasEntry("level3[line_items][5][quantity]", "8"),
                                        Matchers.hasEntry("level3[line_items][5][tax_amount]", "118301040"),
                                        Matchers.hasEntry("level3[line_items][5][unit_cost]", "134433000"),
                                        Matchers.hasEntry("level3[merchant_reference]", "P-00031467")
                                ),
                                "check request payload")
                        .build());
    }

    @Test
    public void case_02_ItShouldProperlySendL3DataWhenAmountIsNegativeAndIsLessThanMinInteger() throws Exception {
        final String case_02_response = "/com/zuora/opg/test/json/stripe_2/creditcard/payment/case_02/payment_response_for_moto.json";
        final OpenPaymentGateway opg = buildOpenPaymentGatewayForTest(false,
                StripeTestHelper.buildPaymentGatewaySettingRepositoryForTest(null));
        final String idempotencyKey = zUtility.getUUID(64);
        final PaymentGateway paymentGateway = StripeTestHelper.buildGatewayInstanceForTest();
        final PaymentMethod paymentMethod = StripeTestHelper.buildGoodCreditCardForTest();
        final BillingAccount billingAccount = BillingAccountBuilder.aBillingAccount()
                .withId("acc#1241234123")
                .withAccountNumber("A-99999988")
                .withCurrency(CurrencyBuilder.of("Rupiah", "IDR", "360"))
                .build();
        final Payment payment = PaymentBuilder.aPayment()
                .withId("p#21412453134")
                .withPaymentAmount(Decimal.valueOf("39738420.98"))
                .withPaymentNumber("P-00031423")
                .withPaymentMethod(paymentMethod)
                .withBillingAccount(billingAccount)
                .build();

        // use gateway instance setting for unit test
        expectingGatewayInstanceSettings(gatewayConfigurationHandler, paymentGateway,
                StripeTestHelper.buildGatewayInstanceSettingForUnitTest(paymentGateway, true));

        Map<String, String> requestMap = OpgRequestMapHelper.constructPaymentCallRequestMap(payment, paymentGateway, false);

        requestMap.put(OpenPaymentGatewayConstants.FRAMEWORK_IDEMPOTENCY_KEY, idempotencyKey);
        requestMap.put(OpenPaymentGatewayConstants.REQUEST_ZUORA_ECOMMERCE_INDICATOR, OpenPaymentGatewayConstants.ECOMMERCE_INDICATOR_MOTO);
        requestMap.put(OpenPaymentGatewayConstants.REQUEST_MIT_EXTRACT_SCP, OpenPaymentGatewayConstants.FLAG_YES);


        final String[][] l3Data = new String[][]{
                {"1149734660","8a12894591879afb0191897231e033e4","None","SKU-00000030","Cloud Recording 5TB","1","7468500","","821535","0.00E+00","0.00E+00","7468500","8a128adf8f0fe71d018f292de2626c06","AvalaraTax"},
                {"1120434790","8a1294b58f27caee018f29c7ca2239af","","SKU-00000020","Standard Biz Annual -- Proration Credit","43","2134990","Host","-7477306.64","0.00E+00","0.00E+00","-67975514.95","8a128adf8f0fe71d018f292de2476bd2","AvalaraTax"},
                {"1120434791","8a1294b58f27caee018f29c7ca2239b0","","SKU-00000021","500 Participants meeting Annual -- Proration Credit","5","8962200","Host","-3649770.25","0.00E+00","0.00E+00","-33179729.51","8a128adf8f0fe71d018f292de2fa6c1f","AvalaraTax"},
                {"1120434792","8a1294b58f27caee018f29c7ca2239b1","","SKU-00000020","Standard Biz Annual -- Proration","52","2134990","Host","9042324.31","0.00E+00","0.00E+00","82202948.31","8a128adf8f0fe71d018f292de2476bd4","AvalaraTax"},
                {"1120434793","8a1294b58f27caee018f29c7ca2239b2","","SKU-00000021","500 Participants meeting Annual -- Proration","6","8962200","Host","4379724.3","0.00E+00","0.00E+00","39815675.41","8a128adf8f0fe71d018f292de1cf6bca","AvalaraTax"},
                {"1142378166","8a1296a290e2fd9b0190e9d1122d6984","None","SKU-00000030","Cloud Recording 5TB","1","7468500","","821535","0.00E+00","0.00E+00","7468500","8a128adf8f0fe71d018f292de2626c06","AvalaraTax"},
        };
        List<Map<String, String>> invoiceItemList = OpgRequestMapHelper.constructInvoiceItemList(List.of(l3Data));

        // Sale
        expectingConnectorResponse(httpsConnector,
                Matchers.allOf(
                        Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"),
                        Matchers.hasEntry("METHOD", "POST"),
                        Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"),
                        Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE),
                        Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION),
                        Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3")
                ),
                HashMapBuilder.<String, Object>builder()
                        .put(HttpConnectorCommonUtil.STATUS_CODE, "200")
                        .put(HttpConnectorCommonUtil.MESSAGE_BODY, StripeTestHelper.loadStripeMockResponseFromResource(case_02_response))
                        .build()
        );

        Map<String, String> responseMap = opg.performPaymentOperation(requestMap, paymentGateway, invoiceItemList, new ArrayList<>());
        assertEquals("Approved", responseMap.get("ZuoraResponseCode"));
        assertEquals("200", responseMap.get("GatewayResponseCode"));
        assertEquals("Approved", responseMap.get("GatewayResponseMessage"));
        assertEquals("ch_3MonYqGT0RtLbTJ21lqGh4Pa", responseMap.get("GatewayReferenceId"));
        assertEquals("695148101731176", responseMap.get("MITReceivedTXID"));
        assertTrue(StringUtils.isBlank(responseMap.get("MITReceivedToken1")));
        assertEquals("pi_3MonYqGT0RtLbTJ21YSOxVgu", responseMap.get("GatewaySecondReferenceId"));


        verifyConnectorRequest(httpsConnector,
                ConnectorHeadersVerifierBuilder.newInstance()
                        .withSize(9)
                        .matches(Matchers.hasEntry("URL", "https://api.stripe.com/v1/payment_intents"), "Check URL")
                        .matches(Matchers.hasEntry("METHOD", "POST"), "Method is POST")
                        .matches(Matchers.hasEntry("Authorization", "Bearer mocked_secret_key#018230141038"))
                        .matches(Matchers.hasEntry("Content-Type", StripeTestHelper.API_HEADER_CONTENT_TYPE))
                        .matches(Matchers.hasEntry("Stripe-Version", StripeTestHelper.API_HEADER_STRIPE_VERSION))
                        .matches(Matchers.hasEntry("SOCKET_TIMEOUT", "7890"))
                        .matches(Matchers.hasEntry("CONNECTION_TIMEOUT", "3456"))
                        .matches(Matchers.hasEntry("Idempotency-Key", idempotencyKey + "_withl3"))
                        .matchesRequestPayload(
                                new StripeRequestPayloadExtractor(),
                                Matchers.allOf(
                                        Matchers.hasEntry("payment_method_data[type]", "card"),
                                        Matchers.hasEntry("payment_method_data[card][number]", "4111111111111111"),
                                        Matchers.hasEntry("payment_method_data[card][exp_year]", "2049"),
                                        Matchers.hasEntry("payment_method_data[card][exp_month]", "8"),
                                        Matchers.hasEntry("payment_method_data[billing_details][name]", "TestName11 TestName22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line1]", "Add11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][line2]", "Add22"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][city]", "TestCity11"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][postal_code]", "11111"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][state]", "DE"),
                                        Matchers.hasEntry("payment_method_data[billing_details][address][country]", "US"),
                                        Matchers.hasEntry("payment_method_options[card][moto]", "true"),
                                        Matchers.hasEntry("amount", "3973842098"),
                                        Matchers.hasEntry("currency", "IDR"),
                                        Matchers.hasEntry("confirm", "true"),
                                        Matchers.hasEntry("metadata[zpayment_number]", "P-00031423"),
                                        Matchers.hasEntry("level3[customer_reference]", "A-99999988"),
                                        Matchers.hasEntry("level3[line_items][0][discount_amount]", "0"),
                                        Matchers.hasEntry("level3[line_items][0][product_code]", "SKU-00000030"),
                                        Matchers.hasEntry("level3[line_items][0][product_description]", "None"),
                                        Matchers.hasEntry("level3[line_items][0][quantity]", "1"),
                                        Matchers.hasEntry("level3[line_items][0][tax_amount]", "82153500"),
                                        Matchers.hasEntry("level3[line_items][0][unit_cost]", "746850000"),
                                        Matchers.hasEntry("level3[line_items][1][discount_amount]", "0"),
                                        Matchers.hasEntry("level3[line_items][1][product_code]", "SKU-00000020"),
                                        Matchers.hasEntry("level3[line_items][1][product_description]", "Standard Biz Annual -- Pr"),
                                        Matchers.hasEntry("level3[line_items][1][quantity]", "1"),
                                        Matchers.hasEntry("level3[line_items][1][tax_amount]", "904232431"),
                                        Matchers.hasEntry("level3[line_items][1][unit_cost]", "8220294831"),
                                        Matchers.hasEntry("level3[line_items][2][discount_amount]", "0"),
                                        Matchers.hasEntry("level3[line_items][2][product_code]", "SKU-00000021"),
                                        Matchers.hasEntry("level3[line_items][2][product_description]", "500 Participants meeting "),
                                        Matchers.hasEntry("level3[line_items][2][quantity]", "1"),
                                        Matchers.hasEntry("level3[line_items][2][tax_amount]", "437972430"),
                                        Matchers.hasEntry("level3[line_items][2][unit_cost]", "3981567541"),
                                        Matchers.hasEntry("level3[line_items][3][discount_amount]", "0"),
                                        Matchers.hasEntry("level3[line_items][3][product_code]", "SKU-00000030"),
                                        Matchers.hasEntry("level3[line_items][3][product_description]", "None"),
                                        Matchers.hasEntry("level3[line_items][3][quantity]", "1"),
                                        Matchers.hasEntry("level3[line_items][3][tax_amount]", "82153500"),
                                        Matchers.hasEntry("level3[line_items][3][unit_cost]", "746850000"),
                                        Matchers.hasEntry("level3[line_items][4][discount_amount]", "11228232135"),
                                        Matchers.hasEntry("level3[line_items][4][product_code]", "Credit"),
                                        Matchers.hasEntry("level3[line_items][4][product_description]", "Proration credit"),
                                        Matchers.hasEntry("level3[line_items][4][quantity]", "1"),
                                        Matchers.hasEntry("level3[line_items][4][tax_amount]", "0"),
                                        Matchers.hasEntry("level3[line_items][4][unit_cost]", "0"),
                                        Matchers.hasEntry("level3[merchant_reference]", "P-00031423")
                                ),
                                "check request payload")
                        .build());
    }
}
