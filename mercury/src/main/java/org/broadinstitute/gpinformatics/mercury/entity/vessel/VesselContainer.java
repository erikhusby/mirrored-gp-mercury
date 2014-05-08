package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.hibernate.annotations.Parent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class represents A role of a vessel that contains other vessels, e.g. a rack of tubes, a plate of wells, or a
 * flowcell of lanes.
 * This class does not stand alone, it is embedded in a {@link LabVessel}.  This is re-use through delegation, rather than
 * through inheritance.  Delegation is preferable, because a LabVessel could have multiple roles, but multiple
 * inheritance is not supported.  As an example of multiple roles, A Cryo straw is held in a visotube, which is held
 * in a goblet; a visotube is both a container and a containee.
 */
@SuppressWarnings("UnusedDeclaration")
@Embeddable
public class VesselContainer<T extends LabVessel> {

    /**
     * Predicate that determines whether its argument represents a rack of tubes.
     */
    public static final Predicate<LabVessel> IS_LAB_VESSEL_A_RACK = new Predicate<LabVessel>() {
        @Override
        public boolean apply(@Nullable LabVessel labVessel) {
            return labVessel != null && labVessel.getType() == LabVessel.ContainerType.TUBE_FORMATION;
        }
    };

    /* rack holds tubes, tubes have barcodes and can be removed.
    * plate holds wells, wells can't be removed.
    * flowcell holds lanes.
    * PTP holds regions.
    * smartpac holds smrtcells, smrtcells are removed, but not replaced.
    * striptube holds tubes, tubes can't be removed, don't have barcodes. */
    @ManyToMany(targetEntity = LabVessel.class, cascade = CascadeType.PERSIST)
    // have to specify name, generated name is too long for Oracle
    @JoinTable(schema = "mercury", name = "lv_map_position_to_vessel")
    @MapKeyEnumerated(EnumType.STRING)
    // hbm2ddl always uses mapkey
    @MapKeyColumn(name = "mapkey")
    // the map value has to be LabVessel, not T, because JPAMetaModelEntityProcessor can't handle type parameters
    private final Map<VesselPosition, LabVessel> mapPositionToVessel = new LinkedHashMap<>();

    @Transient
    private Map<LabVessel, VesselPosition> vesselToMapPosition;

    @OneToMany(mappedBy = "sourceVessel")
    private Set<SectionTransfer> sectionTransfersFrom = new HashSet<>();

    @OneToMany(mappedBy = "targetVessel")
    private Set<SectionTransfer> sectionTransfersTo = new HashSet<>();

    @OneToMany(mappedBy = "sourceVessel")
    private Set<CherryPickTransfer> cherryPickTransfersFrom = new HashSet<>();

    @OneToMany(mappedBy = "targetVessel")
    private Set<CherryPickTransfer> cherryPickTransfersTo = new HashSet<>();

    @OneToMany(mappedBy = "targetVessel")
    private Set<VesselToSectionTransfer> vesselToSectionTransfersTo = new HashSet<>();

    @SuppressWarnings("InstanceVariableMayNotBeInitialized")
    @Parent
    private LabVessel embedder;

    public VesselContainer() {
    }

    public VesselContainer(LabVessel embedder) {
        this.embedder = embedder;
    }

    /**
     * Find the source rack for the specified LabVessel.
     */
    @Nullable
    public LabVessel getSourceRack() {

        // Find shortest paths to the first rack, if any.
        List<List<LabEvent>> listOfLabEventLists =
                shortestPathsToVesselsSatisfyingPredicate(IS_LAB_VESSEL_A_RACK);

        if (listOfLabEventLists.isEmpty()) {
            return null;
        }

        List<LabEvent> labEventList = listOfLabEventLists.iterator().next();
        // The path Lists should always be nonempty, get the last element in the List for the source rack.
        LabEvent firstEvent = labEventList.get(labEventList.size() - 1);
        // There will only be one source rack.
        return firstEvent.getSourceLabVessels().iterator().next();
    }

    @Nullable
    public T getVesselAtPosition(VesselPosition position) {
        //noinspection unchecked
        return (T) mapPositionToVessel.get(position);
    }

    public Set<LabEvent> getTransfersFrom() {
        Set<LabEvent> transfersFrom = new HashSet<>();
        for (SectionTransfer sectionTransfer : sectionTransfersFrom) {
            transfersFrom.add(sectionTransfer.getLabEvent());
        }
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfersFrom) {
            transfersFrom.add(cherryPickTransfer.getLabEvent());
        }
        return transfersFrom;
    }

    /**
     * Gets transfers to this container.  Includes re-arrays.
     *
     * @return transfers to
     */
    public Set<LabEvent> getTransfersToWithRearrays() {
        Set<LabEvent> transfersTo = getTransfersTo();
        // Need to follow Re-arrays, otherwise the chain of custody is broken.  Ignore re-arrays that add tubes
        for (LabVessel labVessel : mapPositionToVessel.values()) {
            for (VesselContainer<?> vesselContainer : labVessel.getContainers()) {
                Set<T> containedVessels = getContainedVessels();
                Set<? extends LabVessel> containedVesselsOther = vesselContainer.getContainedVessels();
                if (!vesselContainer.equals(this) && (containedVessels.containsAll(containedVesselsOther) ||
                                                      containedVesselsOther.containsAll(containedVessels))) {
                    transfersTo.addAll(vesselContainer.getTransfersTo());
                }
            }
        }

        return transfersTo;
    }

    /**
     * Gets transfers to this container.  Does not include re-arrays, to avoid recursion.
     *
     * @return transfers to
     */
    public Set<LabEvent> getTransfersTo() {
        Set<LabEvent> transfersTo = new HashSet<>();
        for (SectionTransfer sectionTransfer : sectionTransfersTo) {
            transfersTo.add(sectionTransfer.getLabEvent());
        }
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfersTo) {
            transfersTo.add(cherryPickTransfer.getLabEvent());
        }
        for (VesselToSectionTransfer vesselToSectionTransfer : vesselToSectionTransfersTo) {
            transfersTo.add(vesselToSectionTransfer.getLabEvent());
        }
        return transfersTo;
    }

    /**
     * This method gets all of the positions within this vessel that contain the sample instance passed in.
     *
     * @param sampleInstance The sample instance to search for positions of within the vessel
     *
     * @return This returns a list of vessel positions within this vessel that contain the sample instances passed in.
     */
    public Set<VesselPosition> getPositionsOfSampleInstance(@Nonnull SampleInstance sampleInstance) {
        Set<VesselPosition> positions = getPositions();
        Set<VesselPosition> positionList = new HashSet<>();
        for (VesselPosition position : positions) {
            for (SampleInstance curSampleInstance : getSampleInstancesAtPosition(position)) {
                if (curSampleInstance.getStartingSample().equals(sampleInstance.getStartingSample())) {
                    positionList.add(position);
                }
            }
        }
        return positionList;
    }

    public Set<VesselPosition> getPositionsOfSampleInstance(@Nonnull SampleInstance sampleInstance,
                                                            LabVessel.SampleType sampleType) {
        Set<VesselPosition> positions = getPositions();
        Set<VesselPosition> positionList = new HashSet<>();
        for (VesselPosition position : positions) {
            for (SampleInstance curSampleInstance : getSampleInstancesAtPosition(position, sampleType)) {
                if (curSampleInstance.getStartingSample().equals(sampleInstance.getStartingSample())) {
                    positionList.add(position);
                }
            }
        }
        return positionList;
    }

    public Set<SampleInstance> getSampleInstancesAtPosition(VesselPosition position, LabVessel.SampleType sampleType,
                                                            @Nullable LabBatch.LabBatchType batchType) {
        LabVessel.TraversalResults traversalResults = traverseAncestors(position, sampleType, batchType);
        return traversalResults.getSampleInstances();
    }

    public Set<SampleInstance> getSampleInstancesAtPosition(VesselPosition position) {
        return getSampleInstancesAtPosition(position, LabVessel.SampleType.ANY, null);
    }

    public Set<SampleInstance> getSampleInstancesAtPosition(VesselPosition position, LabVessel.SampleType sampleType) {
        return getSampleInstancesAtPosition(position, sampleType, null);
    }

    LabVessel.TraversalResults traverseAncestors(VesselPosition position, LabVessel.SampleType sampleType,
                                                 LabBatch.LabBatchType labBatchType) {
        LabVessel.TraversalResults traversalResults = new LabVessel.TraversalResults();
        T vesselAtPosition = getVesselAtPosition(position);

        if (vesselAtPosition == null) {
            List<LabVessel.VesselEvent> ancestors = getAncestors(position);
            for (LabVessel.VesselEvent ancestor : ancestors) {
                LabVessel labVessel = ancestor.getLabVessel();
                // todo jmt put this logic in VesselEvent?
                if (labVessel == null) {
                    traversalResults.add(ancestor.getVesselContainer().traverseAncestors(ancestor.getPosition(),
                            sampleType, labBatchType));
                } else {
                    traversalResults.add(labVessel.traverseAncestors(sampleType, labBatchType));
                    traversalResults.applyEvent(ancestor.getLabEvent(), labVessel);
                }
            }
        } else {
            traversalResults.add(vesselAtPosition.traverseAncestors(sampleType, labBatchType));
        }
        traversalResults.completeLevel();
        return traversalResults;
    }

    public void evaluateCriteria(VesselPosition position, TransferTraverserCriteria transferTraverserCriteria,
                                 TransferTraverserCriteria.TraversalDirection traversalDirection, LabEvent labEvent,
                                 int hopCount) {
        T vesselAtPosition = getVesselAtPosition(position);
        TransferTraverserCriteria.Context context =
                new TransferTraverserCriteria.Context(vesselAtPosition, this, position, labEvent, hopCount,
                        traversalDirection);
        TransferTraverserCriteria.TraversalControl traversalControl =
                transferTraverserCriteria.evaluateVesselPreOrder(context);
        if (vesselAtPosition != null) {
            // handle re-arrays of tubes - look in any other racks that the tube has been in
            if (getEmbedder() instanceof TubeFormation) {
                TubeFormation thisTubeFormation = (TubeFormation) getEmbedder();
                for (VesselContainer<?> vesselContainer : vesselAtPosition.getContainers()) {
                    if (OrmUtil.proxySafeIsInstance(vesselContainer.getEmbedder(), TubeFormation.class)) {
                        TubeFormation otherTubeFormation =
                                OrmUtil.proxySafeCast(vesselContainer.getEmbedder(), TubeFormation.class);
                        if (!otherTubeFormation.getDigest().equals(thisTubeFormation.getDigest())) {
                            if (traversalDirection == TransferTraverserCriteria.TraversalDirection.Ancestors) {
                                vesselContainer.traverseAncestors(vesselContainer.getPositionOfVessel(vesselAtPosition),
                                        transferTraverserCriteria, traversalDirection, hopCount);
                            } else {
                                vesselContainer
                                        .traverseDescendants(vesselContainer.getPositionOfVessel(vesselAtPosition),
                                                transferTraverserCriteria, traversalDirection, hopCount);
                            }
                        }
                    }
                }
            }
        }
        if (traversalControl == TransferTraverserCriteria.TraversalControl.ContinueTraversing) {
            if (traversalDirection == TransferTraverserCriteria.TraversalDirection.Ancestors) {
                traverseAncestors(position, transferTraverserCriteria, traversalDirection, hopCount);
            } else {
                traverseDescendants(position, transferTraverserCriteria, traversalDirection, hopCount);
            }
        }
        transferTraverserCriteria.evaluateVesselPostOrder(context);
    }

    private void traverseAncestors(VesselPosition position, TransferTraverserCriteria transferTraverserCriteria,
                                   TransferTraverserCriteria.TraversalDirection traversalDirection, int hopCount) {
        for (SectionTransfer sectionTransfer : sectionTransfersTo) {
            if (sectionTransfer.getTargetVesselContainer().equals(this)) {
                VesselContainer<?> sourceVesselContainer = sectionTransfer.getSourceVesselContainer();
                // todo jmt replace indexOf with map lookup
                int targetWellIndex = sectionTransfer.getTargetSection().getWells().indexOf(position);
                if (targetWellIndex < 0) {
                    // the position parameter isn't in the section, so skip the transfer
                    continue;
                }
                VesselPosition sourcePosition = sectionTransfer.getSourceSection().getWells().get(
                        targetWellIndex);
                sourceVesselContainer.evaluateCriteria(sourcePosition, transferTraverserCriteria, traversalDirection,
                        sectionTransfer.getLabEvent(), hopCount + 1);
            }
        }
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfersTo) {
            // todo jmt optimize this
            if (cherryPickTransfer.getTargetPosition() == position && cherryPickTransfer.getTargetVesselContainer()
                    .equals(this)) {
                VesselContainer<?> sourceVesselContainer = cherryPickTransfer.getSourceVesselContainer();
                VesselPosition sourcePosition = cherryPickTransfer.getSourcePosition();
                sourceVesselContainer.evaluateCriteria(sourcePosition, transferTraverserCriteria, traversalDirection,
                        cherryPickTransfer.getLabEvent(), hopCount + 1);
            }
        }
        for (VesselToSectionTransfer vesselToSectionTransfer : vesselToSectionTransfersTo) {
            if (vesselToSectionTransfer.getTargetVesselContainer().equals(this)) {
                int targetWellIndex = vesselToSectionTransfer.getTargetSection().getWells().indexOf(position);
                if (targetWellIndex < 0) {
                    // the position parameter isn't in the section, so skip the transfer
                    continue;
                }
                vesselToSectionTransfer.getSourceVessel()
                        .evaluateCriteria(transferTraverserCriteria, traversalDirection,
                                vesselToSectionTransfer.getLabEvent(), hopCount + 1);
            }
        }
    }

    private void traverseDescendants(VesselPosition position, TransferTraverserCriteria transferTraverserCriteria,
                                     TransferTraverserCriteria.TraversalDirection traversalDirection, int hopCount) {
        for (SectionTransfer sectionTransfer : sectionTransfersFrom) {
            if (sectionTransfer.getSourceVesselContainer().equals(this)) {
                VesselContainer<?> targetVesselContainer = sectionTransfer.getTargetVesselContainer();
                // todo jmt replace indexOf with map lookup
                int sourceWellIndex = sectionTransfer.getSourceSection().getWells().indexOf(position);
                if (sourceWellIndex < 0) {
                    // the position parameter isn't in the section, so skip the transfer
                    continue;
                }
                VesselPosition targetPosition = sectionTransfer.getTargetSection().getWells().get(
                        sourceWellIndex);
                targetVesselContainer.evaluateCriteria(targetPosition, transferTraverserCriteria, traversalDirection,
                        sectionTransfer.getLabEvent(), hopCount + 1);
            }
        }
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfersFrom) {
            // todo jmt optimize this
            if (cherryPickTransfer.getSourcePosition() == position && cherryPickTransfer.getSourceVesselContainer()
                    .equals(this)) {
                VesselContainer<?> targetVesselContainer = cherryPickTransfer.getTargetVesselContainer();
                VesselPosition targetPosition = cherryPickTransfer.getTargetPosition();
                targetVesselContainer.evaluateCriteria(targetPosition, transferTraverserCriteria, traversalDirection,
                        cherryPickTransfer.getLabEvent(), hopCount + 1);
            }
        }
        // handle VesselToVesselTransfers and un-racked VesselToSectionTransfers
        T vessel = getVesselAtPosition(position);
        if (vessel != null) {
            vessel.traverseDescendants(transferTraverserCriteria, traversalDirection, hopCount);
        }
    }

    public VesselPosition getPositionOfVessel(LabVessel vesselAtPosition) {
        //construct the reverse lookup map if it doesn't exist
        if (vesselToMapPosition == null) {
            vesselToMapPosition = new HashMap<>();
            for (Map.Entry<VesselPosition, LabVessel> stringTEntry : mapPositionToVessel.entrySet()) {
                vesselToMapPosition.put(stringTEntry.getValue(), stringTEntry.getKey());
            }
        }
        return vesselToMapPosition.get(vesselAtPosition);
    }

    public Set<SampleInstance> getSampleInstances(LabVessel.SampleType sampleType, LabBatch.LabBatchType labBatchType) {
        Set<LabVessel> sourceVessels = new HashSet<>();
        return getSampleInstances(sampleType, labBatchType, sourceVessels);
    }

    private Set<SampleInstance> getSampleInstances(LabVessel.SampleType sampleType, LabBatch.LabBatchType labBatchType,
                                                   Set<LabVessel> sourceVessels) {
        Set<SampleInstance> sampleInstances = new LinkedHashSet<>();
        for (VesselPosition position : mapPositionToVessel.keySet()) {
            sampleInstances.addAll(getSampleInstancesAtPosition(position, sampleType, labBatchType));
        }
        if (sampleInstances.isEmpty()) {
            for (LabEvent labEvent : embedder.getTransfersTo()) {
                for (LabVessel sourceLabVessel : labEvent.getSourceLabVessels()) {
                    // Breaks cyclic vessel transfer by only visiting each source vessel once per traversal.
                    if (sourceVessels.add(sourceLabVessel)) {
                        VesselContainer<?> vesselContainer = sourceLabVessel.getContainerRole();
                        if (vesselContainer != null) {
                            //noinspection unchecked
                            sampleInstances.addAll(
                                    vesselContainer.getSampleInstances(sampleType, labBatchType, sourceVessels));
                        } else {
                            sampleInstances.addAll(sourceLabVessel.getSampleInstances(sampleType, labBatchType));
                        }
                    }
                }
            }
        }
        return sampleInstances;
    }

    /**
     * If this is a plate, this method could return
     * the {@link PlateWell wells}.  If this thing
     * is a {@link TubeFormation}, this method could
     * return the {@link TwoDBarcodedTube} tubes in
     * the rack.
     *
     * @return contained vessels
     */
    public Set<T> getContainedVessels() {
        // Wrap in HashSet so equals works against other Sets
        //noinspection unchecked
        return new HashSet<>((Collection<? extends T>) mapPositionToVessel.values());
    }

    public void addContainedVessel(T child, VesselPosition position) {
        mapPositionToVessel.put(position, child);
        child.addToContainer(this);
    }

    public Set<VesselPosition> getPositions() {
        if (hasAnonymousVessels()) {
            Set<VesselPosition> positions = new HashSet<>();
            VesselPosition[] vesselPositions = getEmbedder().getVesselGeometry().getVesselPositions();
            if (vesselPositions != null) {
                positions.addAll(Arrays.asList(vesselPositions));
            }
            return positions;
        } else {
            return mapPositionToVessel.keySet();
        }
    }

    public Map<VesselPosition, T> getMapPositionToVessel() {
        //noinspection unchecked
        return (Map<VesselPosition, T>) mapPositionToVessel;
    }

    @Transient
    public LabVessel getEmbedder() {
        return embedder;
    }

    public void setEmbedder(LabVessel embedder) {
        this.embedder = embedder;
    }

    public Set<SectionTransfer> getSectionTransfersFrom() {
        return sectionTransfersFrom;
    }

    public Set<SectionTransfer> getSectionTransfersTo() {
        return sectionTransfersTo;
    }

    public Set<CherryPickTransfer> getCherryPickTransfersFrom() {
        return cherryPickTransfersFrom;
    }

    public Set<CherryPickTransfer> getCherryPickTransfersTo() {
        return cherryPickTransfersTo;
    }

    public Set<VesselToSectionTransfer> getVesselToSectionTransfersTo() {
        return vesselToSectionTransfersTo;
    }

    public List<LabVessel.VesselEvent> getAncestors(VesselPosition position) {
        List<LabVessel.VesselEvent> vesselEvents = new ArrayList<>();
        for (SectionTransfer sectionTransfer : sectionTransfersTo) {
            // todo jmt replace indexOf with map lookup
            int targetWellIndex = sectionTransfer.getTargetSection().getWells().indexOf(position);
            if (targetWellIndex < 0) {
                // the position parameter isn't in the section, so skip the transfer
                continue;
            }
            VesselPosition sourcePosition = sectionTransfer.getSourceSection().getWells().get(targetWellIndex);
            VesselContainer<?> sourceVesselContainer = sectionTransfer.getSourceVesselContainer();
            vesselEvents.add(new LabVessel.VesselEvent(sourceVesselContainer.getVesselAtPosition(sourcePosition),
                    sourceVesselContainer, sourcePosition, sectionTransfer.getLabEvent()));
        }
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfersTo) {
            // todo jmt optimize this
            if (cherryPickTransfer.getTargetPosition() == position && cherryPickTransfer.getTargetVesselContainer()
                    .equals(this)) {
                VesselPosition sourcePosition = cherryPickTransfer.getSourcePosition();
                VesselContainer<?> sourceVesselContainer = cherryPickTransfer.getSourceVesselContainer();
                vesselEvents.add(new LabVessel.VesselEvent(sourceVesselContainer.getVesselAtPosition(sourcePosition),
                        sourceVesselContainer, sourcePosition, cherryPickTransfer.getLabEvent()));
            }

        }
        for (VesselToSectionTransfer vesselToSectionTransfer : vesselToSectionTransfersTo) {
            // todo jmt replace indexOf with map lookup
            int targetWellIndex = vesselToSectionTransfer.getTargetSection().getWells().indexOf(position);
            if (targetWellIndex < 0) {
                // the position parameter isn't in the section, so skip the transfer
                continue;
            }
            vesselEvents.add(new LabVessel.VesselEvent(vesselToSectionTransfer.getSourceVessel(), null, null,
                    vesselToSectionTransfer.getLabEvent()));
        }
        Collections.sort(vesselEvents, LabVessel.VesselEvent.COMPARE_VESSEL_EVENTS_BY_DATE);
        return vesselEvents;
    }

    public List<LabVessel.VesselEvent> getAncestors(LabVessel containee) {
        return getAncestors(getPositionOfVessel(containee));
    }

    public List<LabVessel.VesselEvent> getDescendants(VesselPosition position) {
        List<LabVessel.VesselEvent> vesselEvents = new ArrayList<>();
        for (SectionTransfer sectionTransfer : sectionTransfersFrom) {
            // todo jmt replace indexOf with map lookup
            int targetWellIndex = sectionTransfer.getSourceSection().getWells().indexOf(position);
            if (targetWellIndex < 0) {
                // the position parameter isn't in the section, so skip the transfer
                continue;
            }
            VesselPosition targetPosition = sectionTransfer.getTargetSection().getWells().get(targetWellIndex);
            VesselContainer<?> targetVesselContainer = sectionTransfer.getTargetVesselContainer();
            vesselEvents.add(new LabVessel.VesselEvent(targetVesselContainer.getVesselAtPosition(targetPosition),
                    targetVesselContainer, targetPosition, sectionTransfer.getLabEvent()));
        }
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfersFrom) {
            // todo jmt optimize this
            if (cherryPickTransfer.getSourcePosition() == position && cherryPickTransfer.getSourceVesselContainer()
                    .equals(this)) {
                VesselPosition targetPosition = cherryPickTransfer.getTargetPosition();
                VesselContainer<?> targetVesselContainer = cherryPickTransfer.getTargetVesselContainer();
                vesselEvents.add(new LabVessel.VesselEvent(targetVesselContainer.getVesselAtPosition(targetPosition),
                        targetVesselContainer, targetPosition, cherryPickTransfer.getLabEvent()));
            }

        }
        Collections.sort(vesselEvents, LabVessel.VesselEvent.COMPARE_VESSEL_EVENTS_BY_DATE);
        return vesselEvents;
    }

    public List<LabVessel.VesselEvent> getDescendants(LabVessel containee) {
        return getDescendants(getPositionOfVessel(containee));
    }

    public boolean hasAnonymousVessels() {
        boolean anonymousVessels = false;
        LabVessel.ContainerType type = embedder.getType();
        if (type == LabVessel.ContainerType.STATIC_PLATE
            || type == LabVessel.ContainerType.FLOWCELL
            || type == LabVessel.ContainerType.STRIP_TUBE) {
            anonymousVessels = true;
        }
        return anonymousVessels;
    }

    public void applyCriteriaToAllPositions(TransferTraverserCriteria criteria) {
        Iterator<String> positionNames = getEmbedder().getVesselGeometry().getPositionNames();
        while (positionNames.hasNext()) {
            String positionName = positionNames.next();
            VesselPosition vesselPosition = VesselPosition.getByName(positionName);
            evaluateCriteria(vesselPosition, criteria, TransferTraverserCriteria.TraversalDirection.Ancestors, null, 0);
        }
    }

    public Collection<LabBatch> getAllLabBatches() {
        return getAllLabBatches(null);
    }

    public Collection<LabBatch> getAllLabBatches(@Nullable LabBatch.LabBatchType type) {
        TransferTraverserCriteria.NearestLabBatchFinder batchCriteria =
                new TransferTraverserCriteria.NearestLabBatchFinder(type);
        applyCriteriaToAllPositions(batchCriteria);
        return batchCriteria.getAllLabBatches();
    }

    public Collection<LabBatch> getNearestLabBatches() {
        return getNearestLabBatches(null);
    }

    public Collection<LabBatch> getNearestLabBatches(@Nullable LabBatch.LabBatchType type) {
        TransferTraverserCriteria.NearestLabBatchFinder batchCriteria =
                new TransferTraverserCriteria.NearestLabBatchFinder(type);
        applyCriteriaToAllPositions(batchCriteria);
        return batchCriteria.getNearestLabBatches();
    }

    public Collection<String> getNearestProductOrders() {
        TransferTraverserCriteria.NearestProductOrderCriteria productOrderCriteria =
                new TransferTraverserCriteria.NearestProductOrderCriteria();
        applyCriteriaToAllPositions(productOrderCriteria);
        return productOrderCriteria.getNearestProductOrders();
    }

    public List<LabBatchComposition> getLabBatchCompositions() {
        List<SampleInstance> sampleInstances = new ArrayList<>();
        for (VesselPosition position : getEmbedder().getVesselGeometry().getVesselPositions()) {
            sampleInstances.addAll(getSampleInstancesAtPosition(position));
        }

        Map<LabBatch, LabBatchComposition> batchMap = new HashMap<>();
        for (SampleInstance sampleInstance : sampleInstances) {
            for (LabBatch labBatch : sampleInstance.getAllLabBatches()) {
                LabBatchComposition batchComposition = batchMap.get(labBatch);
                if (batchComposition == null) {
                    batchMap.put(labBatch, new LabBatchComposition(labBatch, 1, sampleInstances.size()));
                } else {
                    batchComposition.addCount();
                }
            }
        }

        List<LabBatchComposition> batchList = new ArrayList<>(batchMap.values());
        Collections.sort(batchList, LabBatchComposition.HIGHEST_COUNT_FIRST);

        return batchList;
    }

    /**
     * This method returns a collection of the closest lab metrics given the metric type.
     *
     * @param metricType The type of metrics to return.
     *
     * @return A collection of the nearest metrics of the given type.
     */
    public Collection<LabMetric> getNearestMetricOfType(LabMetric.MetricType metricType) {
        TransferTraverserCriteria.NearestLabMetricOfTypeCriteria metricTypeCriteria =
                new TransferTraverserCriteria.NearestLabMetricOfTypeCriteria(metricType);
        applyCriteriaToAllPositions(metricTypeCriteria);
        return metricTypeCriteria.getNearestMetrics();
    }

    /**
     * Computes the LCSET(s) that all contained vessels have in common.  Contained vessels that don't have references
     * to LCSETs (e.g. controls) don't disturb the calculation, but they are inferred to be part of the LCSET by being
     * part of the transfer, i.e. controls are "guilty by association".
     *
     * @param section the section of interest for a transfer
     *
     * @return LCSETs, empty if no contained vessels are associated with LCSETs
     */
    public Set<LabBatch> getComputedLcSetsForSection(SBSSection section) {
        Set<LabBatch> computedLcSets = new HashSet<>();
        // find lab batch that is used by every vessel in section
        Map<LabBatch, Integer> mapLabBatchToCount = new HashMap<>();
        int numVesselsWithBucketEntries = 0;
        for (VesselPosition vesselPosition : section.getWells()) {
            T vesselAtPosition = getVesselAtPosition(vesselPosition);
            if (vesselAtPosition != null) {
                Set<BucketEntry> bucketEntries = new HashSet<>();
                for (SampleInstanceV2 sampleInstanceV2 : vesselAtPosition.getSampleInstancesV2()) {
                    bucketEntries.addAll(sampleInstanceV2.getAllBucketEntries());
                }
                if (!bucketEntries.isEmpty()) {
                    numVesselsWithBucketEntries++;
                }
                for (BucketEntry bucketEntry : bucketEntries) {
                    if (bucketEntry.getLabBatch() != null) {
                        LabBatch labBatch = bucketEntry.getLabBatch();
                        if (labBatch.getLabBatchType() == LabBatch.LabBatchType.WORKFLOW) {
                            Integer count = mapLabBatchToCount.get(labBatch);
                            if (count == null) {
                                count = 1;
                            } else {
                                count = count + 1;
                            }
                            mapLabBatchToCount.put(labBatch, count);
                        }
                    }
                }
            }
        }
        if (LabVessel.DIAGNOSTICS) {
            System.out.println("numVesselsWithBucketEntries " + numVesselsWithBucketEntries);
        }
        for (Map.Entry<LabBatch, Integer> labBatchIntegerEntry : mapLabBatchToCount.entrySet()) {
            if (labBatchIntegerEntry.getValue() == numVesselsWithBucketEntries) {
                computedLcSets.add(labBatchIntegerEntry.getKey());
            }
            if (LabVessel.DIAGNOSTICS) {
                System.out.println("LabBatch " + labBatchIntegerEntry.getKey().getBatchName() + " count " +
                        labBatchIntegerEntry.getValue());
            }
        }
        return computedLcSets;
    }

    /**
     * This method gets all of the target lab vessels for the given event types.
     *
     * @param eventTypes The event types to search for vessels at.
     *
     * @return All of the lab vessels for each event type passed in keyed on event.
     */
    public Map<LabEvent, Set<LabVessel>> getVesselsForLabEventTypes(List<LabEventType> eventTypes) {
        TransferTraverserCriteria.VesselForEventTypeCriteria vesselForEventTypeCriteria =
                new TransferTraverserCriteria.VesselForEventTypeCriteria(eventTypes);
        applyCriteriaToAllPositions(vesselForEventTypeCriteria);
        return vesselForEventTypeCriteria.getVesselsForLabEventType();
    }

    /**
     * Returns a list of the most immediate tube ancestors for each well. The "distance" from this plate across upstream
     * plate transfers is not relevant; all upstream branches are traversed until either a tube is found or the branch
     * ends.
     *
     * @return all nearest tube ancestors
     */
    public List<VesselAndPosition> getNearestTubeAncestors() {
        TransferTraverserCriteria.NearestTubeAncestorsCriteria
                criteria = new TransferTraverserCriteria.NearestTubeAncestorsCriteria();
        applyCriteriaToAllPositions(criteria);
        return new ArrayList<>(criteria.getVesselAndPositions());
    }

    /**
     * Internal utility class to abstract a time ordered sequence of {@code LabEvent} transfers.
     * LabEvents are stored in time order, oldest to newest.
     */
    private static class Path {
        /**
         *  The last LabEvent in the internal {@code List} of LabEvents is the earliest chronologically.
         */
        private final List<LabEvent> labEvents;

        /**
         * Constructor for use by calling code in creating a base Path from a single LabEvent.
         */
        private Path(@Nonnull LabEvent labEvent) {
            this(Collections.singletonList(labEvent));
        }

        /**
         * Private general purpose constructor.
         */
        private Path(@Nonnull Iterable<LabEvent> labEvents) {
            this.labEvents = ImmutableList.copyOf(labEvents);
        }

        /**
         * The earliest LabEvent chronologically is the last one in the internal {@code List}.
         */
        private LabEvent getOldestLabEvent() {
            return labEvents.get(labEvents.size() - 1);
        }

        /**
         * Return the {@code Set} of source {@code LabVessel}s on the earlier {@code LabEvent} in
         * this {@code Path}.
         */
        private Set<LabVessel> getSourceLabVessels() {
            return getOldestLabEvent().getSourceLabVessels();
        }

        /**
         * Extend this {@code Path} by the specified {@code LabEvent}, this creates a new Path
         * and does not mutate the receiver.
         */
        private Path copyAndExtendBy(@Nonnull LabEvent labEvent) {
            return new Path(Iterables.concat(labEvents, Collections.singletonList(labEvent)));
        }

        /**
         * Return a {@code List} of {@code Path}s that represent the extension of the current Path by another
         * set of older transfers ({@code LabEvent}s) to the source vessels on the oldest LabEvent.
         */
        private List<Path> extendedPaths() {
            List<Path> paths = new ArrayList<>();
            for (LabVessel labVessel : getSourceLabVessels()) {
                // Note this will not add Paths to the results that cannot be extended due to a lack of
                // transfer events into a particular VesselContainer.  This is the desired behavior when
                // a transfer history has been exhausted.
                for (LabEvent labEvent : labVessel.getContainerRole().getTransfersTo()) {
                    paths.add(copyAndExtendBy(labEvent));
                }
            }
            return paths;
        }

        private List<LabEvent> getLabEvents() {
            return labEvents;
        }

    }

    /**
     * Return only those {@code Path}s in the input {@code List} whose oldest {@code LabEvent} has at least one source
     * {@code LabVessel} that satisfies the {@code Predicate}.
     */
    private static List<Path> filterPathsBySourceLabVessels(@Nonnull List<Path> paths,
                                                            @Nonnull Predicate<LabVessel> predicate) {
        List<Path> filtered = new ArrayList<>();
        for (Path path : paths) {
            // Does the predicate apply to any of the source vessels of this Path?
            if (Iterables.any(path.getSourceLabVessels(), predicate)) {
                filtered.add(path);
            }
        }
        return filtered;
    }

    /**
     * Return a {@code List} of {@code Path}s that represent the extension of the input Paths by traversing to an
     * older generation of {@code LabEvent}s.  If a Path cannot be extended it will not be represented in the
     * output List.
     */
    private static List<Path> copyAndExtendPaths(@Nonnull List<Path> paths) {
        List<Path> ret = new ArrayList<>();
        for (Path path : paths) {
            ret.addAll(path.extendedPaths());
        }
        return ret;
    }

    /**
     * Search the {@code List} of {@code Path}s for {@code LabVessel}s in the transfer history satisfying the
     * {@code Predicate}.  This is a breadth-first search, no transfer history will be explored at a depth
     * greater than that of the shortest Path satisfying the Predicate.
     *
     * The algorithm looks for source vessels on the most recent events of each Path in {@code paths} that
     * satisfy the supplied {@code predicate}.  If any such vessels are found, the {@code Path}s leading to
     * these source vessels are returned.
     *
     * If none of the candidate source vessels on the current List of Paths satisfy the predicate, a new List
     * of Paths is built by extending the current List of Paths.  The extensions are made by traversing to the
     * transfers <b>into</b> the current set of source vessels, if such transfers exist.  If no such transfers
     * exist, a Path is not extended and eliminated from further examination.
     *
     * Having attempted to extend the current List of Paths, the new List of Paths is first examined for
     * emptiness (if none of the current Paths is extensible, the new List of Paths will be empty).
     * If the List of new Paths is empty, there are no source vessels anywhere in the transfer history of this
     * VesselContainer satisfying the predicate and an empty List is returned.  If the new List of Paths is
     * not empty, this method recurses to search the new List of Paths for source vessels satisfying the predicate.
     */
    private static List<Path> searchPathsForVesselsSatisfyingPredicate(@Nonnull List<Path> paths, @Nonnull Predicate<LabVessel> predicate) {
        // Search the sources on the last LabEvent of each path.
        List<Path> filtered = filterPathsBySourceLabVessels(paths, predicate);
        if (!filtered.isEmpty()) {
            // There were vessels at this depth satisfying the predicate, return the paths of LabEvents to
            // those vessels.
            return filtered;
        }

        // There were no vessels at this depth satisfying the predicate, so extend the paths and recurse to
        // continue checking source lab vessels.
        List<Path> extendedPaths = copyAndExtendPaths(paths);
        if (extendedPaths.isEmpty()) {
            // No paths are left to explore and none were found to satisfy the predicate.
            return Collections.emptyList();
        } else {
            // Continue recursing as the predicate has not been satisfied and there are more paths to explore.
            return searchPathsForVesselsSatisfyingPredicate(extendedPaths, predicate);
        }
    }

    /**
     * Returns the shortest Lists of LabEvents from this VesselContainer to source vessels satisfying the predicate.
     * In the event that multiple paths of equal length are found, all paths will be returned.
     *
     * The Lists of LabEvents in the returned List are ordered with the most recent LabEvents first
     * (i.e. the source LabVessel satisfying the predicate is among the sources on the last LabEvent).
     */
    public List<List<LabEvent>> shortestPathsToVesselsSatisfyingPredicate(@Nonnull Predicate<LabVessel> predicate) {
        // The initial List of Paths represents the transfers directly into this VesselContainer.
        List<Path> initialPaths = new ArrayList<>(getTransfersTo().size());
        for (LabEvent labEvent : getTransfersTo()) {
            initialPaths.add(new Path(labEvent));
        }

        List<Path> paths = searchPathsForVesselsSatisfyingPredicate(initialPaths, predicate);
        List<List<LabEvent>> result = new ArrayList<>();

        for (Path path : paths) {
            result.add(path.getLabEvents());
        }

        return result;
    }

    @Transient
    private Map<VesselPosition, List<SampleInstanceV2>> mapPositionToSampleInstances =
            new EnumMap<>(VesselPosition.class);

    public List<SampleInstanceV2> getSampleInstancesAtPositionV2(VesselPosition vesselPosition) {
        List<SampleInstanceV2> sampleInstances = mapPositionToSampleInstances.get(vesselPosition);
        if (sampleInstances == null) {
            T vesselAtPosition = getVesselAtPosition(vesselPosition);

            // Get ancestor events
            List<LabVessel.VesselEvent> ancestorEvents;
            if (vesselAtPosition == null) {
                if (mapPositionToVessel.isEmpty()) {
                    ancestorEvents = getAncestors(vesselPosition);
                } else {
                    ancestorEvents = Collections.emptyList();
                }
            } else {
                ancestorEvents = vesselAtPosition.getAncestors();
            }
            if (ancestorEvents.isEmpty()) {
                sampleInstances = Collections.emptyList();
            } else {
                sampleInstances = getAncestorSampleInstances(vesselAtPosition, ancestorEvents);
            }

            mapPositionToSampleInstances.put(vesselPosition, sampleInstances);
        }
        return sampleInstances;
    }

    public List<SampleInstanceV2> getSampleInstancesV2() {
        List<SampleInstanceV2> sampleInstanceList = new ArrayList<>();
        VesselPosition[] vesselPositions = getEmbedder().getVesselGeometry().getVesselPositions();
        for (VesselPosition vesselPosition : vesselPositions) {
            sampleInstanceList.addAll(getSampleInstancesAtPositionV2(vesselPosition));
        }
        return sampleInstanceList;
    }

    /**
     * Get the SampleInstances for a set of ancestor events.  Static so it can be shared with LabVessel.
     */
    static List<SampleInstanceV2> getAncestorSampleInstances(LabVessel labVessel,
            List<LabVessel.VesselEvent> ancestorEvents) {
        // Get ancestor SampleInstances
        List<SampleInstanceV2> ancestorSampleInstances = new ArrayList<>();
        for (LabVessel.VesselEvent ancestor : ancestorEvents) {
            LabVessel ancestorLabVessel = ancestor.getLabVessel();
            if (ancestorLabVessel == null) {
                ancestorSampleInstances.addAll(ancestor.getVesselContainer().getSampleInstancesAtPositionV2(
                        ancestor.getPosition()));
            } else {
                ancestorSampleInstances.addAll(ancestorLabVessel.getSampleInstancesV2());
            }
        }

        if (ancestorSampleInstances.isEmpty()) {
            return ancestorSampleInstances;
        } else {
            // Filter sample instances that are reagent only
            Iterator<SampleInstanceV2> iterator = ancestorSampleInstances.iterator();
            List<SampleInstanceV2> reagentSampleInstances = new ArrayList<>();
            while (iterator.hasNext()) {
                SampleInstanceV2 sampleInstance = iterator.next();
                if (sampleInstance.isReagentOnly()) {
                    reagentSampleInstances.add(sampleInstance);
                    iterator.remove();
                }
            }

            // BaitSetup has a bait in the source, but no samples in the target (until BaitAddition), so avoid throwing
            // away the bait.
            List<SampleInstanceV2> currentSampleInstances = new ArrayList<>();
            if (ancestorSampleInstances.isEmpty()) {
                currentSampleInstances.add(new SampleInstanceV2());
            } else {
                // Clone ancestors
                for (SampleInstanceV2 ancestorSampleInstance : ancestorSampleInstances) {
                    currentSampleInstances.add(new SampleInstanceV2(ancestorSampleInstance));
                }
            }

            // Apply reagents
            for (SampleInstanceV2 reagentSampleInstance : reagentSampleInstances) {
                for (Reagent reagent : reagentSampleInstance.getReagents()) {
                    for (SampleInstanceV2 currentSampleInstance : currentSampleInstances) {
                        currentSampleInstance.addReagent(reagent);
                    }
                }
            }

            // Apply vessel changes to clones
            if (labVessel != null) {
                for (SampleInstanceV2 currentSampleInstance : currentSampleInstances) {
                    currentSampleInstance.applyVesselChanges(labVessel);
                }
            }

            // Apply events to clones
            for (LabVessel.VesselEvent ancestorEvent : ancestorEvents) {
                for (SampleInstanceV2 currentSampleInstance : currentSampleInstances) {
                    currentSampleInstance.applyEvent(ancestorEvent.getLabEvent());
                }
            }

            return currentSampleInstances;
        }
    }

    /**
     * This is for database-free testing only, when a new transfer makes the caches stale.
     */
    public void clearCaches() {
        mapPositionToSampleInstances.clear();
    }

}
