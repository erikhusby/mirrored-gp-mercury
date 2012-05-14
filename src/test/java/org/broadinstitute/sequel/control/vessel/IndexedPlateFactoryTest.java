package org.broadinstitute.sequel.control.vessel;

import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.File;
import java.util.Map;

/**
 * Test creation of plates
 */
public class IndexedPlateFactoryTest extends ContainerTest {
    @Inject
    private IndexedPlateFactory indexedPlateFactory;

    @Test
    public void testParseFile() {

        File spreadsheet = new File(Thread.currentThread().getContextClassLoader().getResource(
                "testdata/DuplexCOAforBroad.xlsx").getFile());
        Map<String,StaticPlate> mapBarcodeToPlate = indexedPlateFactory.parseFile(
                spreadsheet, IndexedPlateFactory.TechnologiesAndParsers.ILLUMINA_SINGLE);
        Assert.assertEquals(mapBarcodeToPlate.size(), 50, "Wrong number of plates");
    }
}
