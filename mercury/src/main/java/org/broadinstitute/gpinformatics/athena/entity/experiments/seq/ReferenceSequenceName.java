package org.broadinstitute.gpinformatics.athena.entity.experiments.seq;

import org.broadinstitute.gpinformatics.athena.entity.common.Name;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/1/12
 * Time: 2:58 PM
 */
public class ReferenceSequenceName extends Name {
    private long id;

    public ReferenceSequenceName(String name, long id) {
        super(name);
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
