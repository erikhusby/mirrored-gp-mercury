package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;

/**
 * Represents a generic quality review task that can only be updated through the UI as an action. If this task
 * is fired by the engine then it will put the machine into 'Triage' to avoid the engine from picking it up again.
 */
@Entity
@Audited
public class WaitForReviewTask extends Task {

    public WaitForReviewTask() {
    }

    public WaitForReviewTask(String taskName) {
        super();
        setTaskName(taskName);
    }
}
