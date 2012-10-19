package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;

import javax.enterprise.context.RequestScoped;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Boundary bean for working with products.
 *
 */
@Named
@RequestScoped
public class ProductBean {

    @Inject
    private ProductDao productDao;

    @Inject
    private ProductFamilyDao productFamilyDao;

    private List<Product> allProducts;


    public List<Product> getAllProducts() {
        if (allProducts == null) {
            allProducts = productDao.findProducts();
        }
        return allProducts;
    }

    /**
     * Returns a list of SelectItems for all product families
     *
     * @return list of product families
     */
    public List<SelectItem> getProductFamilies() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        Set<String> aList = new HashSet<String>();
        for (ProductFamily productFamily :  productFamilyDao.findAll()) {
            items.add(new SelectItem(productFamily.getProductFamilyId(), productFamily.getName()));
        }
        return items;
    }


}
