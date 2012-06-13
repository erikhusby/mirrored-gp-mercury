package org.broadinstitute.sequel.entity.vessel;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.entity.labevent.Failure;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.sample.StartingSample;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Entity
@NamedQueries({
        @NamedQuery(
                name = "TwoDBarcodedTube.fetchByBarcodes",
                query = "select t from TwoDBarcodedTube t where label in (:barcodes)"
        ),
        @NamedQuery(
                name = "TwoDBarcodedTube.fetchByBarcode",
                query = "select t from TwoDBarcodedTube t where label = :barcode"
        )
})
/**
 * Represents a tube with a two dimensional barcode on its bottom.  These tubes are usually stored in racks.
 */
public class TwoDBarcodedTube extends LabVessel {

    private static Log gLog = LogFactory.getLog(TwoDBarcodedTube.class);
    
    @OneToMany
    private Collection<StatusNote> notes = new HashSet<StatusNote>();

    @Transient
    private StartingSample startingSample;

    public TwoDBarcodedTube(String twoDBarcode) {
        super(twoDBarcode);
        if (twoDBarcode == null) {
            throw new IllegalArgumentException("twoDBarcode must be non-null in TwoDBarcodedTube.TwoDBarcodedTube");
        }
    }

    public TwoDBarcodedTube(String twoDBarcode,StartingSample startingSample) {
        super(twoDBarcode);
        if (twoDBarcode == null) {
             throw new IllegalArgumentException("twoDBarcode must be non-null in TwoDBarcodedTube.TwoDBarcodedTube");
        }
        this.startingSample = startingSample;
    }

    protected TwoDBarcodedTube() {
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
    public LabMetric getMetric(LabMetric.MetricName metricName, MetricSearchMode searchMode, SampleInstance sampleInstance) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean isAncestor(LabVessel progeny) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<LabEvent> getTransfersFrom() {
        return Collections.emptySet();
    }

    @Override
    public Set<LabEvent> getTransfersTo() {
        return Collections.emptySet();
    }

    @Override
    public boolean isProgeny(LabVessel ancestor) {
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
    public Set<SampleInstance> getSampleInstances() {
        return startingSample.getSampleInstances();
    }

    @Override
    public Collection<Project> getAllProjects() {
        Collection<Project> allProjects = new HashSet<Project>();
        for (SampleInstance sampleInstance : getSampleInstances()) {
            if (sampleInstance.getAllProjectPlans() != null) {
                for (ProjectPlan projectPlan : sampleInstance.getAllProjectPlans()) {
                    if (projectPlan != null) {
                        allProjects.add(projectPlan.getProject());
                    }
                }
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
}
