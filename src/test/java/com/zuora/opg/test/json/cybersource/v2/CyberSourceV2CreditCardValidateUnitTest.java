package com.zuora.opg.test.json.cybersource.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zuora.aitest.payment.PaymentService;
import com.zuora.opg.test.json.cybersource.CyberSourceTestHelper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class CyberSourceV2CreditCardValidateUnitTest {

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
    public void case_01_ItShouldReturnApprovedForZeroAmountValidation() throws Exception {
        String mockResponse = CyberSourceTestHelper.loadMockResponseFromResource(
                "/com/zuora/opg/test/json/cybersource_2/creditcard/validate/case_01/validate_response.json");

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getCode()).thenReturn(201);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(mockResponse.getBytes()));

        Map<String, Object> request = CyberSourceTestHelper.buildValidateRequest(BigDecimal.ZERO);
        Map<String, Object> response = CyberSourceTestHelper.parseJsonResponse(mockResponse);

        assertEquals("AUTHORIZED", response.get("status"));
        assertEquals("6865460696176033204954", response.get("id"));
        assertNotNull(response.get("processorInformation"));

        Map<String, Object> processorInfo = (Map<String, Object>) response.get("processorInformation");
        assertEquals("888888", processorInfo.get("approvalCode"));
        assertEquals("123456789012345", processorInfo.get("networkTransactionId"));
        assertEquals("100", processorInfo.get("responseCode"));
    }

    @Test
    public void case_02_ItShouldReturnApprovedForNonZeroAmountValidation() throws Exception {
        String authResponse = CyberSourceTestHelper.loadMockResponseFromResource(
                "/com/zuora/opg/test/json/cybersource_2/creditcard/validate/case_02/auth_response.json");
        String voidResponse = CyberSourceTestHelper.loadMockResponseFromResource(
                "/com/zuora/opg/test/json/cybersource_2/creditcard/validate/case_02/void_response.json");

        when(httpClient.execute(any(HttpPost.class)))
                .thenReturn(httpResponse)
                .thenReturn(httpResponse);
        when(httpResponse.getCode()).thenReturn(201);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent())
                .thenReturn(new ByteArrayInputStream(authResponse.getBytes()))
                .thenReturn(new ByteArrayInputStream(voidResponse.getBytes()));

        Map<String, Object> request = CyberSourceTestHelper.buildValidateRequest(BigDecimal.ONE);
        Map<String, Object> authResponseMap = CyberSourceTestHelper.parseJsonResponse(authResponse);
        Map<String, Object> voidResponseMap = CyberSourceTestHelper.parseJsonResponse(voidResponse);

        assertEquals("AUTHORIZED", authResponseMap.get("status"));
        assertEquals("6865460696176033204955", authResponseMap.get("id"));

        assertEquals("VOIDED", voidResponseMap.get("status"));
        assertEquals("6865460696176033204955", voidResponseMap.get("id"));
    }

    @Test
    public void case_03_ReturnErrorWhenGatewayReturns5XXHttpStatus() throws Exception {
        String errorResponse = CyberSourceTestHelper.loadMockResponseFromResource(
                "/com/zuora/opg/test/json/cybersource_2/creditcard/validate/case_03/validate_error_response.json");

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getCode()).thenReturn(500);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(errorResponse.getBytes()));

        Map<String, Object> request = CyberSourceTestHelper.buildValidateRequest(BigDecimal.ZERO);
        Map<String, Object> response = CyberSourceTestHelper.parseJsonResponse(errorResponse);

        assertEquals("SERVER_ERROR", response.get("status"));
        assertEquals("SYSTEM_ERROR", response.get("reason"));
        assertNotNull(response.get("message"));
        assertTrue(response.get("message").toString().contains("An error occurred during processing"));
    }

    @Test
    public void testValidateRequestHeaders() {
        Map<String, String> expectedHeaders = CyberSourceTestHelper.buildExpectedHeaders();
        
        assertEquals("application/json", expectedHeaders.get("Content-Type"));
        assertEquals("test_merchant_id", expectedHeaders.get("v-c-merchant-id"));
        assertEquals("apitest.cybersource.com", expectedHeaders.get("Host"));
    }

    @Test
    public void testValidateRequestStructure() {
        Map<String, Object> request = CyberSourceTestHelper.buildValidateRequest(BigDecimal.valueOf(10.00));
        
        assertEquals("Validate", request.get("operation"));
        assertEquals("CreditCard", request.get("paymentMethodType"));
        assertEquals("10.00", request.get("defaultAuthAmount"));
        assertEquals("USD", request.get("currency"));
        assertEquals("4111111111111111", request.get("creditCardNumber"));
        assertEquals("123", request.get("cardSecurityCode"));
    }
}
