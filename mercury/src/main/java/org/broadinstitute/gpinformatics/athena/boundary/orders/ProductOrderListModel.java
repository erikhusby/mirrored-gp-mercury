package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.ProductOrderListEntry;
import org.primefaces.model.SelectableDataModel;

import javax.faces.model.ListDataModel;
import java.util.List;

/**
 *
 */
public class ProductOrderListModel extends ListDataModel<ProductOrderListEntry> implements SelectableDataModel<ProductOrderListEntry> {

    public ProductOrderListModel() {
    }

    public ProductOrderListModel(List<ProductOrderListEntry> productOrders) {
        super(productOrders);
    }

    @Override
    public Object getRowKey(ProductOrderListEntry order) {
        return order.getBusinessKey();
    }

    @Override
    public ProductOrderListEntry getRowData(String rowKey) {
        for (ProductOrderListEntry order : this) {
            if (order.getBusinessKey().equals(rowKey)) {
                return order;
            }
        }

        return null;
    }
}
