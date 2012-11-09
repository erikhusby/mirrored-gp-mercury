package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
* Implemented by classes that accumulate information from a traversal of transfer history
*/
public interface TransferTraverserCriteria {

    enum TraversalControl {
        ContinueTraversing,
        StopTraversing
    }

    enum TraversalDirection {
        Ancestors,
        Descendants
    }

    TraversalControl evaluateVesselPreOrder(LabVessel labVessel, LabEvent labEvent, int hopCount);
    void evaluateVesselInOrder(LabVessel labVessel, LabEvent labEvent, int hopCount);
    void evaluateVesselPostOrder(LabVessel labVessel, LabEvent labEvent, int hopCount);

    /**
     * Searches for nearest Lab Batch
     */
    class NearestLabBatchFinder implements TransferTraverserCriteria {

        // index -1 is for batches for sampleInstance's starter (think BSP stock)
        private static final int STARTER_INDEX = -1;

        private final Map<Integer,Collection<LabBatch>> labBatchesAtHopCount = new HashMap<Integer, Collection<LabBatch>>();

        @Override
        public TraversalControl evaluateVesselPreOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
            if (labVessel != null) {
                Collection<LabBatch> labBatches = labVessel.getLabBatches();

                if (!labBatches.isEmpty()) {
                    if (!labBatchesAtHopCount.containsKey(hopCount)) {
                        labBatchesAtHopCount.put(hopCount,new HashSet<LabBatch>());
                    }
                    labBatchesAtHopCount.get(hopCount).addAll(labBatches);
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
        }

        @Override
        public void evaluateVesselPostOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
        }

        public Collection<LabBatch> getNearestLabBatches() {
            int nearest = Integer.MAX_VALUE;
            for (Map.Entry<Integer, Collection<LabBatch>> labBatchesForHopCount : labBatchesAtHopCount.entrySet()) {
                if (labBatchesForHopCount.getKey() < nearest) {
                    nearest = labBatchesForHopCount.getKey();
                }
            }
            return labBatchesAtHopCount.get(nearest);
        }
    }

    /**
     * Traverses transfers to find the single sample libraries.
     */
    class SingleSampleLibraryCriteria implements TransferTraverserCriteria {
        private final Map<MercurySample, Collection<LabVessel>> singleSampleLibrariesForInstance = new HashMap<MercurySample, Collection<LabVessel>>();

        @Override
        public TraversalControl evaluateVesselPreOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
            if (labVessel != null) {
                for (SampleInstance sampleInstance : labVessel.getSampleInstances()) {
                    MercurySample startingSample = sampleInstance.getStartingSample();
                    // todo jmt fix this
//                    if (labVessel.isSingleSampleLibrary(sampleInstance.getSingleProjectPlan().getWorkflowDescription())) {
//                        if (!singleSampleLibrariesForInstance.containsKey(startingSample)) {
//                            singleSampleLibrariesForInstance.put(startingSample,new HashSet<LabVessel>());
//                        }
//                        singleSampleLibrariesForInstance.get(startingSample).add(labVessel);
//                    }
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
        }

        @Override
        public void evaluateVesselPostOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
        }

        public Map<MercurySample,Collection<LabVessel>> getSingleSampleLibraries() {
            return singleSampleLibrariesForInstance;
        }
    }

    /**
     * Build sample instances, by navigating to MercurySample, and applying reagents
     */
    class SampleInstanceCriteria implements TransferTraverserCriteria {

        /** Sample instances encountered during this traversal */
        private Set<SampleInstance> sampleInstances = new LinkedHashSet<SampleInstance>();
        private Set<SampleInstance> currentSampleInstances = new LinkedHashSet<SampleInstance>();
        /** Ensure that reagents are applied only once */
        private boolean reagentsApplied = false;
        /** The first lab event encountered */
        private LabEvent labEvent;

        @Override
        public TraversalControl evaluateVesselPreOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
        }

        @Override
        public void evaluateVesselPostOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
            // todo jmt this class shouldn't have to worry about plate wells that have no informatics contents
            if (labVessel != null) {
                if (labVessel.isSampleAuthority()) {
                    currentSampleInstances.clear();
                    for (MercurySample mercurySample : labVessel.getMercurySamples()) {
                        currentSampleInstances.add(new SampleInstance(mercurySample, null, null));
                    }
                    sampleInstances.addAll(currentSampleInstances);
                }
                if (labVessel.getReagentContentsCount() != null && labVessel.getReagentContentsCount() > 0) {
                    for (Reagent reagent : labVessel.getReagentContents()) {
                        for (SampleInstance sampleInstance : currentSampleInstances) {
                            sampleInstance.addReagent(reagent);
                        }
                    }
                }
                if (labEvent != null) {
//                    applyProjectPlanOverrideIfPresent(labEvent, sampleInstances);
                }
            }
            if(labEvent != null && this.labEvent == null) {
                this.labEvent = labEvent;
            }
        }

        public Set<SampleInstance> getSampleInstances() {
            if(!reagentsApplied) {
                reagentsApplied = true;
/*
                for (Reagent reagent : reagents) {
                    for (SampleInstance sampleInstance : sampleInstances) {
                        sampleInstance.addReagent(reagent);
                    }
                }
*/
                if (labEvent != null) {
                    for (SampleInstance sampleInstance : sampleInstances) {
                        MolecularState molecularState = sampleInstance.getMolecularState();
                        if(molecularState == null) {
                            LabEventType labEventType = labEvent.getLabEventType();
                            molecularState = new MolecularState(labEventType.getNucleicAcidType(), labEventType.getTargetStrand());
                        }
                        sampleInstance.setMolecularState(molecularState);
                    }
                }
            }
            return sampleInstances;
        }
    }
}
