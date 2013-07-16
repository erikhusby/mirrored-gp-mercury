package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

public class IlluminaFlowcellTest {

    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testSequencerModel() {
        IlluminaFlowcell flowcell = new IlluminaFlowcell("FC123", IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell);
        Assert.assertEquals(flowcell.getSequencerModel(),"Illumina HiSeq 2500");
        flowcell = new IlluminaFlowcell("FC123", IlluminaFlowcell.FlowcellType.MiSeqFlowcell);
        Assert.assertEquals(flowcell.getSequencerModel(),"Illumina MiSeq");
        flowcell = new IlluminaFlowcell("FC123", IlluminaFlowcell.FlowcellType.HiSeqFlowcell);
        Assert.assertEquals(flowcell.getSequencerModel(),"Illumina HiSeq 2000");
    }
}
