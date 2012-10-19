package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.Failure;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.GenericLabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.notice.StatusNote;
import org.broadinstitute.gpinformatics.mercury.entity.notice.UserRemarks;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.project.Starter;
import org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.infrastructure.SampleMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.SequencingLibraryAnnotation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowAnnotation;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Formula;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames = {"label"}))
@BatchSize(size = 50)
public abstract class LabVessel implements Starter {

    private final static Logger logger = Logger.getLogger(LabVessel.class.getName());

    @SequenceGenerator(name = "SEQ_LAB_VESSEL", schema = "mercury",  sequenceName = "SEQ_LAB_VESSEL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_VESSEL")
    @Id
    private Long labVesselId;

    private String label;

    private Date createdOn;

    @OneToMany(cascade = CascadeType.PERSIST) // todo jmt should this have mappedBy?
    @JoinTable(schema = "mercury")
    private final Set<JiraTicket> ticketsCreated = new HashSet<JiraTicket>();

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(schema = "mercury")
    private Set<LabBatch> labBatches = new HashSet<LabBatch>();

    @ManyToOne(fetch = FetchType.LAZY)
    private LabVessel readBucketAuthority;


    @ManyToMany(cascade = CascadeType.PERSIST)
    // have to specify name, generated aud name is too long for Oracle
    @JoinTable(schema = "mercury", name = "lv_reagent_contents")
    private Set<Reagent> reagentContents = new HashSet<Reagent>();

    /** Counts the number of rows in the many-to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip  */
    @NotAudited
    @Formula("(select count(*) from lv_reagent_contents where lv_reagent_contents.lab_vessel = lab_vessel_id)")
    private Integer reagentContentsCount = 0;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(schema = "mercury")
    private Set<LabVessel> containers = new HashSet<LabVessel>();

    /** Counts the number of rows in the many-to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip  */
    @NotAudited
    @Formula("(select count(*) from lab_vessel_containers where lab_vessel_containers.lab_vessel = lab_vessel_id)")
    private Integer containersCount = 0;

    @OneToMany(mappedBy = "inPlaceLabVessel")
    private Set<LabEvent> inPlaceLabEvents = new HashSet<LabEvent>();

    @OneToMany // todo jmt should this have mappedBy?
    @JoinTable(schema = "mercury")
    private Collection<StatusNote> notes = new HashSet<StatusNote>();


    @Embedded
    private UserRemarks userRemarks;

    @Transient
    /** todo this is used only for experimental testing for GPLIM-64...should remove this asap! */
    private Collection<? extends LabVessel> chainOfCustodyRoots = new HashSet<LabVessel>();

    @ManyToMany
    private Set<MercurySample> mercurySamples = new HashSet<MercurySample>();

    protected LabVessel(String label) {
        this.label = label;
    }

    protected LabVessel() {
    }

    private Long getLabVesselId() {
        return labVesselId;
    }

    private void setLabVesselId(Long labVesselId) {
        this.labVesselId = labVesselId;
    }

    /**
     * Well A01, Lane 3, Region 6 all might
     * be considered a labeled sub-section
     * of a lab vessel.  Labels are GUIDs
     * for LabVessels; no two LabVessels
     * may share this id.  It's primarily the
     * barcode on the piece of plastic.f
     * @return
     */
    public String getLabel() {
        return label;
    }

    public void addMetric(LabMetric m) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public Collection<LabMetric> getMetrics() {
        throw new RuntimeException("I haven't been written yet.");
    }

    /**
     * A failure of any sort: quant, sequencing,
     * smells bad, not the right size around the
     * hips, etc.
     * @param failureMode
     */
    public void addFailure(Failure failureMode) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public Collection<Failure> getFailures() {
        throw new RuntimeException("I haven't been written yet.");
    }

    /**
     * Reagent templates, how to register "these 40
     * plates contain adaptors laid out like
     * so:..."
     *
     * Special subclass for DNAReagent to deal with
     * indexes and adaptors?  Or give Reagent a way
     * to express how it modifies the molecular envelope?
     * @return
     */
    public Collection<Reagent> getReagentContents() {
        return reagentContents;
    }

    public void addReagent(Reagent reagent) {
        reagentContents.add(reagent);
        if(reagentContentsCount == null) {
            reagentContentsCount = 0;
        }
        reagentContentsCount++;
    }

    public Integer getReagentContentsCount() {
        return reagentContentsCount;
    }

    /**
     * When traipsing our internal lims data, the search
     * mode is important.  If we're referencing sample sheet
     * data that was sent to us by a collaborator, we
     * probably ignore the search mode, or require
     * that it be set to THIS_VESSEL_ONLY, since we'll
     * only have metrics for a single container--and
     * no transfer graph.
     * @param metricName
     * @param searchMode
     * @param sampleInstance
     * @return
     */
    public LabMetric getMetric(LabMetric.MetricName metricName, MetricSearchMode searchMode, SampleInstance sampleInstance) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public boolean isProgeny(LabVessel ancestor) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public boolean isAncestor(LabVessel progeny) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public void addToContainer(VesselContainer vesselContainer) {
        this.containers.add(vesselContainer.getEmbedder());
        if(this.containersCount == null) {
            this.containersCount = 0;
        }
        this.containersCount++;
    }

    public Set<VesselContainer<?>> getContainers() {
        Set<VesselContainer<?>> vesselContainers = new HashSet<VesselContainer<?>>();
        if(containersCount != null && containersCount > 0) {
            for (LabVessel container : containers) {
                vesselContainers.add(OrmUtil.proxySafeCast(container, VesselContainerEmbedder.class).getVesselContainer());
            }
        }

        return Collections.unmodifiableSet(vesselContainers);
    }

    public Integer getContainersCount() {
        return containersCount;
    }

    // todo notion of a "sample group", not a cohorot,
    // but rather an ID for the pool of samples within
    // a container.  useful for finding "related"
    // libraries, related by the group of samples

    /**
     * Get the name of the thing.  This
     * isn't just getName() because that would
     * probably clash with something else.
     * 
     * SGM: 6/15/2012 Update.  Added code to return the
     * <a href="http://en.wikipedia.org/wiki/Base_36#Java_implementation" >Base 36 </a> version of the of the label.
     * This implementation assumes that the label can be converted to a long
     * 
     * @return
     */
    @Transient
    public String getLabCentricName() {
        // todo jmt what should this do?
        String vesselContentName;

        try {

            vesselContentName = Long.toString(Long.parseLong(label), 36);

        } catch (NumberFormatException nfe) {
            vesselContentName = label;
            logger.log(Level.WARNING, "Could not return Base 36 version of label.  Returning original label instead");
        }

        return vesselContentName;
    }

    /**
     * This needs to be really, really fast.
     * @return
     */
    public abstract Set<LabEvent> getTransfersFrom();

    /**
     * This needs to be really, really fast.
     * @return
     */
    public abstract Set<LabEvent> getTransfersTo();

/*
    public void addNoteToProjects(String message) {
        Collection<Project> ticketsToNotify = new HashSet<Project>();
        for (SampleInstance sampleInstance : getSampleInstances()) {
            if (sampleInstance.getAllProjectPlans() != null) {
                for (ProjectPlan projectPlan : sampleInstance.getAllProjectPlans()) {
                    ticketsToNotify.add(projectPlan.getProject());
                }
            }
        }
        for (Project project : ticketsToNotify) {
            project.addJiraComment(message);
        }
    }
*/

    /**
     * When a {@link org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket} is created for a
     * {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel}, let's
     * remember that fact.  It'll be useful when someone wants
     * to know all the lab work that was done for
     * a {@link org.broadinstitute.gpinformatics.mercury.entity.sample.StartingSample}.
     * @param jiraTicket
     */
    public void addJiraTicket(JiraTicket jiraTicket) {
        if (jiraTicket != null) {
            ticketsCreated.add(jiraTicket);
        }
    }

    /**
     * Get all the {@link JiraTicket jira tickets} that were started
     * with this {@link LabVessel}
     * @return
     */
    public Collection<JiraTicket> getJiraTickets() {
        return ticketsCreated;
    }

    public UserRemarks getUserRemarks() {
        return userRemarks;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public Set<LabEvent> getInPlaceEvents() {
        return inPlaceLabEvents;
    }

    public void addInPlaceEvent(LabEvent labEvent) {
        this.inPlaceLabEvents.add(labEvent);
        labEvent.setInPlaceLabVessel(this);
    }

    public abstract CONTAINER_TYPE getType();

    public enum CONTAINER_TYPE {
        STATIC_PLATE,
        PLATE_WELL,
        RACK_OF_TUBES,
        TUBE,
        FLOWCELL,
        STRIP_TUBE,
        STRIP_TUBE_WELL,
        PACBIO_PLATE,
        ILLUMINA_RUN_CHAMBER
    }

    /**
     * Probably a transient that computes the {@link SampleInstance} data
     * on-the-fly by walking the history and applying the
     * StateChange applied during lab work.
     * @return
     */
    public abstract Set<SampleInstance> getSampleInstances();

    /**
     * I'm avoiding parent/child semantics
     * here deliberately to avoid confusion
     * with transfer graphs and the notion
     * of ancestor/descendant.
     *
     * If this {@link LabVessel} is a {@link VesselPosition},
     * then the containing vessel could be a {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate}.
     * If this {@link LabVessel} is a {@link org.broadinstitute.gpinformatics.mercury.entity.run.RunChamber flowcell lane},
     * then perhaps the containing {@link LabVessel} is a {@link org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge flowcell}.
     * @return
     */
    public abstract LabVessel getContainingVessel();

    /**
     * Metrics are captured on vessels, but when we
     * look up the values, we most often want to
     * see them related to a sample aliquot instance.
     *
     * The search mode tells you "how" to walk the
     * transfer graph to find the given metric.
     */
    public enum MetricSearchMode {
        /**
         * Only look for metrics captured
         * on this vessel directly.
         */
        THIS_VESSEL_ONLY,
        /**
         * look anywhere in the transfer graph,
         * the first matching metric you find
         * will be used.
         */
        ANY,
        /**
         * Look for the metric associated with
         * nearest ancestor (including this
         * vessel)
         */
        NEAREST_ANCESTOR,
        /**
         * Look for the metric associated
         * with the nearest descendant (including
         * this vessel)
         */
        NEAREST_DESCENDANT,
        /**
         * Find the metric on the nearest
         * vessell, irrespective of
         * ancestor or descendant.
         */
        NEAREST
    }

    /**
     * Get all events that have happened directly to
     * this vesell.  Really, really fast.  Like
     * right now.
     * @return
     */
    public abstract Collection<LabEvent> getEvents();

    /**
     * Returns all projects.  Convenience method vs.
     * iterating over {@link #getSampleInstances()} and
     * calling {@link org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance#getAllProjectPlans()}
     * @return
     */
//    public abstract Collection<Project> getAllProjects();


    /**
     * PM Dashboard will want to show the most recent
     * event performed on this aliquot.  Implementations
     * traipse through lims history to find the most
     * recent event.
     *
     * For informational use only.  Can be volatile.
     * @return
     */
    public StatusNote getLatestNote() {
        throw new RuntimeException("I haven't been written yet.");
    }

    /**
     * Reporting will want to look at aliquot-level
     * notes, not traipse through our {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent}
     * history.  So every time we do a {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent}
     * or have key things happen like {@link org.broadinstitute.gpinformatics.infrastructure.bsp.AliquotReceiver receiving an aliquot},
     * recording quants, etc. our code will want to post
     * a semi-structured note here for reporting.
     * @param statusNote
     */
    /**
     * Adds a persistent note.  These notes will be used for reporting
     * things like dwell time and reporting status back to other
     * systems like PM Bridge, POEMs, and reporting.  Instead of having
     * these other systems query our operational event information,
     * we can summarize the events in a more flexible way in
     * a sample centric manner.
     * @param statusNote
     */
    public void logNote(StatusNote statusNote) {
//        logger.info(statusNote);
        this.notes.add(statusNote);
    }

    public Collection<StatusNote> getAllStatusNotes() {
        return this.notes;
    }

    public abstract Float getVolume();

    public abstract Float getConcentration();

    public boolean isSampleAuthority() {
        return false;
    }

    @Override
    public boolean isAliquotExpected() {
        return false;
    }

    /**
     * In the context of the given {@link WorkflowDescription}, are there any
     * events for this vessel which are annotated as WorkflowAnnotation#SINGLE_SAMPLE_LIBRARY?
     * @param workflowDescription
     * @return
     */
    public boolean isSingleSampleLibrary(WorkflowDescription workflowDescription) {
        if (workflowDescription == null) {
            throw new RuntimeException("workflowDescription cannot be null.");
        }
        boolean isSingleSample = false;

        final Set<LabEvent> allEvents = new HashSet<LabEvent>();

        Set<VesselContainer<?>> containers = getContainers();

        if (containers != null) {
            for (VesselContainer<? extends LabVessel> container : containers) {
                // todo arz is confused about containers, embedders, and vessels.
                allEvents.addAll(container.getEmbedder().getTransfersTo());
                allEvents.addAll(container.getEmbedder().getInPlaceEvents());
            }
        }
        allEvents.addAll(getInPlaceEvents());
        allEvents.addAll(getTransfersTo());

        for (LabEvent event: allEvents) {
            GenericLabEvent labEvent = OrmUtil.proxySafeCast(event, GenericLabEvent.class);
            Collection<WorkflowAnnotation> workflowAnnotations = workflowDescription.getAnnotations(labEvent.getLabEventType().getName());

            for (WorkflowAnnotation workflowAnnotation : workflowAnnotations) {
                if (workflowAnnotation instanceof SequencingLibraryAnnotation) {
                    isSingleSample = true;
                    break;
                }
            }
        }
        return isSingleSample;
    }

    @Override
    public void addLabBatch(LabBatch labBatch) {
        labBatches.add(labBatch);
    }

    @Override
    public Set<LabBatch> getLabBatches() {
        return labBatches;
    }

    /**
     * Does this container know what it's sample metadata is?
     * Or does it need to look back in the event graph?
     * @return
     */
    public boolean hasSampleMetadata() {
        throw new RuntimeException("not implemented");
    }


    /**
     * Walk the chain of custody back until it can be
     * walked no further.  What you get are the roots
     * of the transfer graph.
     * @return
     */
    public Collection<? extends LabVessel> getChainOfCustodyRoots() {
        // todo the real method should walk transfers...this is just for experimental testing for GPLIM-64
        return chainOfCustodyRoots;
    }

    public void setChainOfCustodyRoots(Collection<? extends LabVessel> roots) {
        // todo this is just for experimental GPLIM-64...this method shouldn't ever
        // be in production.
        this.chainOfCustodyRoots = roots;
    }

    /**
     * What {@link SampleMetadata samples} are contained in
     * this container?  Implementations are expected to
     * walk the transfer graph back to a point where
     * they can lookup {@link SampleMetadata} from
     * an external source like BSP or a spreadsheet
     * uploaded for "walk up" sequencing.
     * @return
     */
    public Set<MercurySample> getMercurySamples() {
        // todo the real method should walk transfers...this is just for experimental testing for GPLIM-64
        // in reality, the implementation would walk back to all roots,
        // detecting vessels along the way where hasSampleMetadata is true.
        if (!mercurySamples.isEmpty()) {
            return mercurySamples;
        }
        // else walk transfers
        throw new RuntimeException("history traversal for empty samples list not implemented");

    }

    /**
     * For vessels that have been pushed over from BSP, we set
     * the list of samples.  Otherwise, the list of samples
     * is empty and is derived from a walk through event history.
     * @param mercurySample
     */
    public void addSample(MercurySample mercurySample) {
        this.mercurySamples.add(mercurySample);
    }

    public void addAllSamples(Set<MercurySample> mercurySamples) {
        this.mercurySamples.addAll(mercurySamples);
    }
}


