package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;

@Entity
@Audited
public class PushIdatsToCloudTask extends ComputeTask {

    public enum IdatColor {
        Red, Green
    }

    public PushIdatsToCloudTask() {
    }

    // TODO This feels like a hack
    public PushIdatsToCloudTask(IdatColor idatColor) {
        setTaskName(idatColor.name() + "_" + System.currentTimeMillis());
    }

    public String getIdatColor() {
        return getTaskName().split("_")[0];
    }
}
