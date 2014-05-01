package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A lab event isn't just at the granularity
 * of what we now consider a station event.
 * <p/>
 * Any lab event has the potential to change the
 * molecular state (see MolecularEnvelope) of the
 * lab vessels it references.  Some lab events may
 * not change the molecular envelope, but for those
 * that do, we expect the LabEvent to know the "expected"
 * molecular envelope of the input materials.  The event
 * itself can also alter the molecular envelope.
 * <p/>
 * This isn't too far off base: a Bravo protocol might
 * add adaptors.  The covaris event shears target DNA.
 * Both events change the molecular state; in order for
 * the lab operation to do its job, there are certain
 * input requirements that need to be met, such as
 * a concentration range, or the presence of adaptors
 * of a particular type.  If the incoming samples do not
 * meet the requirements, bad things happen, which
 * we expose via throwing InvalidPriorMolecularStateException
 * in applyMolecularStateChanges() method.
 * <p/>
 * In this model, LabEvents have to be quite aware of
 * the particulars of their inputs and outputs.  This
 * means it must be easy for lab staff to change expected
 * inputs and outputs based both on the event definition
 * (think: Bravo protocol file) as well as the workflow.
 * <p/>
 * LabEvents can be re-used in different workflows, with
 * different expected ranges, and project managers might
 * want to override these ranges.
 */
// todo rename to "Event"--everything is an event, including
// deltas in an aggregation in zamboni
@Entity
@Audited
@Table(schema = "mercury",
        uniqueConstraints = @UniqueConstraint(columnNames = {"EVENT_LOCATION", "EVENT_DATE", "DISAMBIGUATOR"}),
        name = "lab_event")
public class LabEvent {

    public static final String UI_EVENT_LOCATION = "User Interface";
    public static final String UI_PROGRAM_NAME = "Mercury";

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

    @Id
    @SequenceGenerator(name = "SEQ_LAB_EVENT", schema = "mercury", sequenceName = "SEQ_LAB_EVENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_EVENT")
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

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinTable(schema = "mercury")
    private Set<Reagent> reagents = new HashSet<>();

    /**
     * for transfers using a tip box, e.g. Bravo
     */
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "labEvent")
    private Set<SectionTransfer> sectionTransfers = new HashSet<>();

    /**
     * for random access transfers, e.g. MultiProbe
     */
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "labEvent")
    private Set<CherryPickTransfer> cherryPickTransfers = new HashSet<>();

    /**
     * for transfers from a single vessel to an entire section, e.g. from a tube to a plate
     */
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "labEvent")
    private Set<VesselToSectionTransfer> vesselToSectionTransfers = new HashSet<>();

    /**
     * Typically for tube to tube transfers
     */
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "labEvent")
    private Set<VesselToVesselTransfer> vesselToVesselTransfers = new HashSet<>();

    /**
     * For plate / tube events, that don't involve a transfer e.g. anonymous reagent addition, loading onto an
     * instrument, entry into a bucket
     */
    @ManyToOne(cascade = {CascadeType.PERSIST}, fetch = FetchType.LAZY)
    private LabVessel inPlaceLabVessel;

    // todo jmt delete productOrderId?
    /**
     * Business Key of a product order to which this event is associated
     */
    @Column(name = "PRODUCT_ORDER_ID")
    private String productOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "LAB_EVENT_TYPE")
    private LabEventType labEventType;

    @ManyToOne(cascade = {CascadeType.PERSIST}, fetch = FetchType.LAZY)
    private LabBatch labBatch;

    /**
     * Set by transfer traversal, based on ancestor lab batches and transfers.
     */
    @Transient
    private Set<LabBatch> computedLcSets;

    /**
     * Can be set by a user to indicate the LCSET, in the absence of any distinguishing context, e.g. a set of samples
     * processed in multiple technologies.
     */
    @ManyToOne
    private LabBatch manualOverrideLcSet;

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
            targetLabVessels.add(vesselToVesselTransfer.getTargetLabVessel());
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
        reagents.add(reagent);
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

    public Long getEventOperator() {
        return eventOperator;
    }

    public String getProgramName() {
        return programName;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public Collection<Reagent> getReagents() {
        return reagents;
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

    public LabVessel getInPlaceLabVessel() {
        return inPlaceLabVessel;
    }

    public void setInPlaceLabVessel(LabVessel inPlaceLabVessel) {
        this.inPlaceLabVessel = inPlaceLabVessel;
    }

    /**
     * When vessels are placed in a bucket, an association is made
     * between the vessel and the PO that is driving the work.  When
     * vessels are pulled out of a bucket, we record an event.  That
     * event associates zero or one {@link String product orders}.
     * <p/>
     * This method is the way to mark the transfer graph such that all
     * downstream nodes are considered to be "for" the product order
     * returned here.
     * <p/>
     * Most events will return null.
     */
    public String getProductOrderId() {
        return productOrderId;
    }

    public void setProductOrderId(String productOrder) {
        productOrderId = productOrder;
    }

    public LabEventType getLabEventType() {
        return labEventType;
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

    /**
     * Gets computed LCSET(s) for this transfer, based on the source vessels.
     *
     * @return LCSETs, empty if the source vessels are not associated with an LCSET.
     */
    public Set<LabBatch> getComputedLcSets() {
        if (manualOverrideLcSet != null) {
            return Collections.singleton(manualOverrideLcSet);
        }
        return computeLcSets();
    }

    public void addComputedLcSets(Set<LabBatch> lcSets) {
        if (computedLcSets == null) {
            computedLcSets = new HashSet<>();
        }
        computedLcSets.addAll(lcSets);
    }

    Set<LabBatch> computeLcSets() {
        if (computedLcSets == null) {
            computedLcSets = new HashSet<>();
            for (SectionTransfer sectionTransfer : sectionTransfers) {
                computedLcSets.addAll(sectionTransfer.getSourceVesselContainer().getComputedLcSetsForSection(
                        sectionTransfer.getSourceSection()));
            }
            computedLcSets.addAll(computeLcSetsForCherryPickTransfers());
            if (LabVessel.DIAGNOSTICS) {
                System.out.println("computedLcSets for " + labEventType.getName() + " " + computedLcSets);
            }
        }
        return computedLcSets;
    }

    private Set<LabBatch> computeLcSetsForCherryPickTransfers() {
        Set<LabBatch> computedLcSets = new HashSet<>();
        Map<LabBatch, Integer> mapLabBatchToCount = new HashMap<>();
        int numVesselsWithBucketEntries = 0;
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfers) {
            LabVessel sourceVessel = cherryPickTransfer.getSourceVesselContainer()
                    .getVesselAtPosition(cherryPickTransfer.getSourcePosition());
            if (sourceVessel != null) {
                Set<BucketEntry> bucketEntries = sourceVessel.getBucketEntries();
                if (!bucketEntries.isEmpty()) {
                    numVesselsWithBucketEntries++;
                }
                for (BucketEntry bucketEntry : bucketEntries) {
                    if (bucketEntry.getLabBatch() != null) {
                        LabBatch labBatch = bucketEntry.getLabBatch();
                        if (labBatch.getLabBatchType() == LabBatch.LabBatchType.WORKFLOW) {
                            Integer count = mapLabBatchToCount.get(labBatch);
                            if (count == null) {
                                count = 1;
                            } else {
                                count = count + 1;
                            }
                            mapLabBatchToCount.put(labBatch, count);
                        }
                    }
                }
            }
        } // TODO jmt one pooling bucket entry in a rack of 96 can override the LCSET for the other tubes.
        for (Map.Entry<LabBatch, Integer> labBatchIntegerEntry : mapLabBatchToCount.entrySet()) {
            if (labBatchIntegerEntry.getValue() == numVesselsWithBucketEntries) {
                computedLcSets.add(labBatchIntegerEntry.getKey());
            }
        }
        return computedLcSets;
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

}
