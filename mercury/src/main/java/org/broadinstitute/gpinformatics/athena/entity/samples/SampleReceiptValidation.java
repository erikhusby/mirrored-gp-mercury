package org.broadinstitute.gpinformatics.athena.entity.samples;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;

/**
 * This class represents a validation error associated with a Product Order Sample that is created at the time of a
 * mercury initiated sample receipt.
 */
@Entity
@Audited
@Table(name = "sample_receipt_validation", schema = "athena")
public class SampleReceiptValidation {

    @Id
    @SequenceGenerator(name = "SEQ_SAMP_REC_VALIDATION", schema = "athena", sequenceName = "SEQ_SAMP_REC_VALIDATION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAMP_REC_VALIDATION")
    private Long validationId;

    @ManyToOne(cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "PRODUCT_ORDER_SAMPLE")
    private ProductOrderSample productOrderSample;

    @Column(name = "validation_type", length = 255, nullable = false)
    @Enumerated(EnumType.STRING)
    private SampleValidationType validationType;
    @Column(name = "status", length = 255, nullable = false)
    @Enumerated(EnumType.STRING)
    private SampleValidationStatus status;
    @Column(name = "CREATED_DATE", nullable = false)
    private Date createdDate;
    @Column(name = "CREATED_BY", nullable = false)
    private Long createdBy;
    @Column(name = "MODIFIED_DATE")
    private Date modifiedDate;
    @Column(name = "MODIFIED_BY")
    private long modifiedBy;
    @Column(name = "REASON", nullable = false)
    @Enumerated(EnumType.STRING)
    private SampleValidationReason reason;

    protected SampleReceiptValidation() {
    }

    public SampleReceiptValidation(@Nonnull Long createdBy, @Nonnull SampleValidationType validationType,
                                   @Nonnull SampleValidationReason reason) {
        this(null, createdBy, validationType, reason);
    }

    public SampleReceiptValidation(ProductOrderSample productOrderSample, @Nonnull Long createdBy,
                                   @Nonnull SampleValidationType validationType,
                                   @Nonnull SampleValidationReason reason) {
        this(productOrderSample, createdBy, SampleValidationStatus.PENDING, validationType, reason);
    }


    public SampleReceiptValidation(ProductOrderSample productOrderSample, @Nonnull Long createdBy,
                                   @Nonnull SampleValidationStatus status,
                                   @Nonnull SampleValidationType validationType,
                                   @Nonnull SampleValidationReason reason) {
        this.productOrderSample = productOrderSample;
        this.createdBy = createdBy;
        this.status = status;
        this.validationType = validationType;
        this.reason = reason;

        this.createdDate = new Date();
    }

    public Long getValidationId() {
        return validationId;
    }

    public ProductOrderSample getProductOrderSample() {
        return productOrderSample;
    }

    public void setProductOrderSample(ProductOrderSample productOrderSample) {
        this.productOrderSample = productOrderSample;
    }

    public SampleValidationType getValidationType() {
        return validationType;
    }

    public void setValidationType(SampleValidationType validationType) {
        this.validationType = validationType;
    }

    public SampleValidationStatus getStatus() {
        return status;
    }

    public void setStatus(SampleValidationStatus status) {
        this.status = status;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public long getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(long modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public SampleValidationReason getReason() {
        return reason;
    }

    public void setReason(SampleValidationReason reason) {
        this.reason = reason;
    }

    /**
     * Detach this SampleReceiptValidation from all other objects so it can be removed.
     */
    public void remove() {
        productOrderSample = null;
    }

    public enum SampleValidationReason {
        SAMPLE_NOT_IN_BSP("The sample is not recognized in BSP"),
        MISSING_SAMPLE_FROM_SAMPLE_KIT("Not all of the samples for the sample kit came back"),
        SAMPLES_FROM_MULTIPLE_KITS("The samples being received span multiple sample kits");
        private final String reasonMessage;

        private SampleValidationReason(String reasonMessage) {
            this.reasonMessage = reasonMessage;
        }

        public String getReasonMessage() {
            return reasonMessage;
        }
    }

    public enum SampleValidationType {
        WARNING, BLOCKING;
    }

    public enum SampleValidationStatus {
        PENDING, CLEARED;
    }
}
