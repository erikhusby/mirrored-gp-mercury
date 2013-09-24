package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.Date;

@Stateful
public class ProductOrderSampleEtl extends GenericEntityAndStatusEtl<ProductOrderSample, ProductOrderSample> {

    public ProductOrderSampleEtl() {
    }

    @Inject
    public ProductOrderSampleEtl(ProductOrderSampleDao dao) {
        super(ProductOrderSample.class, "product_order_sample", "product_order_sample_status",
                "athena.product_order_sample_aud", "product_order_sample_id", dao);
    }

    @Override
    Long entityId(ProductOrderSample entity) {
        return entity.getProductOrderSampleId();
    }

    @Override
    Path rootId(Root<ProductOrderSample> root) {
        return root.get(ProductOrderSample_.productOrderSampleId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(ProductOrderSample.class, entityId));
    }

    @Override
    String statusRecord(String etlDateStr, boolean isDelete, ProductOrderSample entity, Date revDate) {
        if (entity != null && entity.getDeliveryStatus() != null) {
            return genericRecord(etlDateStr, isDelete,
                    entity.getProductOrderSampleId(),
                    format(revDate),
                    format(entity.getDeliveryStatus().name())
            );
        } else {
            return null;
        }
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, ProductOrderSample entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderSampleId(),
                format(entity.getProductOrder() != null ? entity.getProductOrder().getProductOrderId() : null),
                format(entity.getName()),
                format(entity.getDeliveryStatus().name()),
                format(entity.getSamplePosition())
        );
    }

}
