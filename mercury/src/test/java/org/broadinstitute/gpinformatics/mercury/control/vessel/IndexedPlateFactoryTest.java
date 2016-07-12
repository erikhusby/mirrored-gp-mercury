package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Map;

/**
 * Test creation of plates
 */
@Test(groups = TestGroups.STUBBY)
public class IndexedPlateFactoryTest extends ContainerTest {
    @Inject
    private IndexedPlateFactory indexedPlateFactory;

    @Test(enabled = true)
    public void testParseFile() {
        Map<String,StaticPlate> mapBarcodeToPlate = indexedPlateFactory.parseStream(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("DuplexCOAforBroad.xlsx"),
                IndexedPlateFactory.TechnologiesAndParsers.ILLUMINA_SINGLE);
        Assert.assertEquals(mapBarcodeToPlate.size(), 50, "Wrong number of plates");
    }
}
