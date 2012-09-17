package org.broadinstitute.sequel.entity.person;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

/**
 * Let's make a very simple person model
 * which basically delgates everything
 * to LDAP.
 *
 * Some people login to squid (users).
 *
 * Some people are collaborators, or names
 * of people that we need to track as
 * program program project managers,
 * scientists, principal investigators (PI).
 *
 * Sometimes users have all these roles,
 * but let's not make a gigantic hierarchical
 * model.
 *
 * If there is an entry for someone in LDAP,
 * they can login to squid.  If not, they
 * can't.  But they can be referenced
 * as a collaborator, PI, or owner of things
 * like projects.
 */
@Entity
@Audited
public class Person {

    @Id
    @SequenceGenerator(name = "SEQ_PERSON", sequenceName = "SEQ_PERSON")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PERSON")
    private Long personId;

    private String username;
    
    private String firstName;
    
    private String lastName;
    
    public Person(String name,
                  String firstName,
                  String lastName) {
        this(name);
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    public Person(String username) {
        this.username = username;
    }

    public Person() {
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    /**
     * LDAP user name
     * @return
     */
    public String getLogin() {
        return username;
    }

    public boolean canLogin() {
        throw new RuntimeException("Method not yet implemented.");
    }

    /**
     * If this person can login to our
     * app, what parts of the app
     * can the access?
     * @return
     */
    public Iterable<PageAccess> getPageAccess() {
        throw new RuntimeException("Method not yet implemented.");
    }

}
