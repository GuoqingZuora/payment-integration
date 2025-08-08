package com.zuora.opg.test.verify;

import java.util.Map;

public interface ConnectorHeadersVerifier {
    void verify(Map<String, String> headers);
}
