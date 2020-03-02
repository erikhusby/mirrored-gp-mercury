/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2020 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.billing;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Audited
@Table(name= "BILLING_CREDIT_ITEM", schema = "athena")
public class BillingCreditItem implements Serializable {
    private static final long serialVersionUID = 4744695001908815106L;

    @Id
    @SequenceGenerator(name = "SEQ_BILLING_CREDIT_ITEM", schema = "athena", sequenceName = "SEQ_BILLING_CREDIT_ITEM", allocationSize = 50)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BILLING_CREDIT_ITEM")
    private Long BillingCreditItemId;

    @Column(name = "QUANTITY_CREDITED")
    private BigDecimal quantityCredited=BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "LEDGER_CREDIT_SOURCE_ID")
    private LedgerEntry ledgerEntry;

    public BillingCreditItem(LedgerEntry ledgerEntry, BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Credit quantities should have positive values");
        }
        this.ledgerEntry = ledgerEntry;
        this.quantityCredited = quantity;
    }

    public BillingCreditItem() {
    }

    public void setLedgerEntry(LedgerEntry ledgerEntry) {
        this.ledgerEntry = ledgerEntry;
    }

    public LedgerEntry getLedgerEntry() {
        return ledgerEntry;
    }


    public void setQuantityCredited(BigDecimal quantityCredited) {
        this.quantityCredited = quantityCredited;
    }

    public BigDecimal getQuantityCredited() {
        return quantityCredited;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof BillingCreditItem)) {
            return false;
        }

        BillingCreditItem that = (BillingCreditItem) other;

        return new EqualsBuilder()
            .append(getQuantityCredited(), that.getQuantityCredited())
            .append(getLedgerEntry(), that.getLedgerEntry())
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(getQuantityCredited())
            .append(getLedgerEntry())
            .toHashCode();
    }
}
