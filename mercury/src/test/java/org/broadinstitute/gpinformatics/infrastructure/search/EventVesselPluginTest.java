package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.EventVesselSourcePositionPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.EventVesselTargetPositionPlugin;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.HashMap;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Validate that the event vessel plugin locates and builds container arrays correctly
 */
@Test(groups = TestGroups.STANDARD)
public class EventVesselPluginTest extends Arquillian {

    @Inject
    private LabEventDao labEventDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    public void testNestedTablePlugin() {

        LabEvent labEvent = labEventDao.findById(LabEvent.class, new Long(617246));

        EventVesselSourcePositionPlugin eventVesselSourcePositionPlugin;
        try {
            eventVesselSourcePositionPlugin =
                    EventVesselSourcePositionPlugin.class.newInstance();
        } catch( Exception ex ) {
            throw new RuntimeException("Instantiation failure", ex );
        }

        // ColumnTabulation and context not required for vessel position matrix
        ConfigurableList.ResultList resultList = eventVesselSourcePositionPlugin.getNestedTableData(labEvent, null, new HashMap<String,Object>());
        // Matrix96 12 columns + label on left, 8 rows + header label
        // Event ia a PoolingTransfer of all tubes into A-01 position

        // Array conversion for spreadsheet export
        Object[][] arrayOutput = resultList.getAsArray();
        Assert.assertEquals( arrayOutput.length, 10, "Raw data array should have 10 discrete rows" );
        Assert.assertEquals( arrayOutput[0].length, 13, "Raw data array should have 13 discrete columns" );

        // Data
        Assert.assertEquals( resultList.getHeaders().size(), 13 );
        Assert.assertEquals( resultList.getResultRows().size(), 8 );

        Assert.assertEquals( resultList.getHeaders().get(0).getViewHeader(),  "");
        Assert.assertEquals( resultList.getHeaders().get(1).getViewHeader(),  "01");
        Assert.assertEquals( resultList.getHeaders().get(12).getViewHeader(), "12");
        Assert.assertEquals( resultList.getResultRows().get(0).getRenderableCells().get(0), "A");
        Assert.assertEquals( resultList.getResultRows().get(7).getRenderableCells().get(0), "H");
        Assert.assertEquals( resultList.getResultRows().get(0).getRenderableCells().get(2), "Vessel Barcode: 0173524221");

        EventVesselTargetPositionPlugin eventVesselTargetPositionPlugin;
        try {
            eventVesselTargetPositionPlugin =
                    EventVesselTargetPositionPlugin.class.newInstance();
        } catch( Exception ex ) {
            throw new RuntimeException("Instantiation failure", ex );
        }

        // ColumnTabulation and context not required for vessel position matrix
        resultList = eventVesselTargetPositionPlugin.getNestedTableData(labEvent, null, new HashMap<String,Object>());
        // Matrix96 12 columns + label on left, 8 rows + header label
        Assert.assertEquals( resultList.getHeaders().size(), 13 );
        Assert.assertEquals( resultList.getResultRows().size(), 8 );

        Assert.assertEquals( resultList.getHeaders().get(0).getViewHeader(),  "");
        Assert.assertEquals( resultList.getHeaders().get(1).getViewHeader(),  "01");
        Assert.assertEquals( resultList.getHeaders().get(12).getViewHeader(), "12");
        Assert.assertEquals( resultList.getResultRows().get(0).getRenderableCells().get(0), "A");
        Assert.assertEquals( resultList.getResultRows().get(7).getRenderableCells().get(0), "H");
        Assert.assertEquals( resultList.getResultRows().get(0).getRenderableCells().get(1), "Vessel Barcode: 0116404353");

    }

}
