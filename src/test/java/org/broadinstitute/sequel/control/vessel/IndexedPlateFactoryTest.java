package org.broadinstitute.sequel.control.vessel;

import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.File;
import java.util.Map;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Test creation of plates
 */
public class IndexedPlateFactoryTest extends ContainerTest {
    @Inject
    private IndexedPlateFactory indexedPlateFactory;

    @Test(enabled = false, groups = EXTERNAL_INTEGRATION)
    public void testParseFile() {

        // Not sure about the following
//        File spreadsheet = new File("C:\\Users\\thompson\\Documents\\Sequencing\\IndexPlates\\NewIndexPlates.xls");
//        File spreadsheet = new File("C:\\Users\\thompson\\Documents\\Sequencing\\IndexPlates\\Broad 500 replicate plates.xls");
        // Following file doesn't have Broad Barcode, but it has only 5 plates
//        File spreadsheet = new File("C:\\Users\\thompson\\Documents\\Sequencing\\IndexPlates\\12062011_Friedrich_COA.xls");
//        File spreadsheet = new File("C:\\Users\\thompson\\Documents\\Sequencing\\IndexPlates\\Broad replicate COA 09 20 11.xls");
//        File spreadsheet = new File("C:\\Users\\thompson\\Documents\\Sequencing\\IndexPlates\\Duplex plate COA 07 20 11(65plates).xls");
//        File spreadsheet = new File("C:\\Users\\thompson\\Documents\\Sequencing\\IndexPlates\\Duplex COA 07 18 11-4 (350plates)-sent.xls");
//        File spreadsheet = new File("C:\\Users\\thompson\\Documents\\Sequencing\\IndexPlates\\Duplex COA for Broad.xlsx");
        // Following file doesn't have Broad Barcode, hence column offsets need to be decremented
//        File spreadsheet = new File("C:\\Users\\thompson\\Documents\\Sequencing\\IndexPlates\\COA - SO# 5882088 - 7-2-10-1-DupeMod.xlsx");
//        Map<String,StaticPlate> mapBarcodeToPlate = indexedPlateFactory.parseAndPersist(
//                spreadsheet, IndexedPlateFactory.TechnologiesAndParsers.ILLUMINA_SINGLE);
        File spreadsheet = new File(Thread.currentThread().getContextClassLoader().getResource(
                "testdata/DuplexCOAforBroad.xlsx").getFile());
        Map<String,StaticPlate> mapBarcodeToPlate = indexedPlateFactory.parseFile(
                spreadsheet, IndexedPlateFactory.TechnologiesAndParsers.ILLUMINA_SINGLE);
        Assert.assertEquals(mapBarcodeToPlate.size(), 50, "Wrong number of plates");
    }
}
