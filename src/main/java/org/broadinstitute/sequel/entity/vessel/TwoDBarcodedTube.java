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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TwoDBarcodedTube extends AbstractLabVessel {

    private static Log gLog = LogFactory.getLog(TwoDBarcodedTube.class);
    
    private String twoDBarcode;
    
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
        return twoDBarcode;
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

    @Override
    public Collection<Reagent> getReagentContents() {
        throw new RuntimeException("I haven't been written yet.");
    }

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
    public Collection<LabVessel> getContainedVessels() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addContainedVessel(LabVessel child) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getLabCentricName() {
        return twoDBarcode;
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

    @Override
    public Set<SampleInstance> getSampleInstances() {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        for (SampleSheet sampleSheet : getSampleSheets()) {
            sampleInstances.addAll(sampleSheet.getSampleInstances(this));
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
        notes.add(statusNote);
    }

    @Override
    public Collection<StatusNote> getAllStatusNotes() {
        return notes;
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
}
