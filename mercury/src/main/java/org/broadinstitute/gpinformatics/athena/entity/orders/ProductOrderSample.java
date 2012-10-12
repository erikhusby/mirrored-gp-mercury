package org.broadinstitute.gpinformatics.athena.entity.orders;

import clover.org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.hibernate.envers.Audited;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
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
@Entity
@Audited
@Table(schema = "athena")
public class ProductOrderSample implements Serializable {

    @Id
    @SequenceGenerator(name="SEQ_ORDER_SAMPLE", schema = "athena", sequenceName="SEQ_ORDER_SAMPLE")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="SEQ_ORDER_SAMPLE")
    private Long productOrderSampleId;

    public static final String BSP_SAMPLE_FORMAT_REGEX = "SM-[A-Z1-9]{4,6}";

    public static final Pattern BSP_SAMPLE_NAME_PATTERN = Pattern.compile(BSP_SAMPLE_FORMAT_REGEX);

    static final IllegalStateException ILLEGAL_STATE_EXCEPTION = new IllegalStateException("Sample data not available");
    private String sampleName;      // This is the name of the BSP or Non-BSP sample.
    private BillingStatus billingStatus = BillingStatus.NotYetBilled;
    private String sampleComment;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "productOrderSample")
    private Set<BillableItem> billableItems = new HashSet<BillableItem>();

    @ManyToOne
    private ProductOrder productOrder;

    @Transient
    private BSPSampleDTO bspDTO;

    @Transient
    private boolean hasBspDTOBeenInitialized = false;

    ProductOrderSample() {
    }

    public ProductOrderSample(String sampleName) {
        this.sampleName = sampleName;
    }

//    public OrderSample(String sampleName, BSPSampleDTO bspDTO) {
//        this.sampleName = sampleName;
//        this.bspDTO = bspDTO;
//    }


    public ProductOrderSample(final String sampleName, final BSPSampleDTO bspDTO, final ProductOrder productOrder) {
        this.sampleName = sampleName;
        this.bspDTO = bspDTO;
        this.productOrder = productOrder;
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

    public String getSampleComment() {
        return sampleComment;
    }

    public void setSampleComment(String sampleComment) {
        this.sampleComment = sampleComment;
    }

    public boolean needsBspMetaData() {
        return isInBspFormat() && !hasBspDTOBeenInitialized;
    }

    private BSPSampleDTO getBspDTO() {
        if (!hasBspDTOBeenInitialized) {
            if (isInBspFormat()) {
                bspDTO = BSPSampleDTO.DUMMY;
                //TODO
                // initialize DTO ?
                //throw new RuntimeException("Not yet Implemented.");
            } else {
                bspDTO = BSPSampleDTO.DUMMY;
            }
        }
        return bspDTO;
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
        return isInBspFormat(sampleName);
    }

    public static boolean isInBspFormat(@NotNull String sampleName) {
        return !StringUtils.isBlank(sampleName)
               && Pattern.matches(ProductOrderSample.BSP_SAMPLE_FORMAT_REGEX, sampleName);
    }

    // Methods delegated to the DTO
    public String getVolume() throws IllegalStateException {
        ensureInBspFormat();
        return getBspDTO().getVolume();
    }

    public String getConcentration() {
        ensureInBspFormat();
        return getBspDTO().getConcentration();
    }

    public String getRootSample() {
        ensureInBspFormat();
        return getBspDTO().getRootSample();
    }

    public String getStockSample() {
        ensureInBspFormat();
        return getBspDTO().getStockSample();
    }

    public String getCollection() {
        ensureInBspFormat();
        return getBspDTO().getCollection();
    }

    public String getCollaboratorsSampleName() {
        ensureInBspFormat();
        return getBspDTO().getCollaboratorsSampleName();
    }

    public String getContainerId() {
        ensureInBspFormat();
        return getBspDTO().getContainerId();
    }

    public String getParticipantId() {
        ensureInBspFormat();
        return getBspDTO().getPatientId();
    }

    public String getOrganism() {
        ensureInBspFormat();
        return getBspDTO().getOrganism();
    }

    public String getStockAtExport() {
        ensureInBspFormat();
        return getBspDTO().getStockAtExport();
    }

    public Boolean isPositiveControl() {
        ensureInBspFormat();
        return getBspDTO().isPositiveControl();
    }

    public Boolean isNegativeControl() {
        ensureInBspFormat();
        return getBspDTO().isNegativeControl();
    }

    public String getSampleLsid() {
        ensureInBspFormat();
        return getBspDTO().getSampleLsid();
    }

    private void ensureInBspFormat() {
        if (!isInBspFormat()) {
            throw ILLEGAL_STATE_EXCEPTION;
        }
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


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductOrderSample)) return false;

        final ProductOrderSample that = (ProductOrderSample) o;

        if (billableItems != null ? !billableItems.equals(that.billableItems) : that.billableItems != null)
            return false;
        if (billingStatus != that.billingStatus) return false;
        if (bspDTO != null ? !bspDTO.equals(that.bspDTO) : that.bspDTO != null) return false;
        if (sampleComment != null ? !sampleComment.equals(that.sampleComment) : that.sampleComment != null) return false;
        if (!productOrder.equals(that.productOrder)) return false;
        if (!sampleName.equals(that.sampleName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = sampleName.hashCode();
        result = 31 * result + billingStatus.hashCode();
        result = 31 * result + (sampleComment != null ? sampleComment.hashCode() : 0);
        result = 31 * result + (billableItems != null ? billableItems.hashCode() : 0);
        result = 31 * result + productOrder.hashCode();
        result = 31 * result + (bspDTO != null ? bspDTO.hashCode() : 0);
        return result;
    }
}
