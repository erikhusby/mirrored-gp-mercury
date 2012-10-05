package org.broadinstitute.gpinformatics.athena.entity.project;

import clover.org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.Order;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;

import javax.persistence.*;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Research Projects hold all the information about a research project
 */
@Entity
public class ResearchProject {

    public static final boolean IRB_ENGAGED = true;
    public static final boolean IRB_NOT_ENGAGED = false;

    public enum Status {
        Open, Archived
    }

    @Id
    @SequenceGenerator(name="seq_research_project_index", sequenceName="seq_research_project_index", allocationSize = 1)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_research_project_index")
    private Long id;

    private Status status;

    // creation/modification information
    private Date createdDate;
    private Long createdBy;
    private Date modifiedDate;
    private Long modifiedBy;

    @Column(unique = true)
    private String title;

    private String synopsis;

    private boolean irbEngaged = IRB_NOT_ENGAGED;

    // People related to the project
    @OneToMany(cascade = CascadeType.PERSIST)
    private Set<ProjectPerson> associatedPeople;

    // Information about externally managed items
    @OneToMany(mappedBy = "researchProject")
    private Set<ResearchProjectCohort> sampleCohorts;

    @OneToMany(mappedBy = "researchProject")
    private Set<ResearchProjectFunding> fundingIDs;

    @OneToMany(mappedBy = "researchProject")
    private Set<ResearchProjectIRB> irbNumbers = new HashSet<ResearchProjectIRB>();

    private String irbNotes;

    @Transient
    private final Set<Order> orders = new HashSet<Order>();

    protected ResearchProject() {}

    public ResearchProject(Long creator, String title, String synopsis) {
        this.title = title;
        this.synopsis = synopsis;
        this.createdBy = creator;
        this.createdDate = new Date();
        this.modifiedBy = creator;
        this.modifiedDate = this.createdDate;
        this.irbNotes = "";
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public Long getId() {
        return id;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public Long getModifiedBy() {
        return modifiedBy;
    }

    public String getIrbNotes() {
        return irbNotes;
    }

    public void addIrbNotes(String irbNotes) {
        this.irbNotes += "\n" + irbNotes;
    }

    public Set<ResearchProjectCohort> getSampleCohorts() {
        return Collections.unmodifiableSet(sampleCohorts);
    }

    public void addCohort(ResearchProjectCohort sampleCohort ){
        if (sampleCohorts == null) {
            sampleCohorts = new HashSet<ResearchProjectCohort>();
        }

        sampleCohorts.add(sampleCohort);
    }

    public void removeCohort(ResearchProjectCohort sampleCohort ){
        sampleCohorts.remove(sampleCohort);
    }

    public boolean isIrbEngaged() {
        return irbEngaged;
    }

    public void setIrbEngaged(boolean irbEngaged) {
        this.irbEngaged = irbEngaged;
    }

    public Set<ResearchProjectIRB> getIrbNumbers() {
        return Collections.unmodifiableSet(irbNumbers);
    }

    public void addIrbNumber(ResearchProjectIRB irbNumber) {
        if (irbNumbers == null) {
            irbNumbers = new HashSet<ResearchProjectIRB>();
        }

        irbNumbers.add(irbNumber);
    }

    public void removeIrbNumber(ResearchProjectIRB irbNumber) {
        irbNumbers.remove(irbNumber);
    }

    public void addPerson(RoleType role, Long personId) {
        if (associatedPeople == null) {
            associatedPeople = new HashSet<ProjectPerson>();
        }

        associatedPeople.add(new ProjectPerson(this, role, personId));
    }

    public Set<Long> getPeople(RoleType role) {
        Set<Long> people = new HashSet<Long> ();

        if (associatedPeople != null) {
            for (ProjectPerson projectPerson : associatedPeople) {
                if (role == projectPerson.getRole()) {
                    people.add(projectPerson.getPersonId());
                }
            }
        }

        return people;
    }

    public Set<String> getFundingIds() {
        Set<String> fundingIdSet = new HashSet<String> ();
        for (ResearchProjectFunding funding : fundingIDs) {
            fundingIdSet.add(funding.getFundingId());
        }

        return fundingIdSet;
    }

    public Set<String> getCohortIds() {
        Set<String> fundingIdSet = new HashSet<String> ();
        for (ResearchProjectCohort cohort : sampleCohorts) {
            fundingIdSet.add(cohort.getCohortId());
        }

        return fundingIdSet;
    }

    public void addFunding(ResearchProjectFunding funding) {
        if (fundingIDs == null) {
            fundingIDs = new HashSet<ResearchProjectFunding>();
        }

        fundingIDs.add(funding);
    }

    public void removeFunding(ResearchProjectFunding funding) {
        fundingIDs.remove(funding);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getIrbNumberString() {
        String[] irbNumbers = new String[getIrbNumbers().size()];
        int i = 0;
        for (ResearchProjectIRB irb : getIrbNumbers()) {
            irbNumbers[i++] = irb.getIrb();
        }

        return StringUtils.join(irbNumbers, ", ");
    }

    public Set<Order> getOrders() {
        return orders;
    }

    public RoleType[] getRoleTypes() {
        return RoleType.values();
    }

    /**
     *
     * @param other The other object
     * @return boolean
     */
    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( !(other instanceof ResearchProject) ) return false;
        ResearchProject castOther = (ResearchProject) other;
        return new EqualsBuilder().append(title, castOther.title).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(title).toHashCode();
    }
}
