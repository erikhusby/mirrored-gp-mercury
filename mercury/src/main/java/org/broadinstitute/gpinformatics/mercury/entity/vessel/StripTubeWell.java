package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
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
    public VesselGeometry getVesselGeometry() {
        return VesselGeometry.STRIP_TUBE_WELL;
    }

    @Override
    public CONTAINER_TYPE getType() {
        return CONTAINER_TYPE.STRIP_TUBE_WELL;
    }

}
