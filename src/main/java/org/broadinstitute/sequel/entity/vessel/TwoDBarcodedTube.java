package org.broadinstitute.sequel.entity.vessel;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.StateChange;
import org.broadinstitute.sequel.entity.labevent.Failure;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TwoDBarcodedTube extends AbstractLabVessel {

    private static Log gLog = LogFactory.getLog(TwoDBarcodedTube.class);
    
    private String twoDBarcode;

    private List<RackOfTubes> racksOfTubes = new ArrayList<RackOfTubes>();
    
    private Collection<StatusNote> notes = new HashSet<StatusNote>();
    
    public TwoDBarcodedTube(String twoDBarcode) {
        super(twoDBarcode);
        if (twoDBarcode == null) {
            throw new IllegalArgumentException("twoDBarcode must be non-null in TwoDBarcodedTube.TwoDBarcodedTube");
        }
        this.twoDBarcode = twoDBarcode;
    }

    public TwoDBarcodedTube(String twoDBarcode,SampleSheet sheet) {
        super(twoDBarcode);
        if (twoDBarcode == null) {
             throw new IllegalArgumentException("twoDBarcode must be non-null in TwoDBarcodedTube.TwoDBarcodedTube");
        }
        if (sheet == null) {
            // todo jmt decide how to follow the spirit of this
//             throw new IllegalArgumentException("sheet must be non-null in TwoDBarcodedTube.TwoDBarcodedTube");
        } else {
            getSampleSheets().add(sheet);
            sheet.addToVessel(this);
        }
        this.twoDBarcode = twoDBarcode;
    }


    @Override
    public String getLabel() {
        return this.twoDBarcode;
    }

    @Override
    public void addMetric(LabMetric m) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabMetric> getMetrics() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addFailure(Failure failureMode) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<Failure> getFailures() {
        throw new RuntimeException("I haven't been written yet.");
    }

/*
    @Override
    public Collection<Reagent> getReagentContents() {
        throw new RuntimeException("I haven't been written yet.");
    }
*/

    @Override
    public void addReagent(Reagent reagent) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public LabMetric getMetric(LabMetric.MetricName metricName, MetricSearchMode searchMode, SampleInstance sampleInstance) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean isAncestor(LabVessel progeny) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean isProgeny(LabVessel ancestor) {
        throw new RuntimeException("I haven't been written yet.");
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
    public LabVessel getContainingVessel() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getLabCentricName() {
        return this.twoDBarcode;
    }

    @Override
    public Collection<StateChange> getStateChanges() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addStateChange(StateChange stateChange) {
        throw new RuntimeException("I haven't been written yet.");
    }


    @Override
    public Collection<SampleInstance> getSampleInstances(SampleSheet sheet) {
        return sheet.getSampleInstances(this);
    }

    // rack.getSampleInstancesInPosition
    //   if tube-level authority
    //   if rack-level authority
    //   for each other rack
    //     rack.getSampleInstancesInPosition

    // a sequence of ALL96 plate to ALL96 plate transfers can be compressed, because the wells can't move
    // in a transfer to or from a rack, the tubes could change position
    // working backwards:
    // pooling transfer is a cherry pick, so authority has to be established tube to tube
    // normalized catch registration is plate to rack, the source wells can't move
    // hybridization is rack to plate, the tubes can move
    @Override
    public Set<SampleInstance> getSampleInstances() {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        if(getSampleSheets().isEmpty()) {
            for (LabVessel labVessel : getSampleSheetAuthorities()) {
                sampleInstances.addAll(labVessel.getSampleInstances());
            }
        } else {
            for (SampleSheet sampleSheet : getSampleSheets()) {
                sampleInstances.addAll(sampleSheet.getSampleInstances(this));
            }
        }
        return sampleInstances;
    }

    @Override
    public Collection<Project> getAllProjects() {
        Collection<Project> allProjects = new HashSet<Project>();
        for (SampleInstance sampleInstance : getSampleInstances()) {
            if (sampleInstance.getProject() != null) {
                allProjects.add(sampleInstance.getProject());
            }
        }
        return allProjects;
    }

    @Override
    public StatusNote getLatestNote() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void logNote(StatusNote statusNote) {
        gLog.info(statusNote);
        this.notes.add(statusNote);
    }

    @Override
    public Collection<StatusNote> getAllStatusNotes() {
        return this.notes;
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
    public void applyTransfer(SectionTransfer sectionTransfer) {
        throw new RuntimeException("Method not yet implemented.");
    }

    public List<RackOfTubes> getRacksOfTubes() {
        return this.racksOfTubes;
    }

    public void setRacksOfTubes(List<RackOfTubes> racksOfTubes) {
        this.racksOfTubes = racksOfTubes;
    }
}
