package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
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
        private final LabVessel labVessel;

        /**
         * The container holding the lab vessel.
         * Null if the lab vessel is not in a container in the current context.
         */
        private final VesselContainer<?> vesselContainer;

        /**
         * The position of the vessel within its holding container.
         * Not null if vesselContainer is not null.
         */
        private final VesselPosition vesselPosition;

        /**
         * The event being visited.
         * Null if there is no event being processed, such as at the beginning of a traversal.
         */
        private final LabEvent event;

        /**
         * The current hop count.
         * Not null.
         */
        private final int hopCount;

        /**
         * The direction of the current traversal.
         * Not null.
         */
        private final TransferTraverserCriteria.TraversalDirection traversalDirection;

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
            vesselContainer = null;
            vesselPosition = null;
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
        public Context(LabVessel labVessel, @Nonnull VesselContainer<?> vesselContainer,
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

        public VesselContainer<?> getVesselContainer() {
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
            if (context.getLabVessel() != null) {
                Collection<LabBatch> labBatches;
                switch (associationType) {
                case GENERAL_LAB_VESSEL:
                    labBatches = context.getLabVessel().getLabBatches();
                    break;
                case DILUTION_VESSEL:
                    labBatches = new HashSet<>();
                    for (LabBatchStartingVessel labBatchStartingVessel : context.getLabVessel().getDilutionReferences()) {
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

    class NearestLabMetricOfTypeCriteria implements TransferTraverserCriteria {
        private LabMetric.MetricType metricType;
        private Map<Integer, List<LabMetric>> labMetricsAtHop = new HashMap<>();

        public NearestLabMetricOfTypeCriteria(LabMetric.MetricType metricType) {
            this.metricType = metricType;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            LabVessel vessel = context.getLabVessel();
            if (context.getLabVessel() != null) {
                for (LabMetric metric : vessel.getMetrics()) {
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
        public void evaluateVesselInOrder(Context context) {
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
            if (context.getLabVessel() != null) {
                if (context.getLabVessel().getMercurySamples() != null) {
                    for (BucketEntry bucketEntry : context.getLabVessel().getBucketEntries()) {
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
     * BarcodedTube(s) that can be found in a target vessel's event history.  When found, not only will the the tube
     * be saved for access, but also an object that relates the tube to its position at its found location will be
     * returned.
     */
    class NearestTubeAncestorsCriteria implements TransferTraverserCriteria {

        private final Set<LabVessel> tubes = new HashSet<>();
        private final Set<VesselAndPosition> vesselAndPositions = new LinkedHashSet<>();

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (OrmUtil.proxySafeIsInstance(context.getLabVessel(), BarcodedTube.class)) {
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
     * hops) BarcodedTube(s) that can be found in a target vessels history.  The user of this method has no need
     * of the tubes position in the container that it is found, and sometimes creating the VesselAndPosition object
     * breaks so this is being used in its place.
     */
    class NearestTubeAncestorCriteria implements TransferTraverserCriteria {

        private LabVessel tube;

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (OrmUtil.proxySafeIsInstance(context.getLabVessel(), BarcodedTube.class)) {
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
        public TraversalControl evaluateVesselPreOrder(
                Context context) {
            LabVessel vessel = context.getLabVessel();
            LabEvent event = context.getEvent();
            if (event != null) {
                evaluteEvent(vessel, event);
            }
            if (vessel != null) {
                //check all in place events and descendant in place events
                for (LabEvent inPlaceEvent : vessel.getInPlaceLabEvents()) {
                    evaluteEvent(vessel, inPlaceEvent);
                }
                Collection<LabVessel> traversalVessels;
                if( context.getTraversalDirection() == TraversalDirection.Ancestors ) {
                    traversalVessels = vessel.getAncestorVessels();
                } else {
                    traversalVessels = vessel.getDescendantVessels();
                }
                for (LabVessel traversalVessel : traversalVessels) {
                    Set<LabEvent> inPlaceEvents = traversalVessel.getInPlaceLabEvents();
                    for (LabEvent inPlaceEvent : inPlaceEvents) {
                        evaluteEvent(vessel, inPlaceEvent);
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
        public void evaluateVesselInOrder(Context context) {
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
        private final Collection<LabVessel.MaterialType> materialTypes;
        private final boolean useTargetVessels;
        private Map<LabVessel.MaterialType, Set<LabVessel>> vesselsForMaterialType= new HashMap<>();

        public LabEventsWithMaterialTypeTraverserCriteria(LabVessel.MaterialType... materialTypes) {
            this(new HashSet<>(Arrays.asList(materialTypes)), true);
        }

        public LabEventsWithMaterialTypeTraverserCriteria(Collection<LabVessel.MaterialType> materialTypes,
                                                          boolean useTargetVessels) {
            this.useTargetVessels = useTargetVessels;
            this.materialTypes = materialTypes;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            LabVessel vessel = context.getLabVessel();
            LabEvent event = context.getEvent();
            if (event != null) {
                evaluateEvent(vessel, event);
            }
            if (vessel != null) {
                //check all in place events and descendant in place events
                for (LabEvent inPlaceEvent : vessel.getInPlaceLabEvents()) {
                    evaluateEvent(vessel, inPlaceEvent);
                }
                Collection<LabVessel> traversalVessels;
                if( context.getTraversalDirection() == TraversalDirection.Ancestors ) {
                    traversalVessels = vessel.getAncestorVessels();
                } else {
                    traversalVessels = vessel.getDescendantVessels();
                }
                for (LabVessel traversalVessel : traversalVessels) {
                    Set<LabEvent> inPlaceEvents = traversalVessel.getInPlaceLabEvents();
                    for (LabEvent inPlaceEvent : inPlaceEvents) {
                        evaluateEvent(vessel, inPlaceEvent);
                    }
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        private void evaluateEvent(LabVessel vessel, LabEvent event) {
            LabVessel.MaterialType eventMaterialType = event.getLabEventType().getResultingMaterialType();
            if (materialTypes.contains(eventMaterialType)) {
                // If this is in place just add the vessel
                if (event.getInPlaceLabVessel() != null) {
                    Set<LabVessel> vessels = vesselsForMaterialType.get(eventMaterialType);
                    if (vessels == null) {
                        vessels = new HashSet<>();
                    }
                    vessels.add(event.getInPlaceLabVessel());
                    vesselsForMaterialType.put(eventMaterialType, vessels);
                }
                // Otherwise check the target or source vessels
                Set<LabVessel> labXferVessels;
                if( useTargetVessels ) {
                    labXferVessels = event.getTargetLabVessels();
                } else {
                    labXferVessels = event.getSourceLabVessels();
                }

                for (LabVessel targetVessel : labXferVessels) {
                    Set<LabVessel> vessels = vesselsForMaterialType.get(eventMaterialType);
                    if (vessels == null) {
                        vessels = new HashSet<>();
                    }
                    if (vessel == null) {
                        vessels.add(targetVessel);
                        vesselsForMaterialType.put(eventMaterialType, vessels);
                    } else {
                        vessels.add(vessel);
                        //if we are a container and we contain the vessel then add it
                        if (targetVessel.getContainerRole() != null
                            && targetVessel.getContainerRole().getContainedVessels().contains(vessel)) {
                            vesselsForMaterialType.put(eventMaterialType, vessels);
                        } else if (targetVessel.equals(vessel)) {
                            vesselsForMaterialType.put(eventMaterialType, vessels);
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

        public Map<LabVessel.MaterialType, Set<LabVessel>> getVesselsForMaterialType() {
            return vesselsForMaterialType;
        }
    }

    /**
     * Capture chain of events following a lab vessel
     */
    public class LabEventDescendantCriteria implements TransferTraverserCriteria {

        private int hopCount = -1;

        private final Set<LabEvent> labEvents = new TreeSet<LabEvent>( LabEvent.BY_EVENT_DATE_LOC );

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (context.getEvent() != null) {
                if(!labEvents.add(context.getEvent())) {
                    // Not sure if/how to avoid possibility of infinite looping on a circular relationship
                    // This prunes off descendant events
//                    return TraversalControl.StopTraversing;
                }
                if (context.getHopCount() > hopCount) {
                    hopCount = context.getHopCount();
                }
            }

            if( context.getLabVessel() != null ) {
                labEvents.addAll(context.getLabVessel().getInPlaceLabEvents());
                for (VesselContainer containerVessel : context.getLabVessel().getContainers()) {
                    labEvents.addAll(containerVessel.getEmbedder().getInPlaceLabEvents());
                }
            }

            // Check for in place events on vessel container (e.g. EndRepair, ABase, APWash)
            if( context.getVesselContainer() != null ) {
                LabVessel containerVessel = context.getVesselContainer().getEmbedder();
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
        public void evaluateVesselInOrder(Context context) {
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public Set<LabEvent> getAllEvents() {
            return labEvents;
        }

    }
}
