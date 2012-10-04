package org.broadinstitute.gpinformatics.athena.entity.orders;

import clover.org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;

import java.io.Serializable;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Class to describe Athena's view of a Sample. A Sample is identified by a sample Id and
 * a billableItem and an optionally comment which may be in most cases empty but on
 * occasion can actually have a value to describe "exceptions" that occur for a particular sample.
 *
 * <p/>
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/28/12
 * Time: 10:26 AM
 */
public class AthenaSample implements Serializable {

    public static final String BSP_SAMPLE_FORMAT_REGEX = "SM-\\w{4,6}";
    private String sampleName;         // This is the name of the sample. It could be a BSP or Non-BSP sample name but it is assume
    private String comment;
    private Set<BillableItem> billableItems;

    //TODO hmc Annotate the DTO as transient when hibernating this class
    private BSPSampleDTO bspDTO;

    public AthenaSample(final String sampleName) {
        this.sampleName = sampleName;
    }

    public AthenaSample(final String sampleName, final BSPSampleDTO bspDTO) {
        this.sampleName = sampleName;
        this.bspDTO = bspDTO;
    }

    public String getSampleName() {
        return sampleName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }


    private BSPSampleDTO getBspDTO() {
        if ( ! hasBSPDTOBeenInitialized() ) {
            //TODO
            // initialize DTO ?
            throw new RuntimeException("Not yet Implemented.");
        }
        return bspDTO;
    }

    public boolean hasBSPDTOBeenInitialized() {
        return bspDTO != null;
    }

    public Set<BillableItem> getBillableItems() {
        return billableItems;
    }

    public void setBillableItems(final Set<BillableItem> billableItems) {
        this.billableItems = billableItems;
    }

    public void setBspDTO(final BSPSampleDTO bspDTO) {
        this.bspDTO = bspDTO;
    }

    public boolean isInBspFormat() {
        return isInBspFormat( getSampleName() );
    }

    public static boolean isInBspFormat(final String sampleName) {
        if (StringUtils.isBlank(sampleName)) {
            return false;
        }

        return Pattern.matches(AthenaSample.BSP_SAMPLE_FORMAT_REGEX, sampleName);
    }

    //Methods delegated to
    public String getVolume() {
        return getBspDTO().getVolume();
    }

    public String getConcentration() {
        return getBspDTO().getConcentration();
    }

    public String getRootSample() {
        return getBspDTO().getRootSample();
    }

    public String getStockSample() {
        return getBspDTO().getStockSample();
    }

    public String getCollection() {
        return getBspDTO().getCollection();
    }

    public String getCollaboratorsSampleName() {
        return getBspDTO().getCollaboratorsSampleName();
    }

    public String getContainerId() {
        return getBspDTO().getContainerId();
    }

    public String getParticipantId() {
        return getBspDTO().getPatientId();
    }

    public String getOrganism() {
        return getBspDTO().getOrganism();
    }

    public String getStockAtExport() {
        return getBspDTO().getStockAtExport();
    }

    public Boolean isPositiveControl() {
        return getBspDTO().isPositiveControl();
    }

    public Boolean isNegativeControl() {
        return getBspDTO().isNegativeControl();
    }

    public String getSampleLsid() {
        return getBspDTO().getSampleLsid();
    }

    public String getGender() {
        //TODO hmc
        throw new RuntimeException("Not yet Implemented.");
    }

    public String getDisease() {
        //TODO hmc
        throw new RuntimeException("Not yet Implemented.");
    }

    public String getSampleType() {
        //TODO hmc
        // Normal or Tumor
        throw new RuntimeException("Not yet Implemented.");
    }


}
