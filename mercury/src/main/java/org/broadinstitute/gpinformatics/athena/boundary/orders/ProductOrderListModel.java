package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.primefaces.model.SelectableDataModel;

import javax.faces.model.ListDataModel;
import java.util.List;

/**
 *
 */
public class ProductOrderListModel extends ListDataModel<ProductOrder> implements SelectableDataModel<ProductOrder> {

    public ProductOrderListModel() {
    }

    public ProductOrderListModel(List<ProductOrder> productOrders) {
        super(productOrders);
    }

    @Override
    public Object getRowKey(ProductOrder order) {
        return order.getBusinessKey();
    }

    @Override
    public ProductOrder getRowData(String rowKey) {
        for (ProductOrder order : this) {
            if (order.getBusinessKey().equals(rowKey)) {
                return order;
            }
        }

        return null;
    }
}
