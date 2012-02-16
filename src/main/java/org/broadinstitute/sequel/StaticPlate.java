package org.broadinstitute.sequel;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A traditional plate.
 */
public class StaticPlate extends AbstractLabVessel implements SBSSectionable {

    private Set<PlateWell> wells = new HashSet<PlateWell>();

    public StaticPlate(String label) {
        super(label);
    }

    @Override
    public LabVessel getContainingVessel() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabVessel> getContainedVessels() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addContainedVessel(LabVessel child) {
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
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        if(wells.isEmpty()) {
            for (LabVessel labVessel : getSampleSheetReferences()) {
                for (SampleSheet sampleSheet : labVessel.getSampleSheets()) {
                    sampleInstances.addAll(sampleSheet.getSampleInstances());
                }
            }
        } else {
            throw new RuntimeException("I haven't been written yet.");
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

    public Set<SampleInstance> getSampleInstancesInWell(String wellPosition) {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        if(wells.isEmpty()) {
            for (LabVessel labVessel : getSampleSheetReferences()) {
                if(labVessel instanceof RackOfTubes) {
                    RackOfTubes rackOfTubes = (RackOfTubes) labVessel;
                    // todo jmt honor sections
                    sampleInstances.addAll(rackOfTubes.getSampleInstancesInPosition(wellPosition));
                }
            }
        } else {
            throw new RuntimeException("I haven't been written yet.");
        }
        return sampleInstances;
    }
}
