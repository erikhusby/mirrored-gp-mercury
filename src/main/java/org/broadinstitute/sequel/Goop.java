package org.broadinstitute.sequel;

import java.util.Collection;

/**
 * Some liquid-y DNA-ish stuff that we wish
 * to sequence.  Goop that is not a sequencing
 * target (but which itself might be DNA) is
 * considered a {@link Reagent}.  <b>If the
 * goop you have is not likely to ever be
 * sequenced, consider it a {@link Reagent}.</b>
 * 
 * You can think of Goop as the liquid-y (possibly 
 * frozen) DNA stuff in a container.  Goop can
 * be a library, a sample, or an aliquot.  Or all
 * three.  There's a problem with semantics for library,
 * sample, and aliquot, so while you might choose to use
 * them as variable names, the class behavior libraries,
 * samples, and aliquots is pretty much the same.
 * 
 * A single Goop might have multiple {@link SampleSheet}s.
 * If Goop is a library, then the {@link SampleInstance}s
 * in the {@link SampleSheet} represent component samples,
 * which themselves are just different {@link Goop}.  It's
 * "Goop all the way down" to support pools of pools.  Fast
 * access to some notion of a root--aka a clinical sample--
 * could be accomplished with {@link #getRootSamples()}.
 */
public interface Goop extends LabTangible, ProjectBranchable, ReadBucketable {

    /**
     * Returns all projects.  Convenience method vs.
     * iterating over {@link #getSampleSheets()} and
     * calling {@link SampleInstance#getProject()}
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
     * notes, not traipse through our {@link LabEvent}
     * history.  So every time we do a {@link LabEvent}
     * or have key things happen like {@link AliquotReceiver receiving an aliquot},
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
    
    public void applyGoop(Goop goop);

    /**
     * Useful method for applying out-of-order updates.
     * @param rootSample
     * @return
     */
    public Collection<SampleInstance> getSampleInstancesFor(StartingSample rootSample);
    
    public void replaceSampleSheet(SampleSheet oldSheet,SampleSheet newSheet);

}
