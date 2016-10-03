package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.PrintStream;
import java.util.ArrayList;

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

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testBasics() {
        MessageCollection messageCollection = new MessageCollection();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ArrayList<Pair<LabVessel, VesselPosition>> vesselPositionPairs = new ArrayList<>();
        StaticPlate chip = staticPlateDao.findByBarcode("200598720152");
        for (VesselPosition vesselPosition : chip.getVesselGeometry().getVesselPositions()) {
            vesselPositionPairs.add(new ImmutablePair<LabVessel, VesselPosition>(chip, vesselPosition));
        }
        sampleSheetFactory.write(new PrintStream(byteArrayOutputStream), vesselPositionPairs, messageCollection);
        System.out.println(byteArrayOutputStream.toString());
        // todo jmt asserts
    }
}