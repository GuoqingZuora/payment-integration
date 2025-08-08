package com.zuora.opg.test.common;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.regex.Pattern;

public class RegularExpressionMatcher extends TypeSafeDiagnosingMatcher<String> {

    protected RegularExpressionMatcher(Pattern pattern) {
        this.pattern = pattern;
    }

    private Pattern pattern;

    @Override
    public void describeTo(Description description) {
        description.appendText("a string matching the pattern ").appendValue(pattern);
    }

    @Override
    protected boolean matchesSafely(String actual, Description mismatchDescription) {
        if (!pattern.matcher(actual).matches()) {
            mismatchDescription.appendText("the string was ").appendValue(actual);
            return false;
        }
        return true;
    }

    /**
     * Creates a matcher that checks if the examined string matches a specified {@link Pattern}.
     *
     * <pre>
     * assertThat(&quot;abc&quot;, matchesRegex(Pattern.compile(&quot;&circ;[a-z]$&quot;));
     * </pre>
     *
     * @param pattern
     *            the pattern to be used.
     * @return The matcher.
     */
    public static Matcher<String> matchesRegex(Pattern pattern) {
        return new RegularExpressionMatcher(pattern);
    }
}

