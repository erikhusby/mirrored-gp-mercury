package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;

@Stateful
public class LedgerEntryEtl extends GenericEntityEtl<LedgerEntry, LedgerEntry> {

    public LedgerEntryEtl() {
    }

    @Inject
    public LedgerEntryEtl(LedgerEntryDao dao) {
        super(LedgerEntry.class, "ledger_entry", dao);
    }

    @Override
    Long entityId(LedgerEntry entity) {
        return entity.getLedgerId();
    }

    @Override
    Path rootId(Root<LedgerEntry> root) {
        return root.get(LedgerEntry_.ledgerId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(LedgerEntry.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, LedgerEntry entity) {
        ProductOrderSample pdoSample = entity.getProductOrderSample();
        PriceItem priceItem = entity.getPriceItem();
        BillingSession billingSession = entity.getBillingSession();
        if (pdoSample == null || priceItem == null || billingSession == null) {
            return null;
        }
        return genericRecord(etlDateStr, isDelete,
                entity.getLedgerId(),
                format(pdoSample.getProductOrderSampleId()),
                format(entity.getQuoteId()),
                format(priceItem.getPriceItemId()),
                format(entity.getPriceItemType().toString()),
                format(entity.getQuantity()),
                format(billingSession.getBillingSessionId()),
                format(entity.getBillingMessage()),
                format(entity.getWorkCompleteDate())
                );
    }
}
