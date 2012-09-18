package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.notice.StatusNote;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.project.Project;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import java.util.Collection;
import java.util.Set;
@Entity
@Audited
public class PlateWell extends LabVessel {

    @ManyToOne(fetch = FetchType.LAZY)
    private StaticPlate plate;

    @Enumerated(EnumType.STRING)
    private VesselPosition vesselPosition;
    
    public PlateWell(StaticPlate p,VesselPosition vesselPosition) {
        super(p.getLabel() + vesselPosition);
        this.plate = p;
        this.vesselPosition = vesselPosition;
    }

    public PlateWell() {
    }

    @Override
    public LabVessel getContainingVessel() {
        return this.plate;
    }

    @Override
    public Set<LabEvent> getTransfersFrom() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<LabEvent> getTransfersTo() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public CONTAINER_TYPE getType() {
        return CONTAINER_TYPE.PLATE_WELL;
    }

    @Override
    public Collection<LabEvent> getEvents() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        return this.plate.getVesselContainer().getSampleInstancesAtPosition(this.vesselPosition);
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
