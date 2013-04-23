package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import java.util.*;

import static org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria.TraversalDirection.Descendants;

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

    /**
     * The context for the current traversal. Contains information about the current query including the lab vessel, the
     * vessel container that the lab vessel is in, the lab event, the traversal depth, and direction of traversal.
     * <p/>
     * At least one of labVessel and vesselContainer will be non-null. vesselContainer may be null in the case where
     * labVessel is not in a container (in the context of the current event). labVessel may be null in the case where
     * all positions of a container are being queried and there is no lab vessel at the current position.
     */
    class Context {

        /**
         * The lab vessel being visited.
         * Null if traversing all positions of a container and there is no lab vessel at the current position.
         */
        private LabVessel labVessel;

        /**
         * The container holding the lab vessel.
         * Null if the lab vessel is not in a container in the current context.
         */
        private VesselContainer vesselContainer;

        /**
         * The position of the vessel within its holding container.
         * Not null if vesselContainer is not null.
         */
        private VesselPosition vesselPosition;

        /**
         * The event being visited.
         * Null if there is no event being processed, such as at the beginning of a traversal.
         */
        private LabEvent event;

        /**
         * The current hop count.
         * Not null.
         */
        private int hopCount;

        /**
         * The direction of the current traversal.
         * Not null.
         */
        private TransferTraverserCriteria.TraversalDirection traversalDirection;

        /**
         * Creates a context for a single lab vessel outside of any vessel container. For example, this could represent
         * a static plate or a single barcoded tube.
         *
         * @param labVessel          the lab vessel
         * @param event              the transfer event traversed to reach this container
         * @param hopCount           the traversal depth
         * @param traversalDirection the direction of traversal
         */
        public Context(@Nonnull LabVessel labVessel, LabEvent event, int hopCount, @Nonnull TraversalDirection traversalDirection) {
            this.labVessel = labVessel;
            this.event = event;
            this.hopCount = hopCount;
            this.traversalDirection = traversalDirection;
        }

        /**
         * Creates a context for a lab vessel in a vessel container. For example, this could represent an individually
         * barcoded tube inside of a tube rack.
         *
         * @param labVessel          the lab vessel
         * @param vesselContainer    the containing vessel
         * @param vesselPosition     the position of labVessel within vesselContainer
         * @param event              the transfer event traversed to reach this container
         * @param hopCount           the traversal depth
         * @param traversalDirection the direction of traversal
         */
        public Context(LabVessel labVessel, @Nonnull VesselContainer vesselContainer, @Nonnull VesselPosition vesselPosition, LabEvent event, int hopCount, @Nonnull TraversalDirection traversalDirection) {
            this.labVessel = labVessel;
            this.vesselContainer = vesselContainer;
            this.vesselPosition = vesselPosition;
            this.event = event;
            this.hopCount = hopCount;
            this.traversalDirection = traversalDirection;
        }

        public LabVessel getLabVessel() {
            return labVessel;
        }

        public VesselContainer getVesselContainer() {
            return vesselContainer;
        }

        public VesselPosition getVesselPosition() {
            return vesselPosition;
        }

        public LabEvent getEvent() {
            return event;
        }

        public int getHopCount() {
            return hopCount;
        }

        public TraversalDirection getTraversalDirection() {
            return traversalDirection;
        }
    }

    /**
     * Callback method called before processing the next level of vessels in the traversal.
     *
     * @param context
     * @return
     */
    TraversalControl evaluateVesselPreOrder(Context context);

    /**
     * TODO: document
     *
     * @param context
     */
    void evaluateVesselInOrder(Context context);

    /**
     * Callback method called after processing the next level of vessels in the traversal.
     *
     * @param context
     */
    void evaluateVesselPostOrder(Context context);

    /**
     * Searches for nearest Lab Batch
     */
    class NearestLabBatchFinder implements TransferTraverserCriteria {

        // index -1 is for batches for sampleInstance's starter (think BSP stock)
        private static final int STARTER_INDEX = -1;

        private final Map<Integer, Collection<LabBatch>> labBatchesAtHopCount = new HashMap<Integer, Collection<LabBatch>>();
        private LabBatch.LabBatchType type;

        /**
         * Constructs a new NearestLabBatchFinder with a LabBatch type filter.
         *
         * @param type This type is used to filter the lab batches. If it is null there is no filtering.
         */
        public NearestLabBatchFinder(LabBatch.LabBatchType type) {
            this.type = type;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (context.getLabVessel() != null) {
                Collection<LabBatch> labBatches = context.getLabVessel().getLabBatches();

                if (!labBatches.isEmpty()) {

                    Collection<LabBatch> batchesAtHop = labBatchesAtHopCount.get(context.getHopCount());
                    if (batchesAtHop == null) {
                        batchesAtHop = new HashSet<LabBatch>();
                    }
                    if (type == null) {
                        batchesAtHop.addAll(labBatches);
                    } else {
                        for (LabBatch labBatch : labBatches) {
                            if (labBatch.getLabBatchType() != null) {
                                if (labBatch.getLabBatchType().equals(type)) {
                                    batchesAtHop.add(labBatch);
                                }
                            }
                        }
                    }
                    // If, after filtering, we have results add them to the map
                    if (batchesAtHop.size() > 0) {
                        labBatchesAtHopCount.put(context.getHopCount(), batchesAtHop);
                    }
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(Context context) {
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public Collection<LabBatch> getNearestLabBatches() {
            int nearest = Integer.MAX_VALUE;
            Set<LabBatch> nearestSet = new HashSet<LabBatch>();
            for (Map.Entry<Integer, Collection<LabBatch>> labBatchesForHopCount : labBatchesAtHopCount.entrySet()) {
                if (labBatchesForHopCount.getKey() < nearest) {
                    nearest = labBatchesForHopCount.getKey();
                }
            }
            Collection<LabBatch> batchesAtHop = labBatchesAtHopCount.get(nearest);
            if (batchesAtHop != null) {
                if (type == null) {
                    nearestSet.addAll(batchesAtHop);
                } else {
                    for (LabBatch labBatch : batchesAtHop) {
                        if (labBatch.getLabBatchType().equals(type)) {
                            nearestSet.add(labBatch);
                        }
                    }
                }

            }

            return nearestSet;
        }

        public Collection<LabBatch> getAllLabBatches() {
            Set<LabBatch> allBatches = new HashSet<LabBatch>();
            for (Collection<LabBatch> collection : labBatchesAtHopCount.values()) {
                allBatches.addAll(collection);
            }
            return allBatches;
        }

    }

    /**
     * Traverses transfers to find the single sample libraries.
     */
    class SingleSampleLibraryCriteria implements TransferTraverserCriteria {
        private final Map<MercurySample, Collection<LabVessel>> singleSampleLibrariesForInstance = new HashMap<MercurySample, Collection<LabVessel>>();

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (context.getLabVessel() != null) {
                for (SampleInstance sampleInstance : context.getLabVessel().getSampleInstances()) {
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
        public void evaluateVesselInOrder(Context context) {
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public Map<MercurySample, Collection<LabVessel>> getSingleSampleLibraries() {
            return singleSampleLibrariesForInstance;
        }
    }

    class NearestProductOrderCriteria implements TransferTraverserCriteria {

        private final Map<Integer, Collection<String>> productOrdersAtHopCount = new HashMap<Integer, Collection<String>>();

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (context.getLabVessel() != null) {
                if (context.getLabVessel().getMercurySamples() != null) {
                    for (BucketEntry bucketEntry : context.getLabVessel().getBucketEntries()) {
                        if (StringUtils.isBlank(bucketEntry.getPoBusinessKey())) { // todo jmt use BucketEntry?
                            continue;
                        }
                        if (!productOrdersAtHopCount.containsKey(context.getHopCount())) {
                            productOrdersAtHopCount.put(context.getHopCount(), new HashSet<String>());
                        }

                        String productOrderKey = bucketEntry.getPoBusinessKey();
                        if (productOrderKey != null) {
                            productOrdersAtHopCount.get(context.getHopCount()).add(productOrderKey);
                        }
                    }
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(Context context) {
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public Collection<String> getNearestProductOrders() {
            int nearest = Integer.MAX_VALUE;
            Set<String> nearestSet = new HashSet<String>();
            for (Map.Entry<Integer, Collection<String>> posForHopCount : productOrdersAtHopCount.entrySet()) {
                if (posForHopCount.getKey() < nearest) {
                    nearest = posForHopCount.getKey();
                }
            }

            if (productOrdersAtHopCount.containsKey(nearest)) {
                nearestSet.addAll(productOrdersAtHopCount.get(nearest));
            }

            return nearestSet;

        }
    }

    class LabVesselDescendantCriteria implements TransferTraverserCriteria {
        private final Map<Integer, List<LabVessel>> labVesselAtHopCount = new TreeMap<Integer, List<LabVessel>>();

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            List<LabVessel> vesselList;
            if (labVesselAtHopCount.containsKey(context.getHopCount())) {
                vesselList = labVesselAtHopCount.get(context.getHopCount());
            } else {
                vesselList = new ArrayList<LabVessel>();
            }

            if (context.getLabVessel() != null) {
                vesselList.add(context.getLabVessel());
            } else if (context.getEvent() != null) {
                if (context.getTraversalDirection() == Descendants) {
                    vesselList.addAll(context.getEvent().getTargetLabVessels());
                } else {
                    vesselList.addAll(context.getEvent().getSourceLabVessels());
                }
            }
            labVesselAtHopCount.put(context.getHopCount(), vesselList);

            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(Context context) {
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
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
