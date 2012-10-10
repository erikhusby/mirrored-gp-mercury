package org.broadinstitute.gpinformatics.mercury.control.dao.person;

import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;

/**
 * Data Access Object for Person
 */
@Stateful
@RequestScoped
public class PersonDAO {
    public Person findByName(String name) {
        return new Person(name);
    }
}
