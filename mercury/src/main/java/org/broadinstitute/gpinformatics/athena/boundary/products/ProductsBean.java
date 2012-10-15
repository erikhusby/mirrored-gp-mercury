package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;

import javax.enterprise.context.RequestScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao.AvailableProductsOnly.NO;
import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao.AvailableProductsOnly.YES;


@Named
@RequestScoped
public class ProductsBean extends AbstractJsfBean {
    @Inject
    private ProductDao productDao;

    private Product selectedProduct = null;

    private ProductsDataModel productsDataModel = new ProductsDataModel();

    private boolean rebuild = true;

    private boolean availableProductsOnly = false;

    private List<Product> filteredProducts;

    private List<Product> selectedProductAddOns;

    private List<PriceItem> selectedProductPriceItems;

    private static final DateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");


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
        selectedProduct = (Product) event.getObject();
        selectedProductAddOns = new ArrayList<Product>(selectedProduct.getAddOns());
        selectedProductPriceItems = new ArrayList<PriceItem>(selectedProduct.getPriceItems());
    }


    public void onRowUnselect(UnselectEvent event) {
        selectedProduct = null;
    }

    public int compare(Object o1, Object o2) {
        if (o1 == o2)
            return 0;

        if (o1 == null)
            return -1;

        if (o2 == null)
            return 1;

        return ((Comparable) o1).compareTo(o2);
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }


    public String getSelectedProductAvailabilityDate() {
        if (selectedProduct == null || selectedProduct.getAvailabilityDate() == null) {
            return "";
        }

        return dateFormat.format(selectedProduct.getAvailabilityDate());
    }


    public String getSelectedProductDiscontinuedDate() {
        if (selectedProduct == null || selectedProduct.getDiscontinuedDate() == null) {
            return "";
        }

        return dateFormat.format(selectedProduct.getDiscontinuedDate());
    }


    public boolean isPriceItemDefaultForSelected(PriceItem priceItem) {
        if (selectedProduct == null) {
            return false;
        }

        if (selectedProduct.getDefaultPriceItem() == null && priceItem == null) {
            return true;
        }

        if (selectedProduct.getDefaultPriceItem() == null || priceItem == null) {
            return false;
        }

        return (selectedProduct.getDefaultPriceItem().equals(priceItem));

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
        rebuild = true;
    }

    public List<Product> getSelectedProductAddOns() {
        return selectedProductAddOns;
    }

    public List<PriceItem> getSelectedProductPriceItems() {
        return selectedProductPriceItems;
    }
}
