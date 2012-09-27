package org.broadinstitute.gpinformatics.athena.entity.experiments.seq;

import org.broadinstitute.gpinformatics.athena.entity.common.Name;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/1/12
 * Time: 2:58 PM
 */
public class OrganismName extends Name {
    private String commonName;
    private long id;
    public OrganismName(String name, String commonName, long id) {
        super(name);
        this.commonName = commonName;
        this.id = id;
    }

    public String getCommonName() {
        return commonName;
    }

    public long getId() {
        return id;
    }
}
