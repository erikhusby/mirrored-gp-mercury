package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.PositionLabBatches;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

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
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A lab event is an informatics model of some of the types of operations that occur in the lab.
 * Some of the operations captured by informatics in a LabEvent are
 * <ul>
 * <li>Sample identification (sample import)</li>
 * <li>Vessel-sample association</li>
 * <li>Vessel content and properties (volume, concentration)</li>
 * <li>Vessel chain of custody (transfer events)</li>
 * <li>Reagent additions</li>
 * <li>Workflow process sequence, including bucketing and rework</li>
 * <li>and others</li>
 * </ul>
 * Lab events originate from liquid handling deck messages (bettalims messages), web services invoked by BSP
 * or other systems, user gestures in the Mercury UI, and others sources.
 * <p/>
 * Any lab event has the potential to change the lab vessel's molecular state which is the aggregate of all
 * samples and reagents in the vessel.  The molecular state is modeled by one or more SampleInstances in the
 * vessel.
 */
// todo rename to "Event"--everything is an event, including
// deltas in an aggregation in zamboni
@Entity
@Audited
@Table(schema = "mercury",
        uniqueConstraints = @UniqueConstraint(columnNames = {"EVENT_LOCATION", "EVENT_DATE", "DISAMBIGUATOR"}),
        name = "lab_event")
public class LabEvent {
    private static final Log log = LogFactory.getLog(LabEvent.class);

    public static final String UI_EVENT_LOCATION = "User Interface";
    public static final String UI_PROGRAM_NAME = "Mercury";

    /**
     * Sort by ascending date, ascending disambiguator
     */
    public static final Comparator<LabEvent> BY_EVENT_DATE = new Comparator<LabEvent>() {
        @Override
        public int compare(LabEvent o1, LabEvent o2) {
            int dateComparison = o1.getEventDate().compareTo(o2.getEventDate());
            if (dateComparison == 0) {
                return o1.getDisambiguator().compareTo(o2.getDisambiguator());
            }
            return dateComparison;
        }
    };

    public static final Comparator<LabEvent> BY_EVENT_DATE_LOC = new Comparator<LabEvent>() {
        @Override
        public int compare(LabEvent o1, LabEvent o2) {
            int dateComparison = o1.getEventDate().compareTo(o2.getEventDate());
            if (dateComparison == 0 && o1.getEventLocation() != null && o2.getEventLocation() != null ) {
                dateComparison = o1.getEventLocation().compareTo(o2.getEventLocation());
            }
            if (dateComparison == 0) {
                dateComparison = o1.getDisambiguator().compareTo(o2.getDisambiguator());
            }
            return dateComparison;
        }
    };

    @Id
    @SequenceGenerator(name = "SEQ_LAB_EVENT", schema = "mercury", sequenceName = "SEQ_LAB_EVENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_EVENT")
    @Column(name = "LAB_EVENT_ID")
    private Long labEventId;

    @Column(name = "EVENT_LOCATION", length = 255)
    private String eventLocation;

    @Column(name = "EVENT_OPERATOR")
    private Long eventOperator;

    @Column(name = "EVENT_DATE")
    private Date eventDate;

    @Column(name = "DISAMBIGUATOR")
    private Long disambiguator = 0L;

    /**
     * The program name is passed into the message using the 'program' attribute and is the script or program which
     * created this lab event (e.g., "FlowcellLoader"). Having the script name saved will help clarify how messaging,
     * scripts and jira workflows inter-relate.
     */
    @Column(name = "PROGRAM_NAME", length = 255)
    private String programName;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "labEvent")
    @BatchSize(size = 100)
    private Set<LabEventReagent> labEventReagents = new HashSet<>();

    /**
     * for transfers using a tip box, e.g. Bravo
     */
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "labEvent", orphanRemoval = true)
    @BatchSize(size = 20)
    private Set<SectionTransfer> sectionTransfers = new HashSet<>();

    /**
     * for random access transfers, e.g. MultiProbe
     */
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "labEvent", orphanRemoval = true)
    @BatchSize(size = 20)
    private Set<CherryPickTransfer> cherryPickTransfers = new HashSet<>();

    /**
     * for transfers from a single vessel to an entire section, e.g. from a tube to a plate
     */
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "labEvent", orphanRemoval = true)
    @BatchSize(size = 20)
    private Set<VesselToSectionTransfer> vesselToSectionTransfers = new HashSet<>();

    /**
     * Typically for tube to tube transfers
     */
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "labEvent", orphanRemoval = true)
    @BatchSize(size = 20)
    private Set<VesselToVesselTransfer> vesselToVesselTransfers = new HashSet<>();

    /**
     * For plate / tube events, that don't involve a transfer e.g. anonymous reagent addition, loading onto an
     * instrument, entry into a bucket
     */
    @ManyToOne(cascade = {CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinColumn(name = "IN_PLACE_LAB_VESSEL")
    @Nullable
    private LabVessel inPlaceLabVessel;

    /**
     * Required for configurable search nested criteria
     */
    @Column(name = "IN_PLACE_LAB_VESSEL", insertable = false, updatable = false)
    private Long inPlaceLabVesselId;

    @Enumerated(EnumType.STRING)
    @Column(name = "LAB_EVENT_TYPE")
    private LabEventType labEventType;

    /** For events that apply to an entire Batch in a Workflow, e.g. add reagent. */
    @ManyToOne(cascade = {CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinColumn(name = "LAB_BATCH")
    private LabBatch labBatch;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinTable(schema = "mercury", name = "le_lab_event_metadatas"
            , joinColumns = {@JoinColumn(name = "LAB_EVENT")}
            , inverseJoinColumns = {@JoinColumn(name = "LAB_EVENT_METADATAS")})
    private Set<LabEventMetadata> labEventMetadatas = new HashSet<>();

    /**
     * Set by transfer traversal, based on ancestor lab batches and transfers.
     */
    // todo jmt rename to computedLabBatches
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinTable(schema = "mercury", name = "le_computed_lcsets"
            , joinColumns = {@JoinColumn(name = "LAB_EVENT")}
            , inverseJoinColumns = {@JoinColumn(name = "COMPUTED_LCSETS")})
    private Set<LabBatch> computedLcSets;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "labEvent")
    @BatchSize(size = 20)
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "VESSEL_POSITION")
    private Map<VesselPosition, PositionLabBatches> mapPositionToLcSets = new HashMap<>();
    // todo jmt table with FK from event, position, FK from LabBatch

    /**
     * Can be set by a user to indicate the LCSET, in the absence of any distinguishing context, e.g. a set of samples
     * processed in multiple technologies.
     */
    @ManyToOne
    @JoinColumn(name = "MANUAL_OVERRIDE_LC_SET")
    private LabBatch manualOverrideLcSet;

    private String workflowQualifier;

    private static Set<LabEventType> eventTypesThatCanFollowBucket = new HashSet<>();

    /** The station event from which this lab event was created by LabEventFactory. */
    @Transient
    private StationEventType stationEventType;

    /**
     * For JPA
     */
    protected LabEvent() {
    }

    public LabEvent(LabEventType labEventType, Date eventDate, String eventLocation, Long disambiguator, Long operator,
                    String programName) {
        this.labEventType = labEventType;
        this.eventDate = eventDate;
        this.eventLocation = eventLocation;
        this.disambiguator = disambiguator;
        this.eventOperator = operator;
        this.programName = programName;
    }

    /**
     * Helper method to search a collection of events for the existence of a particular lab event
     * @param allEvents     Collection of events in which to search for an event
     * @param targetEvent   the specific event to look for
     * @return  true if the event is present in the given collection
     */
    public static boolean isEventPresent(Collection<LabEvent> allEvents, LabEventType targetEvent) {
        for (LabEvent labEvent : allEvents) {
            if (labEvent.getLabEventType() == targetEvent) {
                return true;
            }
        }
        return false;
    }

    /**
     * getTargetVessels will give to the caller the set of vessels that are on the receiving end of a recorded event
     *
     * @return set of LabVessels
     */
    public Set<LabVessel> getTargetLabVessels() {
        Set<LabVessel> targetLabVessels = new HashSet<>();
        for (SectionTransfer sectionTransfer : sectionTransfers) {
            targetLabVessels.add(sectionTransfer.getTargetVesselContainer().getEmbedder());
        }
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfers) {
            targetLabVessels.add(cherryPickTransfer.getTargetVesselContainer().getEmbedder());
        }
        for (VesselToSectionTransfer vesselToSectionTransfer : vesselToSectionTransfers) {
            targetLabVessels.add(vesselToSectionTransfer.getTargetVesselContainer().getEmbedder());
        }
        for (VesselToVesselTransfer vesselToVesselTransfer : vesselToVesselTransfers) {
            targetLabVessels.add(vesselToVesselTransfer.getTargetVessel());
        }

        return targetLabVessels;
    }

    /**
     * getTargetVesselTubes
     *
     * @return
     */
    public Set<LabVessel> getTargetVesselTubes() {
        Set<LabVessel> eventVessels = new HashSet<>();
        for (LabVessel targetVessel : getTargetLabVessels()) {
            if (targetVessel.getContainerRole() != null &&
                OrmUtil.proxySafeIsInstance(targetVessel, TubeFormation.class)) {
                eventVessels.addAll(targetVessel.getContainerRole().getContainedVessels());
            } else {
                eventVessels.add(targetVessel);
            }
        }
        return eventVessels;
    }

    public Set<LabVessel> getSourceVesselTubes() {
        Set<LabVessel> eventVessels = new HashSet<>();
        for (LabVessel sourceVessel : getSourceLabVessels()) {
            if (sourceVessel.getContainerRole() != null &&
                OrmUtil.proxySafeIsInstance(sourceVessel, TubeFormation.class)) {
                eventVessels.addAll(sourceVessel.getContainerRole().getContainedVessels());
            } else {
                eventVessels.add(sourceVessel);
            }
        }
        return eventVessels;
    }


    /**
     * For transfer events, this returns the sources
     * of the transfer
     *
     * @return may return null
     */
    public Set<LabVessel> getSourceLabVessels() {
        Set<LabVessel> sourceLabVessels = new HashSet<>();
        for (SectionTransfer sectionTransfer : sectionTransfers) {
            sourceLabVessels.add(sectionTransfer.getSourceVesselContainer().getEmbedder());
        }
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfers) {
            sourceLabVessels.add(cherryPickTransfer.getSourceVesselContainer().getEmbedder());
        }
        for (VesselToSectionTransfer vesselToSectionTransfer : vesselToSectionTransfers) {
            sourceLabVessels.add(vesselToSectionTransfer.getSourceVessel());
        }
        for (VesselToVesselTransfer vesselToVesselTransfer : vesselToVesselTransfers) {
            sourceLabVessels.add(vesselToVesselTransfer.getSourceVessel());
        }

        return sourceLabVessels;
    }

    public void addReagent(Reagent reagent) {
        labEventReagents.add(new LabEventReagent(this, reagent));
    }

    /** Removes the corresponding lab event reagent. Intended only for data fixup use. */
    public LabEventReagent removeLabEventReagent(Reagent reagent) {
        LabEventReagent found = null;
        for (LabEventReagent labEventReagent : labEventReagents) {
            if (Reagent.BY_NAME_LOT_EXP.compare(reagent, labEventReagent.getReagent()) == 0) {
                if (found != null) {
                    throw new RuntimeException("Identical " + reagent.getName() + " reagents on LabEvent " +
                                               getLabEventId());
                }
                found = labEventReagent;
            }
        }
        if (found != null) {
            labEventReagents.remove(found);
        }
        return found;
    }

    public void addReagentVolume(Reagent reagent, BigDecimal volume) {
        labEventReagents.add(new LabEventReagent(this, reagent, volume));
    }

    public void addReagentMetadata(Reagent reagent, Set<Metadata> metadataSet) {
        labEventReagents.add(new LabEventReagent(this, reagent, metadataSet));
    }

    public void addMetadata(LabEventMetadata labEventMetadata) {
        labEventMetadatas.add(labEventMetadata);
    }

    /**
     * Returns all the lab vessels involved in this
     * operation, regardless of source/destination.
     * <p/>
     * Useful convenience method for alerts
     *
     * @return
     */
    public Collection<LabVessel> getAllLabVessels() {
        Set<LabVessel> allLabVessels = new HashSet<>();
        allLabVessels.addAll(getSourceLabVessels());
        allLabVessels.addAll(getTargetLabVessels());
        if (inPlaceLabVessel != null) {
            allLabVessels.add(inPlaceLabVessel);
        }
        return allLabVessels;
    }

    /**
     * @return Machine name?  Name of the bench? GPS coordinates?
     */
    public String getEventLocation() {
        return eventLocation;
    }

    void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }

    public Long getEventOperator() {
        return eventOperator;
    }


    void setEventOperator(Long eventOperator) {
        this.eventOperator = eventOperator;
    }

    public String getProgramName() {
        return programName;
    }

    public Date getEventDate() {
        return eventDate;
    }

    void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public Collection<Reagent> getReagents() {
        Set<Reagent> reagents = new HashSet<>();
        for (LabEventReagent labEventReagent : labEventReagents) {
            reagents.add(labEventReagent.getReagent());
        }
        return Collections.unmodifiableCollection(reagents);
    }

    public Set<LabEventReagent> getLabEventReagents() {
        return labEventReagents;
    }

    public Set<SectionTransfer> getSectionTransfers() {
        return sectionTransfers;
    }

    public Set<CherryPickTransfer> getCherryPickTransfers() {
        return cherryPickTransfers;
    }

    public Set<VesselToSectionTransfer> getVesselToSectionTransfers() {
        return vesselToSectionTransfers;
    }

    public Set<VesselToVesselTransfer> getVesselToVesselTransfers() {
        return vesselToVesselTransfers;
    }

    public Long getLabEventId() {
        return labEventId;
    }

/*
todo jmt adder methods
    public void setSectionTransfers(Set<SectionTransfer> sectionTransfers) {
        this.sectionTransfers = sectionTransfers;
    }

    public void setCherryPickTransfers(Set<CherryPickTransfer> cherryPickTransfers) {
        this.cherryPickTransfers = cherryPickTransfers;
    }

    public void setVesselToSectionTransfers(Set<VesselToSectionTransfer> vesselToSectionTransfers) {
        this.vesselToSectionTransfers = vesselToSectionTransfers;
    }
*/

    public Long getDisambiguator() {
        return disambiguator;
    }

    public void setDisambiguator(Long disambiguator) {
        this.disambiguator = disambiguator;
    }

    public Set<LabEventMetadata> getLabEventMetadatas() {
        return labEventMetadatas;
    }

    public void setLabEventMetadatas(Set<LabEventMetadata> labEventMetadatas) {
        this.labEventMetadatas = labEventMetadatas;
    }

    @Nullable
    public LabVessel getInPlaceLabVessel() {
        return inPlaceLabVessel;
    }

    public void setInPlaceLabVessel(LabVessel inPlaceLabVessel) {
        this.inPlaceLabVessel = inPlaceLabVessel;
    }

    public LabEventType getLabEventType() {
        return labEventType;
    }

    /** For data fixup use only. */
    void setLabEventType(LabEventType labEventType) {
        this.labEventType = labEventType;
    }

    public void setLabBatch(LabBatch labBatch) {
        this.labBatch = labBatch;
        labBatch.addLabEvent(this);
    }

    public LabBatch getLabBatch() {
        return labBatch;
    }

    public LabBatch getManualOverrideLcSet() {
        return manualOverrideLcSet;
    }

    public void setManualOverrideLcSet(LabBatch manualOverrideLcSet) {
        this.manualOverrideLcSet = manualOverrideLcSet;
    }

    public String getWorkflowQualifier() {
        return workflowQualifier;
    }

    public void setWorkflowQualifier(String workflowQualifier) {
        this.workflowQualifier = workflowQualifier;
    }

    public StationEventType getStationEventType() {
        return stationEventType;
    }

    public void setStationEventType(StationEventType stationEventType) {
        this.stationEventType = stationEventType;
    }

    /**
     * Gets computed LCSET(s) for this transfer, based on the source vessels.
     *
     * @return LCSETs, empty if the source vessels are not associated with an LCSET.
     */
    public Set<LabBatch> getComputedLcSets() {
        if (manualOverrideLcSet != null) {
            return Collections.singleton(manualOverrideLcSet);
        }
        return Collections.emptySet(); //computeLabBatches();
    }

/*
    public void addComputedLcSets(Set<LabBatch> lcSets) {
        if (computedLcSets == null) {
            computedLcSets = new HashSet<>();
        }
        computedLcSets.addAll(lcSets);
    }
*/

    public Map<VesselPosition, PositionLabBatches> getMapPositionToLcSets() {
        return mapPositionToLcSets;
    }

    public Set<LabBatch> computeLabBatches() {
        if (inPlaceLabVessel != null) {
            // Event in-place vessel is mutually exclusive to any event transfers
            if (inPlaceLabVessel.getContainerRole() != null) {
                computedLcSets.addAll(inPlaceLabVessel.getContainerRole()
                        .getNearestLabBatches(LabBatch.LabBatchType.WORKFLOW));
            } else {
                // In place vessel is not a container
                computedLcSets.addAll( inPlaceLabVessel.getWorkflowLabBatches());

            }
            // Revert to transfers if no LCSET for vessel or container
            if (computedLcSets.isEmpty()) {
                for (LabEvent xferEvent : inPlaceLabVessel.getTransfersTo()) {
                    computedLcSets.addAll( xferEvent.getComputedLcSets() );
                }
            }
        } else {
            // No in-place vessel requires analysis of all event transfers
            // First attempt to find the LCSET that all single-sample vessels have in common
            for (SectionTransfer sectionTransfer : sectionTransfers) {
                Set<LabBatch> sectionLcsets = sectionTransfer.getSourceVesselContainer().getComputedLcSetsForSection(
                        sectionTransfer.getSourceSection());
                if( !sectionLcsets.isEmpty() ) {
                    computedLcSets.addAll(sectionLcsets);
                } else {
                    // Try target vessel container(s) when section transfer source vessel container comes up blank
                    // (e.g. IndexedAdapterLigation event from IndexedAdapterPlate96 source)
                    // Results of this are only valid when the event source vessel contains only reagents
                    //            (or has no samples associated with it)
                    // TODO jms - Serious performance hit on some events - revisit after we remove inference of LCSETs for controls.
                    /***
                    System.out.println( Thread.currentThread().getStackTrace()[1] + " - Try target for samples, event " + labEventId );
                    boolean allReagents = true;
                    Set<SampleInstanceV2> sampleInstances =
                            sectionTransfer.getSourceVesselContainer().getSampleInstancesV2();
                    for( SampleInstanceV2 sampleInstanceV2 : sampleInstances ) {
                        if (!sampleInstanceV2.isReagentOnly()) {
                            allReagents = false;
                            break;
                        }
                    }
                    System.out.println(Thread.currentThread().getStackTrace()[1] + " - Source has " + sampleInstances.size() + " samples and reagents only is: " + allReagents );
                    if (allReagents) {
                        computedLcSets.addAll(sectionTransfer.getTargetVesselContainer().getComputedLcSetsForSection(
                                sectionTransfer.getSourceSection()));
                        System.out.println(Thread.currentThread().getStackTrace()[1] + " - Compute target LCSETs done: " + computedLcSets.size() );
                    }
                    **** */
                }
            }
            computedLcSets.addAll(computeLcSetsForCherryPickTransfers());
            computedLcSets.addAll(computeLcSetsForVesselToSectionTransfers());

/*
            todo jmt revisit after we remove inference of LCSETs for controls.  The performance penalty is too high now.
            // Handle issue with orphan source vessels (e.g. bait)
            if (computedLcSets.isEmpty()) {
                for (LabVessel labVessel : getTargetLabVessels()) {
                    for (LabEvent labEvent : labVessel.getTransfersTo()) {
                        // Stop this from being called when traversing from same lab event
                        if( !labEvent.equals( this ) ) {
                            computedLcSets.addAll(labEvent.getComputedLcSets());
                        }
                    }
                }
            }
*/
        }
        if (LabVessel.DIAGNOSTICS) {
            System.out.println("computedLcSets for " + labEventType.getName() + " " + computedLcSets);
        }
        return computedLcSets;
    }

    private Set<LabBatch> computeLcSetsForCherryPickTransfers() {
        Map<SampleInstanceV2.LabBatchDepth, Integer> mapLabBatchToCount = new HashMap<>();
        int numVesselsWithBucketEntries = 0;
        List<CherryPickTransfer> cherryPickTransferList = new ArrayList<>();
        cherryPickTransferList.addAll(cherryPickTransfers);
        Collections.sort(cherryPickTransferList, new Comparator<CherryPickTransfer>() {
            @Override
            public int compare(CherryPickTransfer o1, CherryPickTransfer o2) {
                return o1.getTargetPosition().compareTo(o2.getTargetPosition());
            }
        });
        // Determine whether we're pooling multiple sources into the same destination
        boolean poolMode = false;
        if (cherryPickTransferList.size() > 1 && cherryPickTransferList.get(0).getTargetPosition() ==
                cherryPickTransferList.get(1).getTargetPosition()) {
            poolMode = true;
        }
        if (poolMode) {
            // This handles the case where two LCSETS are reworked in the ICE bucket, and appear in the same
            // IcePoolingTransfer.  There are two LCSETS for the event as a whole, so this doesn't help disambiguate
            // multiple bucket entries, but there is one unambiguous LCSET for each destination position.
            Set<LabBatch> totalLabBatches = new HashSet<>();
            for (int i = 0; i < cherryPickTransferList.size(); i++) {
                CherryPickTransfer cherryPickTransfer = cherryPickTransferList.get(i);
                Set<SampleInstanceV2> sampleInstancesAtPositionV2 = cherryPickTransfer.getSourceVesselContainer()
                        .getSampleInstancesAtPositionV2(cherryPickTransfer.getSourcePosition());
                numVesselsWithBucketEntries = VesselContainer.collateLcSets(mapLabBatchToCount, numVesselsWithBucketEntries,
                        sampleInstancesAtPositionV2);
                VesselPosition targetPosition = cherryPickTransfer.getTargetPosition();
                if (i == cherryPickTransferList.size() - 1 ||
                        targetPosition != cherryPickTransferList.get(i + 1).getTargetPosition()) {
                    PositionLabBatches labBatches = mapPositionToLcSets.get(targetPosition);
                    if (labBatches == null) {
                        labBatches = new PositionLabBatches();
                        mapPositionToLcSets.put(targetPosition, labBatches);
                    }
                    Set<LabBatch> localComputedLcSets = VesselContainer.computeLcSets(mapLabBatchToCount,
                            numVesselsWithBucketEntries);
                    labBatches.getLabBatchSet().addAll(localComputedLcSets);
                    totalLabBatches.addAll(localComputedLcSets);
                    mapLabBatchToCount.clear();
                    numVesselsWithBucketEntries = 0;
                }
            }
            return totalLabBatches;
        } else {
            for (CherryPickTransfer cherryPickTransfer : cherryPickTransfers) {
                Set<SampleInstanceV2> sampleInstancesAtPositionV2 = cherryPickTransfer.getSourceVesselContainer()
                        .getSampleInstancesAtPositionV2(cherryPickTransfer.getSourcePosition());
                numVesselsWithBucketEntries = VesselContainer.collateLcSets(mapLabBatchToCount, numVesselsWithBucketEntries,
                        sampleInstancesAtPositionV2);
            }
            return VesselContainer.computeLcSets(mapLabBatchToCount, numVesselsWithBucketEntries);
        }
    }

    private Set<LabBatch> computeLcSetsForVesselToSectionTransfers() {
        Map<SampleInstanceV2.LabBatchDepth, Integer> mapLabBatchToCount = new HashMap<>();
        int numVesselsWithBucketEntries = 0;
        for (VesselToSectionTransfer vesselToSectionTransfer : vesselToSectionTransfers) {
            Set<SampleInstanceV2> sampleInstancesAtPositionV2 =
                    vesselToSectionTransfer.getSourceVessel().getSampleInstancesV2();
            numVesselsWithBucketEntries = VesselContainer.collateLcSets(mapLabBatchToCount, numVesselsWithBucketEntries,
                    sampleInstancesAtPositionV2);
        }
        return VesselContainer.computeLcSets(mapLabBatchToCount, numVesselsWithBucketEntries);
    }

    /**
     * Utility method used for grabbing the date of a specific lab vessel event.
     * Note that this is designed specifically to grab an event date for an event that only happens once.
     *
     * @param vessels   List of LabVessel objects.
     * @param eventType LabEventType object indicating what type of event to grab.
     *
     * @return Lab vessel event date or null if there wasn't an event of this type found.
     */
    public static Date getLabVesselEventDateByType(Collection<LabVessel> vessels, LabEventType eventType) {

        for (LabVessel vessel : vessels) {
            for (LabEvent event : vessel.getEvents()) {
                if (event.getLabEventType() == eventType) {
                    return event.getEventDate();
                }
            }
        }
        return null;
    }

    /**
     * Tests if a single lcset can be determined for every sample instance on every target vessel of the lab event.
     * The LCSET can still vary from vessel to vessel, i.e. the lab event can be for multiple LCSETS.
     */
    public boolean vesselsHaveSingleLcsets() {
        for (LabVessel labVessel : getTargetLabVessels()) {
            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                if (sampleInstanceV2.getSingleBatch() == null &&
                    CollectionUtils.isNotEmpty(labVessel.getAllLabBatches(LabBatch.LabBatchType.WORKFLOW))) {
                    String batchNames = "";
                    for (BucketEntry bucketEntry : sampleInstanceV2.getAllBucketEntries()) {
                        batchNames += bucketEntry.getLabBatch().getBatchName() + " ";
                    }
                    log.info("Cannot determine LCSET after " + getLabEventType().getName() +
                             " event for vessel=" + labVessel.getLabel() +
                             " sample=" + sampleInstanceV2.getRootOrEarliestMercurySampleName() +
                             " having batches=" + (batchNames.length() > 0 ? batchNames : "(None)"));
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Lab events following a bucketing step should have well defined LCSET(s) on sample instances of all
     * target lab vessels.
     *
     * @return true if an ambiguous lcset is found.
     */
    public boolean hasAmbiguousLcsetProblem() {
        return (eventTypesThatCanFollowBucket.contains(getLabEventType()) && !vesselsHaveSingleLcsets());
    }

    /**
     * Using workflow config, searches the Mercury supported workflows for events that can follow a bucketing
     * step. Takes into account that the optional steps after a bucket may be skipped.
     */
    public static void setupEventTypesThatCanFollowBucket(WorkflowConfig workflowConfig) {
        for (ProductWorkflowDef workflowDef : workflowConfig.getProductWorkflowDefs()) {
            ProductWorkflowDefVersion effectiveWorkflow = workflowDef.getEffectiveVersion();
            boolean collectEvents = false;
            for (WorkflowProcessDef processDef : effectiveWorkflow.getWorkflowProcessDefs()) {
                WorkflowProcessDefVersion effectiveProcess = processDef.getEffectiveVersion();
                for (WorkflowStepDef step : effectiveProcess.getWorkflowStepDefs()) {
                    if (OrmUtil.proxySafeIsInstance(step, WorkflowBucketDef.class)) {
                        // We've hit a bucket. Set the flag to start collecting step's events.
                        collectEvents = true;
                    } else if (collectEvents) {
                        eventTypesThatCanFollowBucket.addAll(step.getLabEventTypes());
                        if (!step.isOptional()) {
                            collectEvents = false;
                        }
                    }
                }
            }
        }
    }

}
