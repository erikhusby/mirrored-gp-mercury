package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

@Stateful
@RequestScoped
/**
 *
 * Dao for {@link Product}s, supporting the browse and CRUD UIs.
 *
 */
public class ProductDao extends GenericDao implements Serializable {

    @Inject
    private UserBean userBean;

    /**
     * Preferring strong types to booleans
     */
    public enum Availability {
        ALL,
        CURRENT,
        CURRENT_OR_FUTURE
    }

    public enum TopLevelOnly {
        YES,
        NO
    }

    public enum IncludePDMOnly {
        YES,
        NO
    }

    /**
     * General purpose product finder method
     *
     * @param availability Do we only want to get available products
     * @param topLevelOnly Do we only want to get top level products
     * @param includePDMOnly Do we only want PDM products
     *
     * @return The chosen products
     */
    public List<Product> findProducts(@Nonnull Availability availability,
                                      @Nonnull TopLevelOnly topLevelOnly,
                                      @Nonnull IncludePDMOnly includePDMOnly) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        List<Predicate> predicateList = new ArrayList<>();
        cq.distinct(true);

        Root<Product> product = cq.from(Product.class);


        switch (availability) {

            case CURRENT:
                // there is an availability date
                predicateList.add(cb.isNotNull(product.get(Product_.availabilityDate)));
                // and it is in the past
                predicateList.add(cb.lessThan(product.get(Product_.availabilityDate), Calendar.getInstance().getTime()));

                // fall through to get the discontinued date!

            case CURRENT_OR_FUTURE:

                // the discontinued date is null or in the future
                predicateList.add(
                        cb.or( cb.isNull(product.get(Product_.discontinuedDate)),
                                cb.greaterThan(product.get(Product_.discontinuedDate), Calendar.getInstance().getTime()))
                );

                break;

            case ALL:
            default:
                break;

        }

        if (topLevelOnly == TopLevelOnly.YES) {
            predicateList.add(cb.equal(product.get(Product_.topLevelProduct), true));
        }

        if (includePDMOnly == IncludePDMOnly.NO) {
            predicateList.add(cb.equal(product.get(Product_.pdmOrderableOnly), false));
        }

        Predicate[] predicates = new Predicate[predicateList.size()];
        cq.where(predicateList.toArray(predicates));

        return getEntityManager().createQuery(cq).getResultList();
    }

    /**
     * Find a Product by the specified part number.  Currently not fetching through to addOns or PriceItems since
     * there probably aren't enough of them to really bog things down, but that decision can be revisited if needed.
     *
     * @param partNumber The part number
     *
     * @return The matching product
     */
    public Product findByPartNumber(String partNumber) {
        return findSingle(Product.class, Product_.partNumber, partNumber);
    }

    /**
     * Find a Product by the specified product name.  Currently not fetching through to addOns or PriceItems since
     * there probably aren't enough of them to really bog things down, but that decision can be revisited if needed.
     *
     * @param productName The product name
     *
     * @return The matching {@link Product}
     */
    public Product findByName(String productName) {
        return findSingle(Product.class, Product_.productName, productName);
    }

    /**
     * Find a Product by part number, eagerly fetching the Product Family.
     *
     * @param partNumber the part number
     * @return the matching {@link Product}
     */
    public Product findByPartNumberEagerProductFamily(String partNumber) {
        // mlc temporarily changed Product to EAGER fetch its ProductFamily until I get fetch profiles working
        return findByPartNumber(partNumber);
    }


    public Product findByBusinessKey(String key) {
        return findByPartNumber(key);
    }


    public List<Product> findByPartNumbers(List<String> partNumbers) {
        return findListByList(Product.class, Product_.partNumber, partNumbers);
    }


    /**
     * Products suitable for use as top-level products in a product order.
     *
     * @return The products
     */
    public List<Product> findTopLevelProductsForProductOrder() {
        boolean includePDMProducts = userBean.isPDMUser() || userBean.isDeveloperUser();
        return findProducts(
                 Availability.CURRENT,
                 TopLevelOnly.YES,
                 includePDMProducts ? IncludePDMOnly.YES : IncludePDMOnly.NO);
    }

    public List<Product> findProductsForProductList() {
        // everybody can see everything
        return findProducts(Availability.ALL, TopLevelOnly.NO, IncludePDMOnly.YES);
    }

    /**
     * Case insensitive search in product names and part numbers on tokenized search words split by whitespace.
     *
     * @param searchText The search text
     * @param products The products to look in
     *
     * @return The matching products
     */
    private List<Product> findInProducts(String searchText, List<Product> products) {
        List<Product> list = new ArrayList<>();
        String[] searchWords = (searchText == null ? "" : searchText).split("\\s");

        Collections.sort(products);

        for (Product product : products) {
            boolean matchAll = true;
            for (String searchWord : searchWords) {
                if (!StringUtils.containsIgnoreCase(product.getProductName(), searchWord) &&
                    !StringUtils.containsIgnoreCase(product.getPartNumber(), searchWord)) {
                    matchAll = false;
                    break;
                }
            }

            if (matchAll) {
                list.add(product);
            }
        }

        return list;
    }

    /**
     * Support finding products for product ordering, sensitive to the user's role as PDM (or pseudo-PDM developer).
     * Only available and top-level products will be returned.  Case insensitive search on tokenized search words
     * split by whitespace.
     *
     * @param searchText The text to search
     *
     * @return The products in the db that matches
     */
    public List<Product> searchProducts(String searchText) {
        return findInProducts(searchText, findTopLevelProductsForProductOrder());
    }

    /**
     * Support finding add-on products for product definition, disallowing the top-level product as a search result
     *
     *
     * @param searchText The text to search
     *
     * @return The products in the db that matches
     */
    public List<Product> searchProductsForAddonsInProductEdit(Product topLevelProduct, String searchText) {
        List<Product> products = findProducts(
                ProductDao.Availability.CURRENT_OR_FUTURE,
                ProductDao.TopLevelOnly.NO,
                ProductDao.IncludePDMOnly.NO);

        // remove top level product from the list if it's showing up there
        products.remove(topLevelProduct);

        return findInProducts(searchText, products);

    }
}
