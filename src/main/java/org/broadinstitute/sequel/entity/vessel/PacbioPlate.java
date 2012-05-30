package org.broadinstitute.sequel.entity.vessel;


import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.run.RunCartridge;
import org.broadinstitute.sequel.entity.run.RunChamber;
import org.broadinstitute.sequel.entity.sample.SampleInstance;

import java.util.Collection;
import java.util.Date;
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
