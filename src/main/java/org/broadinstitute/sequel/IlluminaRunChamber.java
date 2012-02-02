package org.broadinstitute.sequel;


import java.util.*;

public class IlluminaRunChamber extends AbstractLabVessel implements Priceable, RunChamber {

    private IlluminaFlowcell flowcell;
    
    private int laneNumber;

    private LabVessel library;
    
    public IlluminaRunChamber(IlluminaFlowcell flowcell, int laneNumber,LabVessel library) {
        this.flowcell = flowcell;
        this.laneNumber = laneNumber;
        this.library = library;
    }

    @Override
    public String getChamberName() {
        return Integer.toString(laneNumber);
    }
    
    public int getLaneNumber() {
        return this.laneNumber;
    }

    @Override
    public RunConfiguration getRunConfiguration() {
        return flowcell.getRunConfiguration();
    }

    /**
     * The lane on a flowcell doesn't ever contain
     * any other vessel.
     * @return
     */
    @Override
    public Collection<LabVessel> getContainedVessels() {
        return Collections.emptyList();
    }

    /**
     * Modelling problem: you can't add a lab
     * vessel to a flowcell lane
     */
    @Override
    public void addContainedVessel(LabVessel child) {
        throw new RuntimeException("Can't do this!");
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
    public String getLabNameOfPricedItem() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Invoice getInvoice() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<SampleInstance> getSampleInstances() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Date getPriceableCreationDate() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getPriceListItemName() {
        String priceItem = "none";
        IlluminaRunConfiguration runConfig = (IlluminaRunConfiguration)getRunConfiguration();
        if (runConfig.getReadLength() == 76) {
            priceItem = "Illumina 76bp";
        }
        else {
            // do some stuff...
        }
        return priceItem;
    }

    @Override
    public int getMaximumSplitFactor() {
        final Set<SampleInstance> aliquotInstances = new HashSet<SampleInstance>();
        aliquotInstances.addAll(getSampleInstances());
        return aliquotInstances.size();
        // or maybe we should count inique indexes?

        // or maybe the billing app should show both # samples and # indexes
        // in case there is an inconsistency?
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
        return flowcell;
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
