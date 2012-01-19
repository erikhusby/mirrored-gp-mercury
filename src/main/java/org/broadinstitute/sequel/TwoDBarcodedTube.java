package org.broadinstitute.sequel;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.Collections;

public class TwoDBarcodedTube implements LabVessel {

    private static Log gLog = LogFactory.getLog(TwoDBarcodedTube.class);
    
    private Goop goop;
    
    private String twoDBarcode;
    
    public TwoDBarcodedTube(Goop goop,String twoDBarcode) {
        if (goop == null) {
             throw new IllegalArgumentException("goop must be non-null in TwoDBarcodedTube.TwoDBarcodedTube");
        }
        this.goop = goop;
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
    public void addReagent(Reagent r) {
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
    public Goop getGoop() {
        return goop;
    }

    @Override
    public void setGoop(Goop goop) {
        this.goop = goop;
    }

    @Override
    public String getLabCentricName() {
        return twoDBarcode;
    }

    @Override
    public Collection<SampleSheet> getSampleSheets() {
        return getGoop().getSampleSheets();
    }

    @Override
    public void addSampleSheet(SampleSheet sampleSheet) {
        getGoop().addSampleSheet(sampleSheet);
    }
}
