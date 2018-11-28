package org.broadinstitute.gpinformatics.athena.entity.orders;

import javax.ejb.ApplicationException;

/**
 */
@ApplicationException(rollback = true)
public class StaleLedgerUpdateException extends Exception {

    private ProductOrderSample.LedgerUpdate ledgerUpdate;

    public StaleLedgerUpdateException(ProductOrderSample.LedgerUpdate ledgerUpdate) {
        super(String.format(
                "The quantity being billed for %s changed from %s to %s before this request for %s could be submitted.",
                ledgerUpdate.getSampleName(), ledgerUpdate.getOldQuantity(), ledgerUpdate.getCurrentQuantity(),
                ledgerUpdate.getNewQuantity()));
        this.ledgerUpdate = ledgerUpdate;
    }

    public ProductOrderSample.LedgerUpdate getLedgerUpdate() {
        return ledgerUpdate;
    }
}
