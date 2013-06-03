package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;

@Stateful
public class ProductEtl extends GenericEntityEtl<Product, Product> {
    public ProductEtl() {
    }

    @Inject
    public ProductEtl(ProductDao dao) {
        super(Product.class, "product", dao);
    }

    @Override
    Long entityId(Product entity) {
        return entity.getProductId();
    }

    @Override
    Path rootId(Root root) {
        return root.get(Product_.productId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(Product.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, Product entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getProductId(),
                format(entity.getProductName()),
                format(entity.getPartNumber()),
                format(entity.getAvailabilityDate()),
                format(entity.getDiscontinuedDate()),
                format(entity.getExpectedCycleTimeSeconds()),
                format(entity.getGuaranteedCycleTimeSeconds()),
                format(entity.getSamplesPerWeek()),
                format(entity.isTopLevelProduct()),
                format(entity.getWorkflowName()),
                format(entity.getProductFamily() != null ? entity.getProductFamily().getName() : null),
                format(entity.getPrimaryPriceItem() != null ? entity.getPrimaryPriceItem().getPriceItemId() : null)
        );
    }
}
