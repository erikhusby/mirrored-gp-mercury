package org.broadinstitute.sequel;

/**
 * Data Access Object for Person
 */
public class PersonDAO {
    public Person findByName(String name) {
        return new Person(name);
    }
}
