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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A traditional plate.
 */
public class StaticPlate extends AbstractLabVessel implements SBSSectionable, VesselContainerEmbedder<PlateWell> {

    @Embedded
    private VesselContainer<PlateWell> vesselContainer = new VesselContainer<PlateWell>(this);

    public StaticPlate(String label) {
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
    public void applyTransfer(SectionTransfer sectionTransfer) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addStateChange(StateChange stateChange) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        if (this.vesselContainer.getContainedVessels().isEmpty()) {
            for (LabVessel labVessel : getSampleSheetAuthorities()) {
                for (SampleSheet sampleSheet : labVessel.getSampleSheets()) {
                    sampleInstances.addAll(sampleSheet.getSampleInstances());
                }
            }
        } else {
            for (String position : this.vesselContainer.getPositions()) {
                sampleInstances.addAll(getSampleInstancesInPosition(position));
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

    // todo jmt should this be getWellAtPosition().getSampleInstances()? Perhaps not, because the wells may not exist
    public Set<SampleInstance> getSampleInstancesInPosition(String wellPosition) {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        if(getSampleSheetAuthorities().isEmpty()) {
            sampleInstances = Collections.emptySet();
        } else {
            for (LabVessel labVessel : getSampleSheetAuthorities()) {
                // todo jmt generalize to handle rack and tube
                if (labVessel instanceof RackOfTubes) {
                    RackOfTubes rackOfTubes = (RackOfTubes) labVessel;
                    Set<SampleInstance> sampleInstancesInPosition = rackOfTubes.getSampleInstancesInPosition(wellPosition);
                    for (SampleInstance sampleInstance : sampleInstancesInPosition) {
                        LabVessel wellAtPosition = this.getVesselContainer().getVesselAtPosition(wellPosition);
                        if (wellAtPosition != null) {
                            for (Reagent reagent : wellAtPosition.getAppliedReagents()) {
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

                    sampleInstances.addAll(sampleInstancesInPosition);
                }
            }
        }
        return sampleInstances;
    }

    public VesselContainer<PlateWell> getVesselContainer() {
        return this.vesselContainer;
    }

    public void setVesselContainer(VesselContainer<PlateWell> vesselContainer) {
        this.vesselContainer = vesselContainer;
    }
}
