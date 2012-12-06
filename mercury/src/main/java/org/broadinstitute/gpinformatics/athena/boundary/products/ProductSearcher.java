package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Named
public class ProductSearcher {

    @Inject
    private ProductDao productDao;

    /**
     * Case insensitive search in product names and part numbers on tokenized search words split by whitespace.
     *
     * @param searchText
     *
     * @param products
     *
     * @return
     */
    private List<Product> findInProducts(String searchText, List<Product> products) {
        List<Product> list = new ArrayList<Product>();
        String[] searchWords = searchText.split("\\s");

        Collections.sort(products);

        for (Product product : products) {
            for (String searchWord : searchWords) {
                if (StringUtils.containsIgnoreCase(product.getProductName(), searchWord) ||
                    StringUtils.containsIgnoreCase(product.getPartNumber(), searchWord)) {
                    list.add(product);
                    break;
                }
            }
        }
        return list;
    }


    /**
     * Support finding products for product ordering, sensitive to the user's role as PDM (or pseudo-PDM developer).
     * Only available and top-level products will be returned.  Case insensitive search on tokenized search words
     * split by whitespace.
     *
     * @param searchText
     *
     * @return
     */
    public List<Product> searchProductsForProductOrder(String searchText) {

        return findInProducts(searchText, productDao.findTopLevelProductsForProductOrder());
    }


    /**
     * Support finding add-on products for product definition, disallowing the top-level product as a search result
     *
     * @param searchText
     *
     * @return
     */
    public List<Product> searchProductsForAddonsInProductEdit(Product topLevelProduct, String searchText) {
        List<Product> products = productDao.findProducts(
                ProductDao.Availability.CURRENT_OR_FUTURE,
                ProductDao.TopLevelOnly.NO,
                ProductDao.IncludePDMOnly.NO);

        // remove top level product from the list if it's showing up there
        products.remove(topLevelProduct);

        return findInProducts(searchText, products);

    }



}
