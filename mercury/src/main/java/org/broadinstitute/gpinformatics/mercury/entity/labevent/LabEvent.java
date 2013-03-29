package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.hibernate.envers.Audited;
import javax.persistence.CascadeType;
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
 * different expected ranges, and project managers might
 * want to override these ranges.
 */
// todo rename to "Event"--everything is an event, including
    // deltas in an aggregation in zamboni
@Entity
@Audited
@Table(schema = "mercury",
       uniqueConstraints = @UniqueConstraint(columnNames = {"eventLocation", "eventDate", "disambiguator"}),
       name = "lab_event")
public class LabEvent {

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

    private Long eventOperator;

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
    protected LabEvent() {
    }

    public LabEvent(LabEventType labEventType, Date eventDate, String eventLocation, Long disambiguator, Long operator) {
        this.labEventType = labEventType;
        this.setEventDate(eventDate);
        this.setEventLocation(eventLocation);
        this.setDisambiguator(disambiguator);
        this.setEventOperator(operator);
    }

    /**
     * getTargetVessels will give to the caller the set of vessels that are on the receiving end of a recorded event
     *
     * @return set of LabVessels
     */
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
     * getTargetVesselTubes
     *
     * @return
     */
    public Set<LabVessel> getTargetVesselTubes() {
        Set<LabVessel> eventVessels = new HashSet<LabVessel>();
        for(LabVessel targetVessel: this.getTargetLabVessels()) {
            if(targetVessel.getContainerRole() != null &&
               targetVessel instanceof TubeFormation) {
                eventVessels.addAll(targetVessel.getContainerRole().getContainedVessels());
            } else {
                eventVessels.add(targetVessel);
            }
        }
        return eventVessels;
    }

    public Set<LabVessel> getSourceVesselTubes() {
        Set<LabVessel> eventVessels = new HashSet<LabVessel>();
        for(LabVessel sourceVessel: this.getSourceLabVessels()) {
            if(sourceVessel.getContainerRole() != null &&
               sourceVessel instanceof TubeFormation) {
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
        this.reagents.add(reagent);
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
        if (inPlaceLabVessel != null) {
            allLabVessels.add(inPlaceLabVessel);
        }
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

    public Long getEventOperator () {
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

    public Long getLabEventId() {
        return labEventId;
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

    public void setEventOperator( Long eventOperator) {
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
     * event associates zero or one {@link String product orders}.
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

}
