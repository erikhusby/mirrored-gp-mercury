package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;


/**
 * Backing bean for Product list
 */
@Named("productList")
@RequestScoped
public class ProductListBean extends AbstractJsfBean implements Serializable {

    @Inject
    private ProductDao productDao;

    private List<Product> products;

    public List<Product> getProducts() {

        if (products == null) {
            // "lazy" load, except this bean is request scoped so we end up creating a new Product list
            // for every request, including column sorts and each character typed into the search filter.
            //
            // Jon is going to look into second level cache to deal with the round-trip-to-DB problem
            products = productDao.findProductsForProductList();
            Collections.sort(products);

        }

        return products;
    }
}
