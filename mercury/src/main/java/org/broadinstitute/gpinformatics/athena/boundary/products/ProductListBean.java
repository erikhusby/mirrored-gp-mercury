package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductComparator;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * Backing bean for Product list
 */
@Named("productList")
@RequestScoped
public class ProductListBean extends AbstractJsfBean implements Serializable {

    @Inject
    private ProductDao productDao;

    @Inject
    private UserBean userBean;

    private List<Product> products;

    public List<Product> getProducts() {

        if (products == null) {
            // "lazy" load, except this bean is request scoped so we end up creating a new Product list
            // for every request, including column sorts and each character typed into the search filter.
            //
            // Making a broader scoped cache of Products introduces LIEs on the Add-on and price item associations.
            // The ProductDao find method can be modified to left join fetch these associations, but the JPA criteria
            // API has some issues with left join fetching efficiently (it selects every column twice per
            // http://stackoverflow.com/questions/4511368/jpa-2-criteria-fetch-path-navigation).
            //
            // Jon is going to look into second level cache to deal with the round-trip-to-DB problem
            products = productDao.findProducts();
            Collections.sort(products, new ProductComparator());

        }

        return products;
    }


    /**
     * Used for auto-complete in the UI, given a search term
     * @param search list of search terms, whitespace separated. If more than one term is present, all terms must
     *               match a substring in the text. Search is case insensitive.
     *
     * TODO make it clear what the use case for this method is, move it to an appropriate place (not the backing bean
     * TODO for the product list), clean up case insensitivity of search
     */
    // FIXME: refactor for common cases
    public List<Product> getProductCompletions(String search) {
        List<Product> list = new ArrayList<Product>();
        String[] searchStrings = search.toLowerCase().split("\\s");

        boolean showPDMProducts = userBean.isPDMUser() || userBean.isDeveloperUser();
        List<Product> products = productDao.findProducts(
                showPDMProducts ? ProductDao.IncludePDMOnly.YES : ProductDao.IncludePDMOnly.NO);
        Collections.sort(products, new ProductComparator());

        for (Product product : products) {
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

    // TODO hmc may not be the best way to do this
    // TODO make it clear what the use case for this method is, move it to an appropriate place (not the backing bean
    // TODO for the product list), clean up case insensitivity of search, consider whitespace delimited search terms
    // TODO like the method above is doing

    public List<Product> searchProduct(String query) {
        List<Product> allProducts = productDao.findProducts();
        List<Product> products = new ArrayList<Product>();
        Date today = new Date();
        for (Product product : allProducts) {
            String queryCapitalized = query.toUpperCase();
            if ((product.isAvailable() ||
                 (product.getAvailabilityDate() != null && product.getAvailabilityDate().after(today))) &&
                (product.getPartNumber().toUpperCase().contains(queryCapitalized) ||
                 product.getProductName().toUpperCase().contains(queryCapitalized) ||
                 product.getProductFamily().getName().toUpperCase().contains(queryCapitalized))) {
                products.add(product);
            }
        }
        return products;
    }

}
