package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

/**
 * A class that fetches {@link LabVessel} objects
 * associated with a sample from Mercury LIMS.
 */
public class LabEventSampleDTO {

    private Collection<LabVessel> vessels;

    private String sampleKey;

    // This is the BSP sample receipt date format string. (ex. 11/18/2010)
    public static final String BSP_DATE_FORMAT_STRING = "MM/dd/yyyy";

    /**
     * This constructor creates a dto with no values. This is mainly for tests that don't care about the DTO.
     */
    public LabEventSampleDTO() {
        vessels = Collections.emptyList();
    }

    /**
     * A constructor initializing with LabVessel objects associated with a sample.
     *
     * @param labVessels The LabVessel objects associated with a sample.
     * @param sampleKey  Mercury sample key associated with the lab vessels.
     */
    public LabEventSampleDTO(Collection<LabVessel> labVessels, String sampleKey) {
        this.vessels = labVessels;
        this.sampleKey = sampleKey;
    }

    public String getSamplePackagedDate() {
        return getFormattedDate(LabEventType.SAMPLE_PACKAGE);
    }

    private String getFormattedDate(LabEventType labEventType) {
        Date receiptDate = LabEvent.getLabVesselEventDateByType(vessels, labEventType);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(BSP_DATE_FORMAT_STRING);
        if (receiptDate == null) {
            return "";
        }
        return simpleDateFormat.format(receiptDate);
    }

    public Collection<LabVessel> getLabVessels() {
        return vessels;
    }

    public String getSampleKey() {
        return sampleKey;
    }

    public void setSampleKey(String sampleKey) {
        this.sampleKey = sampleKey;
    }
}
