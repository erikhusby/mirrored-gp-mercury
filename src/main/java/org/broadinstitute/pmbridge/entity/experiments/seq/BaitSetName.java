package org.broadinstitute.pmbridge.entity.experiments.seq;

import org.broadinstitute.pmbridge.entity.common.Name;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/1/12
 * Time: 2:58 PM
 */
public class BaitSetName extends Name {
    private long id;

    public BaitSetName(final String name, final long id) {
        super(name);
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
