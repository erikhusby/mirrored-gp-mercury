/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.presentation.search;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class SearchActionBeanTest {
    @DataProvider(name = "inputStrings")
    public Iterator<Object[]> inputStrings() {
        List<Object[]> testCases = new ArrayList<>();

        testCases.add(new Object[]{"SM-HJILE ", Collections.singletonList("SM-HJILE")});

        testCases.add(new Object[]{String.format("%s%s%s", "\u00a0","x", "\u00a0"), Collections.singletonList("x")});
        testCases.add(new Object[]{String.format("%s%s%s", "\u1680","x", "\u1680"), Collections.singletonList("x")});
        testCases.add(new Object[]{String.format("%s%s%s", "\u180e","x", "\u180e"), Collections.singletonList("x")});
        testCases.add(new Object[]{String.format("%s%s%s", "\u2000","x", "\u2000"), Collections.singletonList("x")});
        testCases.add(new Object[]{String.format("%s%s%s", "\u202f","x", "\u202f"), Collections.singletonList("x")});
        testCases.add(new Object[]{String.format("%s%s%s", "\u205f","x", "\u205f"), Collections.singletonList("x")});
        testCases.add(new Object[]{String.format("%s%s%s", "\u3000","x", "\u3000"), Collections.singletonList("x")});

        return testCases.iterator();
    }

    @Test(dataProvider = "inputStrings")
    public void testCleanInputString(String inputString, List<String> output) throws Exception {
        List<String> strings = SearchActionBean.cleanInputString(inputString, true);

        strings.forEach(s -> assertThat(StringUtils.isAsciiPrintable(s), is(true)));
        assertThat(strings, equalTo(output));
    }
}
