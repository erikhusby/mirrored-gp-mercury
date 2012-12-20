package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.CohortListBean;
import org.broadinstitute.gpinformatics.athena.entity.common.StatusType;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Research Projects hold all the information about a research project
 */
@Entity
@Audited
@Table(name = "RESEARCH_PROJECT", schema = "athena")
public class ResearchProject implements Serializable, Comparable<ResearchProject> {

    public static final boolean IRB_ENGAGED = false;
    public static final boolean IRB_NOT_ENGAGED = true;

    public boolean hasJiraTicketKey() {
        return !StringUtils.isBlank(jiraTicketKey);
    }

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
    @SequenceGenerator(name = "seq_research_project_index", schema = "athena",
                       sequenceName = "seq_research_project_index")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_research_project_index")
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

    @Column(name = "SYNOPSIS", nullable = false, length = 4000)
    private String synopsis;

    @Column(name = "IRB_NOT_ENGAGED", nullable = false)
    private boolean irbNotEngaged = IRB_ENGAGED;

    // People related to the project
    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
               orphanRemoval = true)
    private final Set<ProjectPerson> associatedPeople = new HashSet<ProjectPerson>();

    // Information about externally managed items
    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
               orphanRemoval = true)
    private final Set<ResearchProjectCohort> sampleCohorts = new HashSet<ResearchProjectCohort>();

    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
               orphanRemoval = true)
    private final Set<ResearchProjectFunding> projectFunding = new HashSet<ResearchProjectFunding>();

    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
               orphanRemoval = true)
    private final Set<ResearchProjectIRB> irbNumbers = new HashSet<ResearchProjectIRB>();

    private String irbNotes;

    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private List<ProductOrder> productOrders = new ArrayList<ProductOrder>();

    @Index(name = "ix_rp_jira")
    @Column(name = "JIRA_TICKET_KEY", nullable = false)
    private String jiraTicketKey;               // Reference to the Jira Ticket associated to this Research Project

    @Transient
    private String originalTitle;   // This is used for edit to keep track of changes to the object.

    // Initialize our transient data after the object has been loaded from the database.
    @PostLoad
    private void initialize() {
        originalTitle = title;
    }

    public String getBusinessKey() {
        return jiraTicketKey;
    }

    /**
     * no arg constructor for JSF.
     */
    public ResearchProject() {
        this(null, null, null, false);
    }

    /**
     * The full constructor for fields that are not settable.
     *
     * @param creator       The user creating the project
     * @param title         The title (name) of the project
     * @param synopsis      A description of the project
     * @param irbNotEngaged Is this project set up for NO IRB?
     */
    public ResearchProject(Long creator, String title, String synopsis, boolean irbNotEngaged) {
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
        if (jiraTicketKeyIn == null) {
            throw new NullPointerException("Jira Ticket Key cannot be null");
        }
        this.jiraTicketKey = jiraTicketKeyIn;
    }

    /**
     * Clears the ID and JIRA ticket key. THIS METHOD MUST ONLY EVER BE CALLED BY
     * {@link org.broadinstitute.gpinformatics.athena.boundary.projects.ResearchProjectManager#createResearchProject(ResearchProject)}
     * IN THE CASE WHERE THE JIRA ISSUE HAS BEEN CREATED BUT THERE IS AN ERROR PERSISTING THE RESEARCH PROJECT!
     */
    public void rollbackPersist() {
        this.researchProjectId = null;
        this.jiraTicketKey = null;
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

    public void addCohort(ResearchProjectCohort sampleCohort) {
        sampleCohorts.add(sampleCohort);
    }

    public void removeCohort(ResearchProjectCohort sampleCohort) {
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
                irbNumberList[i++] = irb.getIrb() + ": " + irb.getIrbType().getDisplayName();
            }

            return irbNumberList;
        }

        return new String[0];
    }

    public void addIrbNumber(ResearchProjectIRB irbNumber) {
        irbNumbers.add(irbNumber);
    }

    public void clearPeople() {
        associatedPeople.clear();
    }

    public void addPeople(RoleType role, List<BspUser> people) {
        if (people != null) {
            for (BspUser user : people) {
                associatedPeople.add(new ProjectPerson(this, role, user.getUserId()));
            }
        }
    }

    /**
     * This addPerson should only be used for tests (until we mock up BSP Users better there.
     *
     * @param role     The role of the person to add
     * @param personId The user id for the person
     */
    public void addPerson(RoleType role, long personId) {
        associatedPeople.add(new ProjectPerson(this, role, personId));
    }

    public Long[] getPeople(RoleType role) {
        List<Long> people = new ArrayList<Long>();

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
        return getPeople(RoleType.EXTERNAL);
    }

    public void populateFunding(Collection<Funding> fundingSet) {
        projectFunding.clear();
        if ((fundingSet != null) && !fundingSet.isEmpty()) {
            for (Funding funding : fundingSet) {
                projectFunding.add(new ResearchProjectFunding(this, funding.getDisplayName()));
            }
        }
    }

    public void populateIrbs(Collection<Irb> irbs) {
        irbNumbers.clear();
        if ((irbs != null) && !irbs.isEmpty()) {
            for (Irb irb : irbs) {
                irbNumbers.add(new ResearchProjectIRB(this, irb.getIrbType(), irb.getName()));
            }
        }
    }

    public void populateCohorts(Collection<Cohort> cohorts) {
        sampleCohorts.clear();
        if ((cohorts != null) && !cohorts.isEmpty()) {
            for (Cohort cohort : cohorts) {
                sampleCohorts.add(new ResearchProjectCohort(this, cohort.getCohortId()));
            }
        }
    }

    public String[] getFundingIds() {

        if (projectFunding != null) {
            String[] fundingIds = new String[projectFunding.size()];

            int i = 0;
            for (ResearchProjectFunding fundingItem : projectFunding) {
                fundingIds[i++] = fundingItem.getFundingId();
            }

            return fundingIds;
        }

        return new String[0];
    }

    public void addFunding(ResearchProjectFunding funding) {
        projectFunding.add(funding);
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

    public void submit() throws IOException {
        if (jiraTicketKey != null) {
            return;
        }
        JiraService jiraService = ServiceAccessUtility.getBean(JiraService.class);

        Map<String, CustomFieldDefinition> submissionFields = jiraService.getCustomFields();

        List<CustomField> listOfFields = new ArrayList<CustomField>();

        if (!sampleCohorts.isEmpty()) {
            List<String> cohortNames = new ArrayList<String>();

            for (ResearchProjectCohort cohort : sampleCohorts) {
                cohortNames.add(cohort.getCohortId());
            }

            CohortListBean cohortListBean = ServiceAccessUtility.getBean(CohortListBean.class);

            listOfFields.add(new CustomField(submissionFields, RequiredSubmissionFields.COHORTS,
                                             cohortListBean.getCohortListString(cohortNames.toArray(
                                                     new String[cohortNames.size()]))));
        }

        if (!projectFunding.isEmpty()) {
            List<String> fundingSources = new ArrayList<String>();
            for (ResearchProjectFunding fundingSrc : projectFunding) {
                fundingSources.add(fundingSrc.getFundingId());
            }

            listOfFields.add(new CustomField(submissionFields, RequiredSubmissionFields.FUNDING_SOURCE,
                                             StringUtils.join(fundingSources, ',')));
        }

        if (!irbNumbers.isEmpty()) {
            listOfFields.add(new CustomField(submissionFields, RequiredSubmissionFields.IRB_IACUC_NUMBER,
                                             StringUtils.join(getIrbNumbers(), ',')));
        }

        listOfFields.add(new CustomField(submissionFields, RequiredSubmissionFields.IRB_NOT_ENGAGED_FIELD,
                                         irbNotEngaged));

        listOfFields.add(new CustomField(submissionFields, RequiredSubmissionFields.MERCURY_URL, ""));

        BSPUserList bspUserList = ServiceAccessUtility.getBean(BSPUserList.class);
        StringBuilder piList = new StringBuilder();
        boolean first = true;
        for (ProjectPerson currPI : findPeopleByType(RoleType.BROAD_PI)) {
            if (null != bspUserList.getById(currPI.getPersonId())) {
                if (!first) {
                    piList.append("\n");
                }
                piList.append(bspUserList.getById(currPI.getPersonId()).getFirstName())
                      .append(" ")
                      .append(bspUserList.getById(currPI.getPersonId()).getLastName());
                first = false;
            }
        }

        if (!piList.toString().isEmpty()) {
            listOfFields.add(new CustomField(submissionFields, RequiredSubmissionFields.BROAD_PIS, piList.toString()));
        }

        String username = bspUserList.getById(createdBy).getUsername();

        JiraIssue issue = jiraService.createIssue(fetchJiraProject().getKeyPrefix(), username, fetchJiraIssueType(),
                                                  title, synopsis, listOfFields);

        // TODO: Only set the JIRA key once everything else has completed successfully, i.e., adding watchers
        jiraTicketKey = issue.getKey();

        issue.addWatcher(username);

        // Update ticket with link back into Mercury
        MercuryConfig mercuryConfig = ServiceAccessUtility.getBean(MercuryConfig.class);
        CustomField mercuryUrlField = new CustomField(
                submissionFields, RequiredSubmissionFields.MERCURY_URL,
                mercuryConfig.getUrl() + "projects/view.xhtml?researchProject=" + jiraTicketKey);
        issue.updateIssue(Collections.singleton(mercuryUrlField));
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    /**
     * fetchJiraProject is a helper method that binds a specific Jira project to a ResearchProject entity.  This
     * makes it easier for a user of this object to interact with Jira for this entity
     *
     * @return An enum of type
     *         {@link org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields.ProjectType} that
     *         represents the Jira Project for Research Projects
     */
    @Transient
    public CreateFields.ProjectType fetchJiraProject() {
        return CreateFields.ProjectType.Research_Projects;
    }

    /**
     * fetchJiraIssueType is a helper method that binds a specific Jira Issue Type to a ResearchProject entity.  This
     * makes it easier for a user of this object to interact with Jira for this entity
     *
     * @return An enum of type
     *         {@link org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields.IssueType} that
     *         represents the Jira Issue Type for Research Projects
     */
    @Transient
    public CreateFields.IssueType fetchJiraIssueType() {
        return CreateFields.IssueType.RESEARCH_PROJECT;
    }

    /**
     * RequiredSubmissionFields is an enum intended to assist in the creation of a Jira ticket
     * for Research Projects
     */
    public enum RequiredSubmissionFields implements CustomField.SubmissionField {

        //        Sponsoring_Scientist("Sponsoring Scientist"),
        COHORTS("Cohort(s)"),
        FUNDING_SOURCE("Funding Source"),
        IRB_IACUC_NUMBER("IRB/IACUCs"),
        IRB_NOT_ENGAGED_FIELD("IRB Not Engaged?"),
        MERCURY_URL("Mercury URL"),
        BROAD_PIS("Broad PI(s)"),;

        private final String fieldName;

        private RequiredSubmissionFields(String fieldNameIn) {
            fieldName = fieldNameIn;
        }

        @Nonnull @Override
        public String getFieldName() {
            return fieldName;
        }
    }

    @Override
    public boolean equals(Object other) {
        if ((this == other)) {
            return true;
        }

        if (!(other instanceof ResearchProject)) {
            return false;
        }

        ResearchProject castOther = (ResearchProject) other;
        return new EqualsBuilder().append(getJiraTicketKey(), castOther.getJiraTicketKey()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getJiraTicketKey()).toHashCode();
    }

    @Override
    public int compareTo(ResearchProject that) {
        CompareToBuilder builder = new CompareToBuilder();
        builder.append(title, that.getTitle());
        return builder.build();
    }
}
