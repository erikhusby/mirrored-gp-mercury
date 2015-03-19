package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.entity.preference.SearchInstanceList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test creating, saving, retrieving and executing a search.
 */
@Test(groups = TestGroups.STANDARD)
public class ConfigurableSearchTest extends ContainerTest {

    @Inject
    private SearchInstanceEjb searchInstanceEjb;

    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private ConfigurableListFactory configurableListFactory;

    @Inject
    private UserBean userBean;

    /**
     * Create, execute, then delete a global saved lab event search instance
     */
    @Test
    public void testLabEventSearch() {

        // Login a fake user
        userBean.loginTestUser();

        // Create instance
        SearchInstance searchInstance = new SearchInstance();
        String entity = "LabEvent";
        ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity(entity);
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("EventDate",
                configurableSearchDef);
        searchValue.setOperator(SearchInstance.Operator.BETWEEN);
        searchValue.setValues(Arrays.asList("6/2/2014", "6/3/2014"));

        // Add columns
        searchInstance.getPredefinedViewColumns().add("LabEventId");

        // Save instance
        Map<PreferenceType, Preference> mapTypeToPreference = new HashMap<>();
        Map<String,String> newSearchLevels = new HashMap<>();
        Map<String,String> searchInstanceNames = new HashMap<>();
        try {
            searchInstanceEjb.fetchInstances(ColumnEntity.LAB_EVENT, mapTypeToPreference, searchInstanceNames, newSearchLevels);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String newSearchName = "Test-" + entity + "-" + new SimpleDateFormat("MM/dd/yyyy").format(new Date(System.currentTimeMillis()));
        searchInstanceEjb.persistSearch(true, searchInstance, new MessageCollection(),
                PreferenceType.GLOBAL_LAB_EVENT_SEARCH_INSTANCES, newSearchName, mapTypeToPreference);
        preferenceDao.flush();
        preferenceDao.clear();

        // Retrieve instance
        mapTypeToPreference.clear();
        searchInstanceNames.clear();
        newSearchLevels.clear();
        try {
            searchInstanceEjb.fetchInstances( ColumnEntity.LAB_EVENT, mapTypeToPreference, searchInstanceNames, newSearchLevels);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Preference preference = mapTypeToPreference.get(PreferenceType.GLOBAL_LAB_EVENT_SEARCH_INSTANCES);
        SearchInstance fetchedSearchInstance = null;
        try {
            SearchInstanceList searchInstanceList =
                    (SearchInstanceList) preference.getPreferenceDefinition().getDefinitionValue();
            for (SearchInstance instance : searchInstanceList.getSearchInstances()) {
                if (instance.getName().equals(newSearchName)) {
                    fetchedSearchInstance = instance;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Assert.assertNotNull(fetchedSearchInstance);

        // Search
        fetchedSearchInstance.establishRelationships(configurableSearchDef);
        fetchedSearchInstance.postLoad();

        ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                fetchedSearchInstance, configurableSearchDef, null, 1, null, "ASC", entity);
        Assert.assertEquals(firstPageResults.getResultList().getResultRows().size(), 100);

        // Delete instance
        searchInstanceEjb.deleteSearch(new MessageCollection(), PreferenceType.GLOBAL_LAB_EVENT_SEARCH_INSTANCES, newSearchName, mapTypeToPreference);
    }

    /**
     * Create, execute, then delete a global saved mercury sample search instance
     */
    @Test
    public void testMercurySampleSearch() {

        // Login a fake user
        userBean.loginTestUser();

        // Create instance
        SearchInstance searchInstance = new SearchInstance();
        String entity = "MercurySample";
        ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity(entity);
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("LCSET",
                configurableSearchDef);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        // Validate 'LCSET-' will be prepended
        searchValue.setValues(Arrays.asList("6449"));

        // Add columns
        searchInstance.getPredefinedViewColumns().add("PDO");
        searchInstance.getPredefinedViewColumns().add("LCSET");
        searchInstance.getPredefinedViewColumns().add("Mercury Sample ID");
        searchInstance.getPredefinedViewColumns().add("Mercury Sample Tube Barcode");
        // Multi column
        searchInstance.getPredefinedViewColumns().add("All Sample Metadata");

        // Save instance
        Map<PreferenceType, Preference> mapTypeToPreference = new HashMap<>();
        Map<String,String> newSearchLevels = new HashMap<>();
        Map<String,String> searchInstanceNames = new HashMap<>();
        try {
            searchInstanceEjb.fetchInstances(ColumnEntity.MERCURY_SAMPLE, mapTypeToPreference, searchInstanceNames, newSearchLevels);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String newSearchName = "Test-" + entity + "-" + new SimpleDateFormat("MM/dd/yyyy").format(new Date(System.currentTimeMillis()));
        searchInstanceEjb.persistSearch(true, searchInstance, new MessageCollection(),
                PreferenceType.GLOBAL_MERCURY_SAMPLE_SEARCH_INSTANCES, newSearchName, mapTypeToPreference);
        preferenceDao.flush();
        preferenceDao.clear();

        // Retrieve instance
        mapTypeToPreference.clear();
        searchInstanceNames.clear();
        newSearchLevels.clear();
        try {
            searchInstanceEjb.fetchInstances( ColumnEntity.MERCURY_SAMPLE, mapTypeToPreference, searchInstanceNames, newSearchLevels);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Preference preference = mapTypeToPreference.get(PreferenceType.GLOBAL_MERCURY_SAMPLE_SEARCH_INSTANCES);
        SearchInstance fetchedSearchInstance = null;
        try {
            SearchInstanceList searchInstanceList =
                    (SearchInstanceList) preference.getPreferenceDefinition().getDefinitionValue();
            for (SearchInstance instance : searchInstanceList.getSearchInstances()) {
                if (instance.getName().equals(newSearchName)) {
                    fetchedSearchInstance = instance;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Assert.assertNotNull(fetchedSearchInstance);

        // Search
        fetchedSearchInstance.establishRelationships(configurableSearchDef);
        fetchedSearchInstance.postLoad();

        ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                fetchedSearchInstance, configurableSearchDef, null, 1, null, "ASC", entity);
        Assert.assertEquals(firstPageResults.getResultList().getResultRows().size(), 94);

        // Find sample 797366
        ConfigurableList.ResultRow row = null;
        for( ConfigurableList.ResultRow currentRow : firstPageResults.getResultList().getResultRows() ){
            if( currentRow.getResultId().equals("797366")) {
                row = currentRow;
            }
        }
        Assert.assertNotNull(row, "mercurySampleId 797366 not found in results");

        Map<String, Integer> columnNumbersByHeader = new HashMap<>();
        int columnNumber = 0;
        for (ConfigurableList.Header header : firstPageResults.getResultList().getHeaders()) {
            columnNumbersByHeader.put(header.getViewHeader(), columnNumber);
            columnNumber++;
        }

        // Verify data for sample 797366
        List<String> values = row.getRenderableCells();
        Assert.assertEquals( values.get(columnNumbersByHeader.get("PDO")),                                               "PDO-5115",    "Incorrect PDO Value");
        Assert.assertEquals( values.get(columnNumbersByHeader.get("LCSET")),                                             "LCSET-6449",  "Incorrect LCSET Value");
        Assert.assertEquals( values.get(columnNumbersByHeader.get("Mercury Sample ID")),                                 "SM-74PK6",    "Incorrect Mercury Sample ID Value");
        Assert.assertEquals( values.get(columnNumbersByHeader.get("Mercury Sample Tube Barcode")),                       "0175567583",  "Incorrect Mercury Sample Tube Barcode Value");
        Assert.assertEquals( values.get(columnNumbersByHeader.get(Metadata.Key.GENDER.getDisplayName())),                "Male",        "Incorrect Gender Value");
        Assert.assertEquals( values.get(columnNumbersByHeader.get(Metadata.Key.PATIENT_ID.getDisplayName())),            "12005-008",   "Incorrect Patient ID Value");
        Assert.assertEquals( values.get(columnNumbersByHeader.get(Metadata.Key.TUMOR_NORMAL.getDisplayName())),          "Normal",      "Incorrect Tumor/Normal Value");
        Assert.assertEquals( values.get(columnNumbersByHeader.get(Metadata.Key.BUICK_COLLECTION_DATE.getDisplayName())), "06/10/2013",  "Incorrect Collection Date Value");
        Assert.assertEquals( values.get(columnNumbersByHeader.get(Metadata.Key.SAMPLE_ID.getDisplayName())),             "23102117605", "Incorrect Sample ID Value");
        Assert.assertEquals( values.get(columnNumbersByHeader.get(Metadata.Key.BUICK_VISIT.getDisplayName())),           "Screening",   "Incorrect Incorrect Visit Value");

        // Delete instance
        searchInstanceEjb.deleteSearch(new MessageCollection(), PreferenceType.GLOBAL_MERCURY_SAMPLE_SEARCH_INSTANCES, newSearchName, mapTypeToPreference);
    }
}
