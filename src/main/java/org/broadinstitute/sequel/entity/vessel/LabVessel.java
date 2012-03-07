package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.StateChange;
import org.broadinstitute.sequel.entity.labevent.Failure;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;

import java.util.Collection;
import java.util.Set;

/**
 *
 */
public interface LabVessel  {

    void applyTransfer(SectionTransfer sectionTransfer);

    public enum CONTAINER_TYPE {
        STATIC_PLATE,
        TUBE,
        RACK_OF_TUBES
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
    public String getLabCentricName();

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
    public Collection<SampleSheet> getSampleSheets();

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
    public void addSampleSheet(SampleSheet sampleSheet);

    public void addStateChange(StateChange stateChange);

    /**
     * Probably a transient that computes the {@link SampleInstance} data
     * on-the-fly by walking the history and applying the
     * {@link StateChange}s applied during lab work.
     * @return
     */
    public Set<SampleInstance> getSampleInstances();

    public Collection<SampleInstance> getSampleInstances(SampleSheet sheet);

    public Collection<StateChange> getStateChanges();

    /**
     * Well A01, Lane 3, Region 6 all might
     * be considered a labeled sub-section
     * of a lab vessel.  Labels are GUIDs
     * for LabVessels; no two LabVessels
     * may share this id.  It's primarily the
     * barcode on the piece of plastic.f
     * @return
     */
    public String getLabel();

    /**
     * I'm avoiding parent/child semantics
     * here deliberately to avoid confusion
     * with transfer graphs and the notion
     * of ancestor/descendant.
     * 
     * If this {@link LabVessel} is a {@link org.broadinstitute.sequel.entity.vessel.WellName},
     * then the containing vessel could be a {@link org.broadinstitute.sequel.entity.vessel.StaticPlate}.
     * If this {@link LabVessel} is a {@link org.broadinstitute.sequel.entity.run.RunChamber flowcell lane},
     * then perhaps the containing {@link LabVessel} is a {@link org.broadinstitute.sequel.entity.run.RunCartridge flowcell}.
     * @return
     */
    public LabVessel getContainingVessel();

    /**
     * If this is a plate, this method could return
     * the {@link org.broadinstitute.sequel.entity.vessel.PlateWell wells}.  If this thing
     * is a {@link org.broadinstitute.sequel.entity.vessel.RackOfTubes}, this method could
     * return the {@link org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube} tubes in
     * the rack.
     * @return
     */
    public Collection<? extends LabVessel> getContainedVessels();

    public void addContainedVessel(LabVessel child);

    public void addMetric(LabMetric m);

    public Collection<LabMetric> getMetrics();

    /**
     * A failure of any sort: quant, sequencing,
     * smells bad, not the right size around the
     * hips, etc.
     * @param failureMode
     */
    public void addFailure(Failure failureMode);

    public Collection<Failure> getFailures();

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
    public Collection<Reagent> getReagentContents();

    public void addReagent(Reagent r);

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
    public LabMetric getMetric(LabMetric.MetricName metricName,
                               MetricSearchMode searchMode,
                               SampleInstance sampleInstance);

    /**
     * This needs to be really, really fast.
     * @return
     */
    public Collection<LabEvent> getTransfersFrom();

    /**
     * This needs to be really, really fast.
     * @return
     */
    public Collection<LabEvent> getTransfersTo();

    /**
     * Get all events that have happened directly to
     * this vesell.  Really, really fast.  Like
     * right now.
     * @return
     */
    public Collection<LabEvent> getEvents();

    public boolean isAncestor(LabVessel progeny);

    public boolean isProgeny(LabVessel ancestor);

    /**
     * Returns all projects.  Convenience method vs.
     * iterating over {@link #getSampleSheets()} and
     * calling {@link org.broadinstitute.sequel.entity.sample.SampleInstance#getProject()}
     * @return
     */
    public Collection<Project> getAllProjects();


    /**
     * PM Dashboard will want to show the most recent
     * event performed on this aliquot.  Implementations
     * traipse through lims history to find the most
     * recent event.
     *
     * For informational use only.  Can be volatile.
     * @return
     */
    public StatusNote getLatestNote();

    /**
     * Reporting will want to look at aliquot-level
     * notes, not traipse through our {@link org.broadinstitute.sequel.entity.labevent.LabEvent}
     * history.  So every time we do a {@link org.broadinstitute.sequel.entity.labevent.LabEvent}
     * or have key things happen like {@link org.broadinstitute.sequel.control.bsp.AliquotReceiver receiving an aliquot},
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
    public void logNote(StatusNote statusNote);

    public Collection<StatusNote> getAllStatusNotes();

    public Float getVolume();

    public Float getConcentration();

    public void applyReagent(Reagent r);

    public Collection<Reagent> getAppliedReagents();
    
    public void addNoteToProjects(String message);

}
