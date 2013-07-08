package org.broadinstitute.gpinformatics.athena.boundary.search;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * This class is for testing the {@link SearchEjb} class.
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class SearchEjbTest extends Arquillian {
    @Inject
    SearchEjb searchEjb;

    @Test(dataProvider = "Success-Provider")
    public void testSuccessSearch(String searchText) throws Exception {
        SearchEjb.SearchResult searchResult = searchEjb.search(searchText);
        Assert.assertNotNull(searchResult, "Should have returned something for " + searchText);
    }

    @Test(dataProvider = "Failure-Provider")
    public void testFailureSearch(String searchText) throws Exception {
        SearchEjb.SearchResult searchResult = searchEjb.search(searchText);
        Assert.assertNull(searchResult, "Should not have returned something for " + searchText);
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
