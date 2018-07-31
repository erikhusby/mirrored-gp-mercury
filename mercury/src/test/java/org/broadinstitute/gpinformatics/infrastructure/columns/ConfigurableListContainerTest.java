package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.PaginationUtil;
import org.broadinstitute.gpinformatics.infrastructure.search.ResultParamValues;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test configurable list against database and BSP
 */
@Test(groups = TestGroups.STANDARD)
public class ConfigurableListContainerTest extends Arquillian {

    @Inject
    private BSPSampleSearchService bspSampleSearchService;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private UserBean userBean;

    @Inject
    private JiraConfig jiraConfig;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    public void testTrackingSheet() {
        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        LabBatch labBatch = labBatchDao.findByBusinessKey("LCSET-5102");
        List<LabVessel> labVessels = new ArrayList<>();
        for (BucketEntry bucketEntry : labBatch.getBucketEntries()) {
            labVessels.add(bucketEntry.getLabVessel());
        }

        // List order of result columns is flexible, sort default is on first column, explicit sort on barcode.
        int index = -1;
        int barCodeColumnIndex = 0;

        ConfigurableSearchDefinition configurableSearchDef =
                SearchDefinitionFactory.getForEntity( ColumnEntity.LAB_VESSEL.getEntityName());
        for (Map.Entry<String, List<ColumnTabulation>> groupListSearchTermEntry :
                configurableSearchDef.getMapGroupToColumnTabulations().entrySet()) {
            for (ColumnTabulation searchTerm : groupListSearchTermEntry.getValue()) {
                // Some search terms are not available for selection in result list
                // TODO Push method from SearchTerm to ColumnTabulation superclass (or consolidate)
                if( !((SearchTerm) searchTerm).isExcludedFromResultColumns() ) {
                    index++;
                    columnTabulations.add(searchTerm);
                    if( searchTerm.getName().equals("Barcode")) {
                        barCodeColumnIndex = index;
                    }
                }
            }
        }

        ConfigurableList configurableList = new ConfigurableList(columnTabulations, Collections.EMPTY_MAP, barCodeColumnIndex, "ASC", ColumnEntity.LAB_VESSEL);
        // Add any row listeners
        configurableList.addAddRowsListeners(configurableSearchDef);

        SearchContext context = buildSearchContext();
        context.setSearchInstance(buildDummyVesselSearchInstance(configurableSearchDef));

        configurableList.addRows(labVessels, context);

        ConfigurableList.ResultList resultList = configurableList.getResultList();

        Assert.assertEquals(resultList.getResultRows().size(), 92);
        // todo jmt why does 1090466540 return nothing for stock sample?  Search in Mercury shows SM-4NFIJ, BSP shows terminated, material type doesn't include "active stock"

        ConfigurableList.ResultRow resultRow = resultList.getResultRows().get(1);
        Assert.assertEquals(resultRow.getResultId(), "0162998809");

        // Test column values
        List<ConfigurableList.Header> headers = resultList.getHeaders();
        int columnIndex;

        // Find Imported Sample ID
        columnIndex = 0;
        for( ConfigurableList.Header header : headers ) {
            if( header.getViewHeader().equals("Imported Sample ID")) break;
            columnIndex++;
        }
        Assert.assertTrue( columnIndex < headers.size(), "Column header 'Imported Sample ID' not found in results" );
        Assert.assertEquals(resultRow.getRenderableCells().get(columnIndex), "SM-5KWVC");

        // Test LabVesselMetricPlugin
        columnIndex = 0;
        for( ConfigurableList.Header header : headers ) {
            if( header.getViewHeader().equals("Pond Pico ng/uL")) break;
            columnIndex++;
        }
        Assert.assertTrue( columnIndex < headers.size(), "Column header 'Pond Pico ng/uL' not found in results" );
        Assert.assertEquals(resultRow.getRenderableCells().get(columnIndex), "54.68");
        Assert.assertEquals(resultRow.getRenderableCells().get(columnIndex + 1), "(None)"
                , "Incorrect value for 'Pond Pico Decision' column" );

        columnIndex = 0;
        for( ConfigurableList.Header header : headers ) {
            if( header.getViewHeader().equals("ECO QPCR ng/uL")) break;
            columnIndex++;
        }
        Assert.assertTrue( columnIndex < headers.size(), "Column header 'ECO QPCR ng/uL' not found in results" );
        Assert.assertEquals(resultRow.getRenderableCells().get(columnIndex), "26.13");
        Assert.assertEquals(resultRow.getRenderableCells().get(columnIndex + 1), "(None)" );

        // Test LabVesselLatestEventPlugin
        columnIndex = 0;
        for( ConfigurableList.Header header : headers ) {
            if( header.getViewHeader().equals("Latest Event")) break;
            columnIndex++;
        }
        Assert.assertTrue( columnIndex < headers.size(), "Column header 'Latest Event' not found in results" );
        Assert.assertEquals(resultRow.getRenderableCells().get(columnIndex), "DilutionToFlowcellTransfer");
        Assert.assertEquals(resultRow.getRenderableCells().get(columnIndex + 3), "03/06/2014 12:44:45" );


    }


    /**
     * This test verifies the stability of the LabVesselSearchDefinition.VesselsForEventTraverserCriteria
     * Using a sample vessel, validate shearing tube and flowcell are found in the descendant traversal
     */
    public void testVesselDescendantLookups() {
        List<ColumnTabulation> columnTabulations = new ArrayList<>();

        ConfigurableSearchDefinition configurableSearchDef =
                SearchDefinitionFactory.getForEntity( ColumnEntity.LAB_VESSEL.getEntityName());

        columnTabulations.add(configurableSearchDef.getSearchTerm("Barcode"));
        columnTabulations.add(configurableSearchDef.getSearchTerm("Nearest Sample ID"));
        columnTabulations.add(configurableSearchDef.getSearchTerm("Imported Sample ID"));
        columnTabulations.add(configurableSearchDef.getSearchTerm("Event Vessel Barcodes"));
        columnTabulations.add(configurableSearchDef.getSearchTerm("Event Vessel Barcodes"));
        columnTabulations.add(configurableSearchDef.getSearchTerm("Event Vessel Barcodes"));

        Map<Integer,ResultParamValues> resultParamsMap = new HashMap<>();

        // Configure and map result params to "Event Vessel Barcodes" search terms
        ResultParamValues sampleParams = new ResultParamValues("Event Vessel Barcodes", "Sample Barcode");
        sampleParams.addParamValue("eventTypes", "SAMPLE_IMPORT");
        resultParamsMap.put(new Integer(3), sampleParams );

        ResultParamValues shearingParams = new ResultParamValues("Event Vessel Barcodes", "Shearing Barcode");
        shearingParams.addParamValue("eventTypes", "SHEARING_TRANSFER");
        shearingParams.addParamValue("srcOrTarget", "source");
        resultParamsMap.put(new Integer(4), shearingParams );

        ResultParamValues flowcellParams = new ResultParamValues("Event Vessel Barcodes", "Flowcell Barcode");

        flowcellParams.addParamValue("eventTypes", "FLOWCELL_TRANSFER" );
        flowcellParams.addParamValue("eventTypes", "DENATURE_TO_FLOWCELL_TRANSFER");
        flowcellParams.addParamValue("eventTypes", "DILUTION_TO_FLOWCELL_TRANSFER");
        resultParamsMap.put(new Integer(5), flowcellParams );

        ConfigurableList configurableList = new ConfigurableList(columnTabulations, resultParamsMap, 1, "ASC", ColumnEntity.LAB_VESSEL);

        // Add any row listeners
        configurableList.addAddRowsListeners(configurableSearchDef);

        SearchContext context = buildSearchContext();
        context.setSearchInstance(buildDummyVesselSearchInstance(configurableSearchDef));

        configurableList.addRows(labVesselDao.findBySampleKey("SM-7RDNO"), context);

        ConfigurableList.ResultList resultList = configurableList.getResultList();

        Assert.assertEquals(resultList.getResultRows().size(), 1);

        ConfigurableList.ResultRow resultRow = resultList.getResultRows().get(0);
        Assert.assertEquals(resultRow.getResultId(), "1109099877");

        // Test column values
        // Barcode
        Assert.assertEquals(resultRow.getRenderableCells().get(0), "1109099877");
        // Nearest Sample ID
        Assert.assertEquals(resultRow.getRenderableCells().get(1), "SM-7RDNO");
        // Imported Sample ID
        Assert.assertEquals(resultRow.getRenderableCells().get(2), "SM-9MRYP");
        // Imported Sample Tube Barcode
        Assert.assertEquals(resultRow.getRenderableCells().get(3), "0175488349");
        // Shearing Sample Barcode
        Assert.assertEquals(resultRow.getRenderableCells().get(4), "0175488349");
        // Flowcell Barcode
        Assert.assertTrue(resultRow.getRenderableCells().get(5).contains("HJJH5ADXX"));

    }

    /**
     *  BSP user lookup required in column eval expression
     *  Use context to avoid need to test in container
     */
    private SearchContext buildSearchContext(){
        SearchContext evalContext = new SearchContext();
        evalContext.setBspUserList( bspUserList );
        evalContext.setPagination(new PaginationUtil.Pagination(1));
        evalContext.setUserBean(userBean);
        evalContext.setJiraConfig(jiraConfig);
        return evalContext;
    }

    /**
     * Some result display logic in LabVesselSearchDefinition requires a SearchInstance with a SearchValue in context. <br />
     * (As of 07/2016, the display of Infinium array related column values will be quietly ignored) <br />
     * SearchValue is ignored because this test validates ConfigurableList.addRows(...) with a list of entities
     */
    private SearchInstance buildDummyVesselSearchInstance( ConfigurableSearchDefinition configurableSearchDef){

        SearchInstance searchInstance = new SearchInstance();
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("Barcode",
                configurableSearchDef);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Arrays.asList("IGNORED"));
        return searchInstance;
    }
}
