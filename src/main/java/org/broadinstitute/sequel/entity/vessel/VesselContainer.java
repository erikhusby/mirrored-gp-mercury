package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.entity.labevent.CherryPickTransfer;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.hibernate.annotations.Parent;

import javax.persistence.Embeddable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A vessel that contains other vessels, e.g. a rack of tubes, a plate of wells, or a flowcell of lanes
 */
@SuppressWarnings("rawtypes")
@Embeddable
public class VesselContainer<T extends LabVessel> {
    /* rack holds tubes, tubes have barcodes and can be removed.
    * plate holds wells, wells can't be removed.
    * flowcell holds lanes.
    * PTP holds regions.
    * smartpac holds smrtcells, smrtcells are removed, but not replaced.
    * striptube holds tubes, tubes can't be removed, don't have barcodes. */
    private Map<String, T> mapPositionToVessel = new HashMap<String, T>();

    private Set<VesselContainer> sampleSheetAuthorities = new HashSet<VesselContainer>();

    @SuppressWarnings("InstanceVariableMayNotBeInitialized")
    @Parent
    private LabVessel embedder;

    public VesselContainer() {
    }

    public VesselContainer(LabVessel embedder) {
        this.embedder = embedder;
    }

    // todo jmt enum for positions
    public T getVesselAtPosition(String position) {
        return this.mapPositionToVessel.get(position);
    }
    
    public Set<SampleInstance> getSampleInstancesAtPosition(String position) {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        if(this.sampleSheetAuthorities.isEmpty()) {
            for (LabEvent labEvent : this.embedder.getTransfersTo()) {
                for (SectionTransfer sectionTransfer : labEvent.getSectionTransfers()) {
                    sampleInstances.addAll(sectionTransfer.getSourceVesselContainer().getSampleInstancesAtPosition(position));
                }
                for (CherryPickTransfer cherryPickTransfer : labEvent.getCherryPickTransfers()) {
                    if(cherryPickTransfer.getTargetPosition().equals(position)) {
                        sampleInstances.addAll(cherryPickTransfer.getSourceVesselContainer().getSampleInstancesAtPosition(cherryPickTransfer.getSourcePosition()));
                    }
                }
            }
            T vesselAtPosition = getVesselAtPosition(position);
            if(vesselAtPosition != null && !vesselAtPosition.getSampleSheets().isEmpty()) {
                sampleInstances.addAll(vesselAtPosition.getSampleInstances());
            }
        } else {
            for (VesselContainer sampleSheetAuthority : this.sampleSheetAuthorities) {
                Set<SampleInstance> sampleInstancesAtPosition = sampleSheetAuthority.getSampleInstancesAtPosition(position);
                for (SampleInstance sampleInstance : sampleInstancesAtPosition) {
                    LabVessel vesselAtPosition = this.getVesselAtPosition(position);
                    if (vesselAtPosition != null) {
                        for (Reagent reagent : vesselAtPosition.getAppliedReagents()) {
                            if(reagent.getMolecularEnvelopeDelta() != null) {
                                MolecularEnvelope molecularEnvelope = sampleInstance.getMolecularState().getMolecularEnvelope();
                                if(molecularEnvelope == null) {
                                    sampleInstance.getMolecularState().setMolecularEnvelope(reagent.getMolecularEnvelopeDelta());
                                } else {
                                    molecularEnvelope.surroundWith(reagent.getMolecularEnvelopeDelta());
                                }
                            }
                        }
                    }
                }
                sampleInstances.addAll(sampleInstancesAtPosition);
            }
        }
        return sampleInstances;
    }
    
    public Set<SampleInstance> getSampleInstances() {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        if(this.sampleSheetAuthorities.isEmpty()) {
            if (this.mapPositionToVessel.isEmpty()) {
                // This plate has no wells in the database, because its transfers are all section based (i.e. no cherry picks)
                for (LabEvent labEvent : this.embedder.getTransfersTo()) {
                    for (LabVessel labVessel : labEvent.getSourceLabVessels()) {
                        if(labVessel instanceof VesselContainerEmbedder) {
                            sampleInstances.addAll(((VesselContainerEmbedder) labVessel).getVesselContainer().getSampleInstances());
                        }
                    }
                }
            } else {
                for (String position : this.mapPositionToVessel.keySet()) {
                    sampleInstances.addAll(getSampleInstancesAtPosition(position));
                }
            }
        } else {
            for (VesselContainer sampleSheetAuthority : this.sampleSheetAuthorities) {
                sampleInstances.addAll(sampleSheetAuthority.getSampleInstances());
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
    public Collection<T> getContainedVessels() {
        return this.mapPositionToVessel.values();
    }

    public void addContainedVessel(T child, String position) {
        // todo jmt set reference to parent
        this.mapPositionToVessel.put(position, child);
    }

    public Set<String> getPositions() {
        return this.mapPositionToVessel.keySet();
    }

    public LabVessel getEmbedder() {
        return this.embedder;
    }

    public void setEmbedder(LabVessel embedder) {
        this.embedder = embedder;
    }

    public Set<VesselContainer> getSampleSheetAuthorities() {
        return this.sampleSheetAuthorities;
    }

    public void setSampleSheetAuthorities(Set<VesselContainer> sampleSheetAuthorities) {
        this.sampleSheetAuthorities = sampleSheetAuthorities;
    }

    // section transfers
    // position transfers
    // authorities
}
