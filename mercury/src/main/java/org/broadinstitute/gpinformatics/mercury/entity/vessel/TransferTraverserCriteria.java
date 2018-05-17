package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
public abstract class TransferTraverserCriteria {

    public enum TraversalControl {
        ContinueTraversing,
        StopTraversing
    }

    public enum TraversalDirection {
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
    public static class Context {

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
         * The vessel/event in the traversal path.
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

        /**
         * The vessel being visited in the traversal,
         *    default for ancestor traversal is source vessel, default for descendant is target vessel. <br />
         *    If a non-default is required, extract from getVesselEvent().
         * @return  The vessel being visited in the traversal, could be a container in a vessel to section transfer.
         */
        public LabVessel getContextVessel() {
            if( hopCount == 0 ) {
                return startingLabVessel;
            } else {
                if (getTraversalDirection() == TraversalDirection.Ancestors) {
                    return vesselEvent.getSourceLabVessel();
                } else {
                    return vesselEvent.getTargetLabVessel();
                }
            }
        }

        /**
         * The container being visited in the traversal,
         *    default for ancestor traversal is source container, default for descendant is target container. <br />
         *    If a non-default is required, extract from getVesselEvent().
         * @return  The container being visited in the traversal.
         */
        public VesselContainer getContextVesselContainer() {
            if( hopCount == 0 ) {
                return startingVesselContainer;
            } else {
                if (getTraversalDirection() == TraversalDirection.Ancestors) {
                    return vesselEvent.getSourceVesselContainer();
                } else {
                    return vesselEvent.getTargetVesselContainer();
                }
            }
        }

        /**
         * The vessel and its position in a container being visited in the traversal,
         *    default for ancestor traversal is source, default for descendant is target. <br />
         *    If a non-default is required, extract from getVesselEvent().
         * @return  The vessel and its position (null if not in a container) being visited in the traversal.
         */
        public Pair<LabVessel,VesselPosition> getContextVesselAndPosition(){
            if( hopCount == 0 ) {
                return Pair.of( startingLabVessel, startingVesselPosition );
            } else {
                if (getTraversalDirection() == TraversalDirection.Ancestors) {
                    return Pair.of(vesselEvent.getSourceLabVessel(), vesselEvent.getSourcePosition());
                } else {
                    return Pair.of(vesselEvent.getTargetLabVessel(), vesselEvent.getTargetPosition());
                }
            }
        }
    }

    /**
     * Keeps track of vessels and container/positions which have already been visited in the traversal tree.
     * Prevents repeatedly traversing entire tree from same vessel (e.g. 96 times for each pool)
     */
    private final Set<LabVessel> traversedVessels = new HashSet<>();
    private final Set<VesselAndPosition> traversedVesselsAndPositions = new HashSet<>();

    /**
     * Tracks vessels which have already been visited in the traversal tree.
     * @param vessel Vessel being traversed via LabVessel.evaluateCriteria call
     * @return True if vessel has been traversed
     */
    protected boolean hasVesselBeenTraversed( LabVessel vessel ) {
        return !traversedVessels.add(vessel);
    }

    /**
     * There are cases (e.g. LabVessel.findVesselsForLabEventTypes) which share the same criteria
     *   in ancestry and descendant traversals.  Prevent skipping of starting vessel when switching directions.
     */
    public void resetAllTraversed(){
        traversedVessels.clear();
        traversedVesselsAndPositions.clear();
    }

    /**
     * Tracks positions in container vessels which have already been visited in the traversal tree.
     * @param vessel Container vessel being traversed via VesselContainer.evaluateCriteria call
     * @param position Position in the container vessel
     * @return true if position in container has been traversed ( or there is no vessel at position )
     */
    protected boolean hasVesselPositionBeenTraversed( VesselContainer vessel, VesselPosition position ) {
        if( vessel == null || position == null ) {
            return true;
        }

        VesselAndPosition vesselAndPosition = new VesselAndPosition(vessel.getEmbedder(), position );
        return !traversedVesselsAndPositions.add(vesselAndPosition);
    }

    /**
     * Creates a context for traversal starting vessel.
     *
     * @param startingLabVessel  the lab vessel at the start of any traversal
     * @param startingVesselPosition  the lab vessel container position at the start of any traversal
     * @param startingVesselContainer  the lab vessel container
     * @param traversalDirection the direction of traversal
     */
    public static TransferTraverserCriteria.Context buildStartingContext(LabVessel startingLabVessel,
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

    /**
     * Callback method called before processing the next level of vessels in the traversal.
     *
     * @param context
     *
     * @return TraversalControl to either allow evaluator to continue, or stop at this level
     */
    public abstract TraversalControl evaluateVesselPreOrder(Context context);

    /**
     * Callback method called after processing the next level of vessels in the traversal.
     *
     * @param context
     */
    public abstract void evaluateVesselPostOrder(Context context);

    /**
     * Searches for nearest Lab Batch <br />
     * Despite class name, all lab batches are located, method getNearestLabBatches returns the nearest.
     */
    public static class NearestLabBatchFinder extends TransferTraverserCriteria {

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

            contextVessel = context.getContextVessel();

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

        /**
         * In theory, should only return a single lab batch, the nearest.  Not sure what use-case would produce
         * more than one lab batch at the same point in ancestry, but if so, sort by creation date, newest first.
         */
        public Collection<LabBatch> getNearestLabBatches() {
            int nearest = Integer.MAX_VALUE;
            Set<LabBatch> nearestSet = new TreeSet<>(LabBatch.byDateDesc);
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

        /**
         * Return all lab batches in ancestry, sorted by creation date, newest first.
         */
        public Collection<LabBatch> getAllLabBatches() {
            Set<LabBatch> allBatches = new TreeSet<>(LabBatch.byDateDesc);
            for (Collection<LabBatch> collection : labBatchesAtHopCount.values()) {
                allBatches.addAll(collection);
            }
            return allBatches;
        }
    }

    /**
     * Searches for nearest metric of specified type.
     */
    public static class NearestLabMetricOfTypeCriteria extends TransferTraverserCriteria {
        private LabMetric.MetricType metricType;
        private Map<Integer, List<LabMetric>> labMetricsAtHop = new HashMap<>();

        public NearestLabMetricOfTypeCriteria(LabMetric.MetricType metricType) {
            this.metricType = metricType;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            TraversalControl traversalControl = TraversalControl.ContinueTraversing;
            LabVessel contextVessel = context.getContextVessel();

            if (contextVessel != null) {
                for (LabMetric metric : contextVessel.getMetrics()) {
                    if (metric.getName() == metricType) {
                        traversalControl = TraversalControl.StopTraversing;
                        List<LabMetric> metricsAtHop = labMetricsAtHop.get(context.getHopCount());
                        if (metricsAtHop == null) {
                            metricsAtHop = new ArrayList<>();
                            labMetricsAtHop.put(context.getHopCount(), metricsAtHop);
                        }
                        metricsAtHop.add(metric);
                    }
                }
            }
            return traversalControl;
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        @Nullable
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

    public static class NearestProductOrderCriteria extends TransferTraverserCriteria {

        private final Map<Integer, Collection<String>> productOrdersAtHopCount =
                new HashMap<>();

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {

            LabVessel contextVessel = context.getContextVessel();

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

    public static class LabVesselDescendantCriteria extends TransferTraverserCriteria {
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

            if (context.getContextVessel() != null) {
                vesselList.add(context.getContextVessel());
            } else if (context.getContextVesselContainer() != null) {
                vesselList.add(context.getContextVesselContainer().getEmbedder());
            }
            labVesselAtHopCount.put(context.getHopCount(), vesselList);

            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public Collection<LabVessel> getLabVesselDescendants() {
            Set<LabVessel> descendants = new LinkedHashSet<>();
            // Vessel sets sorted by hop count
            for (List<LabVessel> vesselList : labVesselAtHopCount.values()) {
                descendants.addAll(vesselList);
            }
            return descendants;
        }
    }

    public static class LabVesselAncestorCriteria extends TransferTraverserCriteria {
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


            if ( context.getContextVessel() != null ) {
                vesselList.add(context.getContextVessel());
            } else if (context.getContextVesselContainer() != null) {
                vesselList.add(context.getContextVesselContainer().getEmbedder());
            }
            labVesselAtHopCount.put(context.getHopCount(), vesselList);

            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public Collection<LabVessel> getLabVesselAncestors() {
            LinkedHashSet<LabVessel> ancestors = new LinkedHashSet<>();
            // Vessel sets sorted by hop count
            for (List<LabVessel> vesselList : labVesselAtHopCount.values()) {
                ancestors.addAll(vesselList);
            }
            return ancestors;
        }
    }

    /**
     * Gathers abandon state for a given vessel and/or position <br/>
     * <strong>Note:</strong>  Descendant search will report all abandons in the transfers
     * - an abandon along a fork will poison the successful transfers (e.g. reworks) along another fork
     */
    public static class AbandonedLabVesselCriteria extends TransferTraverserCriteria {

        // Holds abandon state for vessels and, optionally, positions
        private MultiValuedMap<LabVessel,AbandonVessel> abandonVessels = new HashSetValuedHashMap<>();
        // Default for Infinium array Autocall is to ignore 'Depleted' abandons
        private boolean ignoreDepletedAbandons = true;

        /**
         * Default constructor (ignores Depleted abandons)
         */
        public AbandonedLabVesselCriteria( ){
            this(true);
        }

        /**
         * Provide the ability to control whether or not Depleted abandons are considered
         * @param ignoreDepletedAbandons Defaults to true, set to false to capture depleted abandons
         */
        public AbandonedLabVesselCriteria( boolean ignoreDepletedAbandons ){
            this.ignoreDepletedAbandons = ignoreDepletedAbandons;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {

            LabVessel contextVessel = context.getContextVessel();
            if (contextVessel != null) {  // Not a container
                if (contextVessel.isVesselAbandoned()) {
                    for ( AbandonVessel abandonVessel : contextVessel.getAbandonVessels() ) {
                        if( isAbandonReasonValid( abandonVessel ) ) {
                            abandonVessels.put(contextVessel, abandonVessel);
                        }
                    }
                }
            } else { // Vessel is a container
                VesselContainer vesselContainer = context.getContextVesselContainer();
                if ( vesselContainer != null ) {
                    Pair<LabVessel, VesselPosition> vesselPositionPair = context.getContextVesselAndPosition();
                    LabVessel labVessel = vesselPositionPair.getLeft();
                    VesselPosition contextVesselPosition = vesselPositionPair.getRight();
                    // Wells might not have an associated plate well vessel
                    if( labVessel == null ) {
                        labVessel = vesselContainer.getEmbedder();
                    }
                    for( AbandonVessel abandonVessel : labVessel.getAbandonVessels() ) {
                        if( abandonVessel.getVesselPosition() == contextVesselPosition
                                && isAbandonReasonValid( abandonVessel ) ) {
                            abandonVessels.put(labVessel, abandonVessel);
                            break;
                        }
                    }
                }
            }

            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        // Should we consider a vessel marked as depleted to be abandoned?
        private boolean isAbandonReasonValid(AbandonVessel abandonVessel) {
            if( ignoreDepletedAbandons && abandonVessel.getReason().equals(AbandonVessel.Reason.DEPLETED) ) {
                return false;
            } else {
                return true;
            }
        }

        // Positions on a plate.
        private boolean isPlateAbandonReasonValid(LabVessel labVessel, VesselPosition contextVesselPosition ) {
            //Multiple positions on a single plate may be abandoned with different reasons.
            for (AbandonVessel abandonVessel : labVessel.getAbandonVessels()) {
                if( contextVesselPosition == abandonVessel.getVesselPosition() ) {
                    // If the plate position is marked as depleted we do not consider it abandoned.
                    if (abandonVessel.getReason().equals(AbandonVessel.Reason.DEPLETED)) {
                        return false;
                    }
                }
            }
            return true;
        }

        public boolean  isAncestorAbandoned() {
            return !abandonVessels.isEmpty();
        }

        /**
         * Ancestry traversal abandoned vessels, and optionally, positions
         * @return A multi valued map keyed by LabVessel, value is a collection of abandoned positions in a container, null if the abandoned vessel is not a container (e.g. BarcodedTube)
         */
        public MultiValuedMap<LabVessel,AbandonVessel> getAncestorAbandonVessels(){
            return abandonVessels;
        }

    }

    /**
     * NearestTubeAncestorsCriteria is a Traverser Criteria object intended to capture the closest (in number of hops)
     * BarcodedTube(s) that can be found in a target vessel's event history.  When found, not only will the the tube
     * be saved for access, but also an object that relates the tube to its position at its found location will be
     * returned.
     */
    public static class NearestTubeAncestorsCriteria extends TransferTraverserCriteria {

        private final Set<LabVessel> tubes = new HashSet<>();
        private final Set<VesselAndPosition> vesselAndPositions = new LinkedHashSet<>();

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            // May support it, but avoid mis-match between name and function
            if (context.getTraversalDirection() != TraversalDirection.Ancestors) {
                throw new IllegalStateException("NearestTubeAncestorsCriteria supports ancestor traversal only");
            }

            Pair<LabVessel, VesselPosition> vesselPositionPair = context.getContextVesselAndPosition();
            LabVessel contextVessel = vesselPositionPair.getLeft();
            LabVessel.VesselEvent contextVesselEvent = context.getVesselEvent();
            VesselPosition contextVesselPosition = vesselPositionPair.getRight();

            if (OrmUtil.proxySafeIsInstance(contextVessel, BarcodedTube.class)) {
                tubes.add(contextVessel);
                if( contextVesselEvent != null && contextVesselPosition != null ) {
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
    public static class NearestTubeAncestorCriteria extends TransferTraverserCriteria {

        private LabVessel tube;

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            // May support it, but avoid mis-match between name and function
            if (context.getTraversalDirection() != TraversalDirection.Ancestors) {
                throw new IllegalStateException("NearestTubeAncestorCriteria supports ancestor traversal only");
            }

            LabVessel contextVessel = context.getContextVessel();

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

    public static class VesselForEventTypeCriteria extends TransferTraverserCriteria {
        private List<LabEventType> types;
        private boolean useTargetVessels = true;
        private Map<LabEvent, Set<LabVessel>> vesselsForLabEventType = new HashMap<>();
        private boolean stopAtFirstFound;

        public VesselForEventTypeCriteria(List<LabEventType> types) {
            this.types = types;
        }

        public VesselForEventTypeCriteria(List<LabEventType> types, boolean useTargetVessels ) {
            this(types);
            this.useTargetVessels = useTargetVessels;
        }

        public VesselForEventTypeCriteria(List<LabEventType> types, boolean useTargetVessels, boolean stopAtFirstFound) {
            this(types, useTargetVessels);
            this.stopAtFirstFound = stopAtFirstFound;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {

            LabVessel.VesselEvent contextVesselEvent = context.getVesselEvent();
            if( contextVesselEvent != null ) {
                if (evaluteVesselEvent(contextVesselEvent, context.getTraversalDirection()) == TraversalControl.StopTraversing) {
                    return TraversalControl.StopTraversing;
                }
            } else {
                // No VesselEvent means we're on starting vessel, process any in place events
                LabVessel contextVessel = context.getContextVessel();
                if( contextVessel == null ) {
                    contextVessel = context.getContextVesselContainer().getEmbedder();
                }
                for (LabEvent inPlaceEvent : contextVessel.getInPlaceLabEvents()) {
                    if (evaluateEvent(contextVessel, inPlaceEvent) == TraversalControl.StopTraversing) {
                        return TraversalControl.StopTraversing;
                    }
                }
            }

            return TraversalControl.ContinueTraversing;
        }

        private TraversalControl evaluteVesselEvent(LabVessel.VesselEvent contextVesselEvent, TraversalDirection traversalDirection){
            LabEvent contextEvent = contextVesselEvent.getLabEvent();

            LabVessel sourceVessel = contextVesselEvent.getSourceLabVessel();
            if( sourceVessel == null ) {
                sourceVessel = contextVesselEvent.getSourceVesselContainer().getEmbedder();
            }

            LabVessel targetVessel = contextVesselEvent.getTargetLabVessel();
            if( targetVessel == null ) {
                targetVessel = contextVesselEvent.getTargetVesselContainer().getEmbedder();
            }

            if( traversalDirection == TraversalDirection.Ancestors ) {
                for (LabEvent inPlaceEvent : sourceVessel.getInPlaceLabEvents()) {
                    if (evaluateEvent(sourceVessel, inPlaceEvent) == TraversalControl.StopTraversing) {
                        return TraversalControl.StopTraversing;
                    }
                }
                if( useTargetVessels ) {
                    // Some ancestry logic wants the event target vessel
                    if (evaluateEvent(targetVessel, contextEvent) == TraversalControl.StopTraversing) {
                        return TraversalControl.StopTraversing;
                    }
                } else {
                    // Ancestor by default uses source vessel
                    if (evaluateEvent(sourceVessel, contextEvent) == TraversalControl.StopTraversing) {
                        return TraversalControl.StopTraversing;
                    }
                }
            } else {
                for (LabEvent inPlaceEvent : targetVessel.getInPlaceLabEvents()) {
                    if (evaluateEvent(targetVessel, inPlaceEvent) == TraversalControl.StopTraversing) {
                        return TraversalControl.StopTraversing;
                    }
                }
                if (evaluateEvent(targetVessel, contextEvent) == TraversalControl.StopTraversing) {
                    return TraversalControl.StopTraversing;
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        private void addVesselForType(LabVessel vessel, LabEvent event){
            LabEvent castEvent = OrmUtil.proxySafeCast(event, LabEvent.class);
            Set<LabVessel> vessels = vesselsForLabEventType.get(castEvent);
            if (vessels == null) {
                vessels = new HashSet<>();
                vesselsForLabEventType.put(castEvent, vessels);
            }
            vessels.add(vessel);
        }

        private TraversalControl evaluateEvent(LabVessel vessel, LabEvent event) {
            if (types.contains(event.getLabEventType())) {
                addVesselForType(vessel, event);
                if (stopAtFirstFound) {
                    return TraversalControl.StopTraversing;
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public Map<LabEvent, Set<LabVessel>> getVesselsForLabEventType() {
            return vesselsForLabEventType;
        }
    }

    /**
     * Traverse LabVessels and LabEvents to find current MaterialType
     */
    public static class NearestMaterialTypeTraverserCriteria extends TransferTraverserCriteria {
        private MaterialType materialType = null;

        public NearestMaterialTypeTraverserCriteria() {
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {

            if (materialType != null) {
                return TraversalControl.StopTraversing;
            }

            LabVessel contextVessel = context.getContextVessel();
            LabVessel.VesselEvent contextVesselEvent = context.getVesselEvent();
            LabEvent contextEvent = contextVesselEvent == null?null:contextVesselEvent.getLabEvent();

            if( context.getHopCount() == 0 ) {
                // Look backwards a step at the starting vessel (or fails starting at a dead-end event)
                if (contextVessel != null) {
                    evaluateTransfers(context.getTraversalDirection(), contextVessel);
                }
            }

            if (contextEvent != null) {
                evaluateEvent(contextEvent);
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
                evaluateEvent(labEvent);
            }
        }

        private void evaluateEvent(LabEvent event) {
            MaterialType resultingMaterialType = event.getLabEventType().getResultingMaterialType();
            if (resultingMaterialType != null) {
                materialType=resultingMaterialType;
            }
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public MaterialType getMaterialType() {
            return materialType;
        }
    }

    /**
     * Capture chain of events following a lab vessel
     */
    public static class LabEventDescendantCriteria extends TransferTraverserCriteria {

        private final Set<LabEvent> labEvents = new TreeSet<>(LabEvent.BY_EVENT_DATE_LOC);
        private List<String> lcsetNames;

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            LabVessel contextVessel = context.getContextVessel();
            VesselContainer<?> contextVesselContainer = context.getContextVesselContainer();
            LabVessel.VesselEvent contextVesselEvent = context.getVesselEvent();

            if(contextVesselEvent != null ) {
                LabEvent labEvent = contextVesselEvent.getLabEvent();
                filterLcset(labEvent);
            }

            if( contextVessel != null ) {
                labEvents.addAll(contextVessel.getInPlaceLabEvents());
                for (VesselContainer<?> containerVessel : contextVessel.getVesselContainers()) {
                    // In place events may apply to containers
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
                        filterLcset(containerEvent);
                        for (LabVessel ancestorLabVessel : containerEvent.getSourceLabVessels()) {
                            if( ancestorLabVessel.getContainerRole() != null ){
                                for (LabEvent labEvent : ancestorLabVessel.getContainerRole().getEmbedder().getTransfersTo()) {
                                    filterLcset(labEvent);
                                }
                            }
                        }
                    }
                }
            }

            return TraversalControl.ContinueTraversing;
        }

        private void filterLcset(LabEvent labEvent) {
            if (lcsetNames == null) {
                labEvents.add(labEvent);
            } else {
                for (LabBatch labBatch : labEvent.getComputedLcSets()) {
                    if (lcsetNames.contains(labBatch.getBatchName())) {
                        labEvents.add(labEvent);
                        break;
                    }
                }
            }
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {
        }

        public Set<LabEvent> getAllEvents() {
            return labEvents;
        }

        public void setLcsetNames(List<String> lcsetNames) {
            this.lcsetNames = lcsetNames;
        }
    }

    public static class VesselPositionForEvent extends TransferTraverserCriteria {

        private Set<LabEventType> eventTypes;
        private LabBatch labBatch;
        private Map<LabEventType, VesselAndPosition> mapTypeToVesselPosition = new HashMap<>();

        public VesselPositionForEvent(Set<LabEventType> eventTypes) {
            this.eventTypes = eventTypes;
        }

        public VesselPositionForEvent(Set<LabEventType> eventTypes, LabBatch labBatch) {
            this.eventTypes = eventTypes;
            this.labBatch = labBatch;
        }

        @Override
        public TraversalControl evaluateVesselPreOrder(Context context) {
            if (context != null) {
                LabVessel.VesselEvent vesselEvent = context.getVesselEvent();
                if (vesselEvent != null) {
                    LabEvent labEvent = vesselEvent.getLabEvent();
                    LabEventType labEventType = labEvent.getLabEventType();
                    if (eventTypes.contains(labEventType)) {
                        if (labBatch == null || labEvent.getComputedLcSets().contains(labBatch)) {
                            mapTypeToVesselPosition.put(labEventType, new VesselAndPosition(
                                    context.getContextVesselContainer().getEmbedder(),
                                    context.getContextVesselAndPosition().getRight()));
                        }
                    }
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        @Override
        public void evaluateVesselPostOrder(Context context) {

        }

        public Map<LabEventType, VesselAndPosition> getMapTypeToVesselPosition() {
            return mapTypeToVesselPosition;
        }
    }
}
