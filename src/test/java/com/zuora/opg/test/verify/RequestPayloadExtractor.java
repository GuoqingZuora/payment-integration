package com.zuora.opg.test.verify;

import java.util.Map;

public interface RequestPayloadExtractor {
    Map<String, Object> extract(String payload);
}
