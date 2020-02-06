package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;

/**
 * For Tasks that will use a compute partition instead of the default dragen partition
 */
@Entity
@Audited
public class ComputeTask extends ProcessTask {
    public ComputeTask(String partition) {
        super(partition);
    }

    public ComputeTask() {
        setPartition("dragen_cpu");
    }

    public boolean requiresDragenPrefix() {
        return false;
    }
}
