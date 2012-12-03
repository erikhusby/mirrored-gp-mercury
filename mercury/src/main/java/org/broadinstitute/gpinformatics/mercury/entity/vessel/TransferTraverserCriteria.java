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
import java.util.LinkedList;
import java.util.List;
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

    class NearestProductOrderCriteria implements TransferTraverserCriteria {

        private final Map<Integer, Collection<String>> productOrdersAtHopCount = new HashMap<Integer, Collection<String>>();

        @Override
        public TraversalControl evaluateVesselPreOrder ( LabVessel labVessel, LabEvent labEvent, int hopCount ) {
            if (labVessel != null) {
                for (SampleInstance sampleInstance : labVessel.getSampleInstances()) {
                    MercurySample startingSample = sampleInstance.getStartingSample();

                    if(!productOrdersAtHopCount.containsKey(hopCount) ) {
                        productOrdersAtHopCount.put(hopCount, new HashSet<String>());
                    }

                    productOrdersAtHopCount.get(hopCount).add(startingSample.getProductOrderKey());
                }
            }
            return TraversalControl.ContinueTraversing;

        }

        @Override
        public void evaluateVesselInOrder ( LabVessel labVessel, LabEvent labEvent, int hopCount ) {

        }

        @Override
        public void evaluateVesselPostOrder ( LabVessel labVessel, LabEvent labEvent, int hopCount ) {

        }

        public Collection<String> getNearestProductOrders() {
            int nearest = Integer.MAX_VALUE;
            for (Map.Entry<Integer, Collection<String>> posForHopCount : productOrdersAtHopCount.entrySet()) {
                if (posForHopCount.getKey() < nearest) {
                    nearest = posForHopCount.getKey();
                }
            }
            return productOrdersAtHopCount.get(nearest);

        }
    }
}
