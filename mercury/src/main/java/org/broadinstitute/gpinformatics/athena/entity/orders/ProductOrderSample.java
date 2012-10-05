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
public class ProductOrderSample implements Serializable {

    public static final String BSP_SAMPLE_FORMAT_REGEX = "SM-[A-Z1-9]{4,6}";
    static final IllegalStateException ILLEGAL_STATE_EXCEPTION = new IllegalStateException("Sample data not available");
    private String sampleName;      // This is the name of the BSP or Non-BSP sample.
    private BillingStatus billingStatus = BillingStatus.NotYetBilled;
    private String comment;
    private Set<BillableItem> billableItems;
    //TODO hmc Annotate the DTO as transient when hibernating this class
    private BSPSampleDTO bspDTO;

    ProductOrderSample() {
    }

    public ProductOrderSample(String sampleName) {
        this.sampleName = sampleName;
    }

    public ProductOrderSample(String sampleName, BSPSampleDTO bspDTO) {
        this.sampleName = sampleName;
        this.bspDTO = bspDTO;
    }

    public String getSampleName() {
        return sampleName;
    }

    public BillingStatus getBillingStatus() {
        return billingStatus;
    }

    public void setBillingStatus(BillingStatus billingStatus) {
        this.billingStatus = billingStatus;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    private BSPSampleDTO getBspDTO() {
        if ( isInBspFormat() && ! hasBSPDTOBeenInitialized() ) {
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

    public void addBillableItem(BillableItem billableItem) {
        billableItems.add(billableItem);
    }

    public void setBspDTO(BSPSampleDTO bspDTO) {
        this.bspDTO = bspDTO;
    }

    public boolean isInBspFormat() {
        return isInBspFormat( getSampleName() );
    }

    public static boolean isInBspFormat(String sampleName) {
        if (StringUtils.isBlank(sampleName)) {
            return false;
        }
        return Pattern.matches(ProductOrderSample.BSP_SAMPLE_FORMAT_REGEX, sampleName);
    }

    // Methods delegated to the DTO
    public String getVolume() throws IllegalStateException {
        if (! isInBspFormat() ) {
            throw ILLEGAL_STATE_EXCEPTION;
        }
        return getBspDTO().getVolume();
    }

    public String getConcentration() {
        if (! isInBspFormat() ) {
            throw ILLEGAL_STATE_EXCEPTION;
        }
        return getBspDTO().getConcentration();
    }

    public String getRootSample() {
        if (! isInBspFormat() ) {
            throw ILLEGAL_STATE_EXCEPTION;
        }
        return getBspDTO().getRootSample();
    }

    public String getStockSample() {
        if (! isInBspFormat() ) {
            throw ILLEGAL_STATE_EXCEPTION;
        }
        return getBspDTO().getStockSample();
    }

    public String getCollection() {
        if (! isInBspFormat() ) {
            throw ILLEGAL_STATE_EXCEPTION;
        }
        return getBspDTO().getCollection();
    }

    public String getCollaboratorsSampleName() {
        if (! isInBspFormat() ) {
            throw ILLEGAL_STATE_EXCEPTION;
        }
        return getBspDTO().getCollaboratorsSampleName();
    }

    public String getContainerId() {
        if (! isInBspFormat() ) {
            throw ILLEGAL_STATE_EXCEPTION;
        }
        return getBspDTO().getContainerId();
    }

    public String getParticipantId() {
        if (! isInBspFormat() ) {
            throw ILLEGAL_STATE_EXCEPTION;
        }
        return getBspDTO().getPatientId();
    }

    public String getOrganism() {
        if (! isInBspFormat() ) {
            throw ILLEGAL_STATE_EXCEPTION;
        }
        return getBspDTO().getOrganism();
    }

    public String getStockAtExport() {
        if (! isInBspFormat() ) {
            throw ILLEGAL_STATE_EXCEPTION;
        }
        return getBspDTO().getStockAtExport();
    }

    public Boolean isPositiveControl() {
        if (! isInBspFormat() ) {
            throw ILLEGAL_STATE_EXCEPTION;
        }
        return getBspDTO().isPositiveControl();
    }

    public Boolean isNegativeControl() {
        if (! isInBspFormat() ) {
            throw ILLEGAL_STATE_EXCEPTION;
        }
        return getBspDTO().isNegativeControl();
    }

    public String getSampleLsid() {
        if (! isInBspFormat() ) {
            throw ILLEGAL_STATE_EXCEPTION;
        }
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
        throw new RuntimeException("Not yet Implemented.");
    }

}
