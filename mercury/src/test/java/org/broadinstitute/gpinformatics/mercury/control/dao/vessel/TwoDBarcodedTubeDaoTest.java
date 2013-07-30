package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.testng.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;

/**
 * Test persist and fetch
 */
public class TwoDBarcodedTubeDAOTest extends ContainerTest {

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFindByBarcodes() throws Exception {
        // todo jmt create tubes
        Map<String,TwoDBarcodedTube> mapBarcodeToTube = twoDBarcodedTubeDAO.findByBarcodes(Arrays.asList(
                "R100515045783",
                "R100515045784",
                "R100515045785",
                "R100515045786"));
        Assert.assertEquals(4, mapBarcodeToTube.size(), "Wrong number of tubes");
    }
}
