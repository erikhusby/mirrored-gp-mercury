package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.ejb.Stateful;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Stateful
public class ProductOrderSampleStatusEtl extends GenericEntityEtl {

    /** {@inheritDoc} */
    @Override
    Class getEntityClass() {
        return ProductOrderSample.class;
    }

    /** {@inheritDoc} */
    @Override
    String getBaseFilename() {
        return "product_order_sample_status";
    }

    /** {@inheritDoc} */
    @Override
    Long entityId(Object entity) {
        return ((ProductOrderSample)entity).getProductOrderSampleId();
    }

    /** This entity does not make entity records. */
    @Override
    Collection<String> entityRecord(String etlDateStr, boolean isDelete, Long entityId) {
        return Collections.emptyList();
    }

    /** This entity does not make entity records. */
    @Override
    Collection<String> entityRecordsInRange(long startId, long endId, String etlDateStr, boolean isDelete) {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    String entityStatusRecord(String etlDateStr, Date revDate, Object revObject, boolean isDelete) {
        ProductOrderSample entity = (ProductOrderSample)revObject;
        if (entity == null) {
            logger.info("Cannot export. Audited ProductOrderSample object is null.");
            return null;
        }

        // Skips entity changes that don't affect status (i.e. status will be null in the Envers entity).
        if (entity.getDeliveryStatus() == null) {
            return null;
        }

        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderSampleId(),
                format(revDate),
                format(entity.getDeliveryStatus().name())
        );
    }

    /** This entity etl does not make entity records. */
    @Override
    boolean isEntityEtl() {
        return false;
    }

}
