package org.broadinstitute.gpinformatics.mercury.control.vessel;

//import com.jprofiler.api.agent.Controller;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.PrintStream;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test generation of the samplesheet.csv
 */
@Test(groups = TestGroups.STANDARD)
public class SampleSheetFactoryTest extends Arquillian {

    @Inject
    private SampleSheetFactory sampleSheetFactory;

    @Inject
    private StaticPlateDao staticPlateDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testBasics() {
//        Controller.startCPURecording(true);
//        Controller.startProbeRecording(Controller.PROBE_NAME_JDBC, true);
        ProductOrder productOrder = productOrderDao.findByBusinessKey("PDO-6743");
        List<Pair<LabVessel, VesselPosition>> vesselPositionPairs = sampleSheetFactory.loadByPdo(
                productOrder);
        MessageCollection messageCollection = new MessageCollection();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        List<Pair<LabVessel, VesselPosition>> vesselPositionPairs = new ArrayList<>();
//        StaticPlate chip = staticPlateDao.findByBarcode("200598720152");
//        for (VesselPosition vesselPosition : chip.getVesselGeometry().getVesselPositions()) {
//            vesselPositionPairs.add(new ImmutablePair<LabVessel, VesselPosition>(chip, vesselPosition));
//        }
        sampleSheetFactory.write(new PrintStream(byteArrayOutputStream), vesselPositionPairs,
                productOrder.getResearchProject(), messageCollection);
        System.out.println(byteArrayOutputStream.toString());
        // todo jmt asserts
//        Controller.stopProbeRecording(Controller.PROBE_NAME_JDBC);
//        Controller.stopCPURecording();
    }
}