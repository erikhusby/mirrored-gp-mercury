package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.entity.OrmUtil;
import org.broadinstitute.sequel.entity.labevent.CherryPickTransfer;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.hibernate.annotations.Parent;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
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

    private void applyProjectPlanOverrideIfPresent(LabEvent event,
            Collection<SampleInstance> sampleInstances) {
        if (event.getProjectPlanOverride() != null) {
            for (SampleInstance sampleInstance : sampleInstances) {
                sampleInstance.resetProjectPlan(event.getProjectPlanOverride());
            }
        }
    }

    public Set<SampleInstance> getSampleInstancesAtPosition(VesselPosition position) {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        examineTransfers(position, sampleInstances);
        T vesselAtPosition = getVesselAtPosition(position);
        if(vesselAtPosition != null) {
            if(vesselAtPosition.getSampleSheetCount() != null && vesselAtPosition.getSampleSheetCount() > 0) {
                sampleInstances.addAll(vesselAtPosition.getSampleInstances());
            }
            // handle re-arrays of tubes - look in any other racks that the tube has been in
            if (vesselAtPosition.getContainersCount() != null && vesselAtPosition.getContainersCount() > 1) {
                for (VesselContainer vesselContainer : vesselAtPosition.getContainers()) {
                    if (!vesselContainer.equals(this)) {
                        vesselContainer.examineTransfers(vesselContainer.getPositionOfVessel(vesselAtPosition), sampleInstances);
                    }
                }
            }
        }
        return sampleInstances;
    }

    private void examineTransfers(VesselPosition position, Set<SampleInstance> sampleInstances) {
        Set<SampleInstance> sampleInstancesAtPosition = new HashSet<SampleInstance>();
        Set<Reagent> reagentsAtPosition = new HashSet<Reagent>();
        for (LabEvent labEvent : this.embedder.getTransfersTo()) {
            for (SectionTransfer sectionTransfer : labEvent.getSectionTransfers()) {
                VesselContainer sourceVesselContainer = sectionTransfer.getSourceVesselContainer();
                // todo jmt replace indexOf with map lookup
                VesselPosition sourcePosition = sectionTransfer.getSourceSection().getWells().get(
                        sectionTransfer.getTargetSection().getWells().indexOf(position));
                sampleInstancesAtPosition.addAll(sourceVesselContainer.getSampleInstancesAtPosition(sourcePosition));
                reagentsAtPosition.addAll(getReagentsAtPosition(sourceVesselContainer, sourcePosition));
                applyProjectPlanOverrideIfPresent(labEvent, sampleInstancesAtPosition);
            }
            for (CherryPickTransfer cherryPickTransfer : labEvent.getCherryPickTransfers()) {
                // todo jmt optimize this
                if(cherryPickTransfer.getTargetPosition().equals(position)) {
                    VesselContainer<?> sourceVesselContainer = cherryPickTransfer.getSourceVesselContainer();
                    sampleInstancesAtPosition.addAll(sourceVesselContainer.
                            getSampleInstancesAtPosition(cherryPickTransfer.getSourcePosition()));
                    reagentsAtPosition.addAll(getReagentsAtPosition(sourceVesselContainer, position));
                    applyProjectPlanOverrideIfPresent(labEvent, sampleInstancesAtPosition);
                }
            }
        }
        for (SampleInstance sampleInstance : sampleInstancesAtPosition) {
            for (Reagent reagent : reagentsAtPosition) {
                sampleInstance.addReagent(reagent);
            }
        }
        sampleInstances.addAll(sampleInstancesAtPosition);
    }

    private VesselPosition getPositionOfVessel(T vesselAtPosition) {
        // todo jmt map in both directions
        for (Map.Entry<VesselPosition, T> stringTEntry : mapPositionToVessel.entrySet()) {
            if(stringTEntry.getValue().equals(vesselAtPosition)) {
                return stringTEntry.getKey();
            }
        }
        return null;
    }

    private Set<Reagent> getReagentsAtPosition(VesselContainer vesselContainer, VesselPosition position) {
        Set<Reagent> reagents = new HashSet<Reagent>();
        LabVessel vesselAtPosition = vesselContainer.getVesselAtPosition(position);
        if (vesselAtPosition != null) {
            if (vesselAtPosition.getReagentContentsCount() != null && vesselAtPosition.getReagentContentsCount() > 0) {
                for (Reagent reagent : vesselAtPosition.getReagentContents()) {
                    reagents.add(reagent);
                }
            }
        }
        return reagents;
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
                        sampleInstances.addAll(OrmUtil.proxySafeCast(sourceLabVessel, VesselContainerEmbedder.class).getVesselContainer().getSampleInstances());
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
}
