package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.notice.StatusNote;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Collection;
import java.util.Set;

/**
 * One of many (usually 8) tubes in a StripTube
 */
@Entity
@Audited
@Table(schema = "mercury")
public class StripTubeWell extends LabVessel {

    protected StripTubeWell(String label) {
        super(label);
    }

    protected StripTubeWell() {
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
        return CONTAINER_TYPE.STRIP_TUBE_WELL;
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
}
