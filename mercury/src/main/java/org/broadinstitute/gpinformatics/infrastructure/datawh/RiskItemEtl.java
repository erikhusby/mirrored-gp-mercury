package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.RiskItemDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
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
        entityClass = RiskItem.class;
        baseFilename = "product_order_sample_risk";
    }

    @Inject
    public RiskItemEtl(RiskItemDao d, ProductOrderSampleDao d2) {
        this();
        dao = d;
        pdoSampleDao = d2;
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
    protected Collection<Long> convertIdsTtoC(Collection<Long> auditIds) {
        String queryString = "select distinct product_order_sample from ATHENA.PO_SAMPLE_RISK_JOIN_AUD " +
                " where risk_item_id in (" + IN_CLAUSE_PLACEHOLDER + ")";
        return lookupAssociatedIds(auditIds, queryString);
    }

    @Override
    protected Collection<ProductOrderSample> convertTtoC(Collection<RiskItem> auditEntities) {
        Collection<Long> riskIds = new ArrayList<Long>();
        for (RiskItem auditedEntity : auditEntities) {
            riskIds.add(auditedEntity.getRiskItemId());
        }
        List<Long> pdoSampleIds = (List<Long>)convertIdsTtoC(riskIds);
        return pdoSampleDao.findListByList(ProductOrderSample.class, null, pdoSampleIds);
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, ProductOrderSample entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderSampleId(),
                format(entity.isOnRisk())
        );
    }
}
