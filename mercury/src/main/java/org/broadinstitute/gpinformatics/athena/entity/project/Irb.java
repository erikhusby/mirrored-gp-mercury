package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;

public class Irb implements Displayable {
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
        return new EqualsBuilder().append(irbType, castOther.getIrbType())
                                  .append(name, castOther.getName()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(irbType).append(name).toHashCode();
    }

    @Override
    public String getDisplayName() {
        return name + ": " + irbType.getDisplayName();
    }
}
