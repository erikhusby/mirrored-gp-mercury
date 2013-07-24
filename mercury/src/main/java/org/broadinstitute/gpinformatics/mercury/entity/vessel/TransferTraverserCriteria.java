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
                new HashMap<>();
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
                        batchesAtHop = new HashSet<>();
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
            Set<LabBatch> nearestSet = new HashSet<>();
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
            Set<LabBatch> allBatches = new HashSet<>();
            for (Collection<LabBatch> collection : labBatchesAtHopCount.values()) {
                allBatches.addAll(collection);
            }
            return allBatches;
        }

    }

    class NearestLabMetricOfTypeCriteria implements TransferTraverserCriteria {
        private LabMetric.MetricType metricType;
        private Map<Integer, Collection<LabMetric>> labMetricsAtHop = new HashMap<>();

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
                            metricsAtHop = new ArrayList<>();
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
                new HashMap<>();

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
            Set<String> nearestSet = new HashSet<>();
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
        private final Map<Integer, List<LabVessel>> labVesselAtHopCount = new TreeMap<>();

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            List<LabVessel> vesselList;
            if (labVesselAtHopCount.containsKey(context.getHopCount())) {
                vesselList = labVesselAtHopCount.get(context.getHopCount());
            } else {
                vesselList = new ArrayList<>();
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
            Set<LabVessel> allVessels = new HashSet<>();
            for (List<LabVessel> vesselList : labVesselAtHopCount.values()) {
                allVessels.addAll(vesselList);
            }
            Map<Date, LabVessel> sortedTreeMap = new TreeMap<>();
            for (LabVessel vessel : allVessels) {
                sortedTreeMap.put(vessel.getCreatedOn(), vessel);
            }
            return new ArrayList<>(sortedTreeMap.values());
        }
    }

    class LabVesselAncestorCriteria implements TransferTraverserCriteria {
        private final Map<Integer, List<LabVessel>> labVesselAtHopCount = new TreeMap<>();

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            List<LabVessel> vesselList;
            if (labVesselAtHopCount.containsKey(context.getHopCount())) {
                vesselList = labVesselAtHopCount.get(context.getHopCount());
            } else {
                vesselList = new ArrayList<>();
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
            Set<LabVessel> allVessels = new HashSet<>();
            for (List<LabVessel> vesselList : labVesselAtHopCount.values()) {
                allVessels.addAll(vesselList);
            }
            Map<Date, LabVessel> sortedTreeMap = new TreeMap<>();
            for (LabVessel vessel : allVessels) {
                sortedTreeMap.put(vessel.getCreatedOn(), vessel);
            }
            return new ArrayList<>(sortedTreeMap.values());
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
        private List<LabEventType> types;
        private Map<LabEvent, Set<LabVessel>> vesselsForLabEventType = new HashMap<>();

        public VesselForEventTypeCriteria(List<LabEventType> type) {
            this.types = type;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(
                Context context) {
            LabVessel vessel = context.getLabVessel();
            LabEvent event = context.getEvent();
            if (event != null) {
                evaluteEvent(vessel, event);
            }
            if (vessel != null) {
                //check all in place events and descendant in place events
                for (LabEvent inPlaceEvent : vessel.getInPlaceEvents()) {
                    evaluteEvent(vessel, inPlaceEvent);
                }
                Collection<LabVessel> descendantVessels = vessel.getDescendantVessels();
                for (LabVessel descendant : descendantVessels) {
                    Set<LabEvent> inPlaceEvents = descendant.getInPlaceEvents();
                    for (LabEvent inPlaceEvent : inPlaceEvents) {
                        evaluteEvent(vessel, inPlaceEvent);
                    }
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        private void evaluteEvent(LabVessel vessel, LabEvent event) {
            if (types.contains(event.getLabEventType())) {
                //if this is in place just add the vessel
                if (event.getInPlaceLabVessel() != null) {
                    Set<LabVessel> vessels = vesselsForLabEventType.get(event);
                    if (vessels == null) {
                        vessels = new HashSet<>();
                    }
                    vessels.add(event.getInPlaceLabVessel());
                    vesselsForLabEventType.put(event, vessels);
                }
                //otherwise check the target vessels
                for (LabVessel targetVessel : event.getTargetLabVessels()) {
                    Set<LabVessel> vessels = vesselsForLabEventType.get(event);
                    if (vessels == null) {
                        vessels = new HashSet<>();
                    }
                    if (vessel == null) {
                        vessels.add(targetVessel);
                        vesselsForLabEventType.put(event, vessels);
                    } else {
                        vessels.add(vessel);
                        //if we are a container and we contain the vessel then add it
                        if (targetVessel.getContainerRole() != null
                            && targetVessel.getContainerRole().getContainedVessels().contains(vessel)) {
                            vesselsForLabEventType.put(event, vessels);
                        } else if (targetVessel.equals(vessel)) {
                            vesselsForLabEventType.put(event, vessels);
                        }
                    }
                }
            }
        }

        @Override
        public void evaluateVesselInOrder(Context context) {
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public Map<LabEvent, Set<LabVessel>> getVesselsForLabEventType() {
            return vesselsForLabEventType;
        }
    }
}
