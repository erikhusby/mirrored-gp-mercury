package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
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
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test configurable list against database and BSP
 */
public class ConfigurableListContainerTest extends Arquillian {

    @Inject
    private BSPSampleSearchService bspSampleSearchService;

    @Inject
    private LabBatchDao labBatchDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testTrackingSheet() {
        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        LabBatch labBatch = labBatchDao.findByBusinessKey("LCSET-5102");
        List<LabVessel> labVessels = new ArrayList<>();
        for (BucketEntry bucketEntry : labBatch.getBucketEntries()) {
            labVessels.add(bucketEntry.getLabVessel());
        }

        ConfigurableSearchDefinition configurableSearchDefinition =
                new SearchDefinitionFactory().buildLabVesselSearchDef();
        for (Map.Entry<String, List<ColumnTabulation>> groupListSearchTermEntry :
                configurableSearchDefinition.getMapGroupToColumnTabulations().entrySet()) {
            for (ColumnTabulation searchTerm : groupListSearchTermEntry.getValue()) {
                columnTabulations.add(searchTerm);
            }
        }

        ConfigurableList configurableList = new ConfigurableList(columnTabulations, 1, "ASC", ColumnEntity.LAB_VESSEL);
        configurableList.addListener(new BspSampleSearchAddRowsListener(bspSampleSearchService));

        configurableList.addRows(labVessels);

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
}
