package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.common.EntityUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.Order;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;

import javax.persistence.*;
import java.util.*;

/**
 * Research Projects hold all the information about a research project
 */

@NamedQueries({
        @NamedQuery(
                name = "ResearchProject.fetchByOwner",
                query = "from ResearchProject rp where rp.createdBy = :owner"),
        @NamedQuery(
                name = "ResearchProject.fetchByName",
                query = "from ResearchProject rp where rp.title = :name"),
        @NamedQuery(
                name = "ResearchProject.fetchAll",
                query = "from ResearchProject rp"),
        @NamedQuery(
                name = "ResearchProject.findById",
                query = "from ResearchProject rp where rp.id = :id")
})
@Entity
public class ResearchProject {

    public enum Status {
        Open, Archived
    }

    @Id
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

    public ResearchProject(Long creator, String title, String synopsis) {
        this.title = title;
        this.synopsis = synopsis;
        this.createdBy = creator;
        this.createdDate = new Date();
        this.modifiedBy = creator;
        this.modifiedDate = this.createdDate;
    }

    public ResearchProject() {}

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

    //Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public void setModifiedBy(Long modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Set<Order> getOrders() {
        return orders;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getIrbNotes() {
        return irbNotes;
    }

    public void setIrbNotes(String irbNotes) {
        this.irbNotes = irbNotes;
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

        associatedPeople.add(new ProjectPerson(role, personId));
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

    public Set<ResearchProjectFunding> getFundingIds() {
        return Collections.unmodifiableSet(fundingIDs);
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
        Set<String> irbNumberString = new HashSet<String> ();
        for (ResearchProjectIRB irb : getIrbNumbers()) {
            irbNumberString.add(irb.getIrb());
        }

        return EntityUtils.flattenSetOfStrings(irbNumberString);
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
