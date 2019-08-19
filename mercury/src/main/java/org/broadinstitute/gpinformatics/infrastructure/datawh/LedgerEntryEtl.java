package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.Optional;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class LedgerEntryEtl extends GenericEntityEtl<LedgerEntry, LedgerEntry> {

    public LedgerEntryEtl() {
    }

    @Inject
    public LedgerEntryEtl(LedgerEntryDao dao) {
        super(LedgerEntry.class, "ledger_entry", "athena.billing_ledger_aud", "ledger_id", dao);
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
        Optional<PriceItem> optionalPriceItem = Optional.ofNullable(entity.getPriceItem());
        Optional<Product> optionalProduct = Optional.ofNullable(entity.getProduct());
        if (pdoSample == null) {
            return null;
        }
        BillingSession billingSession = entity.getBillingSession();
        LedgerEntry.PriceItemType priceItemType = entity.getPriceItemType();
        return genericRecord(etlDateStr, isDelete,
            entity.getLedgerId(),
            format(pdoSample.getProductOrderSampleId()),
            format(entity.getQuoteId()),
            optionalPriceItem.map(priceItem -> format(priceItem.getPriceItemId())).orElse(""),
            format(priceItemType != null ? priceItemType.toString() : null),
            format(entity.getQuantity()),
            format(billingSession != null ? billingSession.getBillingSessionId() : null),
            format(entity.getBillingMessage()),
            format(entity.getWorkCompleteDate()),
            format(entity.getWorkItem()),
            format(entity.getSapDeliveryDocumentId()),
            optionalProduct.map(product -> format(product.getProductId())).orElse("")
        );
    }
}
