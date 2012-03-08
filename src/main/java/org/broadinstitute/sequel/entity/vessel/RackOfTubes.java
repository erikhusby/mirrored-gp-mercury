package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.StateChange;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A rack of tubes
 */
public class RackOfTubes extends AbstractLabVessel implements SBSSectionable {

    /* rack holds tubes, tubes can be removed
     * plate holds wells, wells can't be removed
     * flowcell holds lanes
     * PTP holds regions
     * smartpac holds smrtcells, smrtcells are removed, but not replaced
     * striptube holds tubes, tubes can't be removed, don't have barcodes */
    private Map<String, TwoDBarcodedTube> mapPositionToTube = new HashMap<String, TwoDBarcodedTube>();

    public RackOfTubes(String label) {
        super(label);
    }

    @Override
    public LabVessel getContainingVessel() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<? extends LabVessel> getContainedVessels() {
        return mapPositionToTube.values();
    }

    @Override
    public void addContainedVessel(LabVessel child) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public void addContainedVessel(TwoDBarcodedTube child, String position) {
        mapPositionToTube.put(position, child);
    }

/*
    @Override
    public Collection<LabEvent> getTransfersFrom() {
        throw new RuntimeException("I haven't been written yet.");
    }
*/

/*
    @Override
    public Collection<LabEvent> getTransfersTo() {
        throw new RuntimeException("I haven't been written yet.");
    }
*/

    @Override
    public Collection<LabEvent> getEvents() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public SBSSection getSection() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addStateChange(StateChange stateChange) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        if(getSampleSheetAuthorities().isEmpty()) {
            for (LabVessel labVessel : getContainedVessels()) {
                sampleInstances.addAll(labVessel.getSampleInstances());
            }
        } else {
            for (LabVessel labVessel : getSampleSheetAuthorities()) {
                sampleInstances.addAll(labVessel.getSampleInstances());
            }
        }
        return sampleInstances;
    }
    
    public Set<SampleInstance> getSampleInstancesInPosition(String rackPosition) {
        TwoDBarcodedTube twoDBarcodedTube = mapPositionToTube.get(rackPosition);
        return twoDBarcodedTube.getSampleInstances();
    }

    @Override
    public Collection<SampleInstance> getSampleInstances(SampleSheet sheet) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<StateChange> getStateChanges() {
        throw new RuntimeException("I haven't been written yet.");
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
    public void applyReagent(Reagent r) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<Reagent> getAppliedReagents() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<SampleSheet> getSampleSheets() {
        Set<SampleSheet> sampleSheets = new HashSet<SampleSheet>();
        for (TwoDBarcodedTube twoDBarcodedTube : mapPositionToTube.values()) {
            sampleSheets.addAll(twoDBarcodedTube.getSampleSheets());
        }
        return sampleSheets;
    }

    @Override
    public void applyTransfer(SectionTransfer sectionTransfer) {
        throw new RuntimeException("Method not yet implemented.");
    }
}
