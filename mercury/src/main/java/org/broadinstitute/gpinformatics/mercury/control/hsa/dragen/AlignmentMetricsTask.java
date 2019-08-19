package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;

@Entity
@Audited
public class AlignmentMetricsTask extends Task {

    public AlignmentMetricsTask() {
        super(Status.QUEUED);
    }

    public AlignmentMetricsTask(Status status) {
        super(status);
    }

}
