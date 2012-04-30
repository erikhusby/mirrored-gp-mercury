package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.sample.StateChange;

import java.util.Collection;
import java.util.Set;

/**
 * One of many (usually 8) tubes in a StripTube
 */
public class StripTubeWell extends LabVessel {

    protected StripTubeWell(String label) {
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
}
