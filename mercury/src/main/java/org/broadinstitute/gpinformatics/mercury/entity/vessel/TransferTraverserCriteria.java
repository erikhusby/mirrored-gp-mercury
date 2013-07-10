package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
        public Context(@Nonnull LabVessel labVessel, LabEvent event, int hopCount,
                       @Nonnull TraversalDirection traversalDirection) {
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
        public Context(LabVessel labVessel, @Nonnull VesselContainer vesselContainer,
                       @Nonnull VesselPosition vesselPosition, LabEvent event, int hopCount,
                       @Nonnull TraversalDirection traversalDirection) {
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
     *
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

        private final Map<Integer, Collection<LabBatch>> labBatchesAtHopCount =
                new HashMap<Integer, Collection<LabBatch>>();
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

    class NearestLabMetricOfTypeCriteria implements TransferTraverserCriteria {
        private LabMetric.MetricType metricType;
        private Map<Integer, Collection<LabMetric>> labMetricsAtHop = new HashMap<Integer, Collection<LabMetric>>();

        public NearestLabMetricOfTypeCriteria(LabMetric.MetricType metricType) {
            this.metricType = metricType;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            LabVessel vessel = context.getLabVessel();
            if (context.getLabVessel() != null) {
                for (LabMetric metric : vessel.getMetrics()) {
                    if (metric.getName().equals(metricType)) {
                        Collection<LabMetric> metricsAtHop;
                        metricsAtHop = labMetricsAtHop.get(context.getHopCount());
                        if (metricsAtHop == null) {
                            metricsAtHop = new ArrayList<LabMetric>();
                            labMetricsAtHop.put(context.getHopCount(), metricsAtHop);
                        }
                        metricsAtHop.add(metric);
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

        public Collection<LabMetric> getNearestMetrics() {
            int nearest = Integer.MAX_VALUE;
            for (Map.Entry<Integer, Collection<LabMetric>> labMetricsForHopCount : labMetricsAtHop.entrySet()) {
                if (labMetricsForHopCount.getKey() < nearest) {
                    nearest = labMetricsForHopCount.getKey();
                }
            }
            return labMetricsAtHop.get(nearest);
        }
    }

    class NearestProductOrderCriteria implements TransferTraverserCriteria {

        private final Map<Integer, Collection<String>> productOrdersAtHopCount =
                new HashMap<Integer, Collection<String>>();

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
                if (context.getTraversalDirection() == TraversalDirection.Descendants) {
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

    class LabVesselAncestorCriteria implements TransferTraverserCriteria {
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
                if (context.getTraversalDirection() == TraversalDirection.Ancestors) {
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

        public Collection<LabVessel> getLabVesselAncestors() {
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

    /**
     * NearestTubeAncestorsCriteria is a Traverser Criteria object intended to capture the closest (in number of hops)
     * TwoDBarcodedTube(s) that can be found in a target vessel's event history.  When found, not only will the the tube
     * be saved for access, but also an object that relates the tube to its position at its found location will be
     * returned.
     * <p/>
     * TODO SGM/BR/JMT:  A similar object {@link StaticPlate.NearestTubeAncestorsCriteria} (created before this one)
     * has an extra safety if check on it. Consider merging into here
     */
    class NearestTubeAncestorsCriteria implements TransferTraverserCriteria {

        private Set<LabVessel> tubes = new HashSet<>();
        private Set<VesselAndPosition> vesselAndPositions = new LinkedHashSet<>();

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (OrmUtil.proxySafeIsInstance(context.getLabVessel(), TwoDBarcodedTube.class)) {
                tubes.add(context.getLabVessel());
                vesselAndPositions.add(new VesselAndPosition(context.getLabVessel(), context.getVesselPosition()));
                return TraversalControl.StopTraversing;
            } else {
                return TraversalControl.ContinueTraversing;
            }
        }

        @Override
        public void evaluateVesselInOrder(Context context) {
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public Set<LabVessel> getTubes() {
            return tubes;
        }

        public Set<VesselAndPosition> getVesselAndPositions() {
            return vesselAndPositions;
        }
    }

    /**
     * Similar to NearestTubeAncestorsCriteria, this criteria object is intended to capture the closest (in number of
     * hops) TwoDBarcodedTube(s) that can be found in a target vessels history.  The user of this method has no need
     * of the tubes position in the container that it is found, and sometimes creating the VesselAndPosition object
     * breaks so this is being used in its place.
     */
    class NearestTubeAncestorCriteria implements TransferTraverserCriteria {

        private LabVessel tube;

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (OrmUtil.proxySafeIsInstance(context.getLabVessel(), TwoDBarcodedTube.class)) {
                if (tube == null) {
                    tube = context.getLabVessel();
                }
                return TraversalControl.StopTraversing;
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(Context context) {
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public LabVessel getTube() {
            return tube;
        }

    }

    public class VesselTypeDescendantCriteria<T extends LabVessel> implements TransferTraverserCriteria {
        private Collection<T> descendantsOfVesselType = new HashSet<>();
        private final Class<T> typeParameterClass;

        public VesselTypeDescendantCriteria(Class<T> typeParameterClass) {
            this.typeParameterClass = typeParameterClass;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (OrmUtil.proxySafeIsInstance(context.getLabVessel(), typeParameterClass)) {
                descendantsOfVesselType.add(typeParameterClass.cast(context.getLabVessel()));
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselInOrder(Context context) {
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public Collection<T> getDescendantsOfVesselType() {
            return descendantsOfVesselType;
        }
    }

    public class VesselForEventTypeCriteria implements TransferTraverserCriteria {
        private LabEventType type;
        private Map<LabVessel, LabEvent> vesselsForLabEventType = new HashMap<>();

        public VesselForEventTypeCriteria(LabEventType type) {
            this.type = type;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(
                Context context) {
            LabVessel vessel = context.getLabVessel();
            if (vessel != null) {
                for (LabEvent event : vessel.getEvents()) {
                    if (type.equals(event.getLabEventType())) {
                        for (LabVessel targetVessel : event.getTargetLabVessels()) {
                            if (targetVessel.getContainerRole() != null
                                && targetVessel.getContainerRole().getContainedVessels().contains(vessel)) {
                                vesselsForLabEventType.put(vessel, event);
                            } else if (targetVessel.equals(vessel)) {
                                vesselsForLabEventType.put(vessel, event);
                            }
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

        public Map<LabVessel, LabEvent> getVesselsForLabEventType() {
            return vesselsForLabEventType;
        }
    }
}
