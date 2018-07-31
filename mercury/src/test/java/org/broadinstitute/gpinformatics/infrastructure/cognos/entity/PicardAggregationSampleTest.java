/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.cognos.entity;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;


@Test(groups = TestGroups.DATABASE_FREE)
public class PicardAggregationSampleTest {
    @DataProvider(name = "productOrdersScenarios")
    public Iterator<Object[]> productOrdersScenarios() {
        List<Object[]> testCases = new ArrayList<>();
        testCases.add(new Object[]{"a", Collections.singletonList("a"), true});
        testCases.add(new Object[]{"a", Collections.singletonList("b"), false});
        testCases.add(new Object[]{"a,b", Arrays.asList("a", "b"), true});
        testCases.add(new Object[]{"a ,b ", Arrays.asList("a","b"), true});
        testCases.add(new Object[]{"a ,b ,,", Arrays.asList("a","b"), true});
        testCases.add(new Object[]{"a,b", Arrays.asList("a","b"), true});
        testCases.add(new Object[]{"a,b", Arrays.asList("b","a"), true});
        testCases.add(new Object[]{"a,b", Arrays.asList("b","a", "c"), false});
        testCases.add(new Object[]{"a,b,c", Arrays.asList("b","a"), false});

        return testCases.iterator();
    }

    @Test(dataProvider = "productOrdersScenarios")
    public void testPdoEqualityScenarios(String pdoString, List<String> pdoList, boolean expectedToPass) {
        PicardAggregationSample picardAggregationSample =
            new PicardAggregationSample("rp1", "proj1", pdoString, "s1", "dt1");

        if (expectedToPass) {
            assertThat(picardAggregationSample.getProductOrderList(), containsInAnyOrder(pdoList.toArray()));
        }else {
            assertThat(picardAggregationSample.getProductOrderList(), not(containsInAnyOrder(pdoList.toArray())));
        }
    }

}
