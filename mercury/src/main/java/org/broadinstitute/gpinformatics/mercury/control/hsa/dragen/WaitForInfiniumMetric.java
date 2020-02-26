package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;

@Entity
@Audited
public class WaitForInfiniumMetric extends Task {
    public WaitForInfiniumMetric() {
    }

    public WaitForInfiniumMetric(String taskName) {
        super();
        setTaskName(taskName);
    }
}
