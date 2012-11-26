package org.broadinstitute.gpinformatics.athena.entity.common;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import java.util.Date;

public class ChangeEvent {
    public final Date date;
    public final Person person;

    public ChangeEvent(Person person) {
        this.person = person;
        this.date = new Date();
    }

    public ChangeEvent(Date date, Person person) {
        this.date = date;
        this.person = person;
    }


    public Date getDate() {
        return date;
    }

    public Person getPerson() {
        return person;
    }

    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( !(other instanceof ChangeEvent) ) return false;
        ChangeEvent castOther = (ChangeEvent) other;
        return new EqualsBuilder().append(date, castOther.date)
                                  .append(person.getLogin(), castOther.person.getLogin()).isEquals();
    }

    /**
     *
     * @return int
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(date).append(person.getLogin()).toHashCode();
    }

    @Override
    public String toString() {
        return "ChangeEvent{" + "date=" + date + ", person=" + person + '}';
    }

}