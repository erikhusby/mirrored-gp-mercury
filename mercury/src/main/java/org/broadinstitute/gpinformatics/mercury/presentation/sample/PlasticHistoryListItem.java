package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabBatchComposition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.Date;
import java.util.List;

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
    private List<LabBatchComposition> labBatchCompositions;
    private String eventType;
    private String eventLocation;
    private Long eventOperator;
    private Date eventDate;
    private Date creationDate;

    public PlasticHistoryListItem(LabVessel vessel) {
        label = vessel.getLabel();
        sampleInstanceCount = vessel.getSampleInstanceCount();
        type = vessel.getType().getName();
        pdoKeyCount = vessel.getPdoKeysCount();
        indexCount = vessel.getIndexesCount();
        creationDate = vessel.getCreatedOn();
        labBatchCompositions = vessel.getLabBatchCompositions();
        if (vessel.getLatestEvent() != null) {
            eventType = vessel.getLatestEvent().getLabEventType().getName();
            eventLocation = vessel.getLatestEvent().getEventLocation();
            eventOperator = vessel.getLatestEvent().getEventOperator();
            eventDate = vessel.getLatestEvent().getEventDate();
        }
    }

    public PlasticHistoryListItem(SequencingRun seqRun, LabVessel seqVessel) {
        this(seqVessel);
        type = "Run";
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

    public List<LabBatchComposition> getLabBatchCompositions() {
        return labBatchCompositions;
    }

    public int hashCode() {
        return new HashCodeBuilder()
                .append(label)
                .append(creationDate)
                .append(eventDate)
                .append(eventType)
                .hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj.getClass() != getClass())
            return false;

        PlasticHistoryListItem item = (PlasticHistoryListItem) obj;

        return new EqualsBuilder()
                .append(label, item.label)
                .append(creationDate, item.creationDate)
                .append(eventDate, item.eventDate)
                .append(eventType, item.eventType)
                .isEquals();
    }

}
