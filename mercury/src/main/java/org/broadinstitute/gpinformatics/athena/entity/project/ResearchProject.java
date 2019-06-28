package org.broadinstitute.gpinformatics.athena.entity.project;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.common.StatusType;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraProject;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Research Projects hold all the information about a research project
 */
@Entity
@Audited
@Table(name = "RESEARCH_PROJECT", schema = "athena")
public class ResearchProject implements BusinessObject, JiraProject, Comparable<ResearchProject>, Serializable {

    public static final String REGULATORY_COMPLIANCE_STATEMENT =
            "If orders created from this Research Project involve human-derived samples (even if commercially "
            + "available, or established cell lines), a Broad ORSP ID number is required (e.g. ORSP-1212, IRB-1234, "
            + "NE-1234, NHSR-1234).  Contact orsp@broadinstitute.org for more information about obtaining an ORSP ID "
            + "number. Note: Internal technical development/validation projects using a Coriell cell line have already "
            + "received a blanket determination (ORSP-995).";

    public static final String REGULATORY_COMPLIANCE_STATEMENT_2 =
            "If your order does not involve any material derived from humans "
            + "(e.g. synthetic DNA, mouse samples), then neither ORSP nor IRB review is required.";

    public static final String NOT_FROM_HUMANS_REASON_FILL =
            "My project does not involve samples or cell lines that originated from humans.";

    public static final String FROM_CLINICAL_CELL_LINE =
            "Samples will be processed through a clinical workflow and were received with a signed clinical requisition.";



    public boolean isResearchOnly() {
        return getRegulatoryDesignation() == RegulatoryDesignation.RESEARCH_ONLY;
    }

    public enum RegulatoryDesignation {
        // changing enum names requires integration testing with the pipeline,
        // but descriptions can change without impacting the pipeline
        RESEARCH_ONLY("Research Grade", false),
        CLINICAL_DIAGNOSTICS("Clinical Diagnostics", true),
        GENERAL_CLIA_CAP("General CLIA/CAP Quality System", true);

        private final String description;
        private final boolean isClinical;

        private RegulatoryDesignation(String description, boolean isClinical) {
            this.description = description;
            this.isClinical = isClinical;
        }

        public String getDescription() {
            return description;
        }

        public boolean isClinical() {
            return isClinical;
        }
    }

    public static final boolean IRB_ENGAGED = false;

    public static final boolean IRB_NOT_ENGAGED = true;
    public static final String PREFIX = "RP-";

    private static final long serialVersionUID = 937268527371239980L;

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

    @Column(name = "IRB_NOT_ENGAGED")
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
    @BatchSize(size = 100)
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
            joinColumns = {@JoinColumn(name="RESEARCH_PROJECT")},
            inverseJoinColumns = {@JoinColumn(name="REGULATORY_INFOS")})
    private Collection<RegulatoryInfo> regulatoryInfos = new ArrayList<>();

    // This is used for edit to keep track of changes to the object.
    @Transient
    private String originalTitle;


    @Enumerated(EnumType.STRING)
    @Column(name = "regulatory_designation")
    private RegulatoryDesignation regulatoryDesignation;

    @OneToMany(mappedBy = "researchProject", cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
            orphanRemoval = true)
    private List<SubmissionTracker> submissionTrackers = new ArrayList<>();

    @Column(name = "REPOSITORY_NAME")
    private String submissionRepositoryName;

    /**
     * The Buick ManifestSessions linked to this ResearchProject.
     */
    @OneToMany(mappedBy = "researchProject", cascade = CascadeType.PERSIST)
    private Set<ManifestSession> manifestSessions = new HashSet<>();

    // todo: we can cache the submissiontrackers in a static map
    public SubmissionTracker getSubmissionTracker(SubmissionTuple submissionTuple) {
        Set<SubmissionTracker> foundSubmissionTrackers = new HashSet<>();
        for (SubmissionTracker submissionTracker : getSubmissionTrackers()) {
            if (submissionTracker.getSubmissionTuple().equals(submissionTuple)) {
                if (!foundSubmissionTrackers.add(submissionTracker)){
                    throw new RuntimeException("More then one result found");
                }
            }
        }
        if (foundSubmissionTrackers.isEmpty()) {
            return null;
        }
        return foundSubmissionTrackers.iterator().next();
    }

    /**
     * no arg constructor.
     */
    public ResearchProject() {
        this(null, null, null, false, null);
    }

    public ResearchProject(BspUser user) {
        this(user.getUserId(), null, null, false, null);
    }

    /**
     * The full constructor for fields that are not settable.
     *
     * @param createdBy     The user creating the project
     * @param title         The title (name) of the project
     * @param synopsis      A description of the project
     * @param irbNotEngaged Is this project set up for NO IRB?
     * @param regulatoryDesignation The regulatory designation for this research project
     */
    public ResearchProject(Long createdBy, String title, String synopsis, boolean irbNotEngaged,
                           RegulatoryDesignation regulatoryDesignation) {
        createdDate = new Date();
        modifiedDate = createdDate;
        irbNotes = "";

        this.title = title;
        this.synopsis = synopsis;
        this.createdBy = createdBy;
        this.modifiedBy = createdBy;
        this.irbNotEngaged = irbNotEngaged;
        this.regulatoryDesignation = regulatoryDesignation;
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

    /**
     * If the title is being used to populate javascript code, a special character in the title could corrupt
     * the script, resulting in page rendering errors.
     * @return websafe title which won't corrupt javascript.
     */
    public String getWebSafeTitle() {
        return StringEscapeUtils.escapeEcmaScript(title);
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

    public String getSubmissionRepositoryName() {
        return submissionRepositoryName;
    }

    public void setSubmissionRepositoryName(String submissionSiteName) {
        this.submissionRepositoryName = submissionSiteName;
    }

    public Collection<RegulatoryInfo> getRegulatoryInfos() {
        return regulatoryInfos;
    }

    public Map<String, RegulatoryInfo> getRegulatoryByIdentifier() {
        Collection<RegulatoryInfo> regulatoryInfos = getRegulatoryInfos();
        return Maps.uniqueIndex(regulatoryInfos, new Function<RegulatoryInfo, String>() {
            @Override
            public String apply(@Nullable RegulatoryInfo input) {
                return input.getIdentifier();
            }
        });
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

    public RegulatoryDesignation getRegulatoryDesignation() {
        return regulatoryDesignation;
    }
    public void setRegulatoryDesignation(RegulatoryDesignation regulatoryDesignation) {
        this.regulatoryDesignation = regulatoryDesignation;
    }

    public String getRegulatoryDesignationDescription() {
        return getRegulatoryDesignation().getDescription();
    }

    /**
     * Returns a stable code for the regulatory designation so that
     * the UI description can change independent of the underlying
     * code by which the pipeline knows the regulatory designation.
     */
    public String getRegulatoryDesignationCodeForPipeline() {
        String regulatoryDesignationCode = null;
        if (regulatoryDesignation != null) {
            regulatoryDesignationCode = regulatoryDesignation.name();
        }
        return regulatoryDesignationCode;
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

    /**
     * @return a full list of cohorts in the same way cohort Ids are retrieved.
     */

    public ResearchProjectCohort[] getCohorts() {
        int i = 0;
        ResearchProjectCohort[] cohorts = new ResearchProjectCohort[sampleCohorts.size()];
        for (ResearchProjectCohort researchProjectCohort : sampleCohorts) {
            cohorts[i++] = researchProjectCohort;
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

    /**
     * Add to the list of people associated with this project.
     * @param role the role of the people being added
     * @param people the people to add
     * @return true if any people were added
     */
    public boolean addPeople(@Nonnull RoleType role, @Nonnull Collection<BspUser> people) {
        int peopleSize = associatedPeople.size();

        for (BspUser user : people) {
            associatedPeople.add(new ProjectPerson(this, role, user.getUserId()));
        }

        return peopleSize != associatedPeople.size();
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

    /**
     * Find all people in project with given RoleType.
     * <p/>
     * <b>Note</b> that it is possible for a ProjectPerson to have one role in the project but have a different role in
     * their UserBean. A case where this could happen is when a user is a PM in a project and their Role is revoked
     * for some reason. Because of this it is important  to check the project role and the individual's role when
     * determining access.
     */
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

    public void addProductOrder(ProductOrder productOrder) {
        productOrders.add(productOrder);
    }

    public void removeProductOrder(ProductOrder productOrder) {
        productOrders.remove(productOrder);
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

    /**
     * Recursively Find all Research Projects which are parents of this one.
     *
     * @return Collection of ResearchProjects sorted by ascending depth.
     */
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
        for (RegulatoryInfo info : regulatoryInfo) {
            info.addResearchProject(this);
        }
    }

    public void removeRegulatoryInfo(RegulatoryInfo regulatoryInfo) {
        regulatoryInfos.remove(regulatoryInfo);
        regulatoryInfo.removeResearchProject(this);
    }

    public Set<ProductOrderSample> collectSamples() {

        Set<ProductOrderSample> allProductOrderSamples = new HashSet<>();
        for(ProductOrder order:productOrders) {
            allProductOrderSamples.addAll(order.getSamples());
        }
        return allProductOrderSamples;
    }

    public List<SubmissionTracker> getSubmissionTrackers() {
        return submissionTrackers;
    }

    public void setSubmissionTrackers(List<SubmissionTracker> submissionTrackers) {
        this.submissionTrackers = submissionTrackers;
    }

    public void addSubmissionTracker(SubmissionTracker... submissionTracker) {
        submissionTrackers.addAll(Arrays.asList(submissionTracker));

        for(SubmissionTracker tracker:submissionTracker) {
            tracker.setResearchProject(this);
        }
    }

    public Set<ManifestSession> getManifestSessions() {
        return manifestSessions;
    }

    public void addManifestSession(ManifestSession manifestSession) {
        this.manifestSessions.add(manifestSession);
    }

    /**
     * Collects all manifest records eligible for validation.  Records for which validation has been
     * run and has found errors are not eligible for validation.
     */
    public List<ManifestRecord> collectNonQuarantinedManifestRecords() {
        List<ManifestRecord> allRecords = new ArrayList<>();

        for (ManifestSession manifestSession : getManifestSessions()) {
            allRecords.addAll(manifestSession.getNonQuarantinedRecords());
        }

        return allRecords;
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

