package com.zuora.opg.test.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.zuora.zpayment.openpaymentgateway.engine.connector.HttpConnectorCommonUtil;

import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;


public class ConnectorHeadersVerifierBuilder {
    private List<ConnectorHeadersVerifier> verifiers = new ArrayList<>();

    public static ConnectorHeadersVerifierBuilder newInstance() {
        return new ConnectorHeadersVerifierBuilder();
    }

    public ConnectorHeadersVerifierBuilder matches(Matcher<Map<? extends String, ? extends String>> matcher, String message) {
        if (Objects.isNull(matcher)) {
            throw new IllegalArgumentException("matcher is null.");
        }

        verifiers.add(headers -> {
            assertTrue(message, matcher.matches(headers));
        });
        return this;
    }

    public ConnectorHeadersVerifierBuilder notMatches(Matcher<Map<? extends String, ? extends String>> matcher, String message) {
        if (Objects.isNull(matcher)) {
            throw new IllegalArgumentException("matcher is null.");
        }

        verifiers.add(headers -> {
            assertFalse(message, matcher.matches(headers));
        });
        return this;
    }

    public ConnectorHeadersVerifierBuilder withSize(int size) {
        verifiers.add(headers -> {
            assertEquals("Expecting the header contains " + size + " elements", size, headers.size());
        });
        return this;
    }

    public ConnectorHeadersVerifierBuilder matchesRequestPayload(
            RequestPayloadExtractor payloadExtractor,
            Matcher<Map<? extends String, Object>> payloadMatcher,
            String message) {
        if (Objects.isNull(payloadExtractor)) {
            throw new IllegalArgumentException("payloadExtractor is null.");
        }

        if (Objects.isNull(payloadMatcher)) {
            throw new IllegalArgumentException("payloadMatcher is null.");
        }

        verifiers.add(headers -> {
            String rawPayload = headers.get(HttpConnectorCommonUtil.REQUEST_BODY);
            Map<String, Object> payload = payloadExtractor.extract(rawPayload);
            assertTrue(message, payloadMatcher.matches(payload));
        });
        return this;
    }

    public ConnectorHeadersVerifier build() {
        if (verifiers.isEmpty()) {
            throw new IllegalStateException("There is no verifier specified yet.");
        }

        return (headers) -> {
            for (ConnectorHeadersVerifier verifier : verifiers) {
                verifier.verify(headers);
            }
        };
    }

    public <K> ConnectorHeadersVerifierBuilder matches(Matcher<Map<? extends K, ?>> hasKey) {
        if (Objects.isNull(hasKey)) {
            throw new IllegalArgumentException("matcher is null.");
        }

        verifiers.add(headers -> {
            assertTrue(hasKey.matches(headers));
        });
        return this;
    }

    public <K> ConnectorHeadersVerifierBuilder matchesPattern(Matcher<Map<? extends K, ?>> hasKey, String regex, String message) {
        if (Objects.isNull(hasKey)) {
            throw new IllegalArgumentException("matcher is null.");
        }

        verifiers.add(headers -> {
            assertTrue(message, Pattern.matches(regex, headers.get("Idempotency-Key")));
        });
        return this;
    }
}