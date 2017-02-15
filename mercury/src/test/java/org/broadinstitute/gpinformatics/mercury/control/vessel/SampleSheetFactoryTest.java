package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test generation of the samplesheet.csv
 */
@Test(groups = TestGroups.STANDARD)
public class SampleSheetFactoryTest extends Arquillian {

    private static final String CHIP_1 = "200584330132";
    private static final String CHIP_2 = "200584330041";

    @Inject
    private SampleSheetFactory sampleSheetFactory;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testBasics() {
        ProductOrder productOrder = productOrderDao.findByBusinessKey("PDO-9246");
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(Arrays.asList(CHIP_1,
                CHIP_2));
        List<Pair<LabVessel, VesselPosition>> vesselPositionPairs = new ArrayList<>();
        for (LabVessel labVessel : mapBarcodeToVessel.values()) {
            if (labVessel.getLabel().equals(CHIP_1)) {
                vesselPositionPairs.add(new ImmutablePair<>(labVessel, VesselPosition.R12C01));
            } else if (labVessel.getLabel().equals(CHIP_2)) {
                vesselPositionPairs.add(new ImmutablePair<>(labVessel, VesselPosition.R07C02));
            }
        }

        Assert.assertEquals(vesselPositionPairs.size(), 2);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        sampleSheetFactory.write(new PrintStream(byteArrayOutputStream), vesselPositionPairs, productOrder);
        Assert.assertTrue(byteArrayOutputStream.size() > 500);
        // todo jmt more asserts
    }
}