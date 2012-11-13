package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.ProductOrderId;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
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
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * A lab event isn't just at the granularity
 * of what we now consider a station event.
 *
 * Any lab event has the potential to change the
 * molecular state (see MolecularEnvelope) of the
 * lab vessels it references.  Some lab events may
 * not change the molecular envelope, but for those
 * that do, we expect the LabEvent to know the "expected"
 * molecular envelope of the input materials.  The event
 * itself can also alter the molecular envelope.
 *
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
 *
 * In this model, LabEvents have to be quite aware of
 * the particulars of their inputs and outputs.  This
 * means it must be easy for lab staff to change expected
 * inputs and outputs based bot on the event definition
 * (think: Bravo protocol file) as well as the workflow.
 *
 * LabEvents can be re-used in different workflows, with
 * different expected ranges, and project manaagers might
 * want to override these ranges.
 */
// todo rename to "Event"--everything is an event, including
    // deltas in an aggregation in zamboni
@Entity
@Audited
@Table(schema = "mercury",
       uniqueConstraints = @UniqueConstraint(columnNames = {"eventLocation", "eventDate", "disambiguator"}),
       name = "lab_event")
public abstract class LabEvent {

    public static final String UI_EVENT_LOCATION = "User Interface";

    public static final Comparator<LabEvent> byEventDate = new Comparator<LabEvent>() {
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

    private String eventLocation;

    // todo jmt this should change to a Long
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private Person eventOperator;

    private Date eventDate;

    private Long disambiguator = 0L;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(schema = "mercury")
    private Set<Reagent> reagents = new HashSet<Reagent>();

    // todo jmt a single transfer superclass that permits all section, position, vessel combinations
    /** for transfers using a tip box, e.g. Bravo */
    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "labEvent")
    private Set<SectionTransfer> sectionTransfers = new HashSet<SectionTransfer>();

    /** for random access transfers, e.g. MultiProbe */
    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "labEvent")
    private Set<CherryPickTransfer> cherryPickTransfers = new HashSet<CherryPickTransfer>();

    /** for transfers from a single vessel to an entire section, e.g. from a tube to a plate */
    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "labEvent")
    private Set<VesselToSectionTransfer> vesselToSectionTransfers = new HashSet<VesselToSectionTransfer>();

    /** Typically for tube to tube transfers */
    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "labEvent")
    private Set<VesselToVesselTransfer> vesselToVesselTransfers = new HashSet<VesselToVesselTransfer>();

    /** For plate / tube events, that don't involve a transfer e.g. anonymous reagent addition, loading onto an
     * instrument, entry into a bucket */
    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private LabVessel inPlaceLabVessel;

    // todo jmt delete?
    private String quoteServerBatchId;

//    @ManyToOne(fetch = FetchType.LAZY)
//    private BasicProjectPlan projectPlanOverride;

    /**
     * Business Key of a product order to which this event is associated
     */
    private String productOrderId;


    @Enumerated(EnumType.STRING)
    private LabEventType labEventType;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private LabBatch labBatch;

    /** For JPA */
    LabEvent() {
    }

    public LabEvent(LabEventType labEventType, Date eventDate, String eventLocation, Long disambiguator, Person operator) {
        this.labEventType = labEventType;
        this.setEventDate(eventDate);
        this.setEventLocation(eventLocation);
        this.setDisambiguator(disambiguator);
        this.setEventOperator(operator);
    }

    public Set<LabVessel> getTargetLabVessels() {
        Set<LabVessel> targetLabVessels = new HashSet<LabVessel>();
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
     * For transfer events, this returns the sources
     * of the transfer
     * @return may return null
     */
    public Set<LabVessel> getSourceLabVessels() {
        Set<LabVessel> sourceLabVessels = new HashSet<LabVessel>();
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
        throw new RuntimeException("I haven't been written yet.");
    }

    /**
     * Returns all the lab vessels involved in this
     * operation, regardless of source/destination.
     *
     * Useful convenience method for alerts
     * @return
     */
    public Collection<LabVessel> getAllLabVessels() {
        Set<LabVessel> allLabVessels = new HashSet<LabVessel>();
        allLabVessels.addAll(getSourceLabVessels());
        allLabVessels.addAll(getTargetLabVessels());
        return allLabVessels;
    }

    /**
     * Machine name?  Name of the bench?
     * GPS coordinates?
     * @return
     */
    public String getEventLocation() {
        return this.eventLocation;
    }

    public Person getEventOperator() {
        return this.eventOperator;
    }

    public Date getEventDate() {
        return this.eventDate;
    }

    public Collection<Reagent> getReagents() {
        return this.reagents;
    }

    public Set<SectionTransfer> getSectionTransfers() {
        return this.sectionTransfers;
    }

    public void setQuoteServerBatchId(String batchId) {
        this.quoteServerBatchId = batchId;
    }

    public String getQuoteServerBatchId() {
        return quoteServerBatchId;
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

    /**
     * An "override" of the {@link org.broadinstitute.gpinformatics.mercury.entity.project.BasicProjectPlan} effectively says "From
     * this point forward in the transfer graph, consider all work
     * related to the given projectPlan.  In this way, we're "overriding"
     * the {@link org.broadinstitute.gpinformatics.mercury.entity.project.BasicProjectPlan} referenced by {@link org.broadinstitute.gpinformatics.mercury.entity.sample.StartingSample#getRootProjectPlan()}
     * @param projectPlan
     */
//    public void setProjectPlanOverride(BasicProjectPlan projectPlan) {
//        if (projectPlan == null) {
//            throw new RuntimeException("projectPlan override cannot be null.");
//        }
//        this.projectPlanOverride = projectPlan;
//    }

    /**
     * See setProjectPlanOverride(org.broadinstitute.gpinformatics.mercury.entity.project.BasicProjectPlan).
     * @return
     */
//    public BasicProjectPlan getProjectPlanOverride() {
//        return projectPlanOverride;
//    }

    public void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }

    public void setEventOperator(Person eventOperator) {
        this.eventOperator = eventOperator;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
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
     * This bit can be used to help identify the single sample ancestor across different
     * workflows over time.  Instead of having a complex single sample ancestor finder method
     * that must understand all possible variations of workflows, instead we mark on the
     * workflow BPMN diagram which transition results in the official single sample
     * library.  When we need to find the single sample library for some downstream pool,
     * which just search the history to find the event that has this bit turned on.
     * @param isSingleSampleLibrary
     */
    public void setIsSingleSampleLibrary(boolean isSingleSampleLibrary) {

    }

    /**
     * When vessels are placed in a bucket, an association is made
     * between the vessel and the PO that is driving the work.  When
     * vessels are pulled out of a bucket, we record an event.  That
     * event associates zero or one {@link ProductOrderId product orders}.
     *
     * This method is the way to mark the transfer graph such that all
     * downstream nodes are considered to be "for" the product order
     * returned here.
     *
     * Most events will return null.
     * @return
     */
    public String getProductOrderId () {
        return productOrderId;
    }

    public void setProductOrderId( String productOrder) {
        this.productOrderId = productOrder;
    }

    public LabEventType getLabEventType() {
        return this.labEventType;
    }

    public void setLabBatch(LabBatch labBatch) {
        this.labBatch = labBatch;
    }

    public LabBatch getLabBatch() {
        return labBatch;
    }

    /**
     * Are the sources in the expected molecular state?
     *
     * In the messaging world, we are told of events
     * after they happen.  So refusing to persist
     * an event is unacceptable, although we might
     * want to log/alert when this exception is thrown.
     *
     * On the other hand, if we want to up-front validation
     * prior to events, we can leave it to the client
     * to respond to this exception without getting
     * our transactions confused.
     *
     * Probably we'll want generic source/target
     * molecular state checks done up at the
     * abstract superclass level and then let
     * subclasses override them?
     *
     * @throws InvalidMolecularStateException
     */
    public void validateSourceMolecularState() throws InvalidMolecularStateException {
        if (getSourceLabVessels().isEmpty() && !this.labEventType.isExpectedEmptySources()) {
            throw new InvalidMolecularStateException("No sources.");
        }
/*
        for (LabVessel source: getSourceLabVessels()) { // todo jmt remove
            if (!this.labEventType.isExpectedEmptySources()) {
                if (source.getSampleInstances().isEmpty()) {
                    throw new InvalidMolecularStateException("Source " + source.getLabCentricName() + " is empty");
                }
            }
        }
*/
    }

    /**
     * Are the targets in the expected molecular state?
     *
     * In the messaging world, we are told of events
     * after they happen.  So refusing to persist
     * an event is unacceptable, although we might
     * want to log/alert when this exception is thrown.
     *
     * On the other hand, if we want to up-front validation
     * prior to events, we can leave it to the client
     * to respond to this exception without getting
     * our transactions confused.
     *
     * @throws InvalidMolecularStateException
     *
     * Probably we'll want generic source/target
     * molecular state checks done up at the
     * abstract superclass level and then let
     * subclasses override them?
     * @throws InvalidMolecularStateException
     */
    public void validateTargetMolecularState() throws InvalidMolecularStateException {
        if (getTargetLabVessels().isEmpty() && getInPlaceLabVessel() == null) {
            throw new InvalidMolecularStateException("No destinations!");
        }
/*
        for (LabVessel target: getTargetLabVessels()) {
            if (!this.labEventType.isExpectedEmptyTargets()) {
                if (target.getSampleInstances().isEmpty()) {
                    throw new InvalidMolecularStateException("Target " + target.getLabCentricName() + " is empty");
                }
            }
        }
*/
    }

    /**
     * This is the change to sample state that this
     * operation accomplishes.
     *
     * A lab event could be someone scanning
     * a rack of tubes and saying "I hereby
     * bless thee as having this arrangement
     * of molecular indexes."  In this example,
     * there's no transfer for us to track,
     * and we don't bother to ask for the details
     * of how we managed to get the
     * indexes applied.  But we would configure
     * the magical "apply molecular indexes"
     * event type in the database such that
     * any applcation of the event would
     * have this effect.
     *
     * For example:
     * adding molecular indexes
     * changing volume
     * changing concentration
     * changing from RNA to DNA
     * changing target sample size by fragmentation
     * @return
     * @throws InvalidMolecularStateException when this LabEvent is being
     * applied in such a way that the molecular state change it causes is not
     * what is expected.
     *
     * After writing this method, I know think we only need
     * a single {@link LabEvent} class to handle most Logic for properly handling
     * {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularState} changes can be written
     * once.  The need to customize behavior of
     * {@link #validateSourceMolecularState()}, {@link #validateTargetMolecularState()},
     * and {@link #applyMolecularStateChanges()}  is
     * pretty unlikely.
     * @throws InvalidMolecularStateException
     */
    public void applyMolecularStateChanges() throws InvalidMolecularStateException {
        // apply reagents in message
/*
        for (LabVessel target: getTargetLabVessels()) {
            for (LabVessel source: getSourcesForTarget(target)) {
                // apply all goop from all sources
                for (SampleSheet sampleSheet : source.getSampleSheets()) {
                    target.addSampleSheet(sampleSheet);
                }
            }
            // after the target goop is transferred,
            // apply the reagent
            for (Reagent reagent : getReagents()) {
                target.applyReagent(reagent);
            }
        }
*/

        /**
         * Here is why we probably only need a single {@link #applyMolecularStateChanges()}
         * method.
         */
/*
        for (LabVessel target: getTargetLabVessels()) {
            // todo jmt restore this
            // check the molecular state per target.
            Set<MolecularStateTemplate> molecularStateTemplatesInTarget = new HashSet<MolecularStateTemplate>();
            for (SampleInstance sampleInstance : target.getSampleInstances()) {
                molecularStateTemplatesInTarget.add(sampleInstance.getMolecularState().getMolecularStateTemplate());
            }
            // allowing for jumbled {@link MolecularState} is probably
            // one of those things we'd override per {@link LabEvent}
            // subclass.  In the worst case, an implementation of
            // {@link LabEvent} might have to dip into {@link Project}
            // data to make some sort of special case
            if (molecularStateTemplatesInTarget.size() > 1) {
                StringBuilder errorMessage = new StringBuilder("Molecular state will not be uniform as a result of this operation.  " + target.getLabCentricName() + " has " + molecularStateTemplatesInTarget.size() + " different molecular states:\n");
                for (MolecularStateTemplate stateTemplate : molecularStateTemplatesInTarget) {
                    errorMessage.append(stateTemplate.toText());
                }
                // todo post this error message back to PM jira
                throw new InvalidMolecularStateException(errorMessage.toString());
            }
            // if no molecular envelope change, set backlink

            // create pool, or set backlink
            // how to determine that it's a pooling operation? destination section has samples (is not empty), source section has samples

            // set molecular state from map (or set backlink?)

            // setting backlinks must be section based, unless the section is ALL* (without flips)
        }
*/

        for (SectionTransfer sectionTransfer : getSectionTransfers()) {
            sectionTransfer.applyTransfer();
        }
    }

    /**
     * Are we going to change the molecular
     * state?
     *
     * Perhaps this should be up at {@link LabEvent}
     *
     * Events that denature or that transform from
     * RNA into DNA also change molecular state.  So perhaps
     * these
     * @return
     */
/*
    @Transient
    private boolean isMolecularStateBeingChanged() {
        boolean hasMolStateChange = false;
        for (Reagent reagent: getReagents()) {
            if (reagent.getMolecularEnvelopeDelta() != null) {
                hasMolStateChange = true;
            }
        }
        return hasMolStateChange;
    }
*/

}
