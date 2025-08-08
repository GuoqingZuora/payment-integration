package com.zuora.opg.test.json.cybersource.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.zuora.aitest.payment.PaymentService;
import com.zuora.opg.test.json.cybersource.CyberSourceTestHelper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class CyberSourceV2CreditCardPaymentUnitTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private ClassicHttpResponse httpResponse;

    @Mock
    private HttpEntity httpEntity;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
    }

    @Test
    public void case_01_ItShouldReturnApprovedWhenPaymentSucceeds() throws Exception {
        String mockResponse = CyberSourceTestHelper.loadMockResponseFromResource(
                "/com/zuora/opg/test/json/cybersource_2/creditcard/payment/case_01/payment_response.json");

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getCode()).thenReturn(201);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(mockResponse.getBytes()));

        Map<String, Object> request = CyberSourceTestHelper.buildPaymentRequest(BigDecimal.valueOf(10.00));
        Map<String, Object> response = CyberSourceTestHelper.parseJsonResponse(mockResponse);

        assertEquals("AUTHORIZED", response.get("status"));
        assertEquals("6865460696176033204956", response.get("id"));
        assertNotNull(response.get("processorInformation"));

        Map<String, Object> processorInfo = (Map<String, Object>) response.get("processorInformation");
        assertEquals("888890", processorInfo.get("approvalCode"));
        assertEquals("123456789012347", processorInfo.get("networkTransactionId"));
        assertEquals("100", processorInfo.get("responseCode"));

        Map<String, Object> orderInfo = (Map<String, Object>) response.get("orderInformation");
        Map<String, Object> amountDetails = (Map<String, Object>) orderInfo.get("amountDetails");
        assertEquals("10.00", amountDetails.get("totalAmount"));
        assertEquals("USD", amountDetails.get("currency"));
    }

    @Test
    public void case_02_ItShouldReturnFailedWhenPaymentIsDeclined() throws Exception {
        String mockResponse = CyberSourceTestHelper.loadMockResponseFromResource(
                "/com/zuora/opg/test/json/cybersource_2/creditcard/payment/case_02/payment_declined_response.json");

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getCode()).thenReturn(400);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(mockResponse.getBytes()));

        Map<String, Object> request = CyberSourceTestHelper.buildPaymentRequest(BigDecimal.valueOf(10.00));
        Map<String, Object> response = CyberSourceTestHelper.parseJsonResponse(mockResponse);

        assertEquals("DECLINED", response.get("status"));
        assertEquals("AVS_FAILED", response.get("reason"));
        assertNotNull(response.get("message"));
        assertTrue(response.get("message").toString().contains("Address Verification Service"));

        Map<String, Object> processorInfo = (Map<String, Object>) response.get("processorInformation");
        assertEquals("201", processorInfo.get("responseCode"));
    }

    @Test
    public void case_03_ReturnErrorWhenGatewayReturns5XXHttpStatus() throws Exception {
        String errorResponse = CyberSourceTestHelper.loadMockResponseFromResource(
                "/com/zuora/opg/test/json/cybersource_2/creditcard/payment/case_03/payment_error_response.json");

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getCode()).thenReturn(500);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(errorResponse.getBytes()));

        Map<String, Object> request = CyberSourceTestHelper.buildPaymentRequest(BigDecimal.valueOf(10.00));
        Map<String, Object> response = CyberSourceTestHelper.parseJsonResponse(errorResponse);

        assertEquals("SERVER_ERROR", response.get("status"));
        assertEquals("SYSTEM_ERROR", response.get("reason"));
        assertNotNull(response.get("message"));
        assertTrue(response.get("message").toString().contains("An error occurred during processing"));
    }

    @Test
    public void testPaymentRequestHeaders() {
        Map<String, String> expectedHeaders = CyberSourceTestHelper.buildExpectedHeaders();
        
        assertEquals("application/json", expectedHeaders.get("Content-Type"));
        assertEquals("test_merchant_id", expectedHeaders.get("v-c-merchant-id"));
        assertEquals("apitest.cybersource.com", expectedHeaders.get("Host"));
    }

    @Test
    public void testPaymentRequestStructure() {
        Map<String, Object> request = CyberSourceTestHelper.buildPaymentRequest(BigDecimal.valueOf(25.50));
        
        assertEquals("Payment", request.get("operation"));
        assertEquals("CreditCard", request.get("paymentMethodType"));
        assertEquals("25.50", request.get("defaultAuthAmount"));
        assertEquals("USD", request.get("currency"));
        assertEquals("4111111111111111", request.get("creditCardNumber"));
        assertEquals("123", request.get("cardSecurityCode"));
        assertEquals("TestName11 TestName22", request.get("creditCardHolderName"));
    }

    @Test
    public void testPaymentRequestWithDifferentAmounts() {
        Map<String, Object> smallAmountRequest = CyberSourceTestHelper.buildPaymentRequest(BigDecimal.valueOf(0.01));
        Map<String, Object> largeAmountRequest = CyberSourceTestHelper.buildPaymentRequest(BigDecimal.valueOf(999.99));
        
        assertEquals("0.01", smallAmountRequest.get("defaultAuthAmount"));
        assertEquals("999.99", largeAmountRequest.get("defaultAuthAmount"));
    }
}
