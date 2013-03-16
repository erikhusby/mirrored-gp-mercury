package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.Collection;
import java.util.Date;

/**
 * "Polymorphic" representation of vessel or other entity/info to be shown in PlasticHistoryView's list.
 */
public class PlasticHistoryListItem {
    public static String SEQUENCER_RUN_EVENT = "SequencerRun";

    private String label;
    private int sampleInstanceCount;
    private String type;
    private int pdoKeyCount;
    private int indexCount;
    private Collection<LabBatch> labBatches;
    private String eventType;
    private String eventLocation;
    private Long eventOperator;
    private Date eventDate;
    private Date creationDate;
    private String lcSetName;

    public PlasticHistoryListItem(LabVessel vessel) {
        label = vessel.getLabel();
        sampleInstanceCount = vessel.getSampleInstanceCount();
        type = vessel.getType().getName();
        pdoKeyCount = vessel.getPdoKeysCount();
        indexCount = vessel.getIndexesCount();
        labBatches = vessel.getNearestWorkflowLabBatches();
        eventType = vessel.getLatestEvent().getLabEventType().getName();
        eventLocation = vessel.getLatestEvent().getEventLocation();
        eventOperator = vessel.getLatestEvent().getEventOperator();
        eventDate = vessel.getLatestEvent().getEventDate();
        creationDate = vessel.getCreatedOn();
    }

    public PlasticHistoryListItem(SequencingRun seqRun, LabVessel seqVessel) {
        this(seqVessel);
        label = seqRun.getRunBarcode();
        eventType = SEQUENCER_RUN_EVENT;
        eventLocation = seqRun.getMachineName();
        eventOperator = seqRun.getOperator();
        eventDate = seqRun.getRunDate();
    }

    public String getLabel() {
        return label;
    }

    public int getSampleInstanceCount() {
        return sampleInstanceCount;
    }

    public String getType() {
        return type;
    }

    public int getPdoKeyCount() {
        return pdoKeyCount;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public Collection<LabBatch> getLabBatches() {
        return labBatches;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventLocation() {
        return eventLocation;
    }

    public Long getEventOperator() {
        return eventOperator;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public Date getCreationDate() {
        return creationDate;
    }
}
