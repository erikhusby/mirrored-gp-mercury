package org.broadinstitute.sequel;

import java.util.Collection;

/**
 * An aliquot of a sample in a particular
 * molecular state.
 *
 * "Molecular state" describes the molecular
 * changes the target has undergone.  Knowing
 * the molecular state is key to identifying
 * what lab processes the sample instance
 * can undergo.
 *
 * This might all seem a bit too abstract.  Consider
 * what our users are trying to do when the search
 * for lims materials: they're trying to find
 * some piece of plastic that contains a sample
 * in a state that is amenable to a particular
 * "next" lab step.
 *
 * For example, currently you ask for
 * denatured illumina libraries so you
 * can do some topoff sequencing.  But
 * "denatured" doesn't tell the whole story
 * of the state of the DNA.  So you say
 * "Oh, I want the denatured libraries
 * for hybrid selection."  This turns into
 * a bit of a workflow query in the current
 * squid, but if we want users to be able
 * to inject samples in any state into
 * our lab, we don't want to rely on
 * our workflow to tell us the state of
 * the library.  We want to know the molecular
 * state.
 *
 * So instead of saying "give me the denatured
 * libraries which are associated with a
 * hybrid selection work request", we say
 * "give me the libraries that are denatured
 * and are in a molecular status which is
 * amenable to pooling for the catch operation."
 *
 * This might seem like an overly subtle difference,
 * but when a collaborator says to us "Hey, I
 * did my own hybrid selection and pooling, can
 * you just sequence my library?", the current answer
 * is "not unless we backfill a bunch of fake
 * workflow information first.", which then
 * breaks reporting because what we're doing is
 * saying we actually did all the prep work,
 * when in fact we didn't.
 *
 * A good challenge for whether this model
 * works in reality is to take all possible
 * molecular state configurations and map
 * them to solexa_library_type, four54_library_type,
 * etc.  If you can take the molecular state
 * and produce a simple string that summarizes
 * that state, we're all good.  One caveat is that the current library "types"
 * conflate they "why" (for example, 454 library
 * type "RNA rework aliquot") with the molecular
 * status.
 *
 * There's a fuzzy line between molecular
 * state "facts" and measured attributes,
 * like concentration and volume.  Metrics
 * are captured on containers for many things
 * at 320.  See LabVessel.getMetric() for
 * examples.  At the same time, however, external
 * collaborators may ship us samples and tell
 * us various metrics, so we have to be able
 * to resolve metrics either at the
 * aliquot instance level as part of the
 * sample sheet, or by traversing lims event
 * histories.
 * 
 * The unique key of a SampleAliquotInstance
 * is the {@Link SampleAliquot} and the
 * {@link MolecularState}.  You can have the same
 * {@link Goop} in a {@link SampleSheet} or
 * a {@link LabVessel}, in which case they'll
 * have to have different {@link MolecularState}.
 * */
public interface SampleInstance  {

    public enum GSP_CONTROL_ROLE {
        NEGATIVE,POSITIVE,NONE
    }

    /**
     * Positive control or negative control?
     * not a sample attribute, but an attribute
     * of the sample in a group of samples
     * in a container.
     * @return
     */
    public GSP_CONTROL_ROLE getControlRole();

    /**
     * Ultimately from whence this instance
     * was generated.  BSP aliquot?  BSP root?
     * Could also imagine "breaking" xfer history
     * in some way to separate things more thoroughly
     * (think of "just kiosk that" in the current
     * model, except that it would actually work)
     * @return
     */
    public StartingSample getStartingSample();

    /**
     *
     * Major assumption: an aliquot for a production
     * process is only ever used for a single project.
     *
     * The aliquot could be used across sequencing
     * technologies, but the project is always the
     * same.
     *
     * Can be empty for control samples.
     *
     * This one-to-one doesn't work for tech dev,
     * however, so we have to figure that one out.
     *
     * Some parts of the app may throw exceptions
     * if they see multiple active projects.  Maybe
     * when there's >1 active project, our code
     * writes a nice note and adds it to the jira
     * ticket for the project so that people
     * can get an early heads up that it looks
     * like we're in trouble.  We have to allow for
     * this semi-dirty state for dev work primarily.
     *
     * In the current squid, we don't let this ambiguity
     * live past the designation.  Instead, we guess.
     * That makes for some awful problems.  In the new model,
     * we should let this ambiguity persist as long as
     * necessary, and gently nudge watchers of various
     * tickets throughout the process.  By the time
     * the analysis pipeline wakes up, if there are
     * multiple projects for an aliquot, we'll barf
     * loudly.  The billing app allows for this
     * ambiguoity right up until a bill is issued.
     *
     * In the current squid, most of the "wrong work request"
     * problems come up as a result of two things:
     * 1. Concurrent dev and production work on the
     * same aliquot.  This can be resolved with the
     * "dev aliquots" feature, which basically
     * pins a "I'm for dev!" bit on a library.  Implementations
     * of SampleAliquot can examine the vessel they're in
     * to see if the "I'm dev" bit is present.  If it is,
     * then only the "dev" project will be selected.
     *
     * 2. Concurrent work requests for both test
     * sequencing and production sequencing, aggregated
     * to different projects.  This requires a different
     * approach.  Instead, why not have two different
     * {@link FullAccessLabWorkQueue}s, one for test seq
     * and one for production sequencing, and have
     * the system know when a run is a test run.  For a test
     * run, we can aggregate things differently by
     * code, not by project.  Test run data could
     * be aggregated into the same project but marked
     * as test run data so that it could be excluded
     * from bass/submissions.
     *
     * @return
     */
    public Project getProject();
    
    public void setProject(Project p);

    /**
     * What is the molecular state  of this
     * sample in this container?
     * @return
     */
    public MolecularState getMolecularState();

    /**
     * This seems at odds with {@link Project#getWorkflowDescription(SampleInstance)}.
     * We already declared the expected {@link WorkflowDescription} up at
     * the project, right?
     *
     * We have a history of pioneering some kind of sample prep
     * in-house, and then sharing the protocols with the outside
     * world, and then accepting samples prepped with our
     * protocols but outisde the Broad.  In these situations,
     * the sample sheet sent to us tells us what process
     * the sample has been through, which is different
     * from the project being configured with isntructions
     * about what process the sample is supposed to go
     * from when it starts life as an aliquot.
     *
     * Perhaps we should remove the bit on the project
     * and declare that all workflow lookups go through
     * the aliquotInstance.  Either the instance looks
     * up active projects and finds the intended workflow
     * there, or it stores it internal to itself.
     * @return
     */
    public WorkflowDescription getWorkflowDescription();

    /**
     * Is this sample, in this container, earmarked
     * for use in development?
     */
    public boolean isDevelopment();

    public Collection<ReadBucket> getReadBuckets();

}
