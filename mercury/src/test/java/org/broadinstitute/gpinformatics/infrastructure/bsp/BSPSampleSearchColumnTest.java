/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Test(groups = TestGroups.DATABASE_FREE)
public class BSPSampleSearchColumnTest {
    @DataProvider(name = "searchColumnDataProvider")
    public Object[][] searchColumnDataProvider() {
        EnumSet<BSPSampleSearchColumn> uniqueSearchColumns =
                EnumSet.copyOf(Arrays.asList(BSPSampleSearchColumn.PDO_SEARCH_COLUMNS));

        BSPSampleSearchColumn[] arrayWithDupes =
                {BSPSampleSearchColumn.COLLABORATOR_NAME, BSPSampleSearchColumn.COLLABORATOR_NAME,
                        BSPSampleSearchColumn.COLLABORATOR_NAME, BSPSampleSearchColumn.COLLABORATOR_NAME,
                        BSPSampleSearchColumn.COLLABORATOR_NAME};
        EnumSet<BSPSampleSearchColumn> searchColumnsFromDupeArray = EnumSet.copyOf(Arrays.asList(arrayWithDupes));

        return new Object[][]{
                {uniqueSearchColumns, BSPSampleSearchColumn.PDO_SEARCH_COLUMNS, true},
                // This failing case is really here to test my test
                {searchColumnsFromDupeArray, arrayWithDupes, false}
        };
    }

    /**
     * Test whether or not there are duplicate entries in BSPSampleSearchColumn.PDO_SEARCH_COLUMNS
     * <p/>
     * If there are duplicates bspSampleSearch results will not match up with the headers and ug/uL's will look like
     * container id's. Don't ask me how I know this :-)
     *
     * @see BSPSampleSearchColumn#PDO_SEARCH_COLUMNS
     */
    @Test(dataProvider = "searchColumnDataProvider")
    public void testPdoSearchColumns(Set<BSPSampleSearchColumn> testSet, BSPSampleSearchColumn[] testArray,
                                     boolean willPass) {
        List<BSPSampleSearchColumn> bspSampleSearchColumnList = Arrays.asList(testArray);

        Assert.assertEquals(testSet.size() == testArray.length, willPass,
                "Duplicate entries found in BSPSampleSearchColumn.PDO_SEARCH_COLUMNS: " + CollectionUtils
                        .disjunction(bspSampleSearchColumnList, testSet)
        );
    }

}
