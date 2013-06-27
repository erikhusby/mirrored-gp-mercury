package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabBatchComposition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularState;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private static final Log log = LogFactory.getLog(SampleInstance.class);

    private final MercurySample sample;

    private MolecularState molecularState;

    private final List<Reagent> reagents = new ArrayList<>();

    // todo use this when the definitive batch is known
    private LabBatch labBatch;

    // All lab batches found in ancestry.
    private Collection<LabBatch> allLabBatches;

    private String productOrderKey;

    public SampleInstance(MercurySample sample,
                          MolecularState molecularState) {
        this.sample = sample;
        this.molecularState = molecularState;
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

    public void addReagent(Reagent newReagent) {
        // If we're adding a molecular index
        if (OrmUtil.proxySafeIsInstance(newReagent, MolecularIndexReagent.class)) {
            MolecularIndexReagent newMolecularIndexReagent =
                    OrmUtil.proxySafeCast(newReagent, MolecularIndexReagent.class);
            boolean foundExistingIndex = false;
            boolean foundMergedScheme = false;
            // The new index has to be merged with the index in the field, if any
            // E.g. If the field index is Illumina_P7-M, and the new index is Illumina_P5-M, we need a merged index
            // called Illumina_P5-M_P7-M.
            for (int i = 0; i < reagents.size(); i++) {
                Reagent fieldReagent = reagents.get(i);
                if (OrmUtil.proxySafeIsInstance(fieldReagent, MolecularIndexReagent.class)) {
                    foundExistingIndex = true;
                    MolecularIndexReagent fieldMolecularIndexReagent =
                            OrmUtil.proxySafeCast(fieldReagent, MolecularIndexReagent.class);
                    for (MolecularIndex molecularIndex : fieldMolecularIndexReagent.getMolecularIndexingScheme()
                            .getIndexes().values()) {
                        for (MolecularIndexingScheme molecularIndexingScheme :
                                molecularIndex.getMolecularIndexingSchemes()) {
                            if (molecularIndexingScheme.getIndexes().values().containsAll(
                                    newMolecularIndexReagent.getMolecularIndexingScheme().getIndexes().values())) {
                                foundMergedScheme = true;
                                reagents.remove(i);
                                reagents.add(new MolecularIndexReagent(molecularIndexingScheme));
                            }
                        }
                    }
                    break;
                }
            }
            if (!foundExistingIndex) {
                reagents.add(newReagent);
            } else if (!foundMergedScheme) {
                throw new RuntimeException("Failed to find merged molecular index scheme");
            }
        } else {
            reagents.add(newReagent);
        }
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
        List<MolecularIndexReagent> indexes = new ArrayList<>();
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
    @Nullable
    public LabBatch getLabBatch() {
        return labBatch;
    }

    public Collection<LabBatch> getAllLabBatches() {
        return allLabBatches;
    }

    public void addLabBatches(Collection<LabBatch> batches){
        if(allLabBatches == null){
            allLabBatches = new HashSet<>();
        }
        allLabBatches.addAll(batches);
        // todo jmt improve this logic
        if (allLabBatches.size() == 1) {
            labBatch = allLabBatches.iterator().next();
        }
    }

    public Collection<LabBatch> getAllWorkflowLabBatches() {
        Set<LabBatch> workflowBatches = new HashSet<>();
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
        List<LabBatchComposition> filteredBatchCompositions = new ArrayList<>();
        for (LabBatch localLabBatch : getAllWorkflowLabBatches()) {
            for (LabBatchComposition composition : allLabBatchCompositions) {
                if (composition.getLabBatch().equals(localLabBatch)) {
                    filteredBatchCompositions.add(composition);
                }
            }
        }
        Collections.sort(filteredBatchCompositions, LabBatchComposition.HIGHEST_COUNT_FIRST);

        return filteredBatchCompositions;
    }

    @Nullable
    public String getProductOrderKey() {
        return productOrderKey;
    }

    public void setProductOrderKey(String productOrderKey) {
        this.productOrderKey = productOrderKey;
    }

    // todo jmt should this look at bucket entries, rather than lab batches?
    /**
     * Gets the name of the sample's workflow, based on LCSETs.
     * @return workflow name
     */
    @Nullable
    public String getWorkflowName() {
        String workflowName = null;
        for (LabBatch localLabBatch : allLabBatches) {
            if (localLabBatch.getWorkflowName() != null) {
                if(workflowName == null) {
                    workflowName = localLabBatch.getWorkflowName();
                } else {
                    if(!workflowName.equals(localLabBatch.getWorkflowName())) {
                        throw new RuntimeException("Conflicting workflows: " + workflowName + ", " +
                                                   localLabBatch.getWorkflowName());
                    }
                }
            }
        }
        return workflowName;
    }
}
