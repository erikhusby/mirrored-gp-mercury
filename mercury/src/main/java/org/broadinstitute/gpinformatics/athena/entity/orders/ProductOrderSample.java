package org.broadinstitute.gpinformatics.athena.entity.orders;

import clover.org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
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
    @SequenceGenerator(name = "SEQ_ORDER_SAMPLE", schema = "athena", sequenceName = "SEQ_ORDER_SAMPLE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ORDER_SAMPLE")
    private Long productOrderSampleId;

    public static final String BSP_SAMPLE_FORMAT_REGEX = "SM-[A-Z1-9]{4,6}";

    public static final Pattern BSP_SAMPLE_NAME_PATTERN = Pattern.compile(BSP_SAMPLE_FORMAT_REGEX);

    static final IllegalStateException ILLEGAL_STATE_EXCEPTION = new IllegalStateException("Sample data not available");

    @Index(name = "ix_pos_sample_name")
    @Column(nullable = false)
    private String sampleName;      // This is the name of the BSP or Non-BSP sample.

    private BillingStatus billingStatus = BillingStatus.NotYetBilled;

    private String sampleComment;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "productOrderSample")
    private Set<BillableItem> billableItems = new HashSet<BillableItem>();

    @Index(name = "ix_pos_product_order")
    @ManyToOne
    private ProductOrder productOrder;

    @Transient
    private BSPSampleDTO bspDTO = BSPSampleDTO.DUMMY;

    @Transient
    private boolean hasBspDTOBeenInitialized;

    ProductOrderSample() {
    }

    public ProductOrderSample(@Nonnull String sampleName) {
        this.sampleName = sampleName;
    }

    public ProductOrderSample(@Nonnull String sampleName, @Nonnull BSPSampleDTO bspDTO, ProductOrder productOrder) {
        this.sampleName = sampleName;
        setBspDTO(bspDTO);
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

    public BSPSampleDTO getBspDTO() {
        if (!hasBspDTOBeenInitialized) {
            if (isInBspFormat()) {
                bspDTO = ServiceAccessUtility.getSampleDtoByName(getSampleName());
            } else {
                bspDTO = BSPSampleDTO.DUMMY;
            }
            hasBspDTOBeenInitialized = true;
        }
        return bspDTO;
    }

    public Set<BillableItem> getBillableItems() {
        return billableItems;
    }

    public void addBillableItem(BillableItem billableItem) {
        billableItems.add(billableItem);
    }

    public void setBspDTO(@Nonnull BSPSampleDTO bspDTO) {
        if (bspDTO == null) {
            throw new NullPointerException("BSP Sample DTO cannot be null");
        }
        this.bspDTO = bspDTO;
        hasBspDTOBeenInitialized = true;
    }

    public boolean isInBspFormat() {
        return isInBspFormat(sampleName);
    }

    public static boolean isInBspFormat(@Nonnull String sampleName) {
        if (sampleName == null) {
            throw new NullPointerException("Sample name cannot be null");
        }

        return !StringUtils.isBlank(sampleName)
                && Pattern.matches(ProductOrderSample.BSP_SAMPLE_FORMAT_REGEX, sampleName);
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