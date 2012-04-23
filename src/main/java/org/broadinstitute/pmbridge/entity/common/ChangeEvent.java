package org.broadinstitute.pmbridge.entity.common;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.broadinstitute.pmbridge.entity.person.Person;

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

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
     }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}