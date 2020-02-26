package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;

@Entity
@Audited
public class WaitForIdatTask extends Task {
    public WaitForIdatTask() {
    }

    public WaitForIdatTask(String taskName) {
        super();
        setTaskName(taskName);
    }
}
