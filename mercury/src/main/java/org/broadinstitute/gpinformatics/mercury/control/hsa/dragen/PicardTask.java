package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;

@Entity
@Audited
public class PicardTask extends ProcessTask {

    public PicardTask(String partition) {
        super(partition);
    }

    public PicardTask() {
    }

    public boolean requiresDragenPrefix() {
        return false;
    }
}
