package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;

@Entity
@Audited
public class FingerprintUploadTask extends Task {

    public FingerprintUploadTask() {
        super(Status.QUEUED);
    }

    public FingerprintUploadTask(Status status) {
        super(status);
    }
}
