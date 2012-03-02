package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.StateChange;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PlateWell extends AbstractLabVessel {

    private StaticPlate plate;
    
    private Set<Reagent> appliedReagents = new HashSet<Reagent>();
    
    public PlateWell(StaticPlate p,WellName wellName) {
        super(p.getLabel() + wellName);
        plate = p;
    }

    protected PlateWell(String label) {
        super(label);
    }

    @Override
    public LabVessel getContainingVessel() {
        return plate;        
    }

    @Override
    public Collection<LabVessel> getContainedVessels() {
        return Collections.emptyList();
    }

    @Override
    public void addContainedVessel(LabVessel child) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabEvent> getTransfersFrom() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabEvent> getTransfersTo() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabEvent> getEvents() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addStateChange(StateChange stateChange) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        for (SampleSheet sampleSheet : getSampleSheets()) {
            for (SampleInstance sampleInstance : sampleSheet.getSampleInstances()) {
                for (Reagent appliedReagent : appliedReagents) {
                    sampleInstance.getMolecularState().getMolecularEnvelope().surroundWith(appliedReagent.getMolecularEnvelopeDelta());
                }
            }
            sampleInstances.addAll(sampleSheet.getSampleInstances());
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
    public void applyReagent(Reagent reagent) {
        appliedReagents.add(reagent);
    }

    @Override
    public Collection<Reagent> getAppliedReagents() {
        return appliedReagents;
    }

    @Override
    public void applyTransfer(SectionTransfer sectionTransfer) {
        throw new RuntimeException("Method not yet implemented.");
    }
}
