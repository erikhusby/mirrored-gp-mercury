package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry;
import org.primefaces.model.SelectableDataModel;

import javax.faces.model.ListDataModel;
import java.io.Serializable;
import java.util.List;

/**
 *
 */
public class ProductOrderListModel extends ListDataModel<ProductOrderListEntry> implements SelectableDataModel<ProductOrderListEntry>, Serializable {

    public ProductOrderListModel() {
    }

    private List<ProductOrderListEntry> filteredValues;

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

    public List<ProductOrderListEntry> getFilteredValues() {
        return filteredValues;
    }

    public void setFilteredValues(List<ProductOrderListEntry> filteredValues) {
        this.filteredValues = filteredValues;
    }
}
