package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test persist and fetch
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class BarcodedTubeDaoTest extends StubbyContainerTest {

    public BarcodedTubeDaoTest(){}

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Test(groups = TestGroups.STUBBY)
    public void testFindByBarcodes() {
        List<BarcodedTube> barcodedTubes = new ArrayList<>();
        List<String> barcodes = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (BarcodedTube.BarcodedTubeType barcodedTubeType : BarcodedTube.BarcodedTubeType.values()) {
            String barcode = "A" + now++;
            barcodedTubes.add(new BarcodedTube(barcode, barcodedTubeType.getAutomationName()));
            barcodes.add(barcode);
        }
        barcodedTubeDao.persistAll(barcodedTubes);
        barcodedTubeDao.flush();

        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(barcodes);
        Assert.assertEquals(barcodes.size(), mapBarcodeToTube.size(), "Wrong number of tubes");
        // Checks the tube types.
        for (BarcodedTube barcodedTube : barcodedTubes) {
            Assert.assertNotNull(mapBarcodeToTube.get(barcodedTube.getLabel()));
            Assert.assertEquals(mapBarcodeToTube.get(barcodedTube.getLabel()).getTubeType(),barcodedTube.getTubeType());
        }
    }
}
