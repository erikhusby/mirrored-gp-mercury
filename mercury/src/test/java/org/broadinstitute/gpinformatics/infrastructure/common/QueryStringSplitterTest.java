package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@Test(groups = TestGroups.DATABASE_FREE)
public class QueryStringSplitterTest {

    public void testEmptyValuesReturnsEmptyParameterMap() {
        String name = "test";
        List<String> values = Collections.emptyList();
        QueryStringSplitter splitter = new QueryStringSplitter(0, 10);

        List<Map<String, List<String>>> parametersList = splitter.split(name, values);

        assertThat(parametersList, hasSize(1));
        assertThat(parametersList.get(0).size(), equalTo(0));
    }

    public void testThrowsExceptionWhenValueCannotFit() {
        QueryStringSplitter splitter = new QueryStringSplitter(0, 10);

        String name = "test";
        String value = "value_too_long";
        Exception caught = null;
        try {
            splitter.split(name, Collections.singletonList(value));
        } catch (Exception e) {
            caught = e;
        }

        assertThat(caught.getMessage(), containsString(value));
    }

    public void testFewValuesReturnsSingleParameterMap() {
        /*
         * base length = 20 (e.g., "http://broadies.org/")
         * "?q=1&q=2".length() = 8
         * 20 + 8 = 28
         * 28 <= 30
         */
        String name = "q";
        List<String> values = Arrays.asList("1", "2");
        QueryStringSplitter splitter = new QueryStringSplitter(20, 30);

        List<Map<String, List<String>>> parametersList = splitter.split(name, values);

        assertThat(parametersList.get(0), equalTo(Collections.singletonMap(name, values)));
        assertThat(parametersList, hasSize(1)); // make sure there isn't extra data
    }

    public void testMoreValuesReturnsTwoParameterMaps() {
        /*
         * base length = 20 (e.g., "http://broadies.org/")
         * "?q=1&q=2&q=3".length() = 12
         * 20 + 12 = 32
         * 32 > 30 : too long!
         *
         * "?q=1&q=2".length() = 8
         * 20 + 8 = 28
         * 28 <= 30
         *
         * "?q=3".length() = 4
         * 20 + 4 = 24
         * 24 <= 30
         */
        String name = "q";
        List<String> values = Arrays.asList("1", "2", "3");
        QueryStringSplitter splitter = new QueryStringSplitter(20, 30);

        List<Map<String, List<String>>> parametersList = splitter.split(name, values);

        assertThat(parametersList.get(0), equalTo(Collections.singletonMap(name, Arrays.asList("1", "2"))));
        assertThat(parametersList.get(1), equalTo(Collections.singletonMap(name, Collections.singletonList("3"))));
        assertThat(parametersList, hasSize(2)); // make sure there isn't extra data
    }
}
