package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.sample.StateChange;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A rack of tubes
 */
@Entity
public class RackOfTubes extends LabVessel implements SBSSectionable, VesselContainerEmbedder<TwoDBarcodedTube> {

    @Embedded
    private VesselContainer<TwoDBarcodedTube> vesselContainer = new VesselContainer<TwoDBarcodedTube>(this);

    public RackOfTubes(String label) {
        super(label);
    }

    protected RackOfTubes() {
    }

    @Override
    public LabVessel getContainingVessel() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabEvent> getEvents() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public SBSSection getSection() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        return this.getVesselContainer().getSampleInstances();
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
    public Collection<SampleSheet> getSampleSheets() {
        Set<SampleSheet> sampleSheets = new HashSet<SampleSheet>();
        for (TwoDBarcodedTube twoDBarcodedTube : this.vesselContainer.getContainedVessels()) {
            sampleSheets.addAll(twoDBarcodedTube.getSampleSheets());
        }
        return sampleSheets;
    }

    public VesselContainer<TwoDBarcodedTube> getVesselContainer() {
        return this.vesselContainer;
    }

    public void setVesselContainer(VesselContainer<TwoDBarcodedTube> vesselContainer) {
        this.vesselContainer = vesselContainer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RackOfTubes)) return false;

        RackOfTubes that = (RackOfTubes) o;

        if (!this.label.equals(that.getLabel())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return this.label.hashCode();
    }
}
