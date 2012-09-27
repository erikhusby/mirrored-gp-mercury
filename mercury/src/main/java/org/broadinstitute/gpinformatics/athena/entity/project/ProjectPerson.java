package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

/**
 * This class associates a person with a project. Each person has roles in a project and this object
 * stores a single role for a person in a project. The person/project/role should be unique within this
 */
public class ProjectPerson {
    private ProjectPersonId id;

    private ResearchProject project;
    private RoleType role;
    private Person person;

    public ProjectPerson(RoleType role, Person person) {
        this.role = role;
        this.person = person;
    }

    public ProjectPersonId getId() {
        return id;
    }

    public void setId(ProjectPersonId id) {
        this.id = id;
    }

    public ResearchProject getProject() {
        return project;
    }

    public void setProject(ResearchProject project) {
        this.project = project;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    /**
     *
     * @param other The other object
     * @return boolean
     */
    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( !(other instanceof ProjectPerson) ) return false;
        ProjectPerson castOther = (ProjectPerson) other;
        return new EqualsBuilder().append(project, castOther.project).append(person, castOther.person).isEquals();
    }

    /**
     *
     * @return int
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(project).append(person).toHashCode();
    }
}
