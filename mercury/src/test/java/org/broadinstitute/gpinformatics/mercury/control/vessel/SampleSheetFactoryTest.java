package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.tuple.Pair;
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
import org.testng.Assert;
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
        ProductOrder productOrder = productOrderDao.findByBusinessKey("PDO-6743");
        List<Pair<LabVessel, VesselPosition>> vesselPositionPairs = sampleSheetFactory.loadByPdo(
                productOrder);
        Assert.assertEquals(vesselPositionPairs.size(), 288);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        sampleSheetFactory.write(new PrintStream(byteArrayOutputStream), vesselPositionPairs,
                productOrder.getResearchProject());
        System.out.println(byteArrayOutputStream.toString());
        // todo jmt asserts
    }
}