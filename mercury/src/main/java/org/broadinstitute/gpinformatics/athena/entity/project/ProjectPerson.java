package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * This class associates a person with a project. Each person has roles in a project and this object
 * stores a single role for a person in a project. The person/project/role should be unique within this
 */
@Entity
@Audited
@Table(name = "PROJECT_PERSON", schema = "athena")
public class ProjectPerson {

    @Id
    @SequenceGenerator(name="seq_project_person_index", schema = "athena", sequenceName="seq_project_person_index")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_project_person_index")
    private Long projectPersonId;

    /**
     * This is eager fetched because this class' whole purpose is to bridge a specific person and project. If you
     * ever only need the ID, you should write a specific projection query in the DAO
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @Index(name = "ix_person_project")
    @JoinColumn(name = "RESEARCH_PROJECT")
    private ResearchProject researchProject;

    @Column(name = "ROLE")
    @Enumerated(EnumType.STRING)
    private RoleType role;

    /** Person ID is BSP User ID. */
    @Column(name = "PERSON_ID")
    private Long personId;

    protected ProjectPerson() { }

    public ProjectPerson(ResearchProject researchProject, RoleType role, Long personId) {
        this.researchProject = researchProject;
        this.role = role;
        this.personId = personId;
    }

    public Long getProjectPersonId() {
        return projectPersonId;
    }

    public void setProjectPersonId(Long id) {
        this.projectPersonId = id;
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

    @Override
    public boolean equals(Object other) {
        if ((this == other )) return true;
        if (!(other instanceof ProjectPerson) ) {
            return false;
        }

        ProjectPerson castOther = (ProjectPerson) other;
        return new EqualsBuilder()
                .append(getRole(), castOther.getRole()).append(getPersonId(), castOther.getPersonId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getRole()).append(getPersonId()).toHashCode();
    }
}
