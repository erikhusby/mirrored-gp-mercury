package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import com.sun.xml.ws.api.pipe.Tube;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Test persist and fetch
 */
public class BarcodedTubeDaoTest extends ContainerTest {
    @Inject
    private UserTransaction utx;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        if (utx != null) {
            utx.begin();
        }
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        if (utx != null) {
            utx.rollback();
        }
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFindByBarcodes() throws Exception {
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
