package com.zuora.opg.test.common;

import com.zuora.zpayment.openpaymentgateway.engine.templateengine.ZUtility;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import java.util.Map;

import static org.junit.Assert.assertNotNull;

public final class AssertUtils {

    public static void verifyThreeDs2Data(String threeDS2ResponseData, Matcher<Map<String, String>> tMatcher) {
        assertNotNull("3DS2 Data can not be null", threeDS2ResponseData);
        Map<String, String> threeDs2Data = new ZUtility().decodeUrlParameters(threeDS2ResponseData);
        MatcherAssert.assertThat(threeDs2Data, tMatcher);
    }
}
