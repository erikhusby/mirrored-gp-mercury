package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabBatchComposition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularState;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.*;

/**
 * An aliquot of a sample in a particular molecular state.
 * <p/>
 * "Molecular state" describes the molecular changes the target has undergone.  Knowing the molecular state is key to
 * identifying what lab processes the sample instance can undergo.
 * <p/>
 * This might all seem a bit too abstract.  Consider what our users are trying to do when the search for lims materials:
 * they're trying to find some piece of plastic that contains a sample in a state that is amenable to a particular
 * "next" lab step.
 * <p/>
 * For example, currently you ask for denatured illumina libraries so you can do some topoff sequencing.  But
 * "denatured" doesn't tell the whole story of the state of the DNA.  So you say "Oh, I want the denatured libraries for
 * hybrid selection."  This turns into a bit of a workflow query in the current squid, but if we want users to be able
 * to inject samples in any state into our lab, we don't want to rely on our workflow to tell us the state of the
 * library.  We want to know the molecular state.
 * <p/>
 * So instead of saying "give me the denatured libraries which are associated with a hybrid selection work request", we
 * say "give me the libraries that are denatured and are in a molecular status which is amenable to pooling for the
 * catch operation."
 * <p/>
 * This might seem like an overly subtle difference, but when a collaborator says to us "Hey, I did my own hybrid
 * selection and pooling, can you just sequence my library?", the current answer is "not unless we backfill a bunch of
 * fake workflow information first.", which then breaks reporting because what we're doing is saying we actually did all
 * the prep work, when in fact we didn't.
 * <p/>
 * A good challenge for whether this model works in reality is to take all possible molecular state configurations and
 * map them to solexa_library_type, four54_library_type, etc.  If you can take the molecular state and produce a simple
 * string that summarizes that state, we're all good.  One caveat is that the current library "types" conflate they
 * "why" (for example, 454 library type "RNA rework aliquot") with the molecular status.
 * <p/>
 * There's a fuzzy line between molecular state "facts" and measured attributes, like concentration and volume.  Metrics
 * are captured on containers for many things at 320.  See LabVessel.getMetric() for examples.  At the same time,
 * however, external collaborators may ship us samples and tell us various metrics, so we have to be able to resolve
 * metrics either at the aliquot instance level as part of the sample sheet, or by traversing lims event histories.
 * <p/>
 * The unique key of a SampleInstance is the {@link MercurySample} and the {@link MolecularState}.  You can have the
 * same Goop in a SampleSheet or a {@link LabVessel}, in which case they'll have to have different {@link
 * MolecularState}.
 */
public class SampleInstance {

    public enum GspControlRole {
        NEGATIVE, POSITIVE, NONE
    }

    private static final Log log = LogFactory.getLog(SampleInstance.class);

    private final MercurySample sample;

    private final GspControlRole controlRole;

    private MolecularState molecularState;

    private final List<Reagent> reagents = new ArrayList<Reagent>();

    // todo use this when the definitive batch is known
    private LabBatch labBatch;

    // All lab batches found in ancestry.
    private Collection<LabBatch> allLabBatches;

    private String productOrderKey;

    public SampleInstance(MercurySample sample,
                          GspControlRole controlRole,
                          MolecularState molecularState) {
        this.sample = sample;
        this.controlRole = controlRole;
        this.molecularState = molecularState;
    }


    /**
     * Positive control or negative control?
     * not a sample attribute, but an attribute
     * of the sample in a group of samples
     * in a container.
     *
     * @return
     */
    public GspControlRole getControlRole() {
        return controlRole;
    }

    /**
     * Ultimately from whence this instance
     * was generated.  BSP aliquot?  BSP root?
     * Could also imagine "breaking" xfer history
     * in some way to separate things more thoroughly
     * (think of "just kiosk that" in the current
     * model, except that it would actually work)
     *
     * @return
     */
    public MercurySample getStartingSample() {
        return sample;
    }

    /**
     *
     * Major assumption: an aliquot for a production
     * process is only ever used for a single {@link Project}.
     * It can be used for multiple {@link org.broadinstitute.gpinformatics.mercury.entity.project.BasicProjectPlan}s
     * within the same {@link Project}, however, and so
     * the challenge here is to map a {@link SampleInstance}
     * to a single {@link org.broadinstitute.gpinformatics.mercury.entity.project.BasicProjectPlan}
     * whenever possible, and tolerate ambiguity when necessary,
     * for example for "universal LC" with Fluidigm.
     *
     * Can be empty for control samples.
     *
     * It's critical that reworks (topoffs in particular)
     * are <b>not implemented by a new {@link org.broadinstitute.gpinformatics.mercury.entity.project.BasicProjectPlan}!</b>
     * Instead, topoffs are just another entry into the appropriate
     * {@link org.broadinstitute.gpinformatics.mercury.entity.queue.LabWorkQueue}.
     * If you want to know the status of your topoff work,
     * look in the {@link org.broadinstitute.gpinformatics.mercury.entity.queue.LabWorkQueue},
     * or search jira to see what jira tickets exist starting
     * from the {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel}
     * that you put into the queue.
     *
     * If there's more than one possible project for this
     * {@link SampleInstance}, an exception is thrown.
     * If your client can tolerate this ambiguity, use
     * {@link #getAllProjectPlans()}.
     * @return
     */
//    public ProjectPlan getSingleProjectPlan() {
//        if (projectPlans.isEmpty()) {
//            return null;
//        }
//        else if (projectPlans.size() <= 1) {
//            return projectPlans.iterator().next();
//        }
//        else {
//            throw new RuntimeException("There are " + projectPlans.size() + " possible project plans for " + this);
//        }
//    }

    /**
     * @see #getSingleProjectPlan()
     * @return
     */
//    public Collection<ProjectPlan> getAllProjectPlans() {
//        return projectPlans;
//    }

    /**
     * What is the molecular state  of this
     * sample in this container?
     *
     * @return
     */
    public MolecularState getMolecularState() {
        return molecularState;
    }

    public void setMolecularState(MolecularState molecularState) {
        this.molecularState = molecularState;
    }

    /**
     * This seems at odds with Project#getWorkflowDescription(SampleInstance)}.
     * We already declared the expected {org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription} up at
     * the project, right?
     * <p/>
     * We have a history of pioneering some kind of sample prep
     * in-house, and then sharing the protocols with the outside
     * world, and then accepting samples prepped with our
     * protocols but outisde the Broad.  In these situations,
     * the sample sheet sent to us tells us what process
     * the sample has been through, which is different
     * from the project being configured with isntructions
     * about what process the sample is supposed to go
     * from when it starts life as an aliquot.
     * <p/>
     * Perhaps we should remove the bit on the project
     * and declare that all workflow lookups go through
     * the aliquotInstance.  Either the instance looks
     * up active projects and finds the intended workflow
     * there, or it stores it internal to itself.
     *
     * @return
     */
    //public WorkflowDescription getWorkflowDescription();
    public void addReagent(Reagent reagent) {
        reagents.add(reagent);
    }

    public List<Reagent> getReagents() {
        return reagents;
    }

    /**
     * This getter filters the reagents to return only the indexes.
     *
     * @return A list of indexes associated with this sample instance.
     */
    public List<MolecularIndexReagent> getIndexes() {
        List<MolecularIndexReagent> indexes = new ArrayList<MolecularIndexReagent>();
        for (Reagent reagent : reagents) {
            if (OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class)) {
                indexes.add((MolecularIndexReagent) reagent);
            }
        }
        return indexes;
    }

    /**
     * This is set only when there is a single lab batch.
     * @return lab batch
     */
    public LabBatch getLabBatch() {
        return labBatch;
    }

    public Collection<LabBatch> getAllLabBatches() {
        return allLabBatches;
    }

    public void addLabBatches(Collection<LabBatch> batches){
        if(allLabBatches == null){
            allLabBatches = new HashSet<LabBatch>();
        }
        allLabBatches.addAll(batches);
        // todo jmt improve this logic
        if (allLabBatches.size() == 1) {
            labBatch = allLabBatches.iterator().next();
        }
    }

    public Collection<LabBatch> getAllWorkflowLabBatches() {
        Set<LabBatch> workflowBatches = new HashSet<LabBatch>();
        for (LabBatch batch : allLabBatches) {
            if (batch.getLabBatchType() == LabBatch.LabBatchType.WORKFLOW) {
                workflowBatches.add(batch);
            }
        }
        return workflowBatches;
    }

    /**
     * This method gets the batch compositions for this sample instance in the context of a lab vessel.
     *
     * @param vessel The lab vessel used as context to determine the batch compositions.
     *
     * @return An ordered list of batch compositions for this sample given the vessel as context. The most likely
     *         batch will be first in the list.
     */
    public List<LabBatchComposition> getLabBatchCompositionInVesselContext(LabVessel vessel) {
        List<LabBatchComposition> allLabBatchCompositions = vessel.getWorkflowLabBatchCompositions();
        List<LabBatchComposition> filteredBatchCompositions = new ArrayList<LabBatchComposition>();
        for (LabBatch labBatch : getAllWorkflowLabBatches()) {
            for (LabBatchComposition composition : allLabBatchCompositions) {
                if (composition.getLabBatch().equals(labBatch)) {
                    filteredBatchCompositions.add(composition);
                }
            }
        }
        Collections.sort(filteredBatchCompositions, LabBatchComposition.HIGHEST_COUNT_FIRST);

        return filteredBatchCompositions;
    }

    public String getProductOrderKey() {
        return productOrderKey;
    }

    public void setProductOrderKey(String productOrderKey) {
        this.productOrderKey = productOrderKey;
    }
}
