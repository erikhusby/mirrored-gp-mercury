package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularState;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReadBucket;

public class SampleSheetState {

    private static Log gLog = LogFactory.getLog(SampleSheetState.class);
    
    public MolecularState getMolecularState() {
        throw new RuntimeException("Method not yet implemented.");
    }
    
//    public Project getProject() {
//        throw new RuntimeException("Method not yet implemented.");
//    }

    public ReadBucket getReadBucket() {
        throw new RuntimeException("Method not yet implemented.");
    }

}
