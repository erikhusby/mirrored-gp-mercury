package org.broadinstitute.gpinformatics.infrastructure.search;

//import com.jprofiler.api.agent.Controller;

import org.apache.commons.lang3.StringUtils;
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
        String entity = ColumnEntity.LAB_EVENT.getEntityName();
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
        searchInstance.getPredefinedViewColumns().add("Root Sample ID");
        searchInstance.getPredefinedViewColumns().add("Source Layout");
        searchInstance.getPredefinedViewColumns().add("Destination Layout");

        searchInstance.establishRelationships(configurableSearchDef);

        List<String> columnNameList = searchInstance.getPredefinedViewColumns();
        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        for (String columnName : columnNameList) {
            columnTabulations.add(configurableSearchDef.getSearchTerm(columnName));
        }
        columnTabulations.addAll(searchInstance.findTopLevelColumnTabulations());

        ConfigurableList configurableList = new ConfigurableList(columnTabulations, Collections.EMPTY_MAP, 0, "ASC", ColumnEntity.LAB_EVENT);

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
        Assert.assertEquals(data[2][0].toString(), "Source Layout" );
        Assert.assertEquals(data[3][1].toString(), "01");
        Assert.assertEquals(data[3][12].toString(), "12" );
        Assert.assertEquals(data[7][1].toString(), "0154862184");
        // Parent term handled by child
        Assert.assertEquals(data[9][1].toString(), "SM-4CRI7");
        Assert.assertEquals(data[25][0].toString(), "H");
        // Matrix 96 layout nested table
        Assert.assertEquals(data[28][0].toString(), "Destination Layout");
        Assert.assertEquals(data[29][1].toString(), "01" );
        Assert.assertEquals(data[29][12].toString(), "12" );
        Assert.assertEquals(data[30][0].toString(), "A" );
        Assert.assertEquals(data[30][1].toString(), "0116400397");
        // Parent term handled by child
        Assert.assertEquals(data[59][1].toString(), "SM-4CRI7");
        Assert.assertEquals(data[121][0].toString(), "H");

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
        String entity = ColumnEntity.MERCURY_SAMPLE.getEntityName();
        ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity(entity);
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("LCSET",
                configurableSearchDef);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        // Validate 'LCSET-' will be prepended
        searchValue.setValues(Arrays.asList("6449"));

        // Add columns
        searchInstance.getPredefinedViewColumns().add("PDO");
        searchInstance.getPredefinedViewColumns().add("LCSET");
        searchInstance.getPredefinedViewColumns().add("Root Sample ID");
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
        Assert.assertEquals( values.get(columnNumbersByHeader.get("Root Sample ID")),                                    "SM-74PK6",    "Incorrect Root Sample ID Value");
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
        String entity = ColumnEntity.LAB_VESSEL.getEntityName();
        ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity(entity);

        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("PDO", configurableSearchDef);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("PDO-7013"));

        searchInstance.getPredefinedViewColumns().add("Barcode");
        searchInstance.getPredefinedViewColumns().add("DNA Extracted Tube Barcode");

        ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                searchInstance, configurableSearchDef, null, 1, null, "ASC", entity);
        List<ConfigurableList.ResultRow> resultRows = firstPageResults.getResultList().getResultRows();
        Assert.assertEquals(resultRows.size(), 4);
        Assert.assertEquals(resultRows.get(0).getRenderableCells().get(0), "0175568179");
        Assert.assertEquals(resultRows.get(1).getRenderableCells().get(0), "0175568200");
        Assert.assertEquals(resultRows.get(2).getRenderableCells().get(0), "SM-A19ZM");
        Assert.assertEquals(resultRows.get(2).getRenderableCells().get(1), "0175568200");
        Assert.assertEquals(resultRows.get(3).getRenderableCells().get(0), "SM-A19Z9");
        Assert.assertEquals(resultRows.get(3).getRenderableCells().get(1), "E000000293 0175568179");
    }

    @Test
    public void testInfinium() {
//        Controller.startCPURecording(true);
        SearchInstance searchInstance = new SearchInstance();
        String entity = ColumnEntity.LAB_VESSEL.getEntityName();
        ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity(entity);

        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("PDO", configurableSearchDef);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("PDO-9246"));

        searchInstance.getPredefinedViewColumns().add("Infinium DNA Plate Drill Down");
        searchInstance.getPredefinedViewColumns().add("Infinium Amp Plate Drill Down");
        searchInstance.setCustomTraversalOptionName(InfiniumVesselTraversalEvaluator.DNA_PLATE_INSTANCE.getUiName());
        searchInstance.setExcludeInitialEntitiesFromResults(true);
        searchInstance.getTraversalEvaluatorValues().put(LabEventSearchDefinition.TraversalEvaluatorName.DESCENDANTS.getId(), Boolean.TRUE);
        searchInstance.getTraversalEvaluatorValues().put(LabEventSearchDefinition.TraversalEvaluatorName.ANCESTORS.getId(), Boolean.FALSE);

        ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                searchInstance, configurableSearchDef, null, 0, null, "ASC", entity);
        List<ConfigurableList.ResultRow> resultRows = firstPageResults.getResultList().getResultRows();
        Assert.assertEquals(resultRows.size(), 38);
//        Controller.stopCPURecording();
        Assert.assertEquals(resultRows.get(0).getRenderableCells().get(0), "CO-15138260");
        Assert.assertEquals(resultRows.get(1).getRenderableCells().get(0), "CO-18828951");
        Assert.assertEquals(resultRows.get(1).getRenderableCells().get(1), "000016825709");
    }

    /**
     * Tests LabVesselLatestPositionPlugin using one tube prior to implementation of event transfer ancillary vessels,
     *   and the other after implementation.
     * Hopefully both old enough and/or discarded so they're never used again in newer workflows.
     */
    @Test
    public void testVesselPositionPlugin(){
        SearchInstance searchInstance = new SearchInstance();
        String entity = ColumnEntity.LAB_VESSEL.getEntityName();
        ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity(entity);

        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("Barcode", configurableSearchDef);
        searchValue.setOperator(SearchInstance.Operator.IN);

        searchValue.setValues(Arrays.asList("0157493754","0175362315"));

        searchInstance.getPredefinedViewColumns().add("Barcode");
        searchInstance.getPredefinedViewColumns().add("Most Recent Rack and Event");

        ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                searchInstance, configurableSearchDef, null, 0, null, "ASC", entity);
        List<ConfigurableList.ResultRow> resultRows = firstPageResults.getResultList().getResultRows();
        Assert.assertEquals(resultRows.size(), 2);
        Assert.assertEquals(resultRows.get(0).getRenderableCells().get(0), "0157493754");
        Assert.assertEquals(resultRows.get(0).getRenderableCells().get(1), "000006677301");
        Assert.assertEquals(resultRows.get(0).getRenderableCells().get(2), "A06");
        Assert.assertEquals(resultRows.get(0).getRenderableCells().get(3), "Hybridization, 03/03/2014, Cassie Crawford");
        Assert.assertEquals(resultRows.get(1).getRenderableCells().get(0), "0175362315");
        Assert.assertEquals(resultRows.get(1).getRenderableCells().get(1), "000003038103");
        Assert.assertEquals(resultRows.get(1).getRenderableCells().get(2), "E09");
        Assert.assertEquals(resultRows.get(0).getRenderableCells().get(3), "FingerprintingPlateSetup, 10/29/2014, Michael Wilson");
    }

    @Test
    public void testViewOnlyProductOrderType() throws Exception {

        userBean.loginViewOnlyUser();

        SearchInstance searchInstance = new SearchInstance();
        final String productOrderEntityName = ColumnEntity.PRODUCT_ORDER.getEntityName();
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity(productOrderEntityName);

        SearchInstance.SearchValue quoteSearchValue = searchInstance.addTopLevelTerm("Quote Identifier",
                configurableSearchDefinition);
        quoteSearchValue.setOperator(SearchInstance.Operator.EQUALS);
        quoteSearchValue.setValues(Collections.singletonList("GPSPIE8"));

        searchInstance.getPredefinedViewColumns().add(ProductOrderSearchDefinition.QUOTE_IDENTIFIER_COLUMN_HEADER);
        searchInstance.getPredefinedViewColumns().add(ProductOrderSearchDefinition.PDO_TICKET_COLUMN_HEADER);
        searchInstance.getPredefinedViewColumns().add(ProductOrderSearchDefinition.RESEARCH_PROJECT_COLUMN_HEADER);
        searchInstance.getPredefinedViewColumns().add(ProductOrderSearchDefinition.PRODUCT_ORDER_SAMPLES_COLUMN_HEADER);
        searchInstance.getPredefinedViewColumns().add(ProductOrderSearchDefinition.PRODUCTS_COLUMN_HEADER);


        Map<PreferenceType, Preference> mapTypeToPreference = new HashMap<>();
        Map<String,String> newSearchLevels = new HashMap<>();
        Map<String,String> searchInstanceNames = new HashMap<>();
        try {
            searchInstanceEjb.fetchInstances(ColumnEntity.PRODUCT_ORDER, mapTypeToPreference, searchInstanceNames,
                    newSearchLevels);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String newSearchName = "Test-" + productOrderEntityName + "-" +
                               new SimpleDateFormat("MM/dd/yyyy").format(new Date(System.currentTimeMillis()));
        searchInstanceEjb.persistSearch(true, searchInstance, new MessageCollection(),
                PreferenceType.GLOBAL_PRODUCT_ORDER_SEARCH_INSTANCES, newSearchName, mapTypeToPreference);
        preferenceDao.flush();
        preferenceDao.clear();

        // Retrieve instance
        mapTypeToPreference.clear();
        searchInstanceNames.clear();
        newSearchLevels.clear();
        try {
            searchInstanceEjb.fetchInstances( ColumnEntity.PRODUCT_ORDER, mapTypeToPreference, searchInstanceNames, newSearchLevels);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Preference preference = mapTypeToPreference.get(PreferenceType.GLOBAL_PRODUCT_ORDER_SEARCH_INSTANCES);
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


        fetchedSearchInstance.establishRelationships(configurableSearchDefinition);
        fetchedSearchInstance.postLoad();

        testSearch(fetchedSearchInstance, productOrderEntityName, configurableSearchDefinition);

        searchInstanceEjb.deleteSearch(new MessageCollection(), PreferenceType.GLOBAL_PRODUCT_ORDER_SEARCH_INSTANCES,
                newSearchName, mapTypeToPreference);
    }

    @Test
    public void testProductOrderType() throws Exception {

        userBean.loginOSUser();

        SearchInstance searchInstance = new SearchInstance();
        final String productOrderEntityName = ColumnEntity.PRODUCT_ORDER.getEntityName();
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity(productOrderEntityName);

        SearchInstance.SearchValue quoteSearchValue = searchInstance.addTopLevelTerm("Quote Identifier",
                configurableSearchDefinition);
        quoteSearchValue.setOperator(SearchInstance.Operator.EQUALS);
        quoteSearchValue.setValues(Collections.singletonList("GPSPIE8"));

        searchInstance.getPredefinedViewColumns().add(ProductOrderSearchDefinition.QUOTE_IDENTIFIER_COLUMN_HEADER);
        searchInstance.getPredefinedViewColumns().add(ProductOrderSearchDefinition.PDO_TICKET_COLUMN_HEADER);
        searchInstance.getPredefinedViewColumns().add(ProductOrderSearchDefinition.RESEARCH_PROJECT_COLUMN_HEADER);
        searchInstance.getPredefinedViewColumns().add(ProductOrderSearchDefinition.PRODUCT_ORDER_SAMPLES_COLUMN_HEADER);
        searchInstance.getPredefinedViewColumns().add(ProductOrderSearchDefinition.PRODUCTS_COLUMN_HEADER);

        searchInstance.establishRelationships(configurableSearchDefinition);
        searchInstance.postLoad();

        testSearch(searchInstance, productOrderEntityName, configurableSearchDefinition);
    }

    private void testSearch(SearchInstance searchInstance, String productOrderEntityName,
                            ConfigurableSearchDefinition configurableSearchDefinition) {
        ConfigurableListFactory.FirstPageResults pageResults =
                configurableListFactory.getFirstResultsPage(searchInstance, configurableSearchDefinition,
                        null, 1, null, "ASC",
                        productOrderEntityName);

        ConfigurableList.ResultRow row = null;
        for( ConfigurableList.ResultRow currentRow : pageResults.getResultList().getResultRows() ){
            row = currentRow;
            break;
        }

        Map<String, Integer> colunnNumbersByHeader = new HashMap<>();
        int columnNumber = 0;
        for (ConfigurableList.Header header : pageResults.getResultList().getHeaders()) {
            colunnNumbersByHeader.put(header.getViewHeader(), columnNumber);
            columnNumber++;
        }

        List<String> rowValues = row.getRenderableCells();
        Assert.assertEquals(rowValues.get(colunnNumbersByHeader.get(ProductOrderSearchDefinition.PDO_TICKET_COLUMN_HEADER)), "Draft-220113 -- Johan Nilsson_Lund University_Heart Transplant_PCR-PLUS_FFPE_XXTimepoints", "Incorrect PDO found");
        Assert.assertEquals(rowValues.get(colunnNumbersByHeader.get(ProductOrderSearchDefinition.QUOTE_IDENTIFIER_COLUMN_HEADER)), "GPSPIE8", "Incorrect quote found");
        Assert.assertEquals(rowValues.get(colunnNumbersByHeader.get(ProductOrderSearchDefinition.RESEARCH_PROJECT_COLUMN_HEADER)), "RP-797", "Incorrect RP found");

        final String samples = rowValues
                .get(colunnNumbersByHeader.get(ProductOrderSearchDefinition.PRODUCT_ORDER_SAMPLES_COLUMN_HEADER));

        List<String> testSamples = Arrays.asList("SM-D8WVS", "SM-D8WVT", "SM-D8WVU", "SM-D8WVV", "SM-D8WVX",
                "SM-D8WW2", "SM-D8WW4", "SM-D8WW5", "SM-D8WW7", "SM-D8WW8", "SM-D8WW9", "SM-D8WJL", "SM-D8WJM",
                "SM-D8WJN", "SM-D8WJO", "SM-D8WJP", "SM-D8WJQ", "SM-D8WJR");

        Assert.assertTrue(Arrays.stream(StringUtils.split(samples, " ")).allMatch(testSamples::contains),
                "Incorrect Samples found");

        Assert.assertTrue(rowValues.get(colunnNumbersByHeader.get(ProductOrderSearchDefinition.PRODUCTS_COLUMN_HEADER)).contains("XTNL-WGS-010307"), "Incorrect product part number found");
    }
}
