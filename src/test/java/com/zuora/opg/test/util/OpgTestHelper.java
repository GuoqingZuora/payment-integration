package com.zuora.opg.test.util;

import com.zuora.billing.opg.test.support.setting.PaymentGatewaySettingRepositoryInMemoryImpl;
import com.zuora.billing.opg.test.verify.ConnectorHeadersVerifierBuilder;
import com.zuora.zbilling.payment.business.PaymentGatewaySettingRepository;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

public class OpgTestHelper {
    public static PaymentGatewaySettingRepository buildPaymentGatewaySettingRepository(
            Map<String, String> overrideSettings) {
        Properties properties = new Properties();
        properties.put("PaymentGateway.BehaviorMode", "ResponseTimeInsensitive");
        properties.put("PaymentGateway.Connector.ConnectionTimeout", "30000");
        properties.put("PaymentGateway.Connector.SocketTimeout", "60000");
        properties.put("PaymentGateway.Connector.RetryAttempts", "1");
        properties.put("PaymentGateway.Connector.SafeRetryAttempts", "3");
        properties.put("PaymentGateway.Connector.RetryEnabled", "false");
        properties.put("PaymentGateway.opg.plugin.enabled", "false");

        if (Objects.nonNull(overrideSettings)) {
            properties.putAll(overrideSettings);
        }

        return new PaymentGatewaySettingRepositoryInMemoryImpl(properties);
    }

    ;

    public static String generateResponseFromResource(String resourceName) {
        String plainTextResponse = "";
        try (InputStream inputStream = OpgTestHelper.class.getResourceAsStream(resourceName)) {
            plainTextResponse = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while loading mocked response from resource: " + resourceName, e);
        }
        return plainTextResponse;
    }

    public static void validateXML(String xmlSchemaPath, String payload) {
        try {
            URL schemaFile = ConnectorHeadersVerifierBuilder.class.getResource(xmlSchemaPath);
            Source xmlFile = new StreamSource( new StringReader(payload));
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaFile);
            Validator validator = schema.newValidator();
            validator.validate(xmlFile);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while validating XML payload", e);
        }
    }
}
