package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class Irb {
    private final String name;
    private final ResearchProjectIRB.IrbType irbType;

    public Irb(String name, ResearchProjectIRB.IrbType irbType) {
        this.name = name;
        this.irbType = irbType;
    }

    public String getName() {
        return name;
    }

    public ResearchProjectIRB.IrbType getIrbType() {
        return irbType;
    }

    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) {
            return true;
        }

        if ( !(other instanceof Irb) ) {
            return false;
        }

        Irb castOther = (Irb) other;
        return new EqualsBuilder().append(getIrbType(), castOther.getIrbType())
                                  .append(getName(), castOther.getName()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getIrbType()).append(getName()).toHashCode();
    }
}
