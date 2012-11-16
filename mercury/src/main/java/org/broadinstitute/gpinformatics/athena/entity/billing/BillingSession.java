package org.broadinstitute.gpinformatics.athena.entity.billing;


import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.Date;

/**
 * This handles the billing session
 *
 * @author hrafal
 */
@Entity
@Audited
@Table(name= "BILLING_SESSION", schema = "athena")
public class BillingSession {
    @Id
    @SequenceGenerator(name = "SEQ_BILLING_SESSION", schema = "athena", sequenceName = "SEQ_BILLING_SESSION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BILLING_SESSION")
    private Long billingSessionId;

    @Column(name="CREATED_DATE")
    private Date createdDate;

    @Column(name="CREATED_BY")
    private Long createdBy;

    @Column(name="BILLED_DATE")
    private Date billedDate;

    BillingSession() {}

    public BillingSession(@Nonnull Long createdBy) {
        this.createdBy = createdBy;
        this.createdDate = new Date();
    }

    public Long getBillingSessionId() {
        return billingSessionId;
    }

    public void setBillingSessionId(Long billingSessionId) {
        this.billingSessionId = billingSessionId;
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

    public Date getBilledDate() {
        return billedDate;
    }

    public void setBilledDate(Date billedDate) {
        this.billedDate = billedDate;
    }
}
