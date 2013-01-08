package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.infrastructure.SampleMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.notice.StatusNote;
import org.broadinstitute.gpinformatics.mercury.entity.notice.UserRemarks;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Formula;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A piece of plastic or glass that holds sample, reagent or other plastic.
 * In-place lab events can apply to any LabVessel, whereas SectionTransfers and CherryPickTransfers apply to
 * LabVessels with a VesselContainer role (racks and plates), and VesselToVessel and VesselToSection transfers
 * apply to containees (tubes and wells).
 */
@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames = {"label"}))
@BatchSize(size = 50)
public abstract class LabVessel implements Serializable {

    //todo SGM:  create comparator for sorting Containers THEN Create getter that gets sorted containers

    private final static Logger logger = Logger.getLogger(LabVessel.class.getName());

    @SequenceGenerator(name = "SEQ_LAB_VESSEL", schema = "mercury", sequenceName = "SEQ_LAB_VESSEL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_VESSEL")
    @Id
    private Long labVesselId;

    private String label;

    private Date createdOn;

    // todo jmt liquid vs solid?  Not a class level role?  Large tubes can hold both.
    private Float volume;

    private Float concentration;

    @OneToMany(cascade = CascadeType.PERSIST) // todo jmt should this have mappedBy?
    @JoinTable(schema = "mercury")
    private final Set<JiraTicket> ticketsCreated = new HashSet<JiraTicket>();

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(schema = "mercury")
    private Set<LabBatch> labBatches = new HashSet<LabBatch>();

    // todo jmt separate role for reagents?
    @ManyToMany(cascade = CascadeType.PERSIST)
    // have to specify name, generated aud name is too long for Oracle
    @JoinTable(schema = "mercury", name = "lv_reagent_contents")
    private Set<Reagent> reagentContents = new HashSet<Reagent>();

    /**
     * Counts the number of rows in the many-to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip
     */
    @NotAudited
    @Formula("(select count(*) from lv_reagent_contents where lv_reagent_contents.lab_vessel = lab_vessel_id)")
    private Integer reagentContentsCount = 0;

    // todo jmt separate role for containee?
    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(schema = "mercury")
    private Set<LabVessel> containers = new HashSet<LabVessel>();

    /**
     * Counts the number of rows in the many-to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip
     */
    @NotAudited
    @Formula("(select count(*) from lab_vessel_containers where lab_vessel_containers.lab_vessel = lab_vessel_id)")
    private Integer containersCount = 0;

    /**
     * Reagent additions and machine loaded events, i.e. not transfers
     */
    @OneToMany(mappedBy = "inPlaceLabVessel", cascade = CascadeType.PERSIST)
    private Set<LabEvent> inPlaceLabEvents = new HashSet<LabEvent>();

    @OneToMany // todo jmt should this have mappedBy?
    @JoinTable(schema = "mercury")
    private Collection<StatusNote> notes = new HashSet<StatusNote>();

    @OneToMany(mappedBy = "labVessel")
    private Set<BucketEntry> bucketEntries = new HashSet<BucketEntry>();

    @Embedded
    private UserRemarks userRemarks;

    @Transient
    /** todo this is used only for experimental testing for GPLIM-64...should remove this asap! */
    private Collection<? extends LabVessel> chainOfCustodyRoots = new HashSet<LabVessel>();

    // todo jmt separate role for sample holder?
    @ManyToMany(cascade = CascadeType.PERSIST)
    private Set<MercurySample> mercurySamples = new HashSet<MercurySample>();

    @OneToMany(mappedBy = "sourceVessel")
    private Set<VesselToVesselTransfer> vesselToVesselTransfersThisAsSource = new HashSet<VesselToVesselTransfer>();

    @OneToMany(mappedBy = "targetLabVessel")
    private Set<VesselToVesselTransfer> vesselToVesselTransfersThisAsTarget = new HashSet<VesselToVesselTransfer>();

    protected LabVessel(String label) {
        createdOn = new Date();
        this.label = label;
    }

    protected LabVessel() {
    }

    /**
     * Well A01, Lane 3, Region 6 all might
     * be considered a labeled sub-section
     * of a lab vessel.  Labels are GUIDs
     * for LabVessels; no two LabVessels
     * may share this id.  It's primarily the
     * barcode on the piece of plastic.f
     *
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
     * Reagent templates, how to register "these 40
     * plates contain adaptors laid out like
     * so:..."
     * <p/>
     * Special subclass for DNAReagent to deal with
     * indexes and adaptors?  Or give Reagent a way
     * to express how it modifies the molecular envelope?
     *
     * @return reagents
     */
    public Set<Reagent> getReagentContents() {
        if (getReagentContentsCount() != null && getReagentContentsCount() > 0) {
            return reagentContents;
        }
        return Collections.emptySet();
    }

    public void addReagent(Reagent reagent) {
        reagentContents.add(reagent);
        if (reagentContentsCount == null) {
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
     *
     * @param metricName
     * @param searchMode
     * @param sampleInstance
     * @return
     */
    public LabMetric getMetric(LabMetric.MetricName metricName, MetricSearchMode searchMode, SampleInstance sampleInstance) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public void addToContainer(VesselContainer vesselContainer) {
        this.containers.add(vesselContainer.getEmbedder());
        if (this.containersCount == null) {
            this.containersCount = 0;
        }
        this.containersCount++;
    }

    //Utility method for getting containers as a list so they can be displayed in a display table column
    public List<VesselContainer<?>> getContainerList() {
        return new ArrayList<VesselContainer<?>>(getContainers());
    }

    public Set<VesselContainer<?>> getContainers() {
        Set<VesselContainer<?>> vesselContainers = new HashSet<VesselContainer<?>>();
        if (containersCount != null && containersCount > 0) {
            for (LabVessel container : containers) {
                vesselContainers.add(container.getContainerRole());
            }
        }

        return Collections.unmodifiableSet(vesselContainers);
    }

    // todo notion of a "sample group", not a cohort,
    // but rather an ID for the pool of samples within
    // a container.  useful for finding "related"
    // libraries, related by the group of samples

    /**
     * Get the name of the thing.  This
     * isn't just getName() because that would
     * probably clash with something else.
     * <p/>
     * SGM: 6/15/2012 Update.  Added code to return the
     * <a href="http://en.wikipedia.org/wiki/Base_36#Java_implementation" >Base 36 </a> version of the of the label.
     * This implementation assumes that the label can be converted to a long
     *
     * @return
     */
    @Transient
    public String getLabCentricName() {
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
     * Get LabEvents that are transfers from this vessel
     *
     * @return transfers
     */
    public Set<LabEvent> getTransfersFrom() {
        if (getContainerRole() == null) {
            Set<LabEvent> transfersFrom = new HashSet<LabEvent>();
            for (VesselContainer<?> vesselContainer : getContainers()) {
                transfersFrom.addAll(vesselContainer.getTransfersFrom());
            }
            return transfersFrom;
        } else {
            return getContainerRole().getTransfersFrom();
        }
        // todo jmt vessel to vessel transfers
    }

    /**
     * Get LabEvents that are transfers to this vessel
     *
     * @return transfers
     */
    public Set<LabEvent> getTransfersTo() {
        if (getContainerRole() == null) {
            Set<LabEvent> transfersTo = new HashSet<LabEvent>();
            for (VesselContainer<?> vesselContainer : getContainers()) {
                transfersTo.addAll(vesselContainer.getTransfersTo());
            }
            return transfersTo;
        } else {
            return getContainerRole().getTransfersTo();
        }
        // todo jmt vessel to vessel transfers
    }

    public abstract VesselGeometry getVesselGeometry();

    /**
     * When a {@link org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket} is created for a
     * {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel}, let's
     * remember that fact.  It'll be useful when someone wants
     * to know all the lab work that was done for
     * a StartingSample.
     *
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
     *
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

    private List<LabEvent> getAllEventsSortedByDate() {
        Map<Date, LabEvent> sortedTreeMap = new TreeMap<Date, LabEvent>();
        for (LabEvent event : getEvents()) {
            sortedTreeMap.put(event.getEventDate(), event);
        }
        return new ArrayList<LabEvent>(sortedTreeMap.values());
    }

    public void addInPlaceEvent(LabEvent labEvent) {
        this.inPlaceLabEvents.add(labEvent);
        labEvent.setInPlaceLabVessel(this);
    }

    public abstract CONTAINER_TYPE getType();

    public static Collection<String> extractPdoKeyList(Collection<LabVessel> labVessels) {

        Set<String> pdoNames = new HashSet<String>();

        for (LabVessel currVessel : labVessels) {
            Collection<String> nearestPdos = currVessel.getNearestProductOrders();

            if (nearestPdos != null && !nearestPdos.isEmpty()) {
                pdoNames.addAll(nearestPdos);
            } else {
                logger.warning("Most recent PDO came up with more than one result");
            }
        }

        return pdoNames;
    }

    public String getNearestLabBatchesString() {
        return "need to implement getNearestLabBatchesString";
    }

    public enum CONTAINER_TYPE {
        STATIC_PLATE("Plate"),
        PLATE_WELL("Plate Well"),
        RACK_OF_TUBES("Tube Rack"),
        TUBE_FORMATION("Tube Formation"),
        TUBE("Tube"),
        FLOWCELL("Flowcell"),
        STRIP_TUBE("Strip Tube"),
        STRIP_TUBE_WELL("Strip Tube Well"),
        PACBIO_PLATE("PacBio Plate"),
        ILLUMINA_RUN_CHAMBER("Illumina Run Chamber");

        private String name;

        CONTAINER_TYPE(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Returned from getAncestors and getDescendants
     */
    static class VesselEvent {
        private LabVessel labVessel;
        private VesselContainer vesselContainer;
        private VesselPosition position;
        private LabEvent labEvent;

        public VesselEvent(LabVessel labVessel, VesselContainer vesselContainer, VesselPosition position,
                           LabEvent labEvent) {
            this.labVessel = labVessel;
            this.vesselContainer = vesselContainer;
            this.position = position;
            this.labEvent = labEvent;
        }

        public LabVessel getLabVessel() {
            return labVessel;
        }

        public LabEvent getLabEvent() {
            return labEvent;
        }

        public VesselPosition getPosition() {
            return position;
        }

        public VesselContainer getVesselContainer() {
            return vesselContainer;
        }
    }

    /**
     * Computes the {@link SampleInstance} data
     * on-the-fly by walking the history and applying the
     * StateChange applied during lab work.
     *
     * @return
     */
    public Set<SampleInstance> getSampleInstances() {
        if (getContainerRole() != null) {
            return getContainerRole().getSampleInstances();
        }
        TraversalResults traversalResults = traverseAncestors();
        return traversalResults.getSampleInstances();
    }

    public int getSampleInstanceCount() {
        return getSampleInstances().size();
    }

    /**
     * The results of traversing (ancestor) vessels
     */
    static class TraversalResults {
        private Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        private Set<Reagent> reagents = new HashSet<Reagent>();

        void add(TraversalResults traversalResults) {
            sampleInstances.addAll(traversalResults.getSampleInstances());
            reagents.addAll(traversalResults.getReagents());
        }

        public Set<SampleInstance> getSampleInstances() {
            return sampleInstances;
        }

        public Set<Reagent> getReagents() {
            return reagents;
        }

        public void add(SampleInstance sampleInstance) {
            sampleInstances.add(sampleInstance);
        }

        public void add(Reagent reagent) {
            reagents.add(reagent);
        }

        /**
         * After traversing all ancestors in a level in the hierarchy, apply reagents to that level, if any.
         * Reagents are consumed when they are applied to SampleInstances, they don't continue to be applied to
         * other levels.
         */
        public void completeLevel() {
            if (!sampleInstances.isEmpty() && !reagents.isEmpty()) {
                for (SampleInstance sampleInstance : sampleInstances) {
                    for (Reagent reagent : reagents) {
                        sampleInstance.addReagent(reagent);
                    }
                }
                reagents.clear();
            }
        }
    }

    /**
     * Traverse all ancestors of this vessel, accumulating SampleInstances
     *
     * @return accumulated sampleInstances
     */
    TraversalResults traverseAncestors() {
        TraversalResults traversalResults = new TraversalResults();

        List<VesselEvent> vesselEvents = getAncestors();
        for (VesselEvent vesselEvent : vesselEvents) {
            LabVessel labVessel = vesselEvent.getLabVessel();
            // todo jmt put this logic in VesselEvent?
            if (labVessel == null) {
                traversalResults.add(vesselEvent.getVesselContainer().traverseAncestors(vesselEvent.getPosition()));
            } else {
                traversalResults.add(labVessel.traverseAncestors());
            }
        }
        if (isSampleAuthority()) {
            for (MercurySample mercurySample : mercurySamples) {
                traversalResults.add(new SampleInstance(mercurySample, null, null));
            }
        }
        for (Reagent reagent : getReagentContents()) {
            traversalResults.add(reagent);
        }

        traversalResults.completeLevel();
        return traversalResults;
    }

    public List<SampleInstance> getSampleInstancesList() {
        return new ArrayList<SampleInstance>(getSampleInstances());
    }

    /**
     * Get the immediate ancestor vessels to this vessel, in the transfer graph
     *
     * @return ancestors and events
     */
    private List<VesselEvent> getAncestors() {
        List<VesselEvent> vesselEvents = new ArrayList<VesselEvent>();
        for (VesselToVesselTransfer vesselToVesselTransfer : vesselToVesselTransfersThisAsTarget) {
            vesselEvents.add(new VesselEvent(vesselToVesselTransfer.getSourceVessel(), null, null,
                    vesselToVesselTransfer.getLabEvent()));
        }
        for (LabVessel container : containers) {
            vesselEvents.addAll(container.getContainerRole().getAncestors(this));
        }
        return vesselEvents;
    }

    /**
     * Get the immediate descendant vessels to this vessel, in the transfer graph
     *
     * @return descendant and events
     */
    private List<VesselEvent> getDescendants() {
        List<VesselEvent> vesselEvents = new ArrayList<VesselEvent>();
        for (VesselToVesselTransfer vesselToVesselTransfer : vesselToVesselTransfersThisAsSource) {
            vesselEvents.add(new VesselEvent(vesselToVesselTransfer.getTargetLabVessel(), null, null,
                    vesselToVesselTransfer.getLabEvent()));
        }
        for (LabVessel container : containers) {
            vesselEvents.addAll(container.getContainerRole().getDescendants(this));
        }
        return vesselEvents;
    }

    /**
     * Metrics are captured on vessels, but when we
     * look up the values, we most often want to
     * see them related to a sample aliquot instance.
     * <p/>
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
         * vessel, irrespective of
         * ancestor or descendant.
         */
        NEAREST
    }

    /**
     * Get all events that have happened directly to
     * this vessel.
     *
     * @return in place events, transfers from, transfers to
     */
    public Set<LabEvent> getEvents() {
        Set<LabEvent> events = new HashSet<LabEvent>();
        events.addAll(getInPlaceEvents());
        events.addAll(getTransfersFrom());
        events.addAll(getTransfersTo());
        return events;
    }

    /**
     * PM Dashboard will want to show the most recent
     * event performed on this aliquot.  Implementations
     * traipse through lims history to find the most
     * recent event.
     * <p/>
     * For informational use only.  Can be volatile.
     * <p/>
     * For informational use only.  Can be volatile.
     *
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
     *
     * @param statusNote
     */
    public void logNote(StatusNote statusNote) {
        //        logger.info(statusNote);
        this.notes.add(statusNote);
    }

    public Collection<StatusNote> getAllStatusNotes() {
        return this.notes;
    }

    public Float getVolume() {
        return volume;
    }

    public void setVolume(Float volume) {
        this.volume = volume;
    }

    public Float getConcentration() {
        return concentration;
    }

    public void setConcentration(Float concentration) {
        this.concentration = concentration;
    }

    public boolean isSampleAuthority() {
        return !mercurySamples.isEmpty();
    }

    public Set<BucketEntry> getBucketEntries() {
        return Collections.unmodifiableSet(bucketEntries);
    }

    /* *
     * In the context of the given WorkflowDescription, are there any
     * events for this vessel which are annotated as WorkflowAnnotation#SINGLE_SAMPLE_LIBRARY?
     * @param workflowDescription
     * @return
     */
    /*
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
    */

    public void addLabBatch(LabBatch labBatch) {
        labBatches.add(labBatch);
    }

    public Set<LabBatch> getLabBatches() {
        return labBatches;
    }

    public List<LabBatch> getLabBatchesList() {
        return new ArrayList<LabBatch>(getLabBatches());
    }

    // todo jmt can the next three methods be deleted?

    /**
     * Walk the chain of custody back until it can be
     * walked no further.  What you get are the roots
     * of the transfer graph.
     *
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
     *
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

    public List<MercurySample> getMercurySamplesList() {
        List<MercurySample> mercurySamplesList = new ArrayList<MercurySample>();
        if (!mercurySamples.isEmpty()) {
            mercurySamplesList.addAll(getMercurySamples());
        }
        return mercurySamplesList;
    }

    /**
     * For vessels that have been pushed over from BSP, we set
     * the list of samples.  Otherwise, the list of samples
     * is empty and is derived from a walk through event history.
     *
     * @param mercurySample
     */
    public void addSample(MercurySample mercurySample) {
        this.mercurySamples.add(mercurySample);
    }

    public void addAllSamples(Set<MercurySample> mercurySamples) {
        this.mercurySamples.addAll(mercurySamples);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof LabVessel))
            return false;

        LabVessel labVessel = (LabVessel) o;

        if (label != null ? !label.equals(labVessel.label) : labVessel.label != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return label != null ? label.hashCode() : 0;
    }

    public int compareTo(LabVessel other) {
        CompareToBuilder builder = new CompareToBuilder();

        builder.append(label, other.getLabel());

        return builder.toComparison();
    }

    /**
     * This is over ridden by subclasses that implement {@link VesselContainerEmbedder}
     *
     * @return object representing this vessel's role as a container of other vessels
     */
    public VesselContainer getContainerRole() {
        return null;
    }

    /**
     * Visits nodes in the transfer graph, and applies criteria.
     *
     * @param transferTraverserCriteria object that accumulates results of traversal
     * @param traversalDirection        ancestors or descendants
     */
    public void evaluateCriteria(TransferTraverserCriteria transferTraverserCriteria,
                                 TransferTraverserCriteria.TraversalDirection traversalDirection) {
        evaluateCriteria(transferTraverserCriteria, traversalDirection, null, 0);
    }

    void evaluateCriteria(TransferTraverserCriteria transferTraverserCriteria,
                          TransferTraverserCriteria.TraversalDirection traversalDirection, LabEvent labEvent, int hopCount) {
        transferTraverserCriteria.evaluateVesselPreOrder(this, labEvent, hopCount);
        if (traversalDirection == TransferTraverserCriteria.TraversalDirection.Ancestors) {
            for (VesselEvent vesselEvent : getAncestors()) {
                evaluateVesselEvent(transferTraverserCriteria, traversalDirection, hopCount, vesselEvent);
            }
        } else if (traversalDirection == TransferTraverserCriteria.TraversalDirection.Descendants) {
            for (VesselEvent vesselEvent : getDescendants()) {
                evaluateVesselEvent(transferTraverserCriteria, traversalDirection, hopCount, vesselEvent);
            }
        } else {
            throw new RuntimeException("Unknown direction " + traversalDirection.name());
        }
        transferTraverserCriteria.evaluateVesselPostOrder(this, labEvent, hopCount);
    }

    private void evaluateVesselEvent(TransferTraverserCriteria transferTraverserCriteria,
                                     TransferTraverserCriteria.TraversalDirection traversalDirection, int hopCount, VesselEvent vesselEvent) {
        LabVessel labVessel = vesselEvent.getLabVessel();
        if (labVessel == null) {
            vesselEvent.getVesselContainer().evaluateCriteria(vesselEvent.getPosition(),
                    transferTraverserCriteria, traversalDirection, vesselEvent.getLabEvent(), hopCount);
        } else {
            labVessel.evaluateCriteria(transferTraverserCriteria, traversalDirection, vesselEvent.getLabEvent(),
                    hopCount + 1);
        }
    }

    public LabEvent getLatestEvent() {
        LabEvent event = null;
        List<LabEvent> eventsList = getAllEventsSortedByDate();

        int size = eventsList.size();
        if (size > 0) {
            event = eventsList.get(size - 1);
        }
        return event;
    }

    public Collection<String> getNearestProductOrders() {
        TransferTraverserCriteria.NearestProductOrderCriteria nearestProductOrderCriteria =
                new TransferTraverserCriteria.NearestProductOrderCriteria();

        evaluateCriteria(nearestProductOrderCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
        return nearestProductOrderCriteria.getNearestProductOrders();
    }

    public Collection<LabBatch> getNearestLabBatches() {
        TransferTraverserCriteria.NearestLabBatchFinder batchCriteria =
                new TransferTraverserCriteria.NearestLabBatchFinder();
        evaluateCriteria(batchCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
        return batchCriteria.getNearestLabBatches();
    }

    public List<LabBatch> getNearestLabBatchesList() {
        List<LabBatch> batchList = new ArrayList<LabBatch>();
        Collection<LabBatch> batchCollection = getNearestLabBatches();
        if (batchCollection != null) {
            batchList = new ArrayList<LabBatch>(batchCollection);
        }
        return batchList;
    }

    public Collection<LabVessel> getDescendantVessels() {
        TransferTraverserCriteria.LabVesselDescendantCriteria descendantCriteria = new TransferTraverserCriteria.LabVesselDescendantCriteria();
        evaluateCriteria(descendantCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
        return descendantCriteria.getLabVesselDescendants();
    }

}
