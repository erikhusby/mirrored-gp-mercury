package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Copy contents/changes of PDO RegulatoryInfo ManyToMany relationship to warehouse
 * Denormalized and linked OneToMany to PRODUCT_ORDER in warehouse using PDO_REGULATORY_INFOS table.
 * (As of 04/2015, less than 10% of PDOs have multiple regulatory info)
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class PDORegulatoryInfoEtl extends GenericEntityEtl<ProductOrder,ProductOrder> {

    public PDORegulatoryInfoEtl() {
    }

    @Inject
    public PDORegulatoryInfoEtl(ProductOrderDao dao) {
        super(ProductOrder.class, "pdo_regulatory_infos", "athena.product_order_aud", "product_order_id", dao);
    }

    @Override
    Long entityId(ProductOrder entity) {
        return entity.getProductOrderId();
    }

    @Override
    Path rootId(Root<ProductOrder> root) {
        return root.get(ProductOrder_.productOrderId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        ProductOrder productOrder = dao.findById(ProductOrder.class, entityId);
        return dataRecords(etlDateStr, isDelete, productOrder);
    }

    /**
     * Export RegulatoryInfo related to each PDO
     * @param etlDateStr
     * @param isDelete
     * @param productOrder
     * @return
     */
    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, ProductOrder productOrder) {
        Collection<String> records = new ArrayList<>();
        if( !isDelete ) {
            for(RegulatoryInfo regInfo : productOrder.getRegulatoryInfos()) {
                records.add( genericRecord(etlDateStr, isDelete,
                                productOrder.getProductOrderId(),
                                regInfo.getRegulatoryInfoId(),
                                format(regInfo.getIdentifier()),
                                format(regInfo.getType().getName()),
                                format(regInfo.getName()) )
                );
            }
        }
        return records;
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, ProductOrder entity) {
        return null;
    }
}
