package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

/**
 * This class associates a person with a project. Each person has roles in a project and this object
 * stores a single role for a person in a project. The person/project/role should be unique within this
 */
public class ProjectPerson {
    private ProjectPersonID id;

    private ResearchProject project;
    private RoleType role;
    private Person person;

    public ProjectPerson(RoleType role, Person person) {
        this.role = role;
        this.person = person;
    }

    public ProjectPersonID getId() {
        return id;
    }

    public void setId(ProjectPersonID id) {
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
}
