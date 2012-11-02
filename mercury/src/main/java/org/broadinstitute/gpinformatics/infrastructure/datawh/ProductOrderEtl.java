package org.broadinstitute.gpinformatics.infrastructure.datawh;

import oracle.sql.DATE;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class ProductOrderEtl extends GenericEntityEtl {
    @Inject
    ProductOrderDao dao;

    @Override
    Class getEntityClass() {
        return ProductOrder.class;
    }

    @Override
    String getBaseFilename() {
        return "product_order";
    }

    @Override
    Long entityId(Object entity) {
        return ((ProductOrder)entity).getProductOrderId();
    }

    /**
     * Makes a data record from selected entity fields, in a format that matches the corresponding
     * SqlLoader control file.
     * @param etlDateStr date
     * @param isDelete indicates deleted entity
     * @param entityId look up this entity
     * @return delimited SqlLoader record
     */
    @Override
    String makeRecord(String etlDateStr, boolean isDelete, Long entityId) {
        ProductOrder entity = dao.findById(ProductOrder.class, entityId);
        if (entity == null) {
            return null;
        } else {
            return Util.makeRecord(etlDateStr, false,
                    entity.getProductOrderId(),
                    Util.format(entity.getResearchProject().getResearchProjectId()),
                    Util.format(entity.getProduct() != null ? entity.getProduct().getProductId() : null),
                    Util.format(entity.getOrderStatus().getDisplayName()),
                    Util.format(entity.getCreatedDate()),
                    Util.format(entity.getModifiedDate()),
                    Util.format(entity.getTitle()),
                    Util.format(entity.getQuoteId()),
                    Util.format(entity.getJiraTicketKey()));
        }
    }
}
