package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn_;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class ProductOrderAddOnEtl extends GenericEntityEtl<ProductOrderAddOn, ProductOrderAddOn> {

    public ProductOrderAddOnEtl() {
    }

    @Inject
    public ProductOrderAddOnEtl(ProductOrderDao dao) {
        super(ProductOrderAddOn.class, "product_order_add_on", "athena.product_add_ons_aud", "add_ons", dao);
    }

    @Override
    Long entityId(ProductOrderAddOn entity) {
        return entity.getProductOrderAddOnId();
    }

    @Override
    Path rootId(Root<ProductOrderAddOn> root) {
        return root.get(ProductOrderAddOn_.productOrderAddOnId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(ProductOrderAddOn.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, ProductOrderAddOn entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderAddOnId(),
                format(entity.getProductOrder() != null ? entity.getProductOrder().getProductOrderId() : null),
                format(entity.getAddOn().getProductId())
        );
    }
}
