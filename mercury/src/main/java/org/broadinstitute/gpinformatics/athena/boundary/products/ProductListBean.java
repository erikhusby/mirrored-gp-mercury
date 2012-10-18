package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.Serializable;
import java.util.List;


/**
 * Singleton cache of {@link Product}s.
 */
@Named
@Singleton
public class ProductListBean implements Serializable {

    @Inject
    private ProductDao productDao;

    private List<Product> products;

    @PostConstruct
    public void postConstruct() {
        products = productDao.findProducts();
    }

    public List<Product> getProducts() {
        return products;
    }
}
