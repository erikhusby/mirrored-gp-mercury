package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.GenericLabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The batch of work, as tracked by a person
 * in the lab.  A batch is basically an lc set.
 */
@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames = {"batchName"}))
public class LabBatch {

    @Id
    @SequenceGenerator(name = "SEQ_LAB_BATCH", schema = "mercury", sequenceName = "SEQ_LAB_BATCH")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_BATCH")
    private Long labBatchId;

    public static final String LCSET_PROJECT_PREFIX = "LCSET";

    @ManyToMany(cascade = CascadeType.PERSIST)
    // have to specify name, generated aud name is too long for Oracle
    @JoinTable(schema = "mercury", name = "lb_starting_lab_vessels")
    private Set<LabVessel> startingLabVessels = new HashSet<LabVessel>();

    private boolean isActive = true;

    private String batchName;

    @ManyToOne(fetch = FetchType.LAZY)
    private JiraTicket jiraTicket;

//    @ManyToOne(fetch = FetchType.LAZY)
//    private ProjectPlan projectPlan;

    // todo jmt get Hibernate to sort this
    @OneToMany(mappedBy = "labBatch")
    private Set<GenericLabEvent> labEvents = new LinkedHashSet<GenericLabEvent>();

    /**
     * Create a new batch with the given name
     * and set of {@link Starter starting materials}
     * @param batchName
     * @param starters
     */
//    public LabBatch(ProjectPlan projectPlan,
//                    String batchName,
//                    Set<Starter> starters) {
//        if (projectPlan == null) {
//            throw new NullPointerException("ProjectPlan cannot be null.");
//        }
//        if (batchName == null) {
//            throw new NullPointerException("BatchName cannot be null");
//        }
//        if (starters == null) {
//            throw new NullPointerException("starters cannot be null");
//        }
//        this.projectPlan = projectPlan;
//        this.batchName = batchName;
//        for (Starter starter : starters) {
//            addStarter(starter);
//        }
//    }

    public LabBatch(
            String batchName,
            Set<LabVessel> starters) {
        if (batchName == null) {
            throw new NullPointerException("BatchName cannot be null");
        }
        if (starters == null) {
            throw new NullPointerException("starters cannot be null");
        }
        this.batchName = batchName;
        for (LabVessel starter : starters) {
            addLabVessel(starter);
        }
    }


    protected LabBatch() {
    }

//    public ProjectPlan getProjectPlan() {
//        // todo could have different project plans per
//        // starter, make this a map accessible by Starter.
//        return projectPlan;
//    }

    public Set<LabVessel> getStartingLabVessels() {
        return startingLabVessels;
    }

    public void addLabVessel(LabVessel labVessel) {
        if (labVessel == null) {
            throw new NullPointerException("vessel cannot be null.");
        }
        startingLabVessels.add(labVessel);
        labVessel.addLabBatch(this);
    }

    public boolean getActive() {
        return isActive;
    }
    
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
    
    public String getBatchName() {
        return batchName;
    }

    public void setJiraTicket(JiraTicket jiraTicket) {
        this.jiraTicket = jiraTicket;
    }

    public JiraTicket getJiraTicket() {
        return jiraTicket;
    }

//    public void setProjectPlanOverride(LabVessel vessel,ProjectPlan planOverride) {
//        throw new RuntimeException("I haven't been written yet.");
//    }

//    public ProjectPlan getProjectPlanOverride(LabVessel labVessel) {
//        throw new RuntimeException("I haven't been written yet.");
//    }

    public Set<GenericLabEvent> getLabEvents() {
        return labEvents;
    }

    public void setLabEvents(Set<GenericLabEvent> labEvents) {
        this.labEvents = labEvents;
    }


    /**
     * Submits the contents of this Lab Batch to Jira to create a new LCSET Ticket
     */
    public void submit () throws IOException {


        Map<String, CustomFieldDefinition> submissionFields = ServiceAccessUtility.getJiraCustomFields ();

        List<CustomField> listOfFields = new ArrayList<CustomField> ();

        listOfFields.add ( new CustomField ( submissionFields.get ( RequiredSubmissionFields.PROTOCOL.getFieldName () ),
                                             "", CustomField.SingleFieldType.TEXT ) );
        listOfFields.add ( new CustomField ( submissionFields.get (
                RequiredSubmissionFields.WORK_REQUEST_IDS.getFieldName () ), "", CustomField.SingleFieldType.TEXT ) );

        CreateIssueResponse batchTicket = ServiceAccessUtility.createJiraTicket ( fetchJiraProject ().getKeyPrefix (),
                                                                                  fetchJiraIssueType (), batchName, "",
                                                                                  listOfFields );

        this.setJiraTicket ( new JiraTicket ( batchTicket.getTicketName (), batchTicket.getTicketName () ) );
        jiraTicket.setLabBatch ( this );
    }

    public void addJiraLink (String targetTicketKey) throws IOException {

        ServiceAccessUtility.addJiraPublicLink( AddIssueLinkRequest.LinkType.Related ,this.jiraTicket.getTicketName(),
                                                targetTicketKey);
    }

    /**
     * This is a helper method that binds a specific Jira project to an ProductOrder entity.  This
     * makes it easier for a user of this object to interact with Jira for this entity.
     *
     * @return An enum of type
     *         {@link org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest.Fields.ProjectType} that
     *         represents the Jira Project for Product Orders
     */
    @Transient
    public CreateIssueRequest.Fields.ProjectType fetchJiraProject() {
        return CreateIssueRequest.Fields.ProjectType.LCSET;
    }

    /**
     * This is a helper method that binds a specific Jira Issue Type to an ProductOrder entity.  This
     * makes it easier for a user of this object to interact with Jira for this entity.
     *
     * @return An enum of type
     *         {@link org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest.Fields.Issuetype} that
     *         represents the Jira Issue Type for Product Orders
     */
    @Transient
    public CreateIssueRequest.Fields.Issuetype fetchJiraIssueType() {
        return CreateIssueRequest.Fields.Issuetype.WHOLE_EXOME_HYBSEL;
    }


    /**
     * RequiredSubmissionFields is an enum intended to assist in the creation of a Jira ticket
     * for Product orders
     *
     */
    public enum RequiredSubmissionFields {
        PROTOCOL("Protocol", true ),

        //Will not have WR ID info in Mercury.  Set to a Blank string
        WORK_REQUEST_IDS("Work Request ID(s)", true ),
        POOLING_STATUS("Pooling Status", true ),
        PRIORITY("priority",false),
        DUE_DATE("duedate", false),

        //User comments at batch creation (Post Dec 1 addition)
        IMPORTANT("Important", true),

        // ??
        NUMBER_OF_CONTROLS("Number of Controls", true),
        NUMBER_OF_SAMPLES("Number of Samples", true),

//        DO not set this value.  Leave at it's default (for now)
        LIBRARY_QC_SEQUENCING_REQUIRED("Library QC Sequencing Required?",true),

        //Radio Button custom field
        PROGRESS_STATUS("Progress Status",true),

        //List of Sample names
        GSSR_IDS("GSSR ID(s)",true),
        ;

        private final String fieldName;
        private final boolean customField;

        private RequiredSubmissionFields ( String fieldNameIn, boolean customFieldInd ) {
            fieldName = fieldNameIn;
            customField = customFieldInd;
        }

        public String getFieldName() {
            return fieldName;
        }
    }



}
