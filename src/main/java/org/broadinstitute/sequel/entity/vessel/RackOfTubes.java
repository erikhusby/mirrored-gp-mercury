package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.sample.StateChange;

import javax.persistence.Embedded;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A rack of tubes
 */
public class RackOfTubes extends AbstractLabVessel implements SBSSectionable, VesselContainerEmbedder<TwoDBarcodedTube> {

    @Embedded
    private VesselContainer<TwoDBarcodedTube> vesselContainer = new VesselContainer<TwoDBarcodedTube>(this);

    public RackOfTubes(String label) {
        super(label);
    }

    @Override
    public LabVessel getContainingVessel() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabEvent> getEvents() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public SBSSection getSection() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addStateChange(StateChange stateChange) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        // todo jmt change this to call VesselContainer.getSampleInstancesInPosition for all positions
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        if(getSampleSheetAuthorities().isEmpty()) {
            if(getTransfersTo().isEmpty()) {
                for (LabVessel labVessel : this.vesselContainer.getContainedVessels()) {
                    sampleInstances.addAll(labVessel.getSampleInstances());
                }
            } else {
                for (LabEvent labEvent : getTransfersTo()) {
                    for (SectionTransfer sectionTransfer : labEvent.getSectionTransfers()) {
                        sectionTransfer.getSourceVesselContainer();
                    }
                }
            }
        } else {
            for (LabVessel labVessel : getSampleSheetAuthorities()) {
                sampleInstances.addAll(labVessel.getSampleInstances());
            }
        }
        return sampleInstances;
    }
    
    public Set<SampleInstance> getSampleInstancesInPosition(String rackPosition) {
        Set<SampleInstance> sampleInstances;
        if(getSampleSheetAuthorities().isEmpty()) {
            TwoDBarcodedTube twoDBarcodedTube = this.vesselContainer.getVesselAtPosition(rackPosition);
            sampleInstances = twoDBarcodedTube.getSampleInstances();
        } else {
            sampleInstances = new HashSet<SampleInstance>();
            for (LabVessel labVessel : getSampleSheetAuthorities()) {
                // todo jmt add getSampleInstancesInPosition to VesselContainer
                if(labVessel instanceof StaticPlate) {
                    sampleInstances.addAll(((StaticPlate) labVessel).getSampleInstancesInPosition(rackPosition));
                } else if(labVessel instanceof VesselContainerEmbedder) {
                    sampleInstances.addAll(((VesselContainerEmbedder) labVessel).getVesselContainer().getVesselAtPosition(rackPosition).getSampleInstances());
                }
            }
        }
        return sampleInstances;
    }

    @Override
    public Collection<SampleInstance> getSampleInstances(SampleSheet sheet) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<StateChange> getStateChanges() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<Project> getAllProjects() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public StatusNote getLatestNote() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void logNote(StatusNote statusNote) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<StatusNote> getAllStatusNotes() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Float getVolume() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Float getConcentration() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void applyReagent(Reagent r) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<Reagent> getAppliedReagents() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<SampleSheet> getSampleSheets() {
        Set<SampleSheet> sampleSheets = new HashSet<SampleSheet>();
        for (TwoDBarcodedTube twoDBarcodedTube : this.vesselContainer.getContainedVessels()) {
            sampleSheets.addAll(twoDBarcodedTube.getSampleSheets());
        }
        return sampleSheets;
    }

    @Override
    public void applyTransfer(SectionTransfer sectionTransfer) {
        throw new RuntimeException("Method not yet implemented.");
    }

    public VesselContainer<TwoDBarcodedTube> getVesselContainer() {
        return this.vesselContainer;
    }

    public void setVesselContainer(VesselContainer<TwoDBarcodedTube> vesselContainer) {
        this.vesselContainer = vesselContainer;
    }
}
