package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.common.StatusType;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
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

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE,CascadeType.PERSIST})
    @JoinColumn(name = "PARENT_RESEARCH_PROJECT", nullable = true, insertable = true, updatable = true)
    @Index(name = "ix_parent_research_project")
    private ResearchProject parentResearchProject;

    /**
     * Set of ResearchProjects that belong under this one.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy="parentResearchProject", cascade = CascadeType.ALL)
    private Set<ResearchProject> childProjects = new TreeSet<ResearchProject>(ResearchProject.BY_TITLE);

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

    /** True if access to this Project's data should be restricted based on user. */
    @Column(name = "ACCESS_CONTROL_ENABLED")
    private boolean accessControlEnabled;

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
     * no arg constructor.
     */
    public ResearchProject() {
        this(null, null, null, false);
    }

    public ResearchProject(BspUser user) {
        this(user.getUserId(), null, null, false);
    }

    /**
     * The full constructor for fields that are not settable.
     *
     * @param createdBy The user creating the project
     * @param title The title (name) of the project
     * @param synopsis A description of the project
     * @param irbNotEngaged Is this project set up for NO IRB?
     */
    public ResearchProject(Long createdBy, String title, String synopsis, boolean irbNotEngaged) {
        createdDate = new Date();
        modifiedDate = createdDate;
        irbNotes = "";

        this.title = title;
        this.synopsis = synopsis;
        this.createdBy = createdBy;
        this.modifiedBy = createdBy;
        this.irbNotEngaged = irbNotEngaged;
        if (createdBy != null) {
            addPerson(RoleType.PM, createdBy);
        }
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

    public ResearchProject getParentProject() {
        return parentResearchProject;
    }

    // Setters

    public Set<ResearchProject> getChildProjects() {
        return childProjects;
    }

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

    public boolean isAccessControlEnabled() {
        return accessControlEnabled;
    }

    public void setAccessControlEnabled(boolean accessControlEnabled) {
        this.accessControlEnabled = accessControlEnabled;
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
     * Clears the ID and JIRA ticket key. THIS METHOD MUST ONLY EVER BE CALLED
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
        String[] irbNumberList = new String[irbNumbers.size()];
        for (ResearchProjectIRB irb : irbNumbers) {
            irbNumberList[i++] = irb.getIrb() + ": " + irb.getIrbType().getDisplayName();
        }

        return irbNumberList;
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

    /**
     * @param role role type to filter on
     * @return list of associated people based on their role type
     */
    private Collection<ProjectPerson> findPeopleByType(RoleType role) {
        List<ProjectPerson> foundPersonList = new ArrayList<ProjectPerson>(associatedPeople.size());

        for (ProjectPerson person : associatedPeople) {
            if (person.getRole() == role) {
                foundPersonList.add(person);
            }
        }

        return foundPersonList;
    }

    public Long[] getPeople(RoleType role) {
        List<Long> people = new ArrayList<Long>();

        for (ProjectPerson person : associatedPeople) {
            if (person.getRole() == role) {
                people.add(person.getPersonId());
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

    public Long[] getOther() {
        return getPeople(RoleType.OTHER);
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
        String[] fundingIds = new String[projectFunding.size()];

        int i = 0;
        for (ResearchProjectFunding fundingItem : projectFunding) {
            fundingIds[i++] = fundingItem.getFundingId();
        }

        return fundingIds;
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
            String[] cohortNames = new String[sampleCohorts.size()];

            int i = 0;
            for (ResearchProjectCohort cohort : sampleCohorts) {
                cohortNames[i++] = cohort.getCohortId();
            }

            BSPCohortList cohortList = ServiceAccessUtility.getBean(BSPCohortList.class);
            listOfFields.add(new CustomField(submissionFields, RequiredSubmissionFields.COHORTS,
                                             cohortList.getCohortListString(cohortNames)));
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
                piList.append(bspUserList.getById(currPI.getPersonId()).getFullName());
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
        AppConfig appConfig = ServiceAccessUtility.getBean(AppConfig.class);
        CustomField mercuryUrlField = new CustomField(
                submissionFields, RequiredSubmissionFields.MERCURY_URL,
                appConfig.getUrl() + ResearchProjectActionBean.ACTIONBEAN_URL_BINDING + "?" +
                        ResearchProjectActionBean.VIEW_ACTION + "&" +
                        ResearchProjectActionBean.RESEARCH_PROJECT_PARAMETER + "=" + jiraTicketKey);

        issue.updateIssue(Collections.singleton(mercuryUrlField));
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public ResearchProject getParentResearchProject() {
        return parentResearchProject;
    }

    public void setParentResearchProject(ResearchProject parentResearchProject) {
        this.parentResearchProject = parentResearchProject;
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

    public Collection<ResearchProject> getAllChildren() {
        return addChildResearchProjects(getChildProjects());
    }

    /**
     * Recursive function to traverse through the full research project hierarchy tree to get all the projects.
     *
     * @param childResearchProjects the list of child research projects
     * @return collection of research projects
     */
    private Collection<ResearchProject> addChildResearchProjects(Collection<ResearchProject> childResearchProjects) {
        for (ResearchProject childResearchProject : childResearchProjects) {
            childResearchProjects.addAll(addChildResearchProjects(childResearchProject.getChildProjects()));
        }
        return childResearchProjects;
    }

    /**
     * Ensure that the parent research project model does not create any loops.
     */
    @PrePersist
    protected void prePersist() {
        Collection<ResearchProject> children = getAllChildren();
        if (children.contains(this) || (children.contains(parentResearchProject))) {
            throw new RuntimeException("Improper Research Project child hierarchy.");
        }
        if (parentResearchProject.equals(this)) {
            throw new RuntimeException("Improper Research Project parent hierarchy.");
        }
    }

    /**
     * Compare by the ResearchProject by it's title, case insensitive.
     */
    public static final Comparator<ResearchProject> BY_TITLE = new Comparator<ResearchProject>() {
        @Override
        public int compare(ResearchProject lhs, ResearchProject rhs) {
            return lhs.getTitle().toUpperCase().compareTo(
                    rhs.getTitle().toUpperCase());
        }
    };

    @Override
    public boolean equals(Object other) {
        if ((this == other)) {
            return true;
        }

        if (!(other instanceof ResearchProject)) {
            return false;
        }

        ResearchProject castOther = (ResearchProject) other;
        return new EqualsBuilder().append(getBusinessKey(), castOther.getBusinessKey()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getBusinessKey()).toHashCode();
    }

    @Override
    public int compareTo(ResearchProject that) {
        CompareToBuilder builder = new CompareToBuilder();
        builder.append(title, that.getTitle());
        return builder.build();
    }
}
