package org.broadinstitute.gpinformatics.athena.boundary.search;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Vector;

/**
 * This class is for testing the {@link SearchEjb} class.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class SearchEjbTest {
    @Inject
    SearchEjb searchEjb;

    @Test(dataProvider = "Success-Provider")
    public void testSuccessSearch(String searchText) throws Exception {
        SearchEjb.SearchResult searchResult = searchEjb.search(searchText);
        Assert.assertNotNull("Should have returned something for " + searchText, searchResult);
    }

    @Test(dataProvider = "Failure-Provider")
    public void testFailureSearch(String searchText) throws Exception {
        SearchEjb.SearchResult searchResult = searchEjb.search(searchText);
        Assert.assertNull("Should not have returned something for " + searchText, searchResult);
    }

    /**
     * This function will provide the parameter data for successful searches.
     *
     * @return data provider data
     */
    @DataProvider(name="Success-Provider")
    public Object[][] successfulSearchItemsTestProvider(){
        return new Object[][]{{"RP-173"},{"PDO-1535"},{"P-EX-0001"}, {"rp-173"},{"pdo-1535"},{"p-ex-0001"}};
    }

    /**
     * This function will provide the parameter data for failed searches.
     *
     * @return data provider data
     */
    @DataProvider(name="Failure-Provider")
    public Object[][] failureSearchItemsTestProvider(){
        return new Object[][]{{"RP-000"},{"PDO-000"},{"P-000"}, {"rp-000"}};
    }

}
