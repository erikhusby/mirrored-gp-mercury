package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.common.StatusType;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraProject;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Research Projects hold all the information about a research project
 */
@Entity
@Audited
@Table(name = "RESEARCH_PROJECT", schema = "athena")
public class ResearchProject implements BusinessObject, JiraProject, Comparable<ResearchProject>, Serializable {
    public static final boolean IRB_ENGAGED = false;

    public static final boolean IRB_NOT_ENGAGED = true;

    /**
     * Compare by modified date.
     */
    public static final Comparator<ResearchProject> BY_DATE = new Comparator<ResearchProject>() {
        @Override
        public int compare(ResearchProject lhs, ResearchProject rhs) {
            return rhs.getModifiedDate().compareTo(lhs.getModifiedDate());
        }
    };

    @Id
    @SequenceGenerator(name = "seq_research_project_index", schema = "athena",
            sequenceName = "seq_research_project_index")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_research_project_index")
    private Long researchProjectId;

    // People related to the project
    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true)
    @BatchSize(size = 500)
    private final Set<ProjectPerson> associatedPeople = new HashSet<>();

    // Information about externally managed items
    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true)
    private final Set<ResearchProjectCohort> sampleCohorts = new HashSet<>();

    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true)
    private final Set<ResearchProjectFunding> projectFunding = new HashSet<>();

    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true)
    private final Set<ResearchProjectIRB> irbNumbers = new HashSet<>();

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

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "PARENT_RESEARCH_PROJECT", nullable = true, insertable = true, updatable = true)
    @Index(name = "ix_parent_research_project")
    private ResearchProject parentResearchProject;

    @Column(name = "SEQUENCE_ALIGNER_KEY", nullable = true)
    private String sequenceAlignerKey;

    @Column(name = "REFERENCE_SEQUENCE_KEY", nullable = true)
    private String referenceSequenceKey;

    /**
     * Set of ResearchProjects that belong under this one.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parentResearchProject")
    private Set<ResearchProject> childProjects = new HashSet<>();
    private String irbNotes;

    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private List<ProductOrder> productOrders = new ArrayList<>();
    /**
     * True if access to this Project's data should be restricted based on user.
     */
    @Column(name = "ACCESS_CONTROL_ENABLED")
    private boolean accessControlEnabled;

    // Reference to the Jira Ticket associated to this Research Project.
    @Index(name = "ix_rp_jira")
    @Column(name = "JIRA_TICKET_KEY", nullable = false)
    private String jiraTicketKey;

    @ManyToMany(cascade = {CascadeType.PERSIST})
    @JoinTable(schema = "athena", name = "RP_REGULATORY_INFOS",
            joinColumns = {@JoinColumn(name="RESEARCH_PROJECT")})
    private Collection<RegulatoryInfo> regulatoryInfos = new ArrayList<>();

    // This is used for edit to keep track of changes to the object.
    @Transient
    private String originalTitle;

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
     * @param createdBy     The user creating the project
     * @param title         The title (name) of the project
     * @param synopsis      A description of the project
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

    public boolean hasJiraTicketKey() {
        return !StringUtils.isBlank(jiraTicketKey);
    }

    // Initialize our transient data after the object has been loaded from the database.
    @PostLoad
    private void initialize() {
        originalTitle = title;
    }

    @Override
    public String getBusinessKey() {
        return jiraTicketKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getName() {
        return title;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
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

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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

    public void setIrbNotes(String irbNotes) {
        this.irbNotes = irbNotes;
    }

    public void addIrbNotes(String irbNotes) {
        this.irbNotes += "\n" + irbNotes;
    }

    public Set<ResearchProject> getChildProjects() {
        return childProjects;
    }

    public String getSequenceAlignerKey() {
        return sequenceAlignerKey;
    }

    public void setSequenceAlignerKey(String sequenceAlignerKey) {
        this.sequenceAlignerKey = sequenceAlignerKey;
    }

    public String getReferenceSequenceKey() {
        return referenceSequenceKey;
    }

    public void setReferenceSequenceKey(String referenceSequenceKey) {
        this.referenceSequenceKey = referenceSequenceKey;
    }

    public boolean isAccessControlEnabled() {
        return accessControlEnabled;
    }

    public void setAccessControlEnabled(boolean accessControlEnabled) {
        this.accessControlEnabled = accessControlEnabled;
    }

    public Collection<RegulatoryInfo> getRegulatoryInfos() {
        return regulatoryInfos;
    }

    public List<String> getRegulatoryInfoStrings() {
        List<String> strings=new ArrayList<>();
        for (RegulatoryInfo regulatoryInfo : regulatoryInfos) {
            strings.add(regulatoryInfo.getDisplayText());
        }
        return strings;
    }

    public void setRegulatoryInfos(Collection<RegulatoryInfo> regulatoryInfos) {
        this.regulatoryInfos = regulatoryInfos;
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

    @Override
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

    public void setIrbNotEngaged(boolean irbNotEngaged) {
        this.irbNotEngaged = irbNotEngaged;
    }

    public List<String> getIrbNumbers() {
        List<String> irbNumberList = new ArrayList<>(irbNumbers.size());
        for (ResearchProjectIRB irb : irbNumbers) {
            irbNumberList.add(irb.getIrb() + ": " + irb.getIrbType().getDisplayName());
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
     *
     * @return list of associated people based on their role type
     */
    public Collection<ProjectPerson> findPeopleByType(RoleType role) {
        List<ProjectPerson> foundPersonList = new ArrayList<>(associatedPeople.size());

        for (ProjectPerson person : associatedPeople) {
            if (person.getRole() == role) {
                foundPersonList.add(person);
            }
        }

        return foundPersonList;
    }

    public Long[] getPeople(RoleType role) {
        List<Long> people = new ArrayList<>();

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

    public Set<ResearchProjectFunding> getProjectFunding() {
        return projectFunding;
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


    public String getOriginalTitle() {
        return originalTitle;
    }

    public ResearchProject getParentResearchProject() {
        return parentResearchProject;
    }

    public void setParentResearchProject(ResearchProject parentResearchProject) {
        // Update parent/child relationships so they are correct. Hibernate will create the correct set of children
        // the next time the objects are retrieved from the database.
        if (this.parentResearchProject != null) {
            this.parentResearchProject.childProjects.remove(this);
        }
        if (parentResearchProject != null) {
            parentResearchProject.childProjects.add(this);
        }
        this.parentResearchProject = parentResearchProject;
    }

    /**
     * Traverse through all the potential parent projects until we get to the root parent.  If the parent research
     * project is null, then it returns itself as the root node.
     */
    public ResearchProject getRootResearchProject() {
        if (parentResearchProject == null) {
            return this;
        }

        return parentResearchProject.getRootResearchProject();
    }

    public Collection<ResearchProject> getAllChildren() {
        Collection<ResearchProject> collectedProjects = new TreeSet<>();
        collectedProjects.addAll(getChildProjects());

        return collectChildResearchProjects(collectedProjects);
    }

    public Collection<ResearchProject> getAllParents() {
        Collection<ResearchProject> collectedProjects = new TreeSet<>();
        if (getParentResearchProject()!=null) {
            collectedProjects.add(getParentResearchProject());
        }
        return collectParentResearchProjects(collectedProjects);
    }

    /**
     * Recursive function to traverse through the full research project hierarchy tree to get all the projects.
     *
     * @param collectedProjects the list of collected child research projects
     *
     * @return collection of research projects
     */
    private static Collection<ResearchProject> collectChildResearchProjects(Collection<ResearchProject> collectedProjects) {
        for (ResearchProject childResearchProject : collectedProjects) {
            collectedProjects.addAll(collectChildResearchProjects(childResearchProject.getChildProjects()));
        }
        return collectedProjects;
    }

    /**
     * Recursive function to traverse through the full research project hierarchy tree to get all the projects.
     *
     * @param collectedProjects the list of collected parent research projects
     *
     * @return collection of research projects
     */
    private static Collection<ResearchProject> collectParentResearchProjects(Collection<ResearchProject> collectedProjects) {
        for (ResearchProject researchProject : collectedProjects) {
            if (researchProject.getParentResearchProject()!=null) {
                collectedProjects.add(researchProject);
                collectedProjects.addAll(
                        collectParentResearchProjects(Arrays.asList(researchProject.getParentResearchProject())));
            }
        }
        return collectedProjects;
    }

    /**
     * Ensure that the parent research project model does not create any loops.
     */
    @PrePersist
    protected void prePersist() {
        if (hasLoop()) {
            throw new RuntimeException("Improper Research Project child hierarchy.");
        }
    }

    /**
     * Function to see if there's an endless loop with parent Research Project references.  Loops are not permitted.
     *
     * @return True if there is a loop.
     */
    private boolean hasLoop() {
        ResearchProject parent = parentResearchProject;
        while (parent != null) {
            // Here we're looking for exact object comparison, so no need to call equals().
            //noinspection ObjectEquality
            if (parent == this) {
                return true;
            }
            parent = parent.parentResearchProject;
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
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

    /**
     * Return true if a user has permission to access this project's output.  This is true if the user is associated
     * with the project in any way.
     *
     * @param userId the user to check
     *
     * @return true if use has permission
     */
    private boolean userHasProjectAccess(long userId) {
        for (ProjectPerson person : associatedPeople) {
            if (person.getPersonId() == userId) {
                return true;
            }
        }

        return false;
    }

    /**
     * Collect the set of all projects underneath this project, including those that are under sub-projects, etc.
     *
     * @param subProjects set to put collected projects in
     */
    private void collectSubProjects(Set<ResearchProject> subProjects) {
        subProjects.add(this);
        for (ResearchProject child : childProjects) {
            child.collectSubProjects(subProjects);
        }
    }

    /**
     * Given a project and a user, collect all projects and sub-projects accessible by this user.
     *
     * @param userId             the user to find
     * @param accessibleProjects the set to put the projects visible to the user in
     */
    public void collectAccessibleByUser(long userId, Set<ResearchProject> accessibleProjects) {
        if (!accessControlEnabled) {
            // Access control not enabled, any user can see the data.
            accessibleProjects.add(this);
        }
        if (userHasProjectAccess(userId)) {
            // User has access, add all sub-projects.
            collectSubProjects(accessibleProjects);
        } else {
            // User doesn't have access, check sub-projects for access.
            for (ResearchProject child : childProjects) {
                child.collectAccessibleByUser(userId, accessibleProjects);
            }
        }
    }

    /**
     * Test if this Project is saved in Jira. It has been persisted to Jira if it has a jiraTicketKey.
     *
     * @return true if it has a jiraTicketKey.
     */
    public boolean isSavedInJira() {
        return !StringUtils.isBlank(getJiraTicketKey());
    }

    public void addRegulatoryInfo(RegulatoryInfo... regulatoryInfo) {
        regulatoryInfos.addAll(Arrays.asList(regulatoryInfo));
    }

    public enum Status implements StatusType {
        Open, Archived;

        public static List<String> getNames() {
            List<String> names = new ArrayList<>();
            for (Status status : Status.values()) {
                names.add(status.name());
            }

            return names;
        }

        @Override
        public String getDisplayName() {
            return name();
        }
    }

}
