package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.common.StatusType;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.io.IOException;
import java.util.*;

/**
 * Research Projects hold all the information about a research project
 */
@Entity
@Audited
@Table(name = "RESEARCH_PROJECT", schema = "athena")
public class ResearchProject {

    public static final boolean IRB_ENGAGED = false;
    public static final boolean IRB_NOT_ENGAGED = true;

    public enum Status implements StatusType {
        Open, Archived;

        @Override
        public String getDisplayName() {
            return name();
        }

        public static List<String> getNames() {
            List<String> names = new ArrayList<String>();
            for (Status status : Status.values()) {
                names.add(status.name());
            }

            return names;
        }
    }

    @Id
    @SequenceGenerator(name="seq_research_project_index", schema = "athena", sequenceName="seq_research_project_index")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_research_project_index")
    private Long researchProjectId;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.Open;

    // creation/modification information
    @Column(name = "CREATED_DATE", nullable = false)
    private Date createdDate;

    @Column(name = "CREATED_BY", nullable = false)
    private Long createdBy;

    @Column(name = "MODIFIED_DATE", nullable = false)
    private Date modifiedDate;

    @Column(name = "MODIFIED_BY", nullable = false)
    private Long modifiedBy;

    @Column(name = "TITLE", unique = true, nullable = false)
    @Index(name = "ix_rp_title")
    private String title;

    @Column(name = "SYNOPSIS", nullable = false)
    private String synopsis;

    @Column(name = "IRB_NOT_ENGAGED", nullable = false)
    private boolean irbNotEngaged = IRB_ENGAGED;

    // People related to the project
    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private Set<ProjectPerson> associatedPeople;

    // Information about externally managed items
    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private Set<ResearchProjectCohort> sampleCohorts;

    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private Set<ResearchProjectFunding> projectFunding;

    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private Set<ResearchProjectIRB> irbNumbers;

    private String irbNotes;

    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private List<ProductOrder> productOrders;

    @Index(name = "ix_rp_jira")
    private String jiraTicketKey;               // Reference to the Jira Ticket associated to this Research Project

    public String getBusinessKey() {
        return title;
    }

    /**
     * no arg constructor for hibernate and JSF.
     */
    public ResearchProject() {
        this(null, null, null, false);
    }

    /**
     * The full constructor for fields that are not settable.
     *
     * @param creator The user creating the project
     * @param title The title (name) of the project
     * @param synopsis A description of the project
     * @param irbNotEngaged Is this project set up for NO IRB?
     */
    public ResearchProject(Long creator, String title, String synopsis, boolean irbNotEngaged) {
        sampleCohorts = new HashSet<ResearchProjectCohort>();
        productOrders = new ArrayList<ProductOrder>();
        createdDate = new Date();
        modifiedDate = createdDate;
        irbNotes = "";

        this.title = title;
        this.synopsis = synopsis;
        this.createdBy = creator;
        this.modifiedBy = creator;
        this.irbNotEngaged = irbNotEngaged;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public boolean getIrbNotEngaged() {
        return irbNotEngaged;
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

    // Setters

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public void setIrbNotEngaged(boolean irbNotEngaged) {
        this.irbNotEngaged = irbNotEngaged;
    }

    public void setIrbNotes(String irbNotes) {
        this.irbNotes = irbNotes;
    }

    /**
     * Sets the last modified by property to the specified user and the last modified date to the current date.
     *
     * @param personId the person who modified the research project
     */
    public void recordModification(Long personId) {
        modifiedBy = personId;
        modifiedDate = new Date();
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

    /**
     *
     * @return Get the cohortIds. Since the cohort list is defaulted to empty, we know that the cohorts will exist
     */
    public String[] getCohortIds() {
        int i = 0;
        String[] cohorts = new String[sampleCohorts.size()];
        for (ResearchProjectCohort cohort : sampleCohorts) {
            cohorts[i++] = cohort.getCohortId();
        }

        return cohorts;
    }

    public void addCohort(ResearchProjectCohort sampleCohort ) {
        sampleCohorts.add(sampleCohort);
    }

    public void removeCohort(ResearchProjectCohort sampleCohort ){
        sampleCohorts.remove(sampleCohort);
    }

    public boolean isIrbNotEngaged() {
        return irbNotEngaged;
    }

    public String[] getIrbNumbers() {
        int i = 0;
        if (irbNumbers != null) {
            String[] irbNumberList = new String[irbNumbers.size()];
            for (ResearchProjectIRB irb : irbNumbers) {
                irbNumberList[i++] = irb.getIrb();
            }

            return irbNumberList;
        }

        return new String[0];
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

    public Long[] getPeople(RoleType role) {
        List<Long> people = new ArrayList<Long> ();

        if (associatedPeople != null) {
            for (ProjectPerson projectPerson : associatedPeople) {
                if (role == projectPerson.getRole()) {
                    people.add(projectPerson.getPersonId());
                }
            }
        }

        return people.toArray(new Long[people.size()]);
    }

    public Long[] getProjectManagers() {
        return getPeople(RoleType.PM);
    }

    public Long[] getBroadPIs() {
        return getPeople(RoleType.BROAD_PI);
    }

    public Long[] getScientists() {
        return getPeople(RoleType.SCIENTIST);
    }

    public Long[] getExternalCollaborators() {
        return getPeople ( RoleType.EXTERNAL );
    }

    public String[] getFundingIds() {

        if (projectFunding != null) {
            String[] fundingIds = new String[projectFunding.size()];

            int i=0;
            for (ResearchProjectFunding fundingItem : projectFunding) {
                fundingIds[i++] = fundingItem.getFundingId();
            }

            return fundingIds;
        }

        return new String[0];
    }

    public void addFunding(ResearchProjectFunding funding) {
        if (projectFunding == null) {
            projectFunding = new HashSet<ResearchProjectFunding>();
        }

        projectFunding.add(funding);
    }

    public void removeFunding(ResearchProjectFunding funding) {
        projectFunding.remove(funding);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<ProductOrder> getProductOrders() {
        return productOrders;
    }

    public RoleType[] getRoleTypes() {
        return RoleType.values();
    }

    public void submit() throws IOException {

        Map<String, CustomFieldDefinition> submissionFields =
                ServiceAccessUtility.getJiraCustomFields ( );

        List<CustomField> listOfFields = new ArrayList<CustomField>();

        //TODO HR, SGM -- Update for Sponsoring Scientist
        listOfFields.add(new CustomField(submissionFields.get(RequiredSubmissionFields.Sponsoring_Scientist.getFieldName()),
                                         associatedPeople.iterator().next().getPersonId().toString()));

        if(!sampleCohorts.isEmpty()) {
            List<String> cohortNames = new ArrayList<String>();

            for(ResearchProjectCohort cohort:sampleCohorts) {
                cohortNames.add(cohort.getCohortId());
            }
            listOfFields.add(new CustomField(submissionFields.get(RequiredSubmissionFields.Cohorts.getFieldName()),
                                             StringUtils.join(cohortNames,',')));
        }
        if(!projectFunding.isEmpty()) {
            List<String> fundingSources = new ArrayList<String>();
            for(ResearchProjectFunding fundingSrc:projectFunding) {
                fundingSources.add(fundingSrc.getFundingId());
            }

            listOfFields.add(new CustomField(submissionFields.get(RequiredSubmissionFields.Funding_Source.getFieldName()),
                                             StringUtils.join(fundingSources,',')));
        }
        if(!irbNumbers.isEmpty()) {
            List<String> irbNums = new ArrayList<String>();
            for(ResearchProjectIRB irb:irbNumbers ){
                irbNums.add(irb.getIrb());
            }
            listOfFields.add(new CustomField(submissionFields.get(RequiredSubmissionFields.IRB_IACUC_Number.getFieldName()),
                                             StringUtils.join(irbNums,',')));
        }
        listOfFields.add(new CustomField(submissionFields.get(RequiredSubmissionFields.IRB_Engaged.getFieldName()),
                                         BooleanUtils.toStringYesNo(irbNotEngaged) ));

        CreateIssueResponse researchProjectResponse =
                ServiceAccessUtility.createJiraTicket(fetchJiraProject().getKeyPrefix(),fetchJiraIssueType(),
                                                      title, synopsis, listOfFields);

        jiraTicketKey = researchProjectResponse.getKey();

        /**
         * TODO SGM --  When the service to retrieve BSP People is implemented, add current user ID here.
         */
//        addWatcher(createdBy.toString());

    }

    public void addPublicComment(String comment) throws IOException{
        ServiceAccessUtility.addJiraComment ( jiraTicketKey, comment );
    }

    public void addWatcher(String personLoginId) throws IOException {
        ServiceAccessUtility.addJiraWatcher ( jiraTicketKey, personLoginId );
    }

    public void addLink(String targetIssueKey) throws IOException {
        ServiceAccessUtility.addJiraPublicLink( AddIssueLinkRequest.LinkType.Related, jiraTicketKey,targetIssueKey);
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

        Sponsoring_Scientist("Sponsoring Scientist"),
        Cohorts("Cohort(s)"),
        Funding_Source("Funding Source"),
        IRB_IACUC_Number("IRB/IACUC Number"),
        IRB_Engaged("IRB Engaged?");

        private String fieldName;

        private RequiredSubmissionFields(String fieldNameIn) {
            fieldName = fieldNameIn;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) {
            return true;
        }

        if ( !(other instanceof ResearchProject) ) {
            return false;
        }

        ResearchProject castOther = (ResearchProject) other;
        return new EqualsBuilder().append(getJiraTicketKey(), castOther.getJiraTicketKey()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getJiraTicketKey()).toHashCode();
    }
}
