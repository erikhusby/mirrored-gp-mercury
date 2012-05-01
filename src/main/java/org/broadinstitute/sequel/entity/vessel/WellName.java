package org.broadinstitute.sequel.entity.vessel;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class WellName {
    @Id
    private String name;

    public WellName(String name) {
        this.name = name;
    }

    public WellName() {
    }

    public String getWellName() {
        return name;
    }

}
