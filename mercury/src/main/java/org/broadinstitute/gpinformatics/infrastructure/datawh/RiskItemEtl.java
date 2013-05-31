package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.RiskItemDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.orders.RiskItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.RiskItem_;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// This is a "cross etl" class that takes in RiskItem and outputs ProductOrderSample updates.

@Stateful
public class RiskItemEtl extends GenericEntityEtl<RiskItem, ProductOrderSample> {
    private ProductOrderSampleDao pdoSampleDao;

    public RiskItemEtl() {
    }

    @Inject
    public RiskItemEtl(RiskItemDao dao, ProductOrderSampleDao pdoSampleDao) {
        super(RiskItem.class, "product_order_sample_risk", dao);
        this.pdoSampleDao = pdoSampleDao;
    }

    @Override
    Long entityId(RiskItem entity) {
        return entity.getRiskItemId();
    }

    @Override
    Path rootId(Root root) {
        return root.get(RiskItem_.riskItemId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(ProductOrderSample.class, entityId));
    }

    @Override
    protected Collection<Long> convertAuditedEntityIdToDataSourceEntityId(Collection<Long> auditIds) {
        String queryString = "select distinct product_order_sample entity_id from ATHENA.PO_SAMPLE_RISK_JOIN_AUD " +
                             " where product_order_sample is not null and risk_item_id in (" + IN_CLAUSE_PLACEHOLDER
                             + ")";
        return lookupAssociatedIds(auditIds, queryString);
    }

    @Override
    protected Collection<ProductOrderSample> convertAuditedEntityToDataSourceEntity(
            Collection<RiskItem> auditEntities) {
        Collection<Long> riskIds = new ArrayList<Long>();
        for (RiskItem auditedEntity : auditEntities) {
            riskIds.add(auditedEntity.getRiskItemId());
        }
        List<Long> pdoSampleIds = new ArrayList<Long>(convertAuditedEntityIdToDataSourceEntityId(riskIds));
        return pdoSampleDao
                .findListByList(ProductOrderSample.class, ProductOrderSample_.productOrderSampleId, pdoSampleIds);
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, ProductOrderSample entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderSampleId(),
                format(entity.isOnRisk()),
                format(entity.getRiskTypeString()),
                format(entity.getRiskString())
        );
    }
}
