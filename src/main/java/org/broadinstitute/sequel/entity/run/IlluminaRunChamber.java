package org.broadinstitute.sequel.entity.run;


import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.vessel.LabMetric;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.StateChange;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.vessel.AbstractLabVessel;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class IlluminaRunChamber extends AbstractLabVessel implements  RunChamber {

    private IlluminaFlowcell flowcell;
    
    private int laneNumber;

    private LabVessel library;
    
    public IlluminaRunChamber(IlluminaFlowcell flowcell, int laneNumber,LabVessel library) {
        super("don't know");
        this.flowcell = flowcell;
        this.laneNumber = laneNumber;
        this.library = library;
    }

    @Override
    public String getChamberName() {
        return Integer.toString(this.laneNumber);
    }
    
    public int getLaneNumber() {
        return this.laneNumber;
    }

    @Override
    public RunConfiguration getRunConfiguration() {
        return this.flowcell.getRunConfiguration();
    }

    @Override
    public Collection<LabEvent> getTransfersFrom() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabEvent> getTransfersTo() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabEvent> getEvents() {
        throw new RuntimeException("I haven't been written yet.");
    }


    @Override
    public Set<SampleInstance> getSampleInstances() {
        throw new RuntimeException("I haven't been written yet.");
    }

    /**
     * Web service call over to zamboni/picard
     * @return
     */
    @Override
    public Iterable<OutputDataLocation> getDataDirectories() {
        //return ZamboniWebService.getDataDirectories(...);
        throw new RuntimeException("Method not yet implemented.");
    }

    @Override
    public LabVessel getContainingVessel() {
        return this.flowcell;
    }


    /**
     * Web service call to zamboni/picard
     * @return
     */
    @Override
    public Collection<LabMetric> getMetrics() {
        //return ZamboniWebService.getMetricsForLane(...);
        throw new RuntimeException("Method not yet implemented.");
    }


    @Override
    public LabMetric getMetric(LabMetric.MetricName metricName, MetricSearchMode searchMode, SampleInstance sampleInstance) {
        throw new RuntimeException("Method not yet implemented.");
    }

    /**
     * Hmmm...if Stalker's flowcellLoaded app sends us barcode
     * scans of reagents that are going onto the sequencer,
     * we could capture these as reagent addition events
     * and then...
     * @return
     */
    @Override
    public Collection<Reagent> getReagentContents() {
        final Collection<Reagent> sequencerReagents = new HashSet<Reagent>();
        for (LabEvent event: getEvents()) {
            sequencerReagents.addAll(event.getReagents());
        }
        return sequencerReagents;
    }

    @Override
    public void applyTransfer(SectionTransfer sectionTransfer) {
        throw new RuntimeException("Method not yet implemented.");
    }

    @Override
    public void addStateChange(StateChange stateChange) {
        throw new RuntimeException("I haven't been written yet.");
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
}
