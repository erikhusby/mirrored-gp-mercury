package org.broadinstitute.gpinformatics.mercury.entity.vessel;


import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.notice.StatusNote;
import org.broadinstitute.gpinformatics.mercury.entity.project.Project;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;

import java.util.Collection;
import java.util.Set;

/**
 * A plate loaded into the pacbio instrument
 * is a plain old plate.  it's also a run
 * cartridge.  and it's probably priceable.
 */
public class PacbioPlate extends RunCartridge {

    public PacbioPlate(String label) {
        super(label);
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
        return CONTAINER_TYPE.PACBIO_PLATE;
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public LabVessel getContainingVessel() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<LabEvent> getEvents() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<Project> getAllProjects() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public StatusNote getLatestNote() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void logNote(StatusNote statusNote) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<StatusNote> getAllStatusNotes() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Float getVolume() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Float getConcentration() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * I'm not sure what a run chamber is for pacbio.
     * Is it a well on the plate?
     * @return
     */
    @Override
    public Iterable<RunChamber> getChambers() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getCartridgeName() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getCartridgeBarcode() {
        throw new RuntimeException("I haven't been written yet.");
    }


}
