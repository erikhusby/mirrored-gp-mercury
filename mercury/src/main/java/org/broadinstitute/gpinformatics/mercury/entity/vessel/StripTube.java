package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.notice.StatusNote;
import org.broadinstitute.gpinformatics.mercury.entity.project.Project;
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

    public enum Positions {
        ONE("1"),
        TWO("2"),
        THREE("3"),
        FOUR("4"),
        FIVE("5"),
        SIX("6"),
        SEVEN("7"),
        EIGHT("8");

        private String display;

        Positions(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return this.display;
        }
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
