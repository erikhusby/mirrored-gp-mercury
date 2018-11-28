package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.hamcrest.Matcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
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

    @Test(expectedExceptions = RuntimeException.class)
    public void testQuerySplitterLimitsThrowsExceptionWhenUrlIsTooLong() {
        // baseUrl:         "https://bass.broadinstitute.org:443/list".length(); // 40
        // fixedValues:     "https://bass.broadinstitute.org:443/list?a=A".length(); // 44
        // fixedVals +more  "https://bass.broadinstitute.org:443/list?a=A&b=B".length(); // 48

        Map<String, List<String>> fixedParameterMap = new HashMap<String, List<String>>() {{
            put("a", Collections.singletonList("A"));
        }};
        //
        QueryStringSplitter splitter = new QueryStringSplitter(40, 44, fixedParameterMap);
        splitter.split("b", Collections.singletonList("B"));
    }

    /**
     * This DataProvider provides these input parameters:
     * String queryKey: the 'key' part of key/value
     * List[] resultValues: each array element contains the values that should be returned from the
     * splitter for each split. All elements are passed into the Splitter.split() method.
     * int baseUrlLength: size of the url minus all the parameters
     * int maxUrlLength: the largest size a url can.
     */
    @DataProvider(name = "querySplitterLimits")
    public Iterator<Object[]> querySplitterLimits() {
        List<Object[]> testCases = new ArrayList<>();
        // baseUrl:         "https://bass.broadinstitute.org:443/list".length(); // 40
        // fixedValues:     "https://bass.broadinstitute.org:443/list?fixedName=fixedValue".length(); // 61
        // One K,V pair &:  "https://bass.broadinstitute.org:443/list?fixedName=fixedValue&q=x".length(); // 65

        String key = "q";
        int baseUrlLength = "https://bass.broadinstitute.org:443/list".length();
        final String fixedName = "fixedName";
        final List<String> fixedValues = Collections.singletonList("fixedValue");

        testCases.add(new Object[]{fixedName, fixedValues,
                key, new List[]{Collections.singletonList("x")},
                baseUrlLength, 65});

        testCases.add(new Object[]{fixedName, fixedValues,
                key, new List[]{Collections.singletonList("x"), Collections.singletonList("y")},
                baseUrlLength, 66});

        return testCases.iterator();
    }


    @Test(dataProvider = "querySplitterLimits")
    public void testQueryStringSplitterFixedMap(final String fixedKey, final List<String> fixedValues, String queryKey,
                                                List<String>[] resultValues, int baseUrlLength, int maxUrlLength) {
                Map<String, List<String>> fixedParameterMap = new HashMap<String, List<String>>() {{
                    put(fixedKey, fixedValues);
                }};
        List<String> values = new ArrayList<>();
        for (List<String> resultValue : resultValues) {
            values.addAll(resultValue);
        }

        QueryStringSplitter splitter = new QueryStringSplitter(baseUrlLength, maxUrlLength, fixedParameterMap);
        List<Map<String, List<String>>> parametersList = splitter.split(queryKey, values);

        assertThat(parametersList, hasSize(resultValues.length));
        for (int i = 0; i < resultValues.length; i++) {
            Matcher<Map<? extends String, ? extends List<String>>> fixedMapEntries = hasEntry(fixedKey, fixedValues);

            assertThat(parametersList.get(i), fixedMapEntries);
            assertThat(parametersList.get(i), hasEntry(queryKey, resultValues[i]));
        }
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
