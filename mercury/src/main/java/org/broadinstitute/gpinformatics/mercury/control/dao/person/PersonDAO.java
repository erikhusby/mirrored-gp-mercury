package org.broadinstitute.gpinformatics.mercury.control.dao.person;

import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

/**
 * Data Access Object for Person
 */
public class PersonDAO {
    public Person findByName(String name) {
        return new Person(name);
    }
}
