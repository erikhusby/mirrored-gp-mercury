package org.broadinstitute.sequel;

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
public class Person {

    public Person(String name) {

    }
    
    public String getFirstName() {
        throw new RuntimeException("Method not yet implemented.");
    }

    public String getLastName() {
        throw new RuntimeException("Method not yet implemented.");
    }

    /**
     * LDAP user name
     * @return
     */
    public String getLogin() {
        throw new RuntimeException("Method not yet implemented.");
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
