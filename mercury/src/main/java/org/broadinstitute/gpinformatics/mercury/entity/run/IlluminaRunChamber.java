package org.broadinstitute.gpinformatics.mercury.entity.run;


import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.notice.StatusNote;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class IlluminaRunChamber extends RunChamber {

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
    public Set<LabEvent> getTransfersFrom() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<LabEvent> getTransfersTo() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public CONTAINER_TYPE getType() {
        return CONTAINER_TYPE.ILLUMINA_RUN_CHAMBER;
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
    public Float getVolume() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Float getConcentration() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
