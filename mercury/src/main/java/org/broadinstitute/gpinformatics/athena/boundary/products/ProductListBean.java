package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.DataTableFilteredValuesBean;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * Backing bean for Products list/view and CRUD pages
 */
@Named("productList")
@RequestScoped
public class ProductListBean extends AbstractJsfBean implements Serializable {

    @Inject
    private DataTableFilteredValuesBean filteredValuesBean;

    @Inject
    private ProductDao productDao;

    private ProductsDataModel productsDataModel;

    /**
     * Hook the JSF preRenderView event to explicitly initiate a long-running conversation in the conversation scoped
     * {@link DataTableFilteredValuesBean}
     */
    public void onPreRenderView() {
        filteredValuesBean.beginConversation();
    }


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


    public List<Product> getFilteredValues() {
        return filteredValuesBean.getFilteredValues();
    }


    public void setFilteredValues(List<Product> filteredValues) {
        filteredValuesBean.setFilteredValues(filteredValues);
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


}
