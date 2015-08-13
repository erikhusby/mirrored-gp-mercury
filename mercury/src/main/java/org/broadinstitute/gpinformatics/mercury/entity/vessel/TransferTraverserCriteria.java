package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
     * The context for the current traversal. Contains information about the current vessel traversal node including: <br />
     * <ul><li>The lab vessel from which the traversal was started</li>
     * <li>The source the lab event/vessel</li>
     * <li>The target lab event/vessel in the traversal path</li>
     * <li>The direction of traversal</li></ul>
     */
    class Context {

        /**
         * The lab vessel, position, container from which the traversal started.
         */
        private LabVessel startingLabVessel;
        private VesselPosition startingVesselPosition;
        private VesselContainer startingVesselContainer;

        /**
         * The vessel event being visited.
         * Null if at the starting lab vessel of a descendant traversal (no event being processed).
         */
        private LabVessel.VesselEvent vesselEvent;

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

        private Context(){}

        /**
         * Creates a context for traversal starting vessel.
         *
         * @param startingLabVessel  the lab vessel at the start of any traversal
         * @param startingVesselPosition  the lab vessel container position at the start of any traversal
         * @param startingVesselContainer  the lab vessel container
         * @param traversalDirection the direction of traversal
         */
        public static Context buildStartingContext(LabVessel startingLabVessel,
                                                   VesselPosition startingVesselPosition,
                                                   VesselContainer startingVesselContainer,
                                                   @Nonnull TraversalDirection traversalDirection) {
            Context context = new Context();
            context.startingLabVessel = startingLabVessel;
            context.startingVesselPosition = startingVesselPosition;
            context.startingVesselContainer = startingVesselContainer;
            context.vesselEvent = null;
            context.hopCount = 0;
            context.traversalDirection = traversalDirection;
            return context;
        }

        /**
         * Creates a context for a node in a traversal.
         *
         * @param vesselEvent  event transfer in the process flow directly from this vessel/container
         * @param hopCount           the traversal depth
         * @param traversalDirection the direction of traversal
         */
        public static Context buildTraversalNodeContext(LabVessel.VesselEvent vesselEvent,
                                                        int hopCount,
                                                        @Nonnull TraversalDirection traversalDirection) {
            Context context = new Context();
            context.startingLabVessel = null;
            context.startingVesselPosition = null;
            context.startingVesselContainer = null;
            context.vesselEvent = vesselEvent;
            context.hopCount = hopCount;
            context.traversalDirection = traversalDirection;
            return context;
        }

        public LabVessel getStartingLabVessel() {
            return startingLabVessel;
        }
        public VesselPosition getStartingVesselPosition() {
            return startingVesselPosition;
        }
        public VesselContainer getStartingVesselContainer() {
            return startingVesselContainer;
        }

        /**
         * The vessel/event in the traversal path.
         * @return
         */
        public LabVessel.VesselEvent getVesselEvent() {
            return vesselEvent;
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
     * Callback method called after processing the next level of vessels in the traversal.
     *
     * @param context
     */
    void evaluateVesselPostOrder(Context context);

    /**
     * Searches for nearest Lab Batch <br />
     * Despite class name, all lab batches are located, method getNearestLabBatches returns the nearest.
     */
    class NearestLabBatchFinder implements TransferTraverserCriteria {

        public enum AssociationType {
            GENERAL_LAB_VESSEL,
            DILUTION_VESSEL
        }

        private final Map<Integer, Collection<LabBatch>> labBatchesAtHopCount = new HashMap<>();
        private final LabBatch.LabBatchType labBatchType;
        private AssociationType associationType;

        /**
         * Constructs a new NearestLabBatchFinder with a LabBatch type filter.
         *
         * @param labBatchType This type is used to filter the lab batches. If it is null there is no filtering.
         */
        public NearestLabBatchFinder(LabBatch.LabBatchType labBatchType) {
            this.labBatchType = labBatchType;
            associationType = AssociationType.GENERAL_LAB_VESSEL;
        }

        public NearestLabBatchFinder(
                LabBatch.LabBatchType labBatchType,
                AssociationType associationType) {
            this.labBatchType = labBatchType;
            this.associationType = associationType;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            LabVessel contextVessel;

            if (context.getTraversalDirection() != TraversalDirection.Ancestors) {
                throw new IllegalStateException("NearestLabBatchFinder supports ancestor traversal only");
            }

            if( context.getHopCount() == 0 ) {
                contextVessel = context.getStartingLabVessel();
            } else {
                contextVessel = context.getVesselEvent().getSourceLabVessel();
            }
            if( contextVessel == null ) {
                return TraversalControl.ContinueTraversing;
            }

            Collection<LabBatch> labBatches;
            switch (associationType) {
            case GENERAL_LAB_VESSEL:
                labBatches = contextVessel.getLabBatches();
                break;
            case DILUTION_VESSEL:
                labBatches = new HashSet<>();
                for (LabBatchStartingVessel labBatchStartingVessel : contextVessel.getDilutionReferences()) {
                    labBatches.add(labBatchStartingVessel.getLabBatch());
                }
                break;
            default:
                throw new RuntimeException("Unexpected enum " + associationType);
            }

            if (!labBatches.isEmpty()) {

                Collection<LabBatch> batchesAtHop = labBatchesAtHopCount.get(context.getHopCount());
                if (batchesAtHop == null) {
                    batchesAtHop = new HashSet<>();
                }
                if (labBatchType == null) {
                    batchesAtHop.addAll(labBatches);
                } else {
                    for (LabBatch labBatch : labBatches) {
                        if (labBatch.getLabBatchType() != null) {
                            if (labBatch.getLabBatchType().equals(labBatchType)) {
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
            return TraversalControl.ContinueTraversing;
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
                if (labBatchType == null) {
                    nearestSet.addAll(batchesAtHop);
                } else {
                    for (LabBatch labBatch : batchesAtHop) {
                        if (labBatch.getLabBatchType().equals(labBatchType)) {
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

    /**
     * Searches for nearest metric of specified type <br />
     * Despite class name, all metrics are located, method getNearestMetrics returns the nearest.
     */
    class NearestLabMetricOfTypeCriteria implements TransferTraverserCriteria {
        private LabMetric.MetricType metricType;
        private Map<Integer, List<LabMetric>> labMetricsAtHop = new HashMap<>();

        public NearestLabMetricOfTypeCriteria(LabMetric.MetricType metricType) {
            this.metricType = metricType;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            LabVessel contextVessel;

            if( context.getHopCount() == 0 ) {
                contextVessel = context.getStartingLabVessel();
            } else {
                LabVessel.VesselEvent contextVesselEvent = context.getVesselEvent();
                if (context.getTraversalDirection() == TraversalDirection.Ancestors) {
                    contextVessel = contextVesselEvent.getSourceLabVessel();
                } else {
                    contextVessel = contextVesselEvent.getTargetLabVessel();
                }
            }

            if (contextVessel != null) {
                for (LabMetric metric : contextVessel.getMetrics()) {
                    if (metric.getName().equals(metricType)) {
                        List<LabMetric> metricsAtHop;
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
        public void evaluateVesselPostOrder(Context context) {
        }

        public List<LabMetric> getNearestMetrics() {
            int nearest = Integer.MAX_VALUE;
            for (Map.Entry<Integer, List<LabMetric>> labMetricsForHopCount : labMetricsAtHop.entrySet()) {
                if (labMetricsForHopCount.getKey() < nearest) {
                    nearest = labMetricsForHopCount.getKey();
                }
            }
            List<LabMetric> labMetrics = labMetricsAtHop.get(nearest);
            if (labMetrics != null) {
                Collections.sort(labMetrics);
            }
            return labMetrics;
        }
    }

    class NearestProductOrderCriteria implements TransferTraverserCriteria {

        private final Map<Integer, Collection<String>> productOrdersAtHopCount =
                new HashMap<>();

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {

            LabVessel contextVessel;

            if( context.getHopCount() == 0 ) {
                contextVessel = context.getStartingLabVessel();
            } else {
                LabVessel.VesselEvent contextVesselEvent = context.getVesselEvent();
                if (context.getTraversalDirection() == TraversalDirection.Ancestors) {
                    contextVessel = contextVesselEvent.getSourceLabVessel();
                } else {
                    contextVessel = contextVesselEvent.getTargetLabVessel();
                }
            }

            if (contextVessel != null) {
                if (contextVessel.getMercurySamples() != null) {
                    for (BucketEntry bucketEntry : contextVessel.getBucketEntries()) {
                        if (!productOrdersAtHopCount.containsKey(context.getHopCount())) {
                            productOrdersAtHopCount.put(context.getHopCount(), new HashSet<String>());
                        }

                        String productOrderKey = bucketEntry.getProductOrder().getBusinessKey();
                        if (productOrderKey != null) {
                            productOrdersAtHopCount.get(context.getHopCount()).add(productOrderKey);
                        }
                    }
                }
            }
            return TraversalControl.ContinueTraversing;
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
            // May support it, but avoid mis-match between name and function
            if (context.getTraversalDirection() != TraversalDirection.Descendants) {
                throw new IllegalStateException("LabVesselDescendantCriteria supports descendant traversal only");
            }

            List<LabVessel> vesselList;
            if (labVesselAtHopCount.containsKey(context.getHopCount())) {
                vesselList = labVesselAtHopCount.get(context.getHopCount());
            } else {
                vesselList = new ArrayList<>();
            }

            if (context.getHopCount() == 0 ) {
                vesselList.add(context.getStartingLabVessel());
            } else if (context.getVesselEvent().getTargetLabVessel() != null) {
                vesselList.add(context.getVesselEvent().getTargetLabVessel());
            } else {
                vesselList.addAll(context.getVesselEvent().getLabEvent().getTargetLabVessels());
            }
            labVesselAtHopCount.put(context.getHopCount(), vesselList);

            return TraversalControl.ContinueTraversing;
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
            // May support it, but avoid mis-match between name and function
            if (context.getTraversalDirection() != TraversalDirection.Ancestors) {
                throw new IllegalStateException("LabVesselAncestorCriteria supports ancestor traversal only");
            }

            List<LabVessel> vesselList;
            if (labVesselAtHopCount.containsKey(context.getHopCount())) {
                vesselList = labVesselAtHopCount.get(context.getHopCount());
            } else {
                vesselList = new ArrayList<>();
            }


            if (context.getHopCount() == 0 ) {
                vesselList.add(context.getStartingLabVessel());
            } else if (context.getVesselEvent().getSourceLabVessel() != null) {
                vesselList.add(context.getVesselEvent().getSourceLabVessel());
            } else {
                vesselList.addAll(context.getVesselEvent().getLabEvent().getTargetLabVessels());
            }
            labVesselAtHopCount.put(context.getHopCount(), vesselList);

            return TraversalControl.ContinueTraversing;
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
     * BarcodedTube(s) that can be found in a target vessel's event history.  When found, not only will the the tube
     * be saved for access, but also an object that relates the tube to its position at its found location will be
     * returned.
     */
    class NearestTubeAncestorsCriteria implements TransferTraverserCriteria {

        private final Set<LabVessel> tubes = new HashSet<>();
        private final Set<VesselAndPosition> vesselAndPositions = new LinkedHashSet<>();

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            // May support it, but avoid mis-match between name and function
            if (context.getTraversalDirection() != TraversalDirection.Ancestors) {
                throw new IllegalStateException("NearestTubeAncestorsCriteria supports ancestor traversal only");
            }

            LabVessel contextVessel;
            VesselPosition contextVesselPosition;
            LabVessel.VesselEvent contextVesselEvent;

            if( context.getHopCount() == 0 ) {
                contextVessel = context.getStartingLabVessel();
                contextVesselPosition = context.getStartingVesselPosition();
                contextVesselEvent = null;
            } else {
                contextVesselEvent = context.getVesselEvent();
                contextVessel = contextVesselEvent.getSourceLabVessel();
                contextVesselPosition = contextVesselEvent.getSourcePosition();
            }

            if (OrmUtil.proxySafeIsInstance(contextVessel, BarcodedTube.class)) {
                tubes.add(contextVessel);
                if( contextVesselEvent != null ) {
                    vesselAndPositions.add(new VesselAndPosition(contextVessel, contextVesselPosition));
                    return TraversalControl.StopTraversing;
                }
            }
            return TraversalControl.ContinueTraversing;
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
     * hops) BarcodedTube(s) that can be found in a target vessels history.  The user of this method has no need
     * of the tubes position in the container that it is found, and sometimes creating the VesselAndPosition object
     * breaks so this is being used in its place.
     */
    class NearestTubeAncestorCriteria implements TransferTraverserCriteria {

        private LabVessel tube;

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            // May support it, but avoid mis-match between name and function
            if (context.getTraversalDirection() != TraversalDirection.Ancestors) {
                throw new IllegalStateException("NearestTubeAncestorCriteria supports ancestor traversal only");
            }

            LabVessel contextVessel;
            if( context.getHopCount() == 0 ) {
                contextVessel = context.getStartingLabVessel();
            } else {
                contextVessel = context.getVesselEvent().getSourceLabVessel();
            }

            if (OrmUtil.proxySafeIsInstance(contextVessel, BarcodedTube.class)) {
                if (tube == null) {
                    tube = contextVessel;
                }
                return TraversalControl.StopTraversing;
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public LabVessel getTube() {
            return tube;
        }

    }

    class VesselTypeDescendantCriteria<T extends LabVessel> implements TransferTraverserCriteria {
        private Collection<T> descendantsOfVesselType = new HashSet<>();
        private final Class<T> typeParameterClass;

        public VesselTypeDescendantCriteria(Class<T> typeParameterClass) {
            this.typeParameterClass = typeParameterClass;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            // May support it, but avoid mis-match between name and function
            if (context.getTraversalDirection() != TraversalDirection.Descendants) {
                throw new IllegalStateException("VesselTypeDescendantCriteria supports descendant traversal only");
            }

            LabVessel contextVessel;
            if( context.getHopCount() == 0 ) {
                contextVessel = context.getStartingLabVessel();
            } else {
                contextVessel = context.getVesselEvent().getTargetLabVessel();
            }

            if (OrmUtil.proxySafeIsInstance(contextVessel, typeParameterClass)) {
                descendantsOfVesselType.add(typeParameterClass.cast(contextVessel));
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public Collection<T> getDescendantsOfVesselType() {
            return descendantsOfVesselType;
        }
    }

    class VesselForEventTypeCriteria implements TransferTraverserCriteria {
        private List<LabEventType> types;
        private boolean useTargetVessels = true;
        private Map<LabEvent, Set<LabVessel>> vesselsForLabEventType = new HashMap<>();

        public VesselForEventTypeCriteria(List<LabEventType> types) {
            this.types = types;
        }

        public VesselForEventTypeCriteria(List<LabEventType> types, boolean useTargetVessels ) {
            this(types);
            this.useTargetVessels = useTargetVessels;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            LabVessel contextVessel;
            LabEvent contextEvent;

            if( context.getHopCount() == 0 ) {
                contextVessel = context.getStartingLabVessel();
                contextEvent = null;
            } else {
                LabVessel.VesselEvent contextVesselEvent = context.getVesselEvent();
                contextEvent = contextVesselEvent.getLabEvent();
                if (context.getTraversalDirection() == TraversalDirection.Ancestors) {
                    contextVessel = contextVesselEvent.getSourceLabVessel();
                } else {
                    contextVessel = contextVesselEvent.getTargetLabVessel();
                }
            }

            if( contextEvent != null && contextVessel != null ) {
                evaluteEvent(contextVessel, contextEvent);
            }

            if (contextVessel != null) {
                //check all in place events and descendant in place events
                for (LabEvent inPlaceEvent : contextVessel.getInPlaceLabEvents()) {
                    evaluteEvent(contextVessel, inPlaceEvent);
                }

                Collection<LabVessel> traversalVessels;
                if( context.getTraversalDirection() == TraversalDirection.Ancestors ) {
                    traversalVessels = contextVessel.getAncestorVessels();
                } else {
                    traversalVessels = contextVessel.getDescendantVessels();
                }
                for (LabVessel traversalVessel : traversalVessels) {
                    Set<LabEvent> inPlaceEvents = traversalVessel.getInPlaceLabEvents();
                    for (LabEvent inPlaceEvent : inPlaceEvents) {
                        evaluteEvent(contextVessel, inPlaceEvent);
                    }
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        private void evaluteEvent(LabVessel vessel, LabEvent event) {
            if (types.contains(event.getLabEventType())) {
                // If this is in place just add the vessel
                if (event.getInPlaceLabVessel() != null) {
                    Set<LabVessel> vessels = vesselsForLabEventType.get(event);
                    if (vessels == null) {
                        vessels = new HashSet<>();
                    }
                    vessels.add(event.getInPlaceLabVessel());
                    vesselsForLabEventType.put(event, vessels);
                }
                // Otherwise check the target or source vessels
                Set<LabVessel> labXferVessels;
                if( useTargetVessels ) {
                    labXferVessels = event.getTargetLabVessels();
                } else {
                    labXferVessels = event.getSourceLabVessels();
                }

                for ( LabVessel targetVessel : labXferVessels ) {
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
        public void evaluateVesselPostOrder(Context context) {
        }

        public Map<LabEvent, Set<LabVessel>> getVesselsForLabEventType() {
            return vesselsForLabEventType;
        }
    }

    /**
     * Traverse LabVessels and LabEvents for events producing MaterialTypes
     */
    class LabEventsWithMaterialTypeTraverserCriteria implements TransferTraverserCriteria {
        private final LabVessel.MaterialType materialType;
        private LabVessel vesselForMaterialType = null;

        public LabEventsWithMaterialTypeTraverserCriteria(LabVessel.MaterialType materialTypes) {
            this.materialType = materialTypes;
        }


        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            LabVessel contextVessel;
            LabEvent contextEvent;

            if( context.getHopCount() == 0 ) {
                contextVessel = context.getStartingLabVessel();
                contextEvent = null;
                // Look backwards a step at the starting vessel
                if (contextVessel != null) {
                    evaluateTransfers(context.getTraversalDirection(), contextVessel);
                }
            } else {
                LabVessel.VesselEvent contextVesselEvent = context.getVesselEvent();
                contextEvent = contextVesselEvent.getLabEvent();
                if (context.getTraversalDirection() == TraversalDirection.Ancestors) {
                    contextVessel = contextVesselEvent.getSourceLabVessel();
                } else {
                    contextVessel = contextVesselEvent.getTargetLabVessel();
                }
            }
            if (contextVessel != null && contextEvent != null) {
                evaluateEvent(contextVessel, contextEvent);
            }

            return TraversalControl.ContinueTraversing;
        }

        private void evaluateTransfers(TraversalDirection traversalDirection, LabVessel vessel) {
            Set<LabEvent> transferEvents;
            if (traversalDirection == TraversalDirection.Ancestors) {
                transferEvents = vessel.getTransfersTo();
            } else {
                transferEvents = vessel.getTransfersFrom();
            }
            for (LabEvent labEvent : transferEvents) {
                evaluateEvent(vessel, labEvent);
            }
        }

        private void evaluateEvent(LabVessel vessel, LabEvent event) {
            if (materialType == event.getLabEventType().getResultingMaterialType() && vesselForMaterialType == null) {
                vesselForMaterialType = vessel;
            }
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public LabVessel getVesselForMaterialType() {
            return vesselForMaterialType;
        }
    }

    /**
     * Capture chain of events following a lab vessel
     */
    class LabEventDescendantCriteria implements TransferTraverserCriteria {

        private final Set<LabEvent> labEvents = new TreeSet<LabEvent>( LabEvent.BY_EVENT_DATE_LOC );

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            LabVessel contextVessel;
            VesselContainer contextVesselContainer = null;
            LabVessel.VesselEvent contextVesselEvent;

            // Starting traversal vessel has no event
            if( context.getHopCount() > 0 ) {
                contextVesselEvent = context.getVesselEvent();
                labEvents.add(contextVesselEvent.getLabEvent());

                if (context.getTraversalDirection() == TraversalDirection.Ancestors) {
                    if( contextVesselEvent.getSourceLabVessel() == null ) {
                        contextVesselContainer = contextVesselEvent.getSourceVesselContainer();
                        contextVessel = contextVesselContainer.getEmbedder();
                    } else {
                        contextVessel = contextVesselEvent.getSourceLabVessel();
                    }
                } else {
                    if( contextVesselEvent.getTargetLabVessel() == null ) {
                        contextVesselContainer = contextVesselEvent.getTargetVesselContainer();
                        contextVessel = contextVesselContainer.getEmbedder();
                    } else {
                        contextVessel = contextVesselEvent.getTargetLabVessel();
                    }
                }

            } else {
                contextVessel = context.getStartingLabVessel();
                if( contextVessel == null ) {
                    contextVessel = context.getStartingVesselContainer().getEmbedder();
                }
            }

            if( contextVessel != null ) {
                labEvents.addAll(contextVessel.getInPlaceLabEvents());
                for (VesselContainer containerVessel : contextVessel.getContainers()) {
                    labEvents.addAll(containerVessel.getEmbedder().getInPlaceLabEvents());
                }
            }

            // Check for in place events on vessel container (e.g. EndRepair, ABase, APWash)
            if( contextVesselContainer != null ) {
                LabVessel containerVessel = contextVesselContainer.getEmbedder();
                if (containerVessel != null) {
                    labEvents.addAll(containerVessel.getInPlaceLabEvents());

                    // Look for what comes in from the side (e.g. IndexedAdapterLigation, BaitAddition)
                    for (LabEvent containerEvent : containerVessel.getTransfersTo()) {
                        labEvents.add(containerEvent);
                        for (LabVessel ancestorLabVessel : containerEvent.getSourceLabVessels()) {
                            if( ancestorLabVessel.getContainerRole() != null ){
                                labEvents.addAll(ancestorLabVessel.getContainerRole().getEmbedder().getTransfersTo());
                            }
                        }
                    }
                }
            }

            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public Set<LabEvent> getAllEvents() {
            return labEvents;
        }

    }
}
