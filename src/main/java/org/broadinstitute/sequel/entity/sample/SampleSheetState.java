package org.broadinstitute.sequel.entity.sample;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.entity.vessel.MolecularState;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.analysis.ReadBucket;

public class SampleSheetState {

    private static Log gLog = LogFactory.getLog(SampleSheetState.class);
    
    public MolecularState getMolecularState() {
        throw new RuntimeException("Method not yet implemented.");
    }
    
    public Project getProject() {
        throw new RuntimeException("Method not yet implemented.");
    }

    public ReadBucket getReadBucket() {
        throw new RuntimeException("Method not yet implemented.");
    }

}
