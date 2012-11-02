package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.notice.StatusNote;
//import org.broadinstitute.gpinformatics.mercury.entity.project.Project;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.hibernate.envers.Audited;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.Collection;
import java.util.Set;

/**
 * Represents a strip tube, several tubes molded into a single piece of plasticware, e.g. 8 tubes in the same formation
 * as a rack column.  The Strip tube has a barcode, but each constituent tube does not.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class StripTube extends LabVessel implements VesselContainerEmbedder<StripTubeWell> {

    protected StripTube() {
    }

    @Embedded
    VesselContainer<StripTubeWell> vesselContainer = new VesselContainer<StripTubeWell>(this);

    public StripTube(String label) {
        super(label);
    }

    @Override
    public VesselContainer<StripTubeWell> getVesselContainer() {
        return vesselContainer;
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        return this.getVesselContainer().getSampleInstances();
    }

    @Override
    public Set<LabEvent> getTransfersFrom() {
        return vesselContainer.getTransfersFrom();
    }

    @Override
    public Set<LabEvent> getTransfersTo() {
        return this.vesselContainer.getTransfersTo();
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return VesselGeometry.STRIP_TUBE;
    }

    @Override
    public CONTAINER_TYPE getType() {
        return CONTAINER_TYPE.STRIP_TUBE;
    }

    // todo jmt remove these empty methods
    @Override
    public LabVessel getContainingVessel() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<LabEvent> getEvents() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
