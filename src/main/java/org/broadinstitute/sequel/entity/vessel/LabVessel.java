package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.entity.OrmUtil;
import org.broadinstitute.sequel.entity.analysis.ReadBucket;
import org.broadinstitute.sequel.entity.labevent.Failure;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.notice.Stalker;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.notice.UserRemarks;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.sample.StateChange;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Formula;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
@Entity
@BatchSize(size = 50)
public abstract class LabVessel  {

    @SequenceGenerator(name = "SEQ_LAB_VESSEL", sequenceName = "SEQ_LAB_VESSEL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_VESSEL")
    @Id
    private Long labVesselId;

    protected String label;

    private Date createdOn;

    @OneToMany(cascade = CascadeType.PERSIST)
    private final Set<JiraTicket> ticketsCreated = new HashSet<JiraTicket>();

    @ManyToMany(cascade = CascadeType.PERSIST)
    private final Set<SampleSheet> sampleSheets = new HashSet<SampleSheet>();

    /** Counts the number of rows in the many-to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip  */
    @Formula("(select count(*) from lab_vessel_sample_sheets where lab_vessel_sample_sheets.lab_vessel = lab_vessel_id)")
    private Integer sampleSheetCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    private MolecularState molecularState;

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    private LabVessel projectAuthority;

    // todo jmt fix this
    @Transient
    private ReadBucket readBucket;

    @ManyToOne(fetch = FetchType.LAZY)
    private LabVessel readBucketAuthority;

    @ManyToMany(cascade = CascadeType.PERSIST)
    private Set<LabEvent> transfersFrom = new HashSet<LabEvent>();

    @ManyToMany(cascade = CascadeType.PERSIST)
    private Set<LabEvent> transfersTo = new HashSet<LabEvent>();

    // todo jmt fix this
    @Transient
    private final Collection<Stalker> stalkers = new HashSet<Stalker>();

    @ManyToMany(cascade = CascadeType.PERSIST)
    private Set<Reagent> reagentContents = new HashSet<Reagent>();

    /** Counts the number of rows in the many-to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip  */
    @Formula("(select count(*) from lab_vessel_reagent_contents where lab_vessel_reagent_contents.lab_vessel = lab_vessel_id)")
    private Integer reagentContentsCount = 0;

    @ManyToMany(cascade = CascadeType.PERSIST)
    private Set<LabVessel> containers = new HashSet<LabVessel>();

    /** Counts the number of rows in the many-to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip  */
    @Formula("(select count(*) from lab_vessel_containers where lab_vessel_containers.lab_vessel = lab_vessel_id)")
    private Integer containersCount = 0;

    @Embedded
    private UserRemarks userRemarks;

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
     * @return
     */
    public String getLabCentricName() {
        // todo jmt what should this do?
        return label;
    }

    /**
     * May return null if no sample sheet
     * has been registered.  Consider
     * a "new" destination in a transfer event.
     * When first encountered, the plate
     * may have no sample sheet, and it's up
     * to the message processor to fill
     * in the sample sheet.
     * @return
     */
    public Collection<SampleSheet> getSampleSheets() {
        return sampleSheets;
    }

    /**
     * We want GSP to let you walk in with a tube, declare
     * the contents of the tube, and then inject the tube
     * into the middle of the lab process, into
     * whatever LabWorkQueue you need.
     *
     * This method is the key to this feature.  We do not
     * care <b>how</b> the sample metadata was built
     * for a container, but we care deeply that
     * we have consistent metadata.  Whether it was
     * built half at 320, half at the collaborator,
     * all at 320, all in Mozambique, it does not
     * matter.  Just declare the sample metadata
     * in one place.
     *
     * @param sampleSheet
     */
    public void addSampleSheet(SampleSheet sampleSheet) {
        this.sampleSheets.add(sampleSheet);
        sampleSheetCount++;
    }

    public Integer getSampleSheetCount() {
        return sampleSheetCount;
    }

    /**
     * This needs to be really, really fast.
     * @return
     */
    public Set<LabEvent> getTransfersFrom() {
        return transfersFrom;
    }

    /**
     * This needs to be really, really fast.
     * @return
     */
    public Set<LabEvent> getTransfersTo() {
        return transfersTo;
    }

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

    /**
     * When a {@link org.broadinstitute.sequel.entity.project.JiraTicket} is created for a
     * {@link org.broadinstitute.sequel.entity.vessel.LabVessel}, let's
     * remember that fact.  It'll be useful when someone wants
     * to know all the lab work that was done for
     * a {@link org.broadinstitute.sequel.entity.sample.StartingSample}.
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

    public enum CONTAINER_TYPE {
        STATIC_PLATE,
        TUBE,
        RACK_OF_TUBES
    }

    /**
     * Probably a transient that computes the {@link SampleInstance} data
     * on-the-fly by walking the history and applying the
     * {@link StateChange}s applied during lab work.
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
     * then the containing vessel could be a {@link org.broadinstitute.sequel.entity.vessel.StaticPlate}.
     * If this {@link LabVessel} is a {@link org.broadinstitute.sequel.entity.run.RunChamber flowcell lane},
     * then perhaps the containing {@link LabVessel} is a {@link org.broadinstitute.sequel.entity.run.RunCartridge flowcell}.
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
     * iterating over {@link #getSampleSheets()} and
     * calling {@link org.broadinstitute.sequel.entity.sample.SampleInstance#getProject()}
     * @return
     */
    public abstract Collection<Project> getAllProjects();


    /**
     * PM Dashboard will want to show the most recent
     * event performed on this aliquot.  Implementations
     * traipse through lims history to find the most
     * recent event.
     *
     * For informational use only.  Can be volatile.
     * @return
     */
    public abstract StatusNote getLatestNote();

    /**
     * Reporting will want to look at aliquot-level
     * notes, not traipse through our {@link org.broadinstitute.sequel.entity.labevent.LabEvent}
     * history.  So every time we do a {@link org.broadinstitute.sequel.entity.labevent.LabEvent}
     * or have key things happen like {@link org.broadinstitute.sequel.infrastructure.bsp.AliquotReceiver receiving an aliquot},
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
    public abstract void logNote(StatusNote statusNote);

    public abstract Collection<StatusNote> getAllStatusNotes();

    public abstract Float getVolume();

    public abstract Float getConcentration();
}
