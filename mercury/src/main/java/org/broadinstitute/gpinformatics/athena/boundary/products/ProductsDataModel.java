package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.primefaces.model.SelectableDataModel;

import javax.faces.model.ListDataModel;
import java.util.List;

public class ProductsDataModel extends ListDataModel<Product> implements SelectableDataModel<Product> {

    public ProductsDataModel(List<Product> products) {
        super(products);
    }


    @Override
    public Object getRowKey(Product rowProduct) {
        return rowProduct.getPartNumber();

    }

    @Override
    public Product getRowData(String rowKey) {
        for (Product product : (List<Product>) getWrappedData()) {
            if (product.getPartNumber().equals(rowKey))
                return product;
        }

        throw new RuntimeException("Product '" + rowKey + "' not found!");
    }


}
