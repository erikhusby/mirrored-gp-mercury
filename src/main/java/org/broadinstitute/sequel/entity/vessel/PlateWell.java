package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.entity.labevent.AbstractLabEvent;
import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.sample.StateChange;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;

import java.util.Collection;
import java.util.Set;

public class PlateWell extends LabVessel {

    private StaticPlate plate;
    private WellName wellName;
    
    public PlateWell(StaticPlate p,WellName wellName) {
        super(p.getLabel() + wellName);
        this.plate = p;
        this.wellName = wellName;
    }

    @Override
    public LabVessel getContainingVessel() {
        return this.plate;
    }

    @Override
    public Set<AbstractLabEvent> getTransfersFrom() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<AbstractLabEvent> getTransfersTo() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabEvent> getEvents() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        return this.plate.getVesselContainer().getSampleInstancesAtPosition(this.wellName.getWellName());
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
}
