package org.broadinstitute.gpinformatics.athena.entity.common;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Date;

public class ChangeEvent {
    public final Date date;
    public final String person;

    public ChangeEvent(String person) {
        this.person = person;
        this.date = new Date();
    }

    public ChangeEvent(Date date, String person) {
        this.date = date;
        this.person = person;
    }


    public Date getDate() {
        return date;
    }

    public String getPerson() {
        return person;
    }

    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( !(other instanceof ChangeEvent) ) return false;
        ChangeEvent castOther = (ChangeEvent) other;
        return new EqualsBuilder().append(date, castOther.date)
                                  .append(person, castOther.person).isEquals();
    }

    /**
     *
     * @return int
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(date).append(person).toHashCode();
    }

    @Override
    public String toString() {
        return "ChangeEvent{" + "date=" + date + ", person=" + person + '}';
    }

}
