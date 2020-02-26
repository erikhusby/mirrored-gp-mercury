package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;

@Entity
@Audited
public class WaitForCustomerFilesTask extends Task {
    public WaitForCustomerFilesTask() {
    }

    public WaitForCustomerFilesTask(String taskName) {
        super();
        setTaskName(taskName);
    }
}
