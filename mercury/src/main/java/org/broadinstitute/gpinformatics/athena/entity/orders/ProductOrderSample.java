package org.broadinstitute.gpinformatics.athena.entity.orders;

import clover.org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
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
 * @author mccrory
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
                bspDTO = ServiceAccessUtility.getSampleName ( this.getSampleName ( ) );
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
    public boolean isSampleReceived() {
        if (! isInBspFormat() ) {
            throw ILLEGAL_STATE_EXCEPTION;
        }
        return ((null != getBspDTO().getRootSample()) &&(!getBspDTO().getRootSample().isEmpty()));
    }

    public boolean isActiveStock() {
        ensureInBspFormat();

        return  ((null != getBspDTO().getStockType()) &&
                (getBspDTO().getStockType().equals(BSPSampleDTO.ACTIVE_IND)));

    }

    public boolean hasFingerprint ( ) {
        ensureInBspFormat();

        return ((null != this.getBspDTO().getFingerprint()) &&
                (!this.getBspDTO().getFingerprint().isEmpty()));

    }

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


    public String getGender() {
        ensureInBspFormat();
        return getBspDTO().getGender ( );
    }

    public String getDisease() {
        if (! isInBspFormat() ) {
           throw ILLEGAL_STATE_EXCEPTION;
        }
        return getBspDTO().getPrimaryDisease ( );
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
    public String getSampleType() {
        ensureInBspFormat();
        return getBspDTO().getSampleType ( );
    }

    public String getTotal() {
        ensureInBspFormat();
        return getBspDTO().getTotal ( );
    }

    public String getFingerprint() {
        ensureInBspFormat();
        return getBspDTO().getFingerprint ( );
    }

    public String getMaterialType() {
        ensureInBspFormat();
        return getBspDTO().getMaterialType();
    }

    public String getStockType() {

        ensureInBspFormat();
        return getBspDTO().getStockType();
    }

    public String getCollaboratorParticipantId() {
        ensureInBspFormat();
         return getBspDTO().getCollaboratorParticipantId();
     }



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductOrderSample)) {
            return false;
        }

        ProductOrderSample that = (ProductOrderSample) o;
        return new EqualsBuilder().append(sampleName, that.sampleName).append(billingStatus, that.billingStatus)
                .append(sampleComment, that.sampleComment).append(billableItems, that.billableItems)
                .append(productOrder, that.productOrder).append(bspDTO, that.bspDTO).build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(sampleName).append(billingStatus)
                .append(sampleComment).append(billableItems).append(productOrder)
                .append(bspDTO).build();
    }
}
