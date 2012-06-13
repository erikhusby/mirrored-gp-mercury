package org.broadinstitute.sequel.entity.vessel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.entity.OrmUtil;
import org.broadinstitute.sequel.entity.labevent.CherryPickTransfer;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.workflow.LabBatch;
import org.hibernate.annotations.Parent;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A vessel that contains other vessels, e.g. a rack of tubes, a plate of wells, or a flowcell of lanes
 */
@Embeddable
public class VesselContainer<T extends LabVessel> {

    private static final Log LOG = LogFactory.getLog(VesselContainer.class);

    /* rack holds tubes, tubes have barcodes and can be removed.
    * plate holds wells, wells can't be removed.
    * flowcell holds lanes.
    * PTP holds regions.
    * smartpac holds smrtcells, smrtcells are removed, but not replaced.
    * striptube holds tubes, tubes can't be removed, don't have barcodes. */
    @ManyToMany(targetEntity = LabVessel.class, cascade = CascadeType.PERSIST)
    // have to specify name, generated name is too long for Oracle
    @JoinTable(name = "lv_map_position_to_vessel")
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "mapkey")
    private final Map<VesselPosition, T> mapPositionToVessel = new HashMap<VesselPosition, T>();

    @OneToMany(mappedBy = "sourceVessel")
    private Set<SectionTransfer> sectionTransfersFrom = new HashSet<SectionTransfer>();

    @OneToMany(mappedBy = "targetVessel")
    private Set<SectionTransfer> sectionTransfersTo = new HashSet<SectionTransfer>();

    @OneToMany(mappedBy = "sourceVessel")
    private Set<CherryPickTransfer> cherryPickTransfersFrom = new HashSet<CherryPickTransfer>();

    @OneToMany(mappedBy = "targetVessel")
    private Set<CherryPickTransfer> cherryPickTransfersTo = new HashSet<CherryPickTransfer>();

    @SuppressWarnings("InstanceVariableMayNotBeInitialized")
    @Parent
    private LabVessel embedder;

    public VesselContainer() {
    }

    public VesselContainer(LabVessel embedder) {
        this.embedder = embedder;
    }

    public T getVesselAtPosition(VesselPosition position) {
        return this.mapPositionToVessel.get(position);
    }

    private static void applyProjectPlanOverrideIfPresent(LabEvent event,
            Collection<SampleInstance> sampleInstances) {
        if (event.getProjectPlanOverride() != null) {
            for (SampleInstance sampleInstance : sampleInstances) {
                sampleInstance.resetProjectPlan(event.getProjectPlanOverride());
            }
        }
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
        return transfersTo;
    }

    public interface TransferTraverserCriteria {
        enum TraversalControl {
            ContinueTraversing,
            StopTraversing
        }
        TraversalControl evaluateVessel(LabVessel labVessel, LabEvent labEvent, int hopCount);
    }

    /**
     * Traverses transfers to find the single sample libraries.
     */
    static class SingleSampleLibraryCriteria implements TransferTraverserCriteria {
        private final Map<SampleInstance,Collection<LabVessel>> singleSampleLibrariesForInstance = new HashMap<SampleInstance, Collection<LabVessel>>();

        @Override
        public TraversalControl evaluateVessel(LabVessel labVessel, LabEvent labEvent, int hopCount) {
            for (SampleInstance sampleInstance : labVessel.getSampleInstances()) {
                if (labVessel.isSingleSampleLibrary(sampleInstance.getSingleProjectPlan().getWorkflowDescription())) {
                    if (!singleSampleLibrariesForInstance.containsKey(sampleInstance)) {
                        singleSampleLibrariesForInstance.put(sampleInstance,new HashSet<LabVessel>());
                    }
                    singleSampleLibrariesForInstance.get(sampleInstance).add(labVessel);
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        public Map<SampleInstance,Collection<LabVessel>> getSingleSampleLibraries() {
            return singleSampleLibrariesForInstance;
        }
    }

    /**
     * Traverses transfer history to find the single sample libraries, as defined
     * by the {@link org.broadinstitute.sequel.entity.workflow.WorkflowAnnotation}
     * for the {@link org.broadinstitute.sequel.entity.project.WorkflowDescription}
     * @param position
     * @return
     */
    public Map<SampleInstance,Collection<LabVessel>> getSingleSampleAncestors(VesselPosition position) {
        SingleSampleLibraryCriteria singleSampleLibraryCriteria = new SingleSampleLibraryCriteria();

        evaluateCriteria(position, singleSampleLibraryCriteria, TraversalDirection.Ancestors, null, 0);
        return singleSampleLibraryCriteria.getSingleSampleLibraries();
    }

    static class SampleInstanceCriteria implements TransferTraverserCriteria {

        private Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        private Set<Reagent> reagents = new HashSet<Reagent>();
        private boolean reagentsApplied = false;
        @Override
        public TraversalControl evaluateVessel(LabVessel labVessel, LabEvent labEvent, int hopCount) {
            // todo jmt this class shouldn't have to worry about plate wells that have no informatics contents
            if (labVessel != null) {
                if (labVessel.isSampleAuthority()) {
                    sampleInstances.addAll(labVessel.getSampleInstances());
                }
                if (labVessel.getReagentContentsCount() != null && labVessel.getReagentContentsCount() > 0) {
                    reagents.addAll(labVessel.getReagentContents());
                }
                if (labEvent != null) {
                    applyProjectPlanOverrideIfPresent(labEvent, sampleInstances);
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        public Set<SampleInstance> getSampleInstances() {
            if(!reagentsApplied) {
                reagentsApplied = true;
                for (Reagent reagent : reagents) {
                    for (SampleInstance sampleInstance : sampleInstances) {
                        sampleInstance.addReagent(reagent);
                    }
                }
            }
            return sampleInstances;
        }
    }

    public enum TraversalDirection {
        Ancestors,
        Descendants
    }

    public Set<SampleInstance> getSampleInstancesAtPosition(VesselPosition position) {

        SampleInstanceCriteria sampleInstanceCriteria = new SampleInstanceCriteria();

        evaluateCriteria(position, sampleInstanceCriteria, TraversalDirection.Ancestors, null, 0);
        return sampleInstanceCriteria.getSampleInstances();
    }

    public void evaluateCriteria(VesselPosition position, TransferTraverserCriteria transferTraverserCriteria,
            TraversalDirection traversalDirection, LabEvent labEvent, int hopCount) {
       T vesselAtPosition = getVesselAtPosition(position);
        TransferTraverserCriteria.TraversalControl traversalControl = transferTraverserCriteria.evaluateVessel(
                vesselAtPosition, labEvent, hopCount);
        if(vesselAtPosition != null) {
            // handle re-arrays of tubes - look in any other racks that the tube has been in
            if (this.getEmbedder() instanceof RackOfTubes) {
                RackOfTubes thisRackOfTubes = (RackOfTubes) this.getEmbedder();
                if (vesselAtPosition.getContainersCount() != null && vesselAtPosition.getContainersCount() > 1) {
                    for (VesselContainer vesselContainer : vesselAtPosition.getContainers()) {
                        if(OrmUtil.proxySafeIsInstance(vesselContainer.getEmbedder(), RackOfTubes.class)) {
                            RackOfTubes otherRackOfTubes = OrmUtil.proxySafeCast(vesselContainer.getEmbedder(), RackOfTubes.class);
                            if(!otherRackOfTubes.getDigest().equals(thisRackOfTubes.getDigest())) {
                                if(traversalDirection == TraversalDirection.Ancestors) {
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
        }
        if (traversalControl == TransferTraverserCriteria.TraversalControl.ContinueTraversing) {
            if(traversalDirection == TraversalDirection.Ancestors) {
                traverseAncestors(position, transferTraverserCriteria, traversalDirection, hopCount);
            } else {
                traverseDescendants(position, transferTraverserCriteria, traversalDirection, hopCount);
            }
        }
    }

    private void traverseAncestors(VesselPosition position, TransferTraverserCriteria transferTraverserCriteria,
            TraversalDirection traversalDirection, int hopCount) {
        for (SectionTransfer sectionTransfer : this.sectionTransfersTo) {
            if (sectionTransfer.getTargetVesselContainer().equals(this)) {
                VesselContainer sourceVesselContainer = sectionTransfer.getSourceVesselContainer();
                // todo jmt replace indexOf with map lookup
                 VesselPosition sourcePosition = sectionTransfer.getSourceSection().getWells().get(
                        sectionTransfer.getTargetSection().getWells().indexOf(position));
                sourceVesselContainer.evaluateCriteria(sourcePosition, transferTraverserCriteria, traversalDirection,
                        sectionTransfer.getLabEvent(), hopCount + 1);
            }
        }
        for (CherryPickTransfer cherryPickTransfer : this.cherryPickTransfersTo) {
            // todo jmt optimize this
            if(cherryPickTransfer.getTargetPosition() == position && cherryPickTransfer.getTargetVesselContainer().equals(this)) {
                VesselContainer<?> sourceVesselContainer = cherryPickTransfer.getSourceVesselContainer();
                VesselPosition sourcePosition = cherryPickTransfer.getSourcePosition();
                sourceVesselContainer.evaluateCriteria(sourcePosition, transferTraverserCriteria, traversalDirection,
                        cherryPickTransfer.getLabEvent(), hopCount + 1);
            }
        }
    }

    private void traverseDescendants(VesselPosition position, TransferTraverserCriteria transferTraverserCriteria,
            TraversalDirection traversalDirection, int hopCount) {
        for (SectionTransfer sectionTransfer : sectionTransfersFrom) {
            if (sectionTransfer.getSourceVesselContainer().equals(this)) {
                VesselContainer targetVesselContainer = sectionTransfer.getTargetVesselContainer();
                // todo jmt replace indexOf with map lookup
                VesselPosition targetPosition = sectionTransfer.getTargetSection().getWells().get(
                        sectionTransfer.getSourceSection().getWells().indexOf(position));
                targetVesselContainer.evaluateCriteria(targetPosition, transferTraverserCriteria, traversalDirection,
                        sectionTransfer.getLabEvent(), hopCount + 1);
            }
        }
        for (CherryPickTransfer cherryPickTransfer : cherryPickTransfersFrom) {
            // todo jmt optimize this
            if(cherryPickTransfer.getSourcePosition() == position && cherryPickTransfer.getSourceVesselContainer().equals(this)) {
                VesselContainer<?> targetVesselContainer = cherryPickTransfer.getTargetVesselContainer();
                VesselPosition targetPosition = cherryPickTransfer.getTargetPosition();
                targetVesselContainer.evaluateCriteria(targetPosition, transferTraverserCriteria, traversalDirection,
                        cherryPickTransfer.getLabEvent(), hopCount + 1);
            }
        }
    }

    public VesselPosition getPositionOfVessel(LabVessel vesselAtPosition) {
        // todo jmt map in both directions
        for (Map.Entry<VesselPosition, T> stringTEntry : mapPositionToVessel.entrySet()) {
            if(stringTEntry.getValue().equals(vesselAtPosition)) {
                return stringTEntry.getKey();
            }
        }
        return null;
    }

    @Transient
    public Set<SampleInstance> getSampleInstances() {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        for (VesselPosition position : this.mapPositionToVessel.keySet()) {
            sampleInstances.addAll(getSampleInstancesAtPosition(position));
        }
        if (sampleInstances.isEmpty()) {
            for (LabEvent labEvent : this.embedder.getTransfersTo()) {
                for (LabVessel sourceLabVessel : labEvent.getSourceLabVessels()) {
                    if(OrmUtil.proxySafeIsInstance(sourceLabVessel, VesselContainerEmbedder.class)) {
                        sampleInstances.addAll(OrmUtil.proxySafeCast(sourceLabVessel,
                                VesselContainerEmbedder.class).getVesselContainer().getSampleInstances());
                        // todo arz fix this, probably by using LabBatch properly
                        applyProjectPlanOverrideIfPresent(labEvent,sampleInstances);
                    }
                }
            }
        }
        return sampleInstances;
    }
    
    /**
     * If this is a plate, this method could return
     * the {@link PlateWell wells}.  If this thing
     * is a {@link RackOfTubes}, this method could
     * return the {@link TwoDBarcodedTube} tubes in
     * the rack.
     * @return contained vessels
     */
    @Transient
    public Collection<T> getContainedVessels() {
        return this.mapPositionToVessel.values();
    }

    public void addContainedVessel(T child, VesselPosition position) {
        this.mapPositionToVessel.put(position, child);
        child.addToContainer(this);
    }

    @Transient
    public Set<VesselPosition> getPositions() {
        return this.mapPositionToVessel.keySet();
    }

    Map<VesselPosition, T> getMapPositionToVessel() {
        return mapPositionToVessel;
    }

    @Transient
    public LabVessel getEmbedder() {
        return this.embedder;
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

}
