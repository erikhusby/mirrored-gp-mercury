package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
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
    private BSPUserList bspUserList;

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

        ConfigurableSearchDefinition configurableSearchDef =
                SearchDefinitionFactory.getForEntity( ColumnEntity.LAB_VESSEL.getEntityName());
        for (Map.Entry<String, List<ColumnTabulation>> groupListSearchTermEntry :
                configurableSearchDef.getMapGroupToColumnTabulations().entrySet()) {
            for (ColumnTabulation searchTerm : groupListSearchTermEntry.getValue()) {
                // Some search terms are not available for selection in result list
                // TODO Push method from SearchTerm to ColumnTabulation superclass (or consolidate)
                if( !((SearchTerm) searchTerm).isExcludedFromResultColumns() ) {
                    columnTabulations.add(searchTerm);
                }
            }
        }

        ConfigurableList configurableList = new ConfigurableList(columnTabulations, 1, "ASC", ColumnEntity.LAB_VESSEL);

        // Add any row listeners
        ConfigurableSearchDefinition.AddRowsListenerFactory addRowsListenerFactory = configurableSearchDef.getAddRowsListenerFactory();
        if( addRowsListenerFactory != null ) {
            for( Map.Entry<String,ConfigurableList.AddRowsListener> entry : addRowsListenerFactory.getAddRowsListeners().entrySet() ) {
                configurableList.addAddRowsListener(entry.getKey(), entry.getValue());
            }
        }

        Map<String, Object> context = buildSearchContext();
        configurableList.addRows(labVessels, context);

        ConfigurableList.ResultList resultList = configurableList.getResultList();

        Assert.assertEquals(resultList.getResultRows().size(), 92);
        // todo jmt why does 1090466540 return nothing for stock sample?  Search in Mercury shows SM-4NFIJ, BSP shows terminated, material type doesn't include "active stock"

        ConfigurableList.ResultRow resultRow = resultList.getResultRows().get(1);
        Assert.assertEquals(resultRow.getResultId(), "0162998809");

        // Find Imported Sample ID
        List<ConfigurableList.Header> headers = resultList.getHeaders();
        int columnIndex = 0;
        for( ConfigurableList.Header header : headers ) {
            if( header.getViewHeader().equals("Imported Sample ID")) break;
            columnIndex++;
        }
        Assert.assertTrue( columnIndex < headers.size(), "Column header 'Imported Sample ID' not found in results" );
        Assert.assertEquals(resultRow.getRenderableCells().get(columnIndex), "SM-5KWVC");

    }

    /**
     *  BSP user lookup required in column eval expression
     *  Use context to avoid need to test in container
     */
    private Map<String, Object> buildSearchContext(){
        Map<String, Object> evalContext = new HashMap<>();
        evalContext.put(SearchDefinitionFactory.CONTEXT_KEY_BSP_USER_LIST, bspUserList );

        return evalContext;
    }
}
