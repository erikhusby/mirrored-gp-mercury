package org.broadinstitute.pmbridge.entity.person;

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

    private String username;
    
    private String firstName;
    
    private String lastName;

    private String personId;

    private RoleType roleType = RoleType.UNSPECIFIED;
    

    public Person(String username,
                      RoleType roleType) {
        this.username = username;
        this.roleType = roleType;
    }

    public Person(String username,
                  String firstName,
                  String lastName,
                  String personId,
                  RoleType roleType) {

        this(username, roleType);
        this.firstName = firstName;
        this.lastName = lastName;
        this.personId = personId;
    }
    
    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUsername() {
        return username;
    }

    public String getPersonId() {
        return personId;
    }

    public RoleType getRoleType() {
        return roleType;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;

        final Person person = (Person) o;

        if (firstName != null ? !firstName.equals(person.firstName) : person.firstName != null) return false;
        if (lastName != null ? !lastName.equals(person.lastName) : person.lastName != null) return false;
        if (personId != null ? !personId.equals(person.personId) : person.personId != null) return false;
        if (roleType != person.roleType) return false;
        if (username != null ? !username.equals(person.username) : person.username != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (personId != null ? personId.hashCode() : 0);
        result = 31 * result + (roleType != null ? roleType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Person{" +
                "username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", personId='" + personId + '\'' +
                ", roleType=" + roleType +
                '}';
    }

}
