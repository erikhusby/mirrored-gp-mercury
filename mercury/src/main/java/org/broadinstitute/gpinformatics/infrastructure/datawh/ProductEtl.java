package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class ProductEtl extends GenericEntityEtl<Product, Product> {
    public ProductEtl() {
    }

    /**
     * This method is used for just the Database Free tests.
     *
     * @param dao the {@link ProductDao} to use with this bean
     */
    @Inject
    public ProductEtl(ProductDao dao) {
        super(Product.class, "product", "athena.product_aud", "product_id", dao);
    }

    @Override
    Long entityId(Product entity) {
        return entity.getProductId();
    }

    @Override
    Path rootId(Root<Product> root) {
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
                format(entity.getPrimaryPriceItem() != null ? entity.getPrimaryPriceItem().getPriceItemId() : null),
                format(entity.getAggregationDataType()),
                format(entity.isExternalOnlyProduct()),
                format(entity.isSavedInSAP())
        );
    }
}
