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
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;

/**
 * This class represents a validation error associated with a Product Order Sample that is created at the time of a
 * mercury initiated sample receipt
 */
@Entity
@Audited
@Table(name = "sample_receipt_validation", schema = "athena")
public class SampleReceiptValidation {

    public enum SampleValidationType {
        WARNING, BLOCKING;
    }

    public enum SampleValidationStatus {
        PENDING, CLEARED;
    }

    @Id
    @SequenceGenerator(name = "SEQ_SAMP_REC_VALIDATION", schema = "athena", sequenceName = "SEQ_SAMP_REC_VALIDATION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAMP_REC_VALIDATION")
    private Long validationId;

    @ManyToOne(cascade = {CascadeType.PERSIST})
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


    protected SampleReceiptValidation() {
    }

    public SampleReceiptValidation(@Nonnull Long createdBy,
                                   @Nonnull ProductOrderSample productOrderSample,
                                   @Nonnull SampleValidationType validationType) {
        this(productOrderSample,createdBy,SampleValidationStatus.PENDING,validationType);
    }

    public SampleReceiptValidation(
            @Nonnull ProductOrderSample productOrderSample, @Nonnull Long createdBy,
            @Nonnull SampleValidationStatus status,
            @Nonnull SampleValidationType validationType) {
        this.productOrderSample = productOrderSample;
        this.createdBy = createdBy;
        this.status = status;
        this.validationType = validationType;

        this.createdDate = new Date();
    }

    public ProductOrderSample getProductOrderSample() {
        return productOrderSample;
    }

    public SampleValidationType getValidationType() {
        return validationType;
    }

    public SampleValidationStatus getStatus() {
        return status;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public long getModifiedBy() {
        return modifiedBy;
    }
}
