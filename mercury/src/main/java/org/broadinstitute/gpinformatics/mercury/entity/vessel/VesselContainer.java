package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.hibernate.annotations.Parent;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import java.util.*;

/**
 * A vessel that contains other vessels, e.g. a rack of tubes, a plate of wells, or a flowcell of lanes
 */
@Embeddable
public class VesselContainer<T extends LabVessel> {

//    private static final Log LOG = LogFactory.getLog(VesselContainer.class);

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
    // todo jmt get Hibernate to sort this
    // the map value has to be LabVessel, not T, because JPAMetaModelEntityProcessor can't handle type parameters
    private final Map<VesselPosition, LabVessel> mapPositionToVessel = new LinkedHashMap<VesselPosition, LabVessel>();

    @Transient
    private Map<LabVessel, VesselPosition> vesselToMapPosition = null;

    @OneToMany(mappedBy = "sourceVessel")
    private Set<SectionTransfer> sectionTransfersFrom = new HashSet<SectionTransfer>();

    @OneToMany(mappedBy = "targetVessel")
    private Set<SectionTransfer> sectionTransfersTo = new HashSet<SectionTransfer>();

    @OneToMany(mappedBy = "sourceVessel")
    private Set<CherryPickTransfer> cherryPickTransfersFrom = new HashSet<CherryPickTransfer>();

    @OneToMany(mappedBy = "targetVessel")
    private Set<CherryPickTransfer> cherryPickTransfersTo = new HashSet<CherryPickTransfer>();

    @OneToMany(mappedBy = "targetVessel")
    private Set<VesselToSectionTransfer> vesselToSectionTransfersTo = new HashSet<VesselToSectionTransfer>();

    @SuppressWarnings("InstanceVariableMayNotBeInitialized")
    @Parent
    private LabVessel embedder;

    public VesselContainer() {
    }

    public VesselContainer(LabVessel embedder) {
        this.embedder = embedder;
    }

    public T getVesselAtPosition(VesselPosition position) {
        //noinspection unchecked
        return (T) mapPositionToVessel.get(position);
    }

    public Set<LabEvent> getTransfersFrom() {
        Set<LabEvent> transfersFrom = new HashSet<LabEvent>();
        for (SectionTransfer sectionTransfer : sectionTransfersFrom) {
            transfersFrom.add(sectionTransfer.getLabEvent());
        }
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfersFrom) {
            transfersFrom.add(cherryPickTransfer.getLabEvent());
        }
        return transfersFrom;
    }

    public Set<LabEvent> getTransfersTo() {
        Set<LabEvent> transfersTo = new HashSet<LabEvent>();
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

    public Collection<LabBatch> getNearestLabBatches(VesselPosition position, @Nullable LabBatch.LabBatchType type) {
        TransferTraverserCriteria.NearestLabBatchFinder batchCriteria = new TransferTraverserCriteria.NearestLabBatchFinder(type);
        evaluateCriteria(position, batchCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors, null, 0);
        return batchCriteria.getNearestLabBatches();
    }

    /**
     * Traverses transfer history to find the single sample libraries, as defined
     * by the WorkflowAnnotation for the WorkflowDescription
     *
     * @param position
     * @return
     */
    public Map<MercurySample, Collection<LabVessel>> getSingleSampleAncestors(VesselPosition position) {
        TransferTraverserCriteria.SingleSampleLibraryCriteria singleSampleLibraryCriteria = new TransferTraverserCriteria.SingleSampleLibraryCriteria();

        evaluateCriteria(position, singleSampleLibraryCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors, null, 0);
        return singleSampleLibraryCriteria.getSingleSampleLibraries();
    }

    public Set<SampleInstance> getSampleInstancesAtPosition(VesselPosition position) {
        LabVessel.TraversalResults traversalResults = traverseAncestors(position);
        return traversalResults.getSampleInstances();
    }

    public List<SampleInstance> getSampleInstancesAtPositionList(VesselPosition position) {
        LabVessel.TraversalResults traversalResults = traverseAncestors(position);
        Map<String, SampleInstance> sampleInstanceMap = new TreeMap<String, SampleInstance>();
        for (SampleInstance sample : traversalResults.getSampleInstances()) {
            sampleInstanceMap.put(sample.getStartingSample().getSampleKey(), sample);
        }
        return new ArrayList<SampleInstance>(sampleInstanceMap.values());
    }

    LabVessel.TraversalResults traverseAncestors(VesselPosition position) {
        LabVessel.TraversalResults traversalResults = new LabVessel.TraversalResults();
        T vesselAtPosition = getVesselAtPosition(position);

        if (vesselAtPosition == null) {
            List<LabVessel.VesselEvent> ancestors = getAncestors(position);
            for (LabVessel.VesselEvent ancestor : ancestors) {
                LabVessel labVessel = ancestor.getLabVessel();
                // todo jmt put this logic in VesselEvent?
                if (labVessel == null) {
                    traversalResults.add(ancestor.getVesselContainer().traverseAncestors(ancestor.getPosition()));
                } else {
                    traversalResults.add(labVessel.traverseAncestors());
                }
            }
        } else {
            traversalResults.add(vesselAtPosition.traverseAncestors());
        }
        traversalResults.completeLevel();
        return traversalResults;
    }

    public void evaluateCriteria(VesselPosition position, TransferTraverserCriteria transferTraverserCriteria,
                                 TransferTraverserCriteria.TraversalDirection traversalDirection, LabEvent labEvent, int hopCount) {
        T vesselAtPosition = getVesselAtPosition(position);
        TransferTraverserCriteria.Context context = new TransferTraverserCriteria.Context(vesselAtPosition, this, position, labEvent, hopCount, traversalDirection);
        TransferTraverserCriteria.TraversalControl traversalControl = transferTraverserCriteria.evaluateVesselPreOrder(context);
        if (vesselAtPosition != null) {
            // handle re-arrays of tubes - look in any other racks that the tube has been in
            if (getEmbedder() instanceof TubeFormation) {
                TubeFormation thisTubeFormation = (TubeFormation) getEmbedder();
                for (VesselContainer<?> vesselContainer : vesselAtPosition.getContainers()) {
                    if (OrmUtil.proxySafeIsInstance(vesselContainer.getEmbedder(), TubeFormation.class)) {
                        TubeFormation otherTubeFormation = OrmUtil.proxySafeCast(vesselContainer.getEmbedder(), TubeFormation.class);
                        if (!otherTubeFormation.getDigest().equals(thisTubeFormation.getDigest())) {
                            if (traversalDirection == TransferTraverserCriteria.TraversalDirection.Ancestors) {
                                vesselContainer.traverseAncestors(vesselContainer.getPositionOfVessel(vesselAtPosition),
                                        transferTraverserCriteria, traversalDirection, hopCount);
                            } else {
                                vesselContainer.traverseDescendants(vesselContainer.getPositionOfVessel(vesselAtPosition),
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
            if (cherryPickTransfer.getTargetPosition() == position && cherryPickTransfer.getTargetVesselContainer().equals(this)) {
                VesselContainer<?> sourceVesselContainer = cherryPickTransfer.getSourceVesselContainer();
                VesselPosition sourcePosition = cherryPickTransfer.getSourcePosition();
                sourceVesselContainer.evaluateCriteria(sourcePosition, transferTraverserCriteria, traversalDirection,
                        cherryPickTransfer.getLabEvent(), hopCount + 1);
            }
        }
        for (VesselToSectionTransfer vesselToSectionTransfer : vesselToSectionTransfersTo) {
            if (vesselToSectionTransfer.getTargetVesselContainer().equals(this)) {
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
                VesselPosition targetPosition = sectionTransfer.getTargetSection().getWells().get(
                        sectionTransfer.getSourceSection().getWells().indexOf(position));
                targetVesselContainer.evaluateCriteria(targetPosition, transferTraverserCriteria, traversalDirection,
                        sectionTransfer.getLabEvent(), hopCount + 1);
            }
        }
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfersFrom) {
            // todo jmt optimize this
            if (cherryPickTransfer.getSourcePosition() == position && cherryPickTransfer.getSourceVesselContainer().equals(this)) {
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
            vesselToMapPosition = new HashMap<LabVessel, VesselPosition>();
            for (Map.Entry<VesselPosition, LabVessel> stringTEntry : mapPositionToVessel.entrySet()) {
                vesselToMapPosition.put(stringTEntry.getValue(), stringTEntry.getKey());
            }
        }
        return vesselToMapPosition.get(vesselAtPosition);
    }

    @Transient
    public Set<SampleInstance> getSampleInstances() {
        Set<SampleInstance> sampleInstances = new LinkedHashSet<SampleInstance>();
        for (VesselPosition position : mapPositionToVessel.keySet()) {
            sampleInstances.addAll(getSampleInstancesAtPosition(position));
        }
        if (sampleInstances.isEmpty()) {
            for (LabEvent labEvent : embedder.getTransfersTo()) {
                for (LabVessel sourceLabVessel : labEvent.getSourceLabVessels()) {
                    VesselContainer<?> vesselContainer = sourceLabVessel.getContainerRole();
                    if (vesselContainer != null) {
                        //noinspection unchecked
                        sampleInstances.addAll(vesselContainer.getSampleInstances());
                        // todo arz fix this, probably by using LabBatch properly
//                        applyProjectPlanOverrideIfPresent(labEvent,sampleInstances);
                    } else {
                        sampleInstances.addAll(sourceLabVessel.getSampleInstances());
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
    @Transient
    public Collection<T> getContainedVessels() {
        //noinspection unchecked
        return (Collection<T>) mapPositionToVessel.values();
    }

    public void addContainedVessel(T child, VesselPosition position) {
        mapPositionToVessel.put(position, child);
        child.addToContainer(this);
    }

    @Transient
    public Set<VesselPosition> getPositions() {
        return mapPositionToVessel.keySet();
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
        List<LabVessel.VesselEvent> vesselEvents = new ArrayList<LabVessel.VesselEvent>();
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
            if (cherryPickTransfer.getTargetPosition() == position && cherryPickTransfer.getTargetVesselContainer().equals(this)) {
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
        return vesselEvents;
    }

    public List<LabVessel.VesselEvent> getAncestors(LabVessel containee) {
        return getAncestors(getPositionOfVessel(containee));
    }

    public List<LabVessel.VesselEvent> getDescendants(VesselPosition position) {
        List<LabVessel.VesselEvent> vesselEvents = new ArrayList<LabVessel.VesselEvent>();
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
            if (cherryPickTransfer.getSourcePosition() == position && cherryPickTransfer.getSourceVesselContainer().equals(this)) {
                VesselPosition targetPosition = cherryPickTransfer.getTargetPosition();
                VesselContainer<?> targetVesselContainer = cherryPickTransfer.getTargetVesselContainer();
                vesselEvents.add(new LabVessel.VesselEvent(targetVesselContainer.getVesselAtPosition(targetPosition),
                        targetVesselContainer, targetPosition, cherryPickTransfer.getLabEvent()));
            }

        }
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


}
