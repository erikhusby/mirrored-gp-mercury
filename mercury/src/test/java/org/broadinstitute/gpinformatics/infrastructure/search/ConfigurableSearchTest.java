package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.entity.preference.SearchInstanceList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnTabulation;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test creating, saving, retrieving and executing a search.
 */
@Test(groups = TestGroups.STANDARD)
public class ConfigurableSearchTest extends Arquillian {

    @Inject
    private SearchInstanceEjb searchInstanceEjb;

    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private ConfigurableListFactory configurableListFactory;

    @Inject
    private UserBean userBean;

    @Inject
    private LabEventDao labEventDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

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
        searchInstance.getPredefinedViewColumns().add("EventDate");
        searchInstance.getPredefinedViewColumns().add("EventType");

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
        Assert.assertEquals(firstPageResults.getPagination().getIdList().size(), 736);

        // Default sort is entity ID column (labEventId) ascending
        Assert.assertEquals(
                firstPageResults.getResultList().getResultRows().get(0).getRenderableCells().get(0), "508066");

        // Re-sort on labEventId descending using database sort
        firstPageResults = configurableListFactory.getFirstResultsPage(
                fetchedSearchInstance, configurableSearchDef, null, null, "labEventId", "DSC", entity);

        Assert.assertEquals(
                firstPageResults.getResultList().getResultRows().get(0).getRenderableCells().get(0), "508801");

        // Delete instance
        searchInstanceEjb.deleteSearch(new MessageCollection(), PreferenceType.GLOBAL_LAB_EVENT_SEARCH_INSTANCES, newSearchName, mapTypeToPreference);
    }


    /**
     * Execute a lab event search with nested table plugins
     *  and search term parent handled by child when creating a spreadsheet download.
     * The raw data source for an Excel download is a 2D array
     */
    @Test
    public void testSearchResultsDownload() {

        // Login a fake user
        userBean.loginTestUser();

        // Create a search instance
        SearchInstance searchInstance = new SearchInstance();
        ConfigurableSearchDefinition configurableSearchDef =
                SearchDefinitionFactory.getForEntity(ColumnEntity.LAB_EVENT.getEntityName());
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("LabEventId",
                configurableSearchDef);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Arrays.asList("268634"));

        // Add columns
        searchInstance.getPredefinedViewColumns().add("LabEventId");
        searchInstance.getPredefinedViewColumns().add("Mercury Sample ID");
        searchInstance.getPredefinedViewColumns().add("Source Layout");
        searchInstance.getPredefinedViewColumns().add("Destination Layout");

        searchInstance.establishRelationships(configurableSearchDef);

        List<String> columnNameList = searchInstance.getPredefinedViewColumns();
        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        for (String columnName : columnNameList) {
            columnTabulations.add(configurableSearchDef.getSearchTerm(columnName));
        }
        columnTabulations.addAll(searchInstance.findTopLevelColumnTabulations());

        ConfigurableList configurableList = new ConfigurableList(columnTabulations, 0, "ASC", ColumnEntity.LAB_EVENT);

        LabEvent labEvent = labEventDao.findById(LabEvent.class, new Long("268634"));
        List<LabEvent> entityList = new ArrayList<>();
        entityList.add(labEvent);

        SearchContext searchContext = new SearchContext();
        searchContext.setSearchInstance(searchInstance);
        searchContext.setColumnEntityType(ColumnEntity.LAB_EVENT);

        configurableList.addRows(entityList, searchContext);

        // Does the logic to extract values from the entity for display
        // Creates a 2D array to pass to spreadsheet creator
        Object[][] data = configurableList.getResultList(false).getAsArray();

        Assert.assertEquals(data[0][0].toString(), "LabEventId" );
        Assert.assertEquals(data[1][0].toString(), "268634" );
        // Matrix 96 layout nested table
        Assert.assertEquals(data[2][1].toString(), "Source Layout" );
        Assert.assertEquals(data[3][2].toString(), "01");
        Assert.assertEquals(data[3][13].toString(), "12" );
        Assert.assertTrue(data[5][2].toString().indexOf("0154862184") >= 0);
        // Parent term handled by child
        Assert.assertTrue(data[5][2].toString().indexOf("SM-4CRI7") >= 0);
        Assert.assertEquals(data[11][1].toString(), "H");
        // Matrix 96 layout nested table
        Assert.assertEquals(data[12][1].toString(), "Destination Layout");
        Assert.assertEquals(data[13][2].toString(), "01" );
        Assert.assertEquals(data[13][13].toString(), "12" );
        Assert.assertEquals(data[14][1].toString(), "A" );
        Assert.assertTrue(data[14][2].toString().indexOf("0116400397") >= 0);
        // Parent term handled by child
        Assert.assertTrue(data[14][2].toString().indexOf("SM-4CRI7") >= 0);
        Assert.assertEquals(data[21][1].toString(), "H");

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
                break;
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

        // Test in-memory sorting of 1 page of results, sort on Patient ID
        columnNumber = columnNumbersByHeader.get(Metadata.Key.PATIENT_ID.getDisplayName());
        firstPageResults = configurableListFactory.getFirstResultsPage(
                fetchedSearchInstance, configurableSearchDef, null, columnNumber, null, "DSC", entity);

        row = firstPageResults.getResultList().getResultRows().get(0);
        Assert.assertEquals( row.getRenderableCells().get(columnNumber), "63014-003");

        row = firstPageResults.getResultList().getResultRows().get(93);
        Assert.assertEquals( row.getRenderableCells().get(columnNumber), "02002-009");

        // Delete instance
        searchInstanceEjb.deleteSearch(new MessageCollection(), PreferenceType.GLOBAL_MERCURY_SAMPLE_SEARCH_INSTANCES, newSearchName, mapTypeToPreference);
    }

    @Test
    public void testEventMaterialType() {
        SearchInstance searchInstance = new SearchInstance();
        String entity = "LabVessel";
        ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity(entity);

        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("PDO", configurableSearchDef);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("PDO-7013"));

        searchInstance.getPredefinedViewColumns().add("Barcode");
        searchInstance.getPredefinedViewColumns().add("DNA Extracted Barcode");

        ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                searchInstance, configurableSearchDef, null, 1, null, "ASC", entity);
        Assert.assertEquals(firstPageResults.getResultList().getResultRows().size(), 4);
    }
}
