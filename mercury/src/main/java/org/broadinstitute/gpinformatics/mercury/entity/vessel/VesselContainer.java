package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Parent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.JoinColumn;
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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
    @JoinTable(schema = "mercury", name = "lv_map_position_to_vessel"
            , joinColumns = {@JoinColumn(name = "LAB_VESSEL")}
            , inverseJoinColumns = {@JoinColumn(name = "MAP_POSITION_TO_VESSEL")})
    @MapKeyEnumerated(EnumType.STRING)
    // hbm2ddl always uses mapkey
    @MapKeyColumn(name = "mapkey")
    // No @BatchSize, fetching many containers of tubes is counterproductive
    // the map value has to be LabVessel, not T, because JPAMetaModelEntityProcessor can't handle type parameters
    private final Map<VesselPosition, LabVessel> mapPositionToVessel = new LinkedHashMap<>();

    @Transient
    private Map<LabVessel, VesselPosition> vesselToMapPosition;

    @OneToMany(mappedBy = "sourceVessel", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @BatchSize(size = 100)
    private Set<SectionTransfer> sectionTransfersFrom = new HashSet<>();

    @OneToMany(mappedBy = "targetVessel", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @BatchSize(size = 100)
    private Set<SectionTransfer> sectionTransfersTo = new HashSet<>();

    @OneToMany(mappedBy = "sourceVessel", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @BatchSize(size = 100)
    private Set<CherryPickTransfer> cherryPickTransfersFrom = new HashSet<>();

    @OneToMany(mappedBy = "targetVessel", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @BatchSize(size = 100)
    private Set<CherryPickTransfer> cherryPickTransfersTo = new HashSet<>();

    @OneToMany(mappedBy = "targetVessel", cascade = CascadeType.PERSIST)
    @BatchSize(size = 100)
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
    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
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

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    @Nullable
    public T getVesselAtPosition(VesselPosition position) {
        //noinspection unchecked
        return (T) mapPositionToVessel.get(position);
    }

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    @Nullable
    public T getImmutableVesselAtPosition(VesselPosition position) {
        LabVessel labVessel = mapPositionToVessel.get(position);
        if (labVessel == null) {
            if (mapPositionToVessel.isEmpty()) {
                //noinspection unchecked
                return (T) new ImmutableLabVessel(this, position);
            } else {
                return null;
            }
        }
        //noinspection unchecked
        return (T) labVessel;
    }

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
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
    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public Set<LabEvent> getTransfersToWithRearrays() {
        Set<LabEvent> transfersTo = getTransfersTo();
        // Need to follow Re-arrays, otherwise the chain of custody is broken.  Ignore re-arrays that add tubes
        for (LabVessel labVessel : mapPositionToVessel.values()) {
            for (VesselContainer<?> vesselContainer : labVessel.getVesselContainers()) {
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
    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
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

    public void evaluateCriteria(VesselPosition position, TransferTraverserCriteria transferTraverserCriteria,
                                 TransferTraverserCriteria.TraversalDirection traversalDirection,
                                 int hopCount) {

        if( transferTraverserCriteria.hasVesselPositionBeenTraversed(this, position)) {
            return;
        }

        TransferTraverserCriteria.Context context = null;
        T vesselAtPosition = getVesselAtPosition(position);
        boolean continueTraversing = true;

        // Have to evaluate traversal starting vessel/position without an event
        if (hopCount == 0) {
            context = TransferTraverserCriteria.buildStartingContext(
                    vesselAtPosition, position, this, traversalDirection );
            if (transferTraverserCriteria.evaluateVesselPreOrder(context) ==
                    TransferTraverserCriteria.TraversalControl.StopTraversing ) {
                continueTraversing = false;
            }
        }

        if (vesselAtPosition != null && continueTraversing) {
            // handle re-arrays of tubes - look in any other racks that the tube has been in
            if (getEmbedder() instanceof TubeFormation) {
                TubeFormation thisTubeFormation = (TubeFormation) getEmbedder();
                for (VesselContainer<?> vesselContainer : vesselAtPosition.getVesselContainers()) {
                    if (OrmUtil.proxySafeIsInstance(vesselContainer.getEmbedder(), TubeFormation.class)) {
                        TubeFormation otherTubeFormation =
                                OrmUtil.proxySafeCast(vesselContainer.getEmbedder(), TubeFormation.class);
                        if (!otherTubeFormation.getDigest().equals(thisTubeFormation.getDigest())) {
                            if (traversalDirection == TransferTraverserCriteria.TraversalDirection.Ancestors) {
                                vesselContainer.traverseAncestors(vesselContainer.getPositionOfVessel(vesselAtPosition),
                                        transferTraverserCriteria, hopCount);
                            } else {
                                vesselContainer
                                        .traverseDescendants(vesselContainer.getPositionOfVessel(vesselAtPosition),
                                                transferTraverserCriteria, hopCount);
                            }
                        }
                    }
                }
            }
        }

        if( continueTraversing ) {
            if (traversalDirection == TransferTraverserCriteria.TraversalDirection.Ancestors) {
                traverseAncestors(position, transferTraverserCriteria, hopCount);
            } else {
                traverseDescendants(position, transferTraverserCriteria, hopCount);
            }
        }

        if (hopCount == 0) {
            transferTraverserCriteria.evaluateVesselPostOrder(context);
        }
    }

    private void traverseAncestors(VesselPosition position, TransferTraverserCriteria transferTraverserCriteria,
                                   int hopCount) {
        // Target of an ancestor traversal is the current vessel/container/position
        T targetVessel = getVesselAtPosition(position);
        TransferTraverserCriteria.Context context;
        boolean continueTraversing = true;

        for (SectionTransfer sectionTransfer : sectionTransfersTo) {
            if (sectionTransfer.getTargetVesselContainer().equals(this)) {
                VesselContainer<?> sourceVesselContainer = sectionTransfer.getSourceVesselContainer();
                // todo jmt replace indexOf with map lookup
                int targetWellIndex = sectionTransfer.getTargetSection().getWells().indexOf(position);
                if (targetWellIndex < 0) {
                    // the position parameter isn't in the section, so skip the transfer
                    continue;
                }
                VesselPosition sourcePosition = sectionTransfer.getSourceSection().getWells()
                        .get(targetWellIndex);
                LabVessel.VesselEvent vesselEvent = new LabVessel.VesselEvent(
                        sourceVesselContainer.getVesselAtPosition(sourcePosition), sourceVesselContainer,
                        sourcePosition, sectionTransfer.getLabEvent(), targetVessel, this, position );
                context = TransferTraverserCriteria.buildTraversalNodeContext(
                        vesselEvent, hopCount + 1, TransferTraverserCriteria.TraversalDirection.Ancestors);
                if( !(transferTraverserCriteria.evaluateVesselPreOrder(context)
                      == TransferTraverserCriteria.TraversalControl.ContinueTraversing)  || !continueTraversing ) {
                    continueTraversing = false;
                } else {
                    sourceVesselContainer.evaluateCriteria(sourcePosition, transferTraverserCriteria,
                            TransferTraverserCriteria.TraversalDirection.Ancestors, hopCount + 1);
                }
                transferTraverserCriteria.evaluateVesselPostOrder(context);
            }
        }
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfersTo) {
            // todo jmt optimize this
            if (cherryPickTransfer.getTargetPosition() == position && cherryPickTransfer.getTargetVesselContainer()
                    .equals(this)) {
                VesselContainer<?> sourceVesselContainer = cherryPickTransfer.getSourceVesselContainer();
                VesselPosition sourcePosition = cherryPickTransfer.getSourcePosition();

                LabVessel.VesselEvent vesselEvent = new LabVessel.VesselEvent(
                        sourceVesselContainer.getVesselAtPosition(sourcePosition), sourceVesselContainer,
                        sourcePosition, cherryPickTransfer.getLabEvent(), targetVessel, this, position );
                context = TransferTraverserCriteria.buildTraversalNodeContext(
                        vesselEvent, hopCount + 1, TransferTraverserCriteria.TraversalDirection.Ancestors);
                if( !(transferTraverserCriteria.evaluateVesselPreOrder(context)
                      == TransferTraverserCriteria.TraversalControl.ContinueTraversing)  || !continueTraversing ) {
                    continueTraversing = false;
                } else {
                    sourceVesselContainer.evaluateCriteria(sourcePosition, transferTraverserCriteria,
                            TransferTraverserCriteria.TraversalDirection.Ancestors, hopCount + 1);
                }
                transferTraverserCriteria.evaluateVesselPostOrder(context);
            }
        }
        for (VesselToSectionTransfer vesselToSectionTransfer : vesselToSectionTransfersTo) {
            if (vesselToSectionTransfer.getTargetVesselContainer().equals(this)) {
                int targetWellIndex = vesselToSectionTransfer.getTargetSection().getWells().indexOf(position);
                if (targetWellIndex < 0) {
                    // the position parameter isn't in the section, so skip the transfer
                    continue;
                }

                LabVessel.VesselEvent vesselEvent = new LabVessel.VesselEvent(
                        vesselToSectionTransfer.getSourceVessel(), null,
                        null, vesselToSectionTransfer.getLabEvent(), targetVessel, this, position );
                context = TransferTraverserCriteria.buildTraversalNodeContext(
                        vesselEvent, hopCount + 1, TransferTraverserCriteria.TraversalDirection.Ancestors);
                if( !(transferTraverserCriteria.evaluateVesselPreOrder(context)
                      == TransferTraverserCriteria.TraversalControl.ContinueTraversing)  || !continueTraversing ) {
                    continueTraversing = false;
                } else {
                    vesselToSectionTransfer.getSourceVessel()
                            .evaluateCriteria(transferTraverserCriteria,
                                    TransferTraverserCriteria.TraversalDirection.Ancestors, hopCount + 1);
                }
                transferTraverserCriteria.evaluateVesselPostOrder(context);
            }
        }
        // handle VesselToVesselTransfers
        if (continueTraversing && targetVessel != null) {
            targetVessel.evaluateCriteria(transferTraverserCriteria,
                    TransferTraverserCriteria.TraversalDirection.Ancestors, hopCount + 1);
        }
    }

    private void traverseDescendants(VesselPosition sourcePosition, TransferTraverserCriteria transferTraverserCriteria,
                                     int hopCount) {
        // Source of a descendant traversal is the current vessel/container/position
        T sourceVessel = getVesselAtPosition(sourcePosition);
        TransferTraverserCriteria.Context context;
        boolean continueTraversing = true;

        for (SectionTransfer sectionTransfer : sectionTransfersFrom) {
            if (sectionTransfer.getSourceVesselContainer().equals(this)) {
                VesselContainer<?> targetVesselContainer = sectionTransfer.getTargetVesselContainer();
                // todo jmt replace indexOf with map lookup
                int sourceWellIndex = sectionTransfer.getSourceSection().getWells().indexOf(sourcePosition);
                if (sourceWellIndex < 0) {
                    // the position parameter isn't in the section, so skip the transfer
                    continue;
                }

                List<VesselPosition> wells = sectionTransfer.getTargetSection().getWells();
                if (sourceWellIndex >= wells.size()) {
                    throw new RuntimeException("For event " + sectionTransfer.getLabEvent().getLabEventId() +
                            ", source section " + sectionTransfer.getSourceSection() + " is larger than destination " +
                            sectionTransfer.getTargetSection());
                }
                VesselPosition targetPosition = wells.get(sourceWellIndex);
                LabVessel targetVessel = targetVesselContainer.getVesselAtPosition(targetPosition);
                LabVessel.VesselEvent vesselEvent = new LabVessel.VesselEvent(
                        sourceVessel, this, sourcePosition, sectionTransfer.getLabEvent(),
                        targetVessel, targetVesselContainer, targetPosition );
                context = TransferTraverserCriteria.buildTraversalNodeContext(
                        vesselEvent, hopCount + 1, TransferTraverserCriteria.TraversalDirection.Descendants);
                if( !(transferTraverserCriteria.evaluateVesselPreOrder(context) == TransferTraverserCriteria.TraversalControl.ContinueTraversing)  || !continueTraversing ) {
                    continueTraversing = false;
                } else {
                    targetVesselContainer.evaluateCriteria(targetPosition, transferTraverserCriteria, TransferTraverserCriteria.TraversalDirection.Descendants,
                            hopCount + 1);
                }
                transferTraverserCriteria.evaluateVesselPostOrder(context);
            }
        }
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfersFrom) {
            // todo jmt optimize this
            if (cherryPickTransfer.getSourcePosition() == sourcePosition
                    && cherryPickTransfer.getSourceVesselContainer().equals(this)) {
                VesselContainer<?> targetVesselContainer = cherryPickTransfer.getTargetVesselContainer();
                VesselPosition targetPosition = cherryPickTransfer.getTargetPosition();
                LabVessel targetVessel = targetVesselContainer.getVesselAtPosition(targetPosition);
                LabVessel.VesselEvent vesselEvent = new LabVessel.VesselEvent(
                        sourceVessel, this, sourcePosition, cherryPickTransfer.getLabEvent(),
                        targetVessel, targetVesselContainer, targetPosition );
                context = TransferTraverserCriteria.buildTraversalNodeContext(
                        vesselEvent, hopCount + 1, TransferTraverserCriteria.TraversalDirection.Descendants);
                if( !(transferTraverserCriteria.evaluateVesselPreOrder(context)
                      == TransferTraverserCriteria.TraversalControl.ContinueTraversing)  || !continueTraversing ) {
                    continueTraversing = false;
                } else {
                    targetVesselContainer.evaluateCriteria(targetPosition, transferTraverserCriteria,
                            TransferTraverserCriteria.TraversalDirection.Descendants, hopCount + 1);
                }
                transferTraverserCriteria.evaluateVesselPostOrder(context);
            }
        }
        // handle VesselToVesselTransfers and un-racked VesselToSectionTransfers
        if (continueTraversing && sourceVessel != null) {
            sourceVessel.evaluateCriteria(transferTraverserCriteria,
                    TransferTraverserCriteria.TraversalDirection.Descendants, hopCount + 1);
        }
    }

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
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

    /**
     * If this is a plate, this method could return
     * the {@link PlateWell wells}.  If this thing
     * is a {@link TubeFormation}, this method could
     * return the {@link BarcodedTube} tubes in
     * the rack.
     *
     * @return contained vessels
     */
    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public Set<T> getContainedVessels() {
        // Wrap in HashSet so equals works against other Sets
        //noinspection unchecked
        return new HashSet<>((Collection<? extends T>) mapPositionToVessel.values());
    }

    public void addContainedVessel(T child, VesselPosition position) {
        mapPositionToVessel.put(position, child);
        child.addToContainer(this);
    }

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
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

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public List<LabVessel.VesselEvent> getAncestors(VesselPosition targetPosition) {
        LabVessel targetVessel = getVesselAtPosition(targetPosition);

        List<LabVessel.VesselEvent> vesselEvents = new ArrayList<>();

        for (SectionTransfer sectionTransfer : sectionTransfersTo) {
            // todo jmt replace indexOf with map lookup
            int targetWellIndex = sectionTransfer.getTargetSection().getWells().indexOf(targetPosition);
            if (targetWellIndex < 0) {
                // the position parameter isn't in the section, so skip the transfer
                continue;
            }
            VesselPosition sourcePosition = sectionTransfer.getSourceSection().getWells().get(targetWellIndex);
            VesselContainer<?> sourceVesselContainer = sectionTransfer.getSourceVesselContainer();
            LabVessel sourceVessel = sourceVesselContainer.getVesselAtPosition(sourcePosition);
            LabVessel.VesselEvent transferNode =
                    new LabVessel.VesselEvent(sourceVessel, sourceVesselContainer, sourcePosition,
                            sectionTransfer.getLabEvent(), targetVessel, this, targetPosition);
            vesselEvents.add(transferNode);
        }
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfersTo) {
            // todo jmt optimize this
            if (cherryPickTransfer.getTargetPosition() == targetPosition
                    && cherryPickTransfer.getTargetVesselContainer().equals(this)) {

                VesselPosition sourcePosition = cherryPickTransfer.getSourcePosition();
                VesselContainer<?> sourceVesselContainer = cherryPickTransfer.getSourceVesselContainer();
                LabVessel sourceVessel = sourceVesselContainer.getVesselAtPosition(sourcePosition);
                LabVessel.VesselEvent transferNode =  new LabVessel.VesselEvent(sourceVessel, sourceVesselContainer,
                        sourcePosition, cherryPickTransfer.getLabEvent(), targetVessel, this, targetPosition);
                vesselEvents.add(transferNode);
            }

        }
        for (VesselToSectionTransfer vesselToSectionTransfer : vesselToSectionTransfersTo) {
            // todo jmt replace indexOf with map lookup
            int targetWellIndex = vesselToSectionTransfer.getTargetSection().getWells().indexOf(targetPosition);
            if (targetWellIndex < 0) {
                // the position parameter isn't in the section, so skip the transfer
                continue;
            }
            LabVessel.VesselEvent transferNode = new LabVessel.VesselEvent(vesselToSectionTransfer.getSourceVessel(),
                    null, null, vesselToSectionTransfer.getLabEvent(), targetVessel, this, targetPosition);
            vesselEvents.add(transferNode);

        }
        Collections.sort(vesselEvents, LabVessel.VesselEvent.COMPARE_VESSEL_EVENTS_BY_DATE);
        return vesselEvents;
    }

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public List<LabVessel.VesselEvent> getAncestors(LabVessel containee) {
        return getAncestors(getPositionOfVessel(containee));
    }

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public List<LabVessel.VesselEvent> getDescendants(VesselPosition sourcePosition) {
        LabVessel sourceVessel = getVesselAtPosition(sourcePosition);

        List<LabVessel.VesselEvent> vesselEvents = new ArrayList<>();
        for (SectionTransfer sectionTransfer : sectionTransfersFrom) {
            // todo jmt replace indexOf with map lookup
            int targetWellIndex = sectionTransfer.getSourceSection().getWells().indexOf(sourcePosition);
            if (targetWellIndex < 0) {
                // the position parameter isn't in the section, so skip the transfer
                continue;
            }
            VesselPosition targetPosition = sectionTransfer.getTargetSection().getWells().get(targetWellIndex);
            VesselContainer<?> targetVesselContainer = sectionTransfer.getTargetVesselContainer();
            LabVessel targetVessel = targetVesselContainer.getVesselAtPosition(targetPosition);
            vesselEvents.add( new LabVessel.VesselEvent( sourceVessel, this, sourcePosition,
                            sectionTransfer.getLabEvent(), targetVessel, targetVesselContainer, targetPosition) );
        }
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfersFrom) {
            // todo jmt optimize this
            if (cherryPickTransfer.getSourcePosition() == sourcePosition && cherryPickTransfer.getSourceVesselContainer()
                    .equals(this)) {
                VesselPosition targetPosition = cherryPickTransfer.getTargetPosition();
                VesselContainer<?> targetVesselContainer = cherryPickTransfer.getTargetVesselContainer();
                LabVessel targetVessel = targetVesselContainer.getVesselAtPosition(targetPosition);
                vesselEvents.add( new LabVessel.VesselEvent( sourceVessel, this, sourcePosition,
                                cherryPickTransfer.getLabEvent(), targetVessel, targetVesselContainer, targetPosition) );
            }

        }
        Collections.sort(vesselEvents, LabVessel.VesselEvent.COMPARE_VESSEL_EVENTS_BY_DATE);
        return vesselEvents;
    }

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public List<LabVessel.VesselEvent> getDescendants(LabVessel containee) {
        return getDescendants(getPositionOfVessel(containee));
    }

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
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

    public void applyCriteriaToAllPositions(TransferTraverserCriteria criteria
            , TransferTraverserCriteria.TraversalDirection direction ) {
        Iterator<String> positionNames = getEmbedder().getVesselGeometry().getPositionNames();
        while (positionNames.hasNext()) {
            String positionName = positionNames.next();
            VesselPosition vesselPosition = VesselPosition.getByName(positionName);
            evaluateCriteria(vesselPosition, criteria, direction, 0);
        }
    }

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public Collection<LabBatch> getAllLabBatches() {
        return getAllLabBatches(null);
    }

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public Collection<LabBatch> getAllLabBatches(@Nullable LabBatch.LabBatchType type) {
        TransferTraverserCriteria.NearestLabBatchFinder batchCriteria =
                new TransferTraverserCriteria.NearestLabBatchFinder(type);
        applyCriteriaToAllPositions(batchCriteria,
                TransferTraverserCriteria.TraversalDirection.Ancestors);
        return batchCriteria.getAllLabBatches();
    }

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public Collection<LabBatch> getNearestLabBatches() {
        return getNearestLabBatches(null);
    }

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public Collection<LabBatch> getNearestLabBatches(@Nullable LabBatch.LabBatchType type) {
        TransferTraverserCriteria.NearestLabBatchFinder batchCriteria =
                new TransferTraverserCriteria.NearestLabBatchFinder(type);
        applyCriteriaToAllPositions(batchCriteria,
                TransferTraverserCriteria.TraversalDirection.Ancestors);
        return batchCriteria.getNearestLabBatches();
    }

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public Collection<String> getNearestProductOrders() {
        TransferTraverserCriteria.NearestProductOrderCriteria productOrderCriteria =
                new TransferTraverserCriteria.NearestProductOrderCriteria();
        applyCriteriaToAllPositions(productOrderCriteria,
                TransferTraverserCriteria.TraversalDirection.Ancestors);
        return productOrderCriteria.getNearestProductOrders();
    }

    /**
     * This method returns a collection of the closest lab metrics given the metric type.
     *
     * @param metricType The type of metrics to return.
     *
     * @return A collection of the nearest metrics of the given type, ordered by ascending run date
     */
    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public List<LabMetric> getNearestMetricOfType(LabMetric.MetricType metricType,
            TransferTraverserCriteria.TraversalDirection traversalDirection) {
        TransferTraverserCriteria.NearestLabMetricOfTypeCriteria metricTypeCriteria =
                new TransferTraverserCriteria.NearestLabMetricOfTypeCriteria(metricType);
        applyCriteriaToAllPositions(metricTypeCriteria, traversalDirection);
        return metricTypeCriteria.getNearestMetrics();
    }

    /**
     * This method returns a collection of the closest lab metrics given the metric type and a position.
     *
     * @return A collection of the nearest metrics of the given type, ordered by ascending run date
     */
    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public List<LabMetric> getNearestMetricOfType(LabMetric.MetricType metricType, VesselPosition vesselPosition,
            TransferTraverserCriteria.TraversalDirection traversalDirection) {
        TransferTraverserCriteria.NearestLabMetricOfTypeCriteria metricTypeCriteria =
                new TransferTraverserCriteria.NearestLabMetricOfTypeCriteria(metricType);
        evaluateCriteria(vesselPosition, metricTypeCriteria, traversalDirection, 0);
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
    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public Set<LabBatch> getComputedLcSetsForSection(SBSSection section, LabEvent labEvent) {
        Map<SampleInstanceV2.LabBatchDepth, Integer> mapLabBatchToCount = new HashMap<>();
        int numVesselsWithWorkflow = 0;
        for (VesselPosition vesselPosition : section.getWells()) {
            numVesselsWithWorkflow = collateLcSets(mapLabBatchToCount, numVesselsWithWorkflow,
                    getSampleInstancesAtPositionV2(vesselPosition));
        }
        return computeLcSets(mapLabBatchToCount, numVesselsWithWorkflow, labEvent);
    }

    /**
     * Calculates the number of workflow associations for each LCSET.
     * @param mapLabBatchToCount map from LCSET to count of workflow associations, this is the primary output from
     *                           this method
     * @param numVesselsWithWorkflow number of vessels that have at least one workflow association
     * @param sampleInstancesAtPosition Sample instances to evaluate
     * @return changed numVesselsWithWorkflow
     */
    public static int collateLcSets(Map<SampleInstanceV2.LabBatchDepth, Integer> mapLabBatchToCount,
            int numVesselsWithWorkflow,
            Set<SampleInstanceV2> sampleInstancesAtPosition ) {
        Set<SampleInstanceV2.LabBatchDepth> labBatches = new HashSet<>();
        if (sampleInstancesAtPosition.size() > 1) {
            // We are dealing with a pool
            // todo jmt hardcoded depth, and -1 as an indicator, are not ideal
            for (SampleInstanceV2 sampleInstance : sampleInstancesAtPosition) {
                LabBatch labBatch = sampleInstance.getSingleBatch();
                if (labBatch == null) {
                    // Didn't find a single batch, so pick the nearest
                    List<LabBatch> allWorkflowBatches = sampleInstance.getAllWorkflowBatches();
                    if (!allWorkflowBatches.isEmpty()) {
                        labBatch = allWorkflowBatches.iterator().next();
                    }
                }
                if (labBatch != null) {
                    labBatches.add(new SampleInstanceV2.LabBatchDepth(100, labBatch));
                }
            }
            numVesselsWithWorkflow = -1;
        } else {
            for (SampleInstanceV2 sampleInstance : sampleInstancesAtPosition) {
                labBatches.addAll(sampleInstance.getAllWorkflowBatchDepths());
                if (!labBatches.isEmpty()) {
                    numVesselsWithWorkflow++;
                }
            }
        }
        for (SampleInstanceV2.LabBatchDepth labBatch : labBatches) {
            Integer count = mapLabBatchToCount.get(labBatch);
            if (count == null) {
                count = 1;
            } else {
                count = count + 1;
            }
            mapLabBatchToCount.put(labBatch, count);
        }
        return numVesselsWithWorkflow;
    }

    /**
     * Determine the common LCSET between sample instances.
     * @param mapLabBatchToCount map from LCSET to count of workflow associations
     * @param numVesselsWithWorkflow    number of vessels that have at least one workflow association
     * @return LCSET that all sample instances have in common
     */
    public static Set<LabBatch> computeLcSets(Map<SampleInstanceV2.LabBatchDepth, Integer> mapLabBatchToCount,
            int numVesselsWithWorkflow, LabEvent labEvent) {
        if (numVesselsWithWorkflow == -1) {
            // The vessels are pooled
            Set<LabBatch> lcsets = new HashSet<>();
            for (SampleInstanceV2.LabBatchDepth labBatchDepth : mapLabBatchToCount.keySet()) {
                lcsets.add(labBatchDepth.getLabBatch());
            }
            return lcsets;
        }
        List<SampleInstanceV2.LabBatchDepth> labBatchDepths = new ArrayList<>();
        if (LabVessel.DIAGNOSTICS) {
            System.out.println("numVesselsWithWorkflow " + numVesselsWithWorkflow);
        }
        for (Map.Entry<SampleInstanceV2.LabBatchDepth, Integer> labBatchIntegerEntry : mapLabBatchToCount.entrySet()) {
            if (labBatchIntegerEntry.getValue() == numVesselsWithWorkflow) {
                labBatchDepths.add(labBatchIntegerEntry.getKey());
            }
            if (LabVessel.DIAGNOSTICS) {
                System.out.println("LabBatch " + labBatchIntegerEntry.getKey().getLabBatch().getBatchName() + " count " +
                                   labBatchIntegerEntry.getValue());
            }
        }
        // If exactly the same set of tubes appears in different LCSETs at different levels, e.g. an initial set of
        // tubes at Shearing are all reworked at Hybridization, we want the deepest LCSET, Hybridization.
        Collections.sort(labBatchDepths, new Comparator<SampleInstanceV2.LabBatchDepth>() {
            @Override
            public int compare(SampleInstanceV2.LabBatchDepth o1, SampleInstanceV2.LabBatchDepth o2) {
                return o2.getDepth() - o1.getDepth();
            }
        });

        Set<LabBatch> computedLcsets = new HashSet<>();
        if (!labBatchDepths.isEmpty()) {
            int previousDepth = labBatchDepths.get(0).getDepth();
            for (SampleInstanceV2.LabBatchDepth labBatchDepth : labBatchDepths) {
                if (labBatchDepth.getDepth() == previousDepth) {
                    computedLcsets.add(labBatchDepth.getLabBatch());
                } else {
                    break;
                }
                previousDepth = labBatchDepth.getDepth();
            }
        }
        // check whether event matches any workflows unambiguously
        for (SampleInstanceV2.LabBatchDepth labBatchDepth : labBatchDepths) {
            LabBatch labBatch = labBatchDepth.getLabBatch();
            if (StringUtils.isEmpty(labBatch.getWorkflowName())) {
                continue;
            }
            ProductWorkflowDefVersion workflowDefVersion = WorkflowLoader.getWorkflowConfig().getWorkflowByName(
                    labBatch.getWorkflowName()).getEffectiveVersion(labEvent.getEventDate());
            Collection<ProductWorkflowDefVersion.LabEventNode> stepsByEventType = workflowDefVersion.findStepsByEventType(
                    labEvent.getLabEventType().getName());
            // PICO_TRANSFER occurs multiple times, so it's not useful
            if (stepsByEventType.size() == 1) {
                ProductWorkflowDefVersion.LabEventNode labEventNode = stepsByEventType.iterator().next();
                if (labEventNode != null) {
                    if (!labEventNode.getPredecessorTransfers().isEmpty() && !labEvent.getAncestorEvents().isEmpty()) {
                        Set<LabEventType> workflowTypes = labEventNode.getPredecessorTransfers().stream().
                                map(ProductWorkflowDefVersion.LabEventNode::getLabEventType).collect(Collectors.toSet());
                        Set<LabEventType> ancestorTypes = labEvent.getAncestorEvents().stream().
                                map(LabEvent::getLabEventType).collect(Collectors.toSet());
                        Set<LabEventType> intersection = workflowTypes.stream()
                                .filter(ancestorTypes::contains)
                                .collect(Collectors.toSet());
                        if (!intersection.isEmpty()) {
                            computedLcsets.clear();
                            computedLcsets.add(labBatch);
                            break;
                        }
                    }
                }
            }
        }

        return computedLcsets;
    }

    /**
     * This method gets all of the target lab vessels for the given event types.
     *
     * @param eventTypes The event types to search for vessels at.
     *
     * @return All of the lab vessels for each event type passed in keyed on event.
     */
    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public Map<LabEvent, Set<LabVessel>> getVesselsForLabEventTypes(List<LabEventType> eventTypes) {
        TransferTraverserCriteria.VesselForEventTypeCriteria vesselForEventTypeCriteria =
                new TransferTraverserCriteria.VesselForEventTypeCriteria(eventTypes);
        applyCriteriaToAllPositions(vesselForEventTypeCriteria,
                TransferTraverserCriteria.TraversalDirection.Ancestors);
        return vesselForEventTypeCriteria.getVesselsForLabEventType();
    }

    /**
     * Returns a list of the most immediate tube ancestors for each well. The "distance" from this plate across upstream
     * plate transfers is not relevant; all upstream branches are traversed until either a tube is found or the branch
     * ends.
     *
     * @return all nearest tube ancestors
     */
    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public List<VesselAndPosition> getNearestTubeAncestors() {
        TransferTraverserCriteria.NearestTubeAncestorsCriteria
                criteria = new TransferTraverserCriteria.NearestTubeAncestorsCriteria();
        applyCriteriaToAllPositions(criteria,
                TransferTraverserCriteria.TraversalDirection.Ancestors);
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
    private Map<VesselPosition, Set<SampleInstanceV2>> mapPositionToSampleInstances =
            new EnumMap<>(VesselPosition.class);

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public Set<SampleInstanceV2> getSampleInstancesAtPositionV2(VesselPosition vesselPosition) {
        Set<SampleInstanceV2> sampleInstances = mapPositionToSampleInstances.get(vesselPosition);
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
                sampleInstances = new TreeSet<>();
                if (vesselAtPosition != null) {
                    sampleInstances.add(new SampleInstanceV2(vesselAtPosition));
                }
            } else {
                sampleInstances = getAncestorSampleInstances(vesselAtPosition, ancestorEvents);
            }

            mapPositionToSampleInstances.put(vesselPosition, sampleInstances);
        }
        return sampleInstances;
    }

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public Set<SampleInstanceV2> getSampleInstancesV2() {
        Set<SampleInstanceV2> sampleInstances = new TreeSet<>();
        VesselPosition[] vesselPositions = getEmbedder().getVesselGeometry().getVesselPositions();
        for (VesselPosition vesselPosition : vesselPositions) {
            sampleInstances.addAll(getSampleInstancesAtPositionV2(vesselPosition));
        }
        return sampleInstances;
    }

    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    public Set<VesselPosition> getPositionsOfSampleInstanceV2(@Nonnull SampleInstanceV2 sampleInstance) {
        Set<VesselPosition> positions = getPositions();
        Set<VesselPosition> positionList = new HashSet<>();
        for (VesselPosition position : positions) {
            for (SampleInstanceV2 curSampleInstance : getSampleInstancesAtPositionV2(position)) {
                MercurySample curMercurySample = curSampleInstance.getRootOrEarliestMercurySample();
                if (curMercurySample != null) {
                    if (curMercurySample.equals(sampleInstance.getRootOrEarliestMercurySample())) {
                        positionList.add(position);
                    }
                }
            }
        }
        return positionList;
    }

    /**
     * Get the SampleInstances for a set of ancestor events.  Static so it can be shared with LabVessel.
     */
    @Transient  // needed here to prevent VesselContainer_.class from including this as a persisted field.
    static Set<SampleInstanceV2> getAncestorSampleInstances(LabVessel labVessel,
            List<LabVessel.VesselEvent> ancestorEvents) {
        // Get ancestor SampleInstances
        List<SampleInstanceV2> ancestorSampleInstances = new ArrayList<>();
        for (LabVessel.VesselEvent ancestor : ancestorEvents) {
            LabVessel ancestorLabVessel = ancestor.getSourceLabVessel();
            if (ancestorLabVessel == null) {
                ancestorSampleInstances.addAll(ancestor.getSourceVesselContainer().getSampleInstancesAtPositionV2(
                        ancestor.getSourcePosition()));
            } else {
                ancestorSampleInstances.addAll(ancestorLabVessel.getSampleInstancesV2());
            }
        }

        if (ancestorSampleInstances.isEmpty()) {
            return new TreeSet<>(ancestorSampleInstances);
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
            // away the bait.  Avoid merging multiple molecular indexes for an empty ancestor well.
            List<SampleInstanceV2> currentSampleInstances = new ArrayList<>();
            if (ancestorSampleInstances.isEmpty() && reagentSampleInstances.size() == 1) {
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
                    currentSampleInstance.applyVesselChanges(labVessel,null);
                }
            }

            // Apply events to clones
            for (LabVessel.VesselEvent ancestorEvent : ancestorEvents) {
                for (SampleInstanceV2 currentSampleInstance : currentSampleInstances) {
                    currentSampleInstance.applyEvent(ancestorEvent, labVessel);
                }
            }

            return new TreeSet<>(currentSampleInstances);
        }
    }

    /**
     * This is for database-free testing only, when a new transfer makes the caches stale.
     */
    public void clearCaches() {
        mapPositionToSampleInstances.clear();
    }

}
