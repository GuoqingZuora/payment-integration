package com.zuora.opg.test.json.cybersource;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CyberSourceTestHelper {

    public static final String API_HEADER_CONTENT_TYPE = "application/json";
    public static final String API_HEADER_V_C_MERCHANT_ID = "test_merchant_id";
    public static final String API_HEADER_HOST = "apitest.cybersource.com";
    public static final String API_BASE_URL = "https://apitest.cybersource.com/pts/v2/payments";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, String> buildGatewaySettings() {
        Map<String, String> settings = new HashMap<>();
        settings.put("organizationId", "test_merchant_id");
        settings.put("SharedKey", "test_shared_key");
        settings.put("sharedsecret", "test_shared_secret");
        settings.put("IsTest", "true");
        return settings;
    }

    public static Map<String, Object> buildCreditCardPaymentMethod() {
        Map<String, Object> paymentMethod = new HashMap<>();
        paymentMethod.put("creditCardNumber", "4111111111111111");
        paymentMethod.put("creditCardExpirationMonth", 8);
        paymentMethod.put("creditCardExpirationYear", 2049);
        paymentMethod.put("creditCardHolderName", "TestName11 TestName22");
        paymentMethod.put("creditCardAddress1", "Add11");
        paymentMethod.put("creditCardAddress2", "Add22");
        paymentMethod.put("creditCardCity", "TestCity11");
        paymentMethod.put("creditCardState", "DE");
        paymentMethod.put("creditCardPostalCode", "11111");
        paymentMethod.put("creditCardCountry", "US");
        paymentMethod.put("firstName", "TestName11");
        paymentMethod.put("lastName", "TestName22");
        paymentMethod.put("email", "test@example.com");
        paymentMethod.put("cardSecurityCode", "123");
        return paymentMethod;
    }

    public static Map<String, Object> buildValidateRequest(BigDecimal amount) {
        Map<String, Object> request = new HashMap<>();
        request.put("operation", "Validate");
        request.put("paymentMethodType", "CreditCard");
        request.put("defaultAuthAmount", amount.toString());
        request.put("currency", "USD");
        request.putAll(buildCreditCardPaymentMethod());
        return request;
    }

    public static Map<String, Object> buildPaymentRequest(BigDecimal amount) {
        Map<String, Object> request = new HashMap<>();
        request.put("operation", "Payment");
        request.put("paymentMethodType", "CreditCard");
        request.put("defaultAuthAmount", amount.toString());
        request.put("currency", "USD");
        request.putAll(buildCreditCardPaymentMethod());
        return request;
    }

    public static String loadMockResponseFromResource(String resourcePath) {
        try (InputStream inputStream = CyberSourceTestHelper.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load mock response from " + resourcePath, e);
        }
    }

    public static Map<String, Object> parseJsonResponse(String jsonResponse) {
        try {
            return objectMapper.readValue(jsonResponse, Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON response", e);
        }
    }

    public static Map<String, String> buildExpectedHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", API_HEADER_CONTENT_TYPE);
        headers.put("v-c-merchant-id", API_HEADER_V_C_MERCHANT_ID);
        headers.put("Host", API_HEADER_HOST);
        return headers;
    }
}
