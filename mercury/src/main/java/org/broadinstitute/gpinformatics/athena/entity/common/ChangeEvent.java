package org.broadinstitute.gpinformatics.athena.entity.common;

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
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ChangeEvent)) return false;

        final ChangeEvent that = (ChangeEvent) o;

        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (person != null ? !person.equals(that.person) : that.person != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = date != null ? date.hashCode() : 0;
        result = 31 * result + (person != null ? person.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ChangeEvent{" +
                "date=" + date +
                ", person=" + person +
                '}';
    }

}