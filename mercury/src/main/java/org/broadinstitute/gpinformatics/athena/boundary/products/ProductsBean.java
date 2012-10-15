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
import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao.AvailableProductsOnly.NO;
import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao.AvailableProductsOnly.YES;


@Named
@RequestScoped
public class ProductsBean extends AbstractJsfBean {
    @Inject
    private ProductDao productDao;

    private Product selectedProduct;

    private final ProductsDataModel productsDataModel = new ProductsDataModel();

    private boolean rebuild = true;

    private boolean availableProductsOnly;

    private List<Product> filteredProducts;

    private List<Product> selectedProductAddOns;

    private List<PriceItem> selectedProductPriceItems;


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

    /**
     * Used for auto-complete in the UI, given a search term
     * @param search list of search terms, whitespace separated. If more than one term is present, all terms must
     *               match a substring in the text. Search is case insensitive.
     */
    // FIXME: refactor for common cases
    public List<Product> getProductCompletions(String search) {
        List<Product> list = new ArrayList<Product>();
        String[] searchStrings = search.toLowerCase().split("\\s");

        for (Product product : getProductsDataModel()) {
            String label = product.getProductName().toLowerCase();
            boolean include = true;
            for (String s : searchStrings) {
                if (!label.contains(s)) {
                    include = false;
                    break;
                }
            }
            if (include) {
                list.add(product);
            }
        }

        return list;
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
        rebuild = true;
    }

    public List<Product> getSelectedProductAddOns() {
        return selectedProductAddOns;
    }

    public List<PriceItem> getSelectedProductPriceItems() {
        return selectedProductPriceItems;
    }
}
