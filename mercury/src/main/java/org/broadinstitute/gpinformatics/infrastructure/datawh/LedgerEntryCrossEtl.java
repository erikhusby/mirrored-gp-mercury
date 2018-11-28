package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

// This is a "cross etl" class that takes in LedgerEntry and outputs ProductOrderSample updates.

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class LedgerEntryCrossEtl extends GenericEntityEtl<LedgerEntry, ProductOrderSample> {

    public LedgerEntryCrossEtl() {
    }

    @Inject
    public LedgerEntryCrossEtl(ProductOrderSampleDao dao) {
        super(LedgerEntry.class, "product_order_sample_bill", "athena.billing_ledger_aud", "ledger_id", dao);
    }

    @Override
    Long entityId(LedgerEntry entity) {
        return entity.getLedgerId();
    }

    @Override
    protected Long dataSourceEntityId(ProductOrderSample entity) {
        return entity.getProductOrderSampleId();
    }

    @Override
    Path rootId(Root<LedgerEntry> root) {
        return root.get(LedgerEntry_.ledgerId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(ProductOrderSample.class, entityId));
    }

    @Override
    protected Collection<Long> convertAuditedEntityIdToDataSourceEntityId(Collection<Long> auditIds) {
        String queryString = "select distinct product_order_sample_id entity_id from ATHENA.BILLING_LEDGER_AUD " +
                " where product_order_sample_id is not null and ledger_id IN ( " + IN_CLAUSE_PLACEHOLDER + " )";
        return lookupAssociatedIds(auditIds, queryString);
    }

    @Override
    protected Collection<ProductOrderSample> convertAuditedEntityToDataSourceEntity(Collection<LedgerEntry> auditEntities) {
        Set<ProductOrderSample> pdoSamples = new HashSet<>();
        for (LedgerEntry ledgerEntry : auditEntities) {
            pdoSamples.add(ledgerEntry.getProductOrderSample());
        }
        return pdoSamples;
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, ProductOrderSample entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderSampleId(),
                format(entity.isCompletelyBilled())
        );
    }
}
