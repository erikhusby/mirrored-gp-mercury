package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.experiments.EntityUtils;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Research Projects hold all the information about a research project
 */
@Entity
@Audited
@Table(schema = "athena")
public class ResearchProject {

    public enum Status {
        Open, Archived
    }

    @Id
    @SequenceGenerator(name="seq_research_project_index", schema = "athena", sequenceName="seq_research_project_index", allocationSize = 1)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_research_project_index")
    private Long researchProjectId;

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
    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "researchProject")
    private Set<ProjectPerson> associatedPeople;

    // Information about externally managed items
    @OneToMany(mappedBy = "researchProject")
    private Set<ResearchProjectCohort> sampleCohorts;

    @OneToMany(mappedBy = "researchProject")
    private Set<ResearchProjectFunding> fundingIDs;

    @OneToMany(mappedBy = "researchProject")
    private Set<ResearchProjectIRB> irbNumbers = new HashSet<ResearchProjectIRB>();

    private String irbNotes;

    @OneToMany(mappedBy = "researchProject")
    private final Set<ProductOrder> productOrders = new HashSet<ProductOrder>();

    private String jiraTicketKey;               // Reference to the Jira Ticket associated to this Research Project

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

    public Long getResearchProjectId() {
        return researchProjectId;
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

    /**
     * getJiraTicketKey allows a user of this class to gain access to the Unique key representing the Jira Ticket for
     * which this Research project is associated
     *
     * @return a {@link String} that represents the unique Jira Ticket key
     */
    public String getJiraTicketKey() {
        return this.jiraTicketKey;
    }

    /**
     * setJiraTicketKey allows a user of this class to associate the key for the Jira Ticket which was created
     * for this Research Project
     *
     * @param jiraTicketKeyIn a {@link String} that represents the unique key to the Jira Ticket to which the current
     *                        Research Project is associated
     */
    public void setJiraTicketKey(String jiraTicketKeyIn) {
        if(jiraTicketKeyIn == null) {
            throw new NullPointerException("Jira Ticket Key cannot be null");
        }
        this.jiraTicketKey = jiraTicketKeyIn;
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

    public Set<ProductOrder> getProductOrders() {
        return productOrders;
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

    /**
     * fetchJiraProject is a helper method that binds a specific Jira project to a ResearchProject entity.  This
     * makes it easier for a user of this object to interact with Jira for this entity
     *
     * @return An enum of type
     * {@link org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest.Fields.ProjectType} that
     * represents the Jira Project for Research Projects
     */
    @Transient
    public CreateIssueRequest.Fields.ProjectType fetchJiraProject() {
        return CreateIssueRequest.Fields.ProjectType.Research_Projects;
    }

    /**
     *
     * fetchJiraIssueType is a helper method that binds a specific Jira Issue Type to a ResearchProject entity.  This
     * makes it easier for a user of this object to interact with Jira for this entity
     *
     * @return An enum of type
     * {@link org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest.Fields.Issuetype} that
     * represents the Jira Issue Type for Research Projects
     */
    @Transient
    public CreateIssueRequest.Fields.Issuetype fetchJiraIssueType() {
        return CreateIssueRequest.Fields.Issuetype.Research_Project;
    }

    /**
     * RequiredSubmissionFields is an enum intended to assist in the creation of a Jira ticket
     * for Research Projects
     */
    public enum RequiredSubmissionFields {

        Sponsoring_Scientist("Sponsoring Scientist");

        private String fieldName;

        private RequiredSubmissionFields(String fieldNameIn) {
            fieldName = fieldNameIn;
        }

        public String getFieldName() {
            return fieldName;
        }
    }



}
