package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
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

        /* Test data for flowcell barcodes and corresponding types. */
        @DataProvider(name = "barcodesWithTypes")
        public Object[][] getBarcodesAndTypes() {
            return new Object[][]{
                    {"A29GC", IlluminaFlowcell.FlowcellType.MiSeqFlowcell},
                    {"A222Y", IlluminaFlowcell.FlowcellType.MiSeqFlowcell},
                    {"A4LLL", IlluminaFlowcell.FlowcellType.MiSeqFlowcell},
                    {"A0AKE", IlluminaFlowcell.FlowcellType.MiSeqFlowcell},
                    {"C0D78ACXX", IlluminaFlowcell.FlowcellType.HiSeqFlowcell},
                    {"D0M0LACXX", IlluminaFlowcell.FlowcellType.HiSeqFlowcell},
                    {"80345ABXX", IlluminaFlowcell.FlowcellType.HiSeqFlowcell},
                    {"800VLABXX", IlluminaFlowcell.FlowcellType.HiSeqFlowcell},
                    {"H01MAADXX", IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell},
                    {"H01MAANXX", IlluminaFlowcell.FlowcellType.HiSeq2500HighOutputFlowcell},
                    {"H01MAAN01", IlluminaFlowcell.FlowcellType.HiSeq2500HighOutputFlowcell},
                    {"H01KNADXX", IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell},
                    {"H0YKNADXX", IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell},
                    {"H0YKNAD23", IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell},
                    {"H0YKNBCXX", IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell},
                    {"H0YKNBC45", IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell},
                    {"D27AEACXX", IlluminaFlowcell.FlowcellType.HiSeqFlowcell},
                    {"D27B9ACXX", IlluminaFlowcell.FlowcellType.HiSeqFlowcell},
                    {"D27B9ACZZ", IlluminaFlowcell.FlowcellType.HiSeqFlowcell},
                    {"CHYH5CCCXY", IlluminaFlowcell.FlowcellType.HiSeqX10Flowcell},
                    {"CHYH5CCCXX", IlluminaFlowcell.FlowcellType.HiSeqX10Flowcell},
                    {"CHYHMLCCIV", IlluminaFlowcell.FlowcellType.HiSeqX10Flowcell},
                    {"CHYH5CALXX", IlluminaFlowcell.FlowcellType.HiSeqX10Flowcell},
                    {"CHYH5CALVI", IlluminaFlowcell.FlowcellType.HiSeqX10Flowcell},
                    {"CHYH5CDMXX", IlluminaFlowcell.FlowcellType.NovaSeqFlowcell},
                    {"CHYH5CDM67", IlluminaFlowcell.FlowcellType.NovaSeqFlowcell},
                    {"CHYH5CDS67", IlluminaFlowcell.FlowcellType.NovaSeqS4Flowcell},
                    {"CHYH5CDR67", IlluminaFlowcell.FlowcellType.NovaSeqS1Flowcell},
                    {"QQYH5CBBXX", IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell},
                    {"QQYH5CBB89", IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell},
                    {"H2JLNBGX3", IlluminaFlowcell.FlowcellType.NextSeqFlowcell},
                    {"QQYH5CBB8", IlluminaFlowcell.FlowcellType.OtherFlowcell},
                    {"flowBcode123412341234123", IlluminaFlowcell.FlowcellType.OtherFlowcell},
                    {"BPA73010-2735", IlluminaFlowcell.FlowcellType.ISeqFlowcell},
            };

        }

}
