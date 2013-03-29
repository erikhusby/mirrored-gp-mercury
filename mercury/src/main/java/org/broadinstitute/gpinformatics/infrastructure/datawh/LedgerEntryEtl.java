package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

// This is a "cross etl" class that takes in LedgerEntry and outputs ProductOrderSample updates.

@Stateful
public class LedgerEntryEtl extends GenericEntityEtl<LedgerEntry, ProductOrderSample> {

    public LedgerEntryEtl() {
        entityClass = LedgerEntry.class;
        baseFilename = "product_order_sample_bill";
    }

    @Inject
    public LedgerEntryEtl(ProductOrderSampleDao d) {
        this();
        dao = d;
    }

    @Override
    Long entityId(LedgerEntry entity) {
        return entity.getLedgerId();
    }

    @Override
    Path rootId(Root root) {
        return root.get(LedgerEntry_.ledgerId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(ProductOrderSample.class, entityId));
    }

    @Override
    protected Collection<Long> convertIdsTtoC(Collection<Long> auditIds) {
        String queryString = "select distinct product_order_sample_id entity_id from ATHENA.BILLING_LEDGER_AUD " +
                " where ledger_id IN ( " + IN_CLAUSE_PLACEHOLDER + " )";
        return lookupAssociatedIds(auditIds, queryString);
    }

    @Override
    protected Collection<ProductOrderSample> convertTtoC(Collection<LedgerEntry> auditEntities) {
        Set<ProductOrderSample> pdoSamples = new HashSet<ProductOrderSample>();
        for (LedgerEntry ledgerEntry : auditEntities) {
            pdoSamples.add(ledgerEntry.getProductOrderSample());
        }
        return pdoSamples;
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, ProductOrderSample entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderSampleId(),
                format(entity.getBillableLedgerItems().size() == 0)
        );
    }

}
