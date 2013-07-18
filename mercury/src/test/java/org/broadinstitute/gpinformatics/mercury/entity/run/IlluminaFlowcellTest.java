package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class IlluminaFlowcellTest {

    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testSequencerModel() {
        IlluminaFlowcell flowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, "FC123");
        Assert.assertEquals(flowcell.getSequencerModel(),"Illumina HiSeq 2500");
        flowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.MiSeqFlowcell, "FC123");
        Assert.assertEquals(flowcell.getSequencerModel(),"Illumina MiSeq");
        flowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeqFlowcell, "FC123");
        Assert.assertEquals(flowcell.getSequencerModel(),"Illumina HiSeq 2000");
    }

    @Test(groups = {TestGroups.DATABASE_FREE}, dataProvider = "barcodesWithTypes")
    public void getFlowcellTypeByBarcode(String barcode, IlluminaFlowcell.FlowcellType flowcellType) {
        IlluminaFlowcell.FlowcellType foundFlowcellType = IlluminaFlowcell.FlowcellType.getTypeForBarcode(barcode);

        Assert.assertNotNull(foundFlowcellType, String.format("%s is not a %s, but was null.", barcode, flowcellType));
        Assert.assertEquals(foundFlowcellType, flowcellType,
                String.format("%s is not a %s, but a %s flowcellType.", barcode, flowcellType, foundFlowcellType));
    }

        /*
        2500's end in ADXX
        MiSeqs are A plus 4 digits/chars
        2000's are (mostly) any other 9 char/digit FC name.
         */
    @DataProvider(name = "barcodesWithTypes")
    public Object[][] getBarcodesAndTypes() {
     return new Object[][] {
       { "123456789", IlluminaFlowcell.FlowcellType.HiSeqFlowcell },
       { "AADXX", IlluminaFlowcell.FlowcellType.MiSeqFlowcell },
       { "012345ADXX", IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell },
       { "BB0CDEADXX",  IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell },
       { "A1569", IlluminaFlowcell.FlowcellType.MiSeqFlowcell },
       { "AQDZ1234x", IlluminaFlowcell.FlowcellType.HiSeqFlowcell }
     };

    }

}
