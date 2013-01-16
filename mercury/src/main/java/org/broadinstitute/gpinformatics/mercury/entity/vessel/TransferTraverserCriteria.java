package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.*;

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

        private final Map<Integer, Collection<LabBatch>> labBatchesAtHopCount = new HashMap<Integer, Collection<LabBatch>>();

        @Override
        public TraversalControl evaluateVesselPreOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
            if (labVessel != null) {
                Collection<LabBatch> labBatches = labVessel.getLabBatches();

                if (!labBatches.isEmpty()) {
                    if (!labBatchesAtHopCount.containsKey(hopCount)) {
                        labBatchesAtHopCount.put(hopCount, new HashSet<LabBatch>());
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
            Set<LabBatch> nearestSet = new HashSet<LabBatch>();
            for (Map.Entry<Integer, Collection<LabBatch>> labBatchesForHopCount : labBatchesAtHopCount.entrySet()) {
                if (labBatchesForHopCount.getKey() < nearest) {
                    nearest = labBatchesForHopCount.getKey();
                }
            }
            if(labBatchesAtHopCount.containsKey(nearest)) {
                nearestSet.addAll(labBatchesAtHopCount.get(nearest));
            }

            return nearestSet;
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

        public Map<MercurySample, Collection<LabVessel>> getSingleSampleLibraries() {
            return singleSampleLibrariesForInstance;
        }
    }

    class NearestProductOrderCriteria implements TransferTraverserCriteria {

        private final Map<Integer, Collection<String>> productOrdersAtHopCount = new HashMap<Integer, Collection<String>>();

        @Override
        public TraversalControl evaluateVesselPreOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
            if (labVessel != null) {
                for (SampleInstance sampleInstance : labVessel.getSampleInstances()) {
                    MercurySample startingSample = sampleInstance.getStartingSample();

                    if (!productOrdersAtHopCount.containsKey(hopCount)) {
                        productOrdersAtHopCount.put(hopCount, new HashSet<String>());
                    }

                    productOrdersAtHopCount.get(hopCount).add(startingSample.getProductOrderKey());
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

        public Collection<String> getNearestProductOrders() {
            int nearest = Integer.MAX_VALUE;
            Set<String> nearestSet = new HashSet<String>();
            for (Map.Entry<Integer, Collection<String>> posForHopCount : productOrdersAtHopCount.entrySet()) {
                if (posForHopCount.getKey() < nearest) {
                    nearest = posForHopCount.getKey();
                }
            }

            if(productOrdersAtHopCount.containsKey(nearest)) {
                nearestSet.addAll(productOrdersAtHopCount.get(nearest));
            }

            return nearestSet;

        }
    }

    class LabVesselDescendantCriteria implements TransferTraverserCriteria {
        private final Map<Integer, List<LabVessel>> labVesselAtHopCount = new TreeMap<Integer, List<LabVessel>>();

        @Override
        public TraversalControl evaluateVesselPreOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
            List<LabVessel> vesselList;
            if (labVesselAtHopCount.containsKey(hopCount)) {
                vesselList = labVesselAtHopCount.get(hopCount);
            } else {
                vesselList = new ArrayList<LabVessel>();
            }

            if (labVessel != null) {
                vesselList.add(labVessel);
            } else if (labEvent != null) {
                vesselList.addAll(labEvent.getTargetLabVessels());
            }
            labVesselAtHopCount.put(hopCount, vesselList);

            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
        }

        @Override
        public void evaluateVesselPostOrder(LabVessel labVessel, LabEvent labEvent, int hopCount) {
        }

        public Collection<LabVessel> getLabVesselDescendants() {
            Set<LabVessel> allVessels = new HashSet<LabVessel>();
            for (List<LabVessel> vesselList : labVesselAtHopCount.values()) {
                allVessels.addAll(vesselList);
            }
            Map<Date, LabVessel> sortedTreeMap = new TreeMap<Date, LabVessel>();
            for (LabVessel vessel : allVessels) {
                sortedTreeMap.put(vessel.getCreatedOn(), vessel);
            }
            return new ArrayList<LabVessel>(sortedTreeMap.values());
        }
    }
}
