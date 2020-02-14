package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;

@Entity
@Audited
public class BclDemultiplexMetricsTask extends Task {
    public BclDemultiplexMetricsTask() {
        super(Status.QUEUED);
    }

    public BclDemultiplexMetricsTask(Status status) {
        super(status);
    }
}
