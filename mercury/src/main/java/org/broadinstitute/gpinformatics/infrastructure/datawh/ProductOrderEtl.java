package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.Date;

@Stateful
public class ProductOrderEtl extends GenericEntityAndStatusEtl<ProductOrder, ProductOrder> {
    private BSPUserList userList;

    public ProductOrderEtl() {
    }

    @Inject
    public ProductOrderEtl(ProductOrderDao dao, BSPUserList userList) {
        super(ProductOrder.class, "product_order", "product_order_status", "athena.product_order_aud", "product_order_id", dao);
        this.userList = userList;
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
        return dataRecords(etlDateStr, isDelete, dao.findById(ProductOrder.class, entityId));
    }

    @Override
    String statusRecord(String etlDateStr, boolean isDelete, ProductOrder entity, Date statusDate) {
        if (entity != null && entity.getOrderStatus() != null) {
            return genericRecord(etlDateStr, isDelete,
                    entity.getProductOrderId(),
                    format(statusDate),
                    format(entity.getOrderStatus().getDisplayName())
            );
        }
        return null;
    }


    @Override
    String dataRecord(String etlDateStr, boolean isDelete, ProductOrder entity) {
        Long personId = entity.getCreatedBy();
        BspUser bspUser = personId != null ? userList.getById(personId) : null;

        return genericRecord(etlDateStr, isDelete,
                entity.getProductOrderId(),
                format(entity.getResearchProject() != null ? entity.getResearchProject().getResearchProjectId() : null),
                format(entity.getProduct() != null ? entity.getProduct().getProductId() : null),
                format(entity.getOrderStatus().getDisplayName()),
                format(entity.getCreatedDate()),
                format(entity.getModifiedDate()),
                format(entity.getTitle()),
                format(entity.getQuoteId()),
                format(entity.getJiraTicketKey()),
                format(bspUser != null ? bspUser.getUsername() : null),
                format(entity.getPlacedDate()),
                format(entity.getSkipRegulatoryReason())
        );
    }
}
