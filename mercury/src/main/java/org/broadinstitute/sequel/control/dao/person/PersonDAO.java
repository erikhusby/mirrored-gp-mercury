package org.broadinstitute.sequel.control.dao.person;

import org.broadinstitute.sequel.entity.person.Person;

/**
 * Data Access Object for Person
 */
public class PersonDAO {
    public Person findByName(String name) {
        return new Person(name);
    }
}
