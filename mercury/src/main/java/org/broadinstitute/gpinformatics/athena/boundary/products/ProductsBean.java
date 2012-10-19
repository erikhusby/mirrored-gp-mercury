package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.DataTableFilteredValuesBean;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


/**
 * Backing bean for Products list/view and CRUD pages
 */
@Named
@RequestScoped
public class ProductsBean extends AbstractJsfBean implements Serializable {

//    @Inject
//    private DataTableFilteredValuesBean filteredValuesBean;

    @Inject
    private ProductDao productDao;

    private Product selectedProduct;

    private ProductsDataModel productsDataModel;

    private List<Product> selectedProductAddOns;

    private List<PriceItem> selectedProductPriceItems;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");

    /**
     * Hook the JSF preRenderView event to explicitly initiate a long-running conversation in the conversation scoped
     * {@link DataTableFilteredValuesBean}
     */
//    public void onPreRenderView() {
//        filteredValuesBean.beginConversation();
//    }


    public ProductsDataModel getProductsDataModel() {


        if (productsDataModel == null) {
            // "lazy" load, except this bean is request scoped so we end up creating a new ProductsDataModel
            // for every request, including column sorts and each character typed into the search filter.
            //
            // Making a broader scoped cache of Products introduces LIEs on the add-on and price item associations.
            // The ProductDao find method can be modified to left join fetch these associations, but the JPA criteria
            // API has some issues with left join fetching efficiently (it selects every column twice per
            // http://stackoverflow.com/questions/4511368/jpa-2-criteria-fetch-path-navigation).
            productsDataModel = new ProductsDataModel(productDao.findProducts());
        }

        return productsDataModel;
    }


    /**
     * Row selection handler
     */
    public void onRowSelect(SelectEvent event) {
        selectedProduct = (Product) event.getObject();
        selectedProductAddOns = new ArrayList<Product>(selectedProduct.getAddOns());
        selectedProductPriceItems = new ArrayList<PriceItem>(selectedProduct.getPriceItems());
    }


    /**
     * Row deselection handler
     */
    public void onRowUnselect(UnselectEvent ignored) {
        selectedProduct = null;
    }


    /**
     * Generic comparison method for column sorts.  Assumes passed in parameters implement {@link Comparable}
     */
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


    /**
     * Handle nulls and date formatting
     *
     * @return
     */
    public String getSelectedProductAvailabilityDate() {
        if (selectedProduct == null || selectedProduct.getAvailabilityDate() == null) {
            return "";
        }

        return DATE_FORMAT.format(selectedProduct.getAvailabilityDate());
    }

    /**
     * Handle nulls and date formatting
     *
     * @return
     */
    public String getSelectedProductDiscontinuedDate() {
        if (selectedProduct == null || selectedProduct.getDiscontinuedDate() == null) {
            return "";
        }

        return DATE_FORMAT.format(selectedProduct.getDiscontinuedDate());
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

//    public List<Product> getFilteredProducts() {
//        return filteredValuesBean.getFilteredValues();
//    }

//    public void setFilteredProducts(List<Product> filteredProducts) {
//        filteredValuesBean.setFilteredValues(filteredProducts);
//    }

    public List<Product> getSelectedProductAddOns() {
        return selectedProductAddOns;
    }

    public List<PriceItem> getSelectedProductPriceItems() {
        return selectedProductPriceItems;
    }
}
