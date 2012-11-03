package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Date;

@Stateless
public class ProductOrderSampleStatusEtl extends GenericEntityEtl {
    @Inject
    ProductOrderSampleDao dao;

    @Override
    Class getEntityClass() {
        return ProductOrderSample.class;
    }

    @Override
    String getBaseFilename() {
        return "product_order_sample_status";
    }

    @Override
    Long entityId(Object entity) {
        return ((ProductOrderSample)entity).getProductOrderSampleId();
    }

    /** This entity does not make entity records. */
    @Override
    String entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        return null;
    }


    /**
     * Makes a data record from entity status fields, and possible the Envers revision date,
     * in a format that matches the corresponding SqlLoader control file.
     * @param etlDateStr date
     * @param revDate Envers revision date
     * @param revObject the Envers versioned entity
     * @return delimited SqlLoader record, or null if entity does not support status recording
     */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object revObject) {
        ProductOrderSample entity = (ProductOrderSample)revObject;
        if (entity == null || entity.getBillingStatus() == null) {
            return null;
        } else {
            return genericRecord(etlDateStr, false,
                    entity.getProductOrderSampleId(),
                    format(entity.getProductOrder() != null ? entity.getProductOrder().getProductOrderId() : null),
                    format(revDate),
                    format(entity.getBillingStatus().getDisplayName()));
        }
    }

    /** This entity etl does not make entity records. */
    @Override
    boolean isEntityEtl() {
        return false;
    }

}
