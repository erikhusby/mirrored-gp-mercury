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

        /*
        2500's end in ADXX
        MiSeqs are A plus 4 digits/chars
        2000's are (mostly) any other 9 char/digit FC name.

        select fc.barcode, mach.model
        from receptacle fc, next_generation_run nrun, lab_machine mach, solexa_run srun
        where fc.receptacle_id = srun.new_flowcell_id
        and srun.run_id = nrun.run_id and nrun.lab_machine_id = mach.lab_machine_id

         */
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
                    {"H01KNADXX", IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell},
                    {"H0YKNADXX", IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell},
                    {"D27AEACXX", IlluminaFlowcell.FlowcellType.HiSeqFlowcell},
                    {"D27B9ACXX", IlluminaFlowcell.FlowcellType.HiSeqFlowcell},
                    {"CHYH5CCCXY", IlluminaFlowcell.FlowcellType.HiSeqX10Flowcell},
                    {"CHYH5CCCXX", IlluminaFlowcell.FlowcellType.HiSeqX10Flowcell},
                    {"CHYH5CALXX", IlluminaFlowcell.FlowcellType.HiSeqX10Flowcell},
                    {"flowBcode123412341234123", IlluminaFlowcell.FlowcellType.OtherFlowcell}
            };

        }

}
