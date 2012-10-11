package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;

import javax.faces.bean.SessionScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao.AvailableProductsOnly.NO;
import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao.AvailableProductsOnly.YES;


@Named
@SessionScoped
public class ProductsBean {

    @Inject
    private ProductDao productDao;

    private Product selectedProduct = null;

    private ProductsDataModel productsDataModel = new ProductsDataModel();

    private boolean rebuild = true;

    private boolean availableProductsOnly = false;

    // initializing this to an empty list makes there be no results on startup
    private List<Product> filteredProducts;


    public ProductsDataModel getProductsDataModel() {

        if (rebuild) {

            // doing an explicit assignment to a temporary variable to highlight the strong type
            ProductDao.AvailableProductsOnly availableOnly = availableProductsOnly ? YES : NO;
            productsDataModel.setWrappedData(productDao.findProducts(availableOnly));

            rebuild = false;
        }

        return productsDataModel;
    }


    public void onRowSelect(SelectEvent event) {
        this.selectedProduct = (Product) event.getObject();
    }


    public void onRowUnselect(UnselectEvent event) {
        this.selectedProduct = null;
    }

    public int genericCompare(Object o1, Object o2) {
        CompareToBuilder builder = new CompareToBuilder();
        builder.append(o1, o2);
        return builder.build();
    }


    public Product getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(Product selectedProduct) {
        this.selectedProduct = selectedProduct;
    }

    public boolean isAvailableProductsOnly() {
        return availableProductsOnly;
    }

    public void setAvailableProductsOnly(boolean availableProductsOnly) {
        this.availableProductsOnly = availableProductsOnly;
    }

    public List<Product> getFilteredProducts() {
        return filteredProducts;
    }

    public void setFilteredProducts(List<Product> filteredProducts) {
        this.filteredProducts = filteredProducts;
    }


    public void onAvailableProductsOnly(AjaxBehaviorEvent ignored) {
        availableProductsOnly = !availableProductsOnly;
        rebuild = true;
    }
}
