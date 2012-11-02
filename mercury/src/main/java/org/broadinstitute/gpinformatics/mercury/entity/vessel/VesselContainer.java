package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.GenericLabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    @JoinTable(schema = "mercury", name = "lv_map_position_to_vessel")
    @MapKeyEnumerated(EnumType.STRING)
    // hbm2ddl always uses mapkey
    @MapKeyColumn(name = "mapkey")
    // todo jmt get Hibernate to sort this
    // the map value has to be LabVessel, not T, because JPAMetaModelEntityProcessor can't handle type parameters
    private final Map<VesselPosition, LabVessel> mapPositionToVessel = new LinkedHashMap<VesselPosition, LabVessel>();

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
        return (T) this.mapPositionToVessel.get(position);
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

    static class NearestLabBatchFinder implements TransferTraverserCriteria {

        // index -1 is for batches for sampleInstance's starter (think BSP stock)
        private static final int STARTER_INDEX = -1;

        private final Map<Integer,Collection<LabBatch>> labBatchesAtHopCount = new HashMap<Integer, Collection<LabBatch>>();

        @Override
        public TraversalControl evaluateVessel(LabVessel labVessel, LabEvent labEvent, int hopCount) {
            if (labVessel != null) {
                Collection<LabBatch> labBatches = labVessel.getLabBatches();

                if (!labBatches.isEmpty()) {
                    if (!labBatchesAtHopCount.containsKey(hopCount)) {
                        labBatchesAtHopCount.put(hopCount,new HashSet<LabBatch>());
                    }
                    labBatchesAtHopCount.get(hopCount).addAll(labBatches);
                }
            }
            return TraversalControl.ContinueTraversing;
        }

        public Collection<LabBatch> getNearestLabBatches() {
            int nearest = Integer.MAX_VALUE;
            for (Map.Entry<Integer, Collection<LabBatch>> labBatchesForHopCount : labBatchesAtHopCount.entrySet()) {
                if (labBatchesForHopCount.getKey() < nearest) {
                    nearest = labBatchesForHopCount.getKey();
                }
            }
            return labBatchesAtHopCount.get(nearest);
        }
    }

    /**
     * Traverses transfers to find the single sample libraries.
     */
    static class SingleSampleLibraryCriteria implements TransferTraverserCriteria {
        private final Map<MercurySample, Collection<LabVessel>> singleSampleLibrariesForInstance = new HashMap<MercurySample, Collection<LabVessel>>();

        @Override
        public TraversalControl evaluateVessel(LabVessel labVessel, LabEvent labEvent, int hopCount) {
            if (labVessel != null) {
                for (SampleInstance sampleInstance : labVessel.getSampleInstances()) {
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

        public Map<MercurySample,Collection<LabVessel>> getSingleSampleLibraries() {
            return singleSampleLibrariesForInstance;
        }
    }

    public Collection<LabBatch> getNearestLabBatches(VesselPosition position) {
        NearestLabBatchFinder batchCriteria = new NearestLabBatchFinder();
        evaluateCriteria(position,batchCriteria,TraversalDirection.Ancestors,null,0);
        return batchCriteria.getNearestLabBatches();
    }

    /**
     * Traverses transfer history to find the single sample libraries, as defined
     * by the {@link org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowAnnotation}
     * for the {@link org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription}
     * @param position
     * @return
     */
    public Map<MercurySample,Collection<LabVessel>> getSingleSampleAncestors(VesselPosition position) {
        SingleSampleLibraryCriteria singleSampleLibraryCriteria = new SingleSampleLibraryCriteria();

        evaluateCriteria(position, singleSampleLibraryCriteria, TraversalDirection.Ancestors, null, 0);
        return singleSampleLibraryCriteria.getSingleSampleLibraries();
    }

    static class SampleInstanceCriteria implements TransferTraverserCriteria {

        /** Sample instances encountered during this traversal */
        private Set<SampleInstance> sampleInstances = new LinkedHashSet<SampleInstance>();
        /** Reagents encountered during this traversal */
        private Set<Reagent> reagents = new LinkedHashSet<Reagent>();
        /** Ensure that reagents are applied only once */
        private boolean reagentsApplied = false;
        /** The first lab event encountered */
        private LabEvent labEvent;

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
//                    applyProjectPlanOverrideIfPresent(labEvent, sampleInstances);
                }
            }
            if(labEvent != null && this.labEvent == null) {
                this.labEvent = labEvent;
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
                if (labEvent != null) {
                    for (SampleInstance sampleInstance : sampleInstances) {
                        MolecularState molecularState = sampleInstance.getMolecularState();
                        if(molecularState == null) {
                            GenericLabEvent genericLabEvent = OrmUtil.proxySafeCast(labEvent, GenericLabEvent.class) ;
                            LabEventType labEventType = genericLabEvent.getLabEventType();
                            molecularState = new MolecularState(labEventType.getNucleicAcidType(), labEventType.getTargetStrand());
                        }
                        sampleInstance.setMolecularState(molecularState);
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

    // todo jmt move this to LabVessel
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
                int targetWellIndex = sectionTransfer.getTargetSection().getWells().indexOf(position);
                if(targetWellIndex < 0) {
                    // the position parameter isn't in the section, so skip the transfer
                    continue;
                }
                VesselPosition sourcePosition = sectionTransfer.getSourceSection().getWells().get(
                        targetWellIndex);
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
        for (Map.Entry<VesselPosition, LabVessel> stringTEntry : mapPositionToVessel.entrySet()) {
            if(stringTEntry.getValue().equals(vesselAtPosition)) {
                return stringTEntry.getKey();
            }
        }
        return null;
    }

    @Transient
    public Set<SampleInstance> getSampleInstances() {
        Set<SampleInstance> sampleInstances = new LinkedHashSet<SampleInstance>();
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
//                        applyProjectPlanOverrideIfPresent(labEvent,sampleInstances);
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
        return (Collection<T>) this.mapPositionToVessel.values();
    }

    public void addContainedVessel(T child, VesselPosition position) {
        this.mapPositionToVessel.put(position, child);
        child.addToContainer(this);
    }

    @Transient
    public Set<VesselPosition> getPositions() {
        return this.mapPositionToVessel.keySet();
    }

    public Map<VesselPosition, T> getMapPositionToVessel() {
        return (Map<VesselPosition, T>) mapPositionToVessel;
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
