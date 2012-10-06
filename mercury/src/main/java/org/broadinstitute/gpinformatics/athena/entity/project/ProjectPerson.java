package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.hibernate.envers.Audited;

import javax.persistence.*;

/**
 * This class associates a person with a project. Each person has roles in a project and this object
 * stores a single role for a person in a project. The person/project/role should be unique within this
 */
@Entity
@Audited
@Table(schema = "athena")
public class ProjectPerson {

    @Id
    @SequenceGenerator(name="seq_project_person_index", schema = "athena", sequenceName="seq_project_person_index", allocationSize = 1)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_project_person_index")
    private Long id;

    @ManyToOne
    private ResearchProject researchProject;

    private RoleType role;
    private Long personId;

    protected ProjectPerson() { }

    public ProjectPerson(ResearchProject researchProject, RoleType role, Long personId) {
        this.researchProject = researchProject;
        this.role = role;
        this.personId = personId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ResearchProject getResearchProject() {
        return researchProject;
    }

    public RoleType getRole() {
        return role;
    }

    public Long getPersonId() {
        return personId;
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
        return new EqualsBuilder()
                .append(role, castOther.role)
                .append(personId, castOther.personId).isEquals();
    }

    /**
     *
     * @return int
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(role)
                .append(personId).toHashCode();
    }
}
