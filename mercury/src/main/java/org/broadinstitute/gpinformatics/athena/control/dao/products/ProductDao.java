package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.jetbrains.annotations.NotNull;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Stateful
@RequestScoped
/**
 *
 * Dao for {@link Product}s, supporting the browse and CRUD UIs.
 *
 */
public class ProductDao extends GenericDao implements Serializable {

    /**
     * Preferring strong types to booleans
     */
    public enum AvailableProductsOnly {
        YES,
        NO
    }


    public enum TopLevelProductsOnly {
        YES,
        NO
    }


    public List<Product> findProducts() {
        return findProducts(AvailableProductsOnly.NO, TopLevelProductsOnly.NO);
    }


    public List<Product> findProducts(AvailableProductsOnly availableProductsOnly) {
        return findProducts(availableProductsOnly, TopLevelProductsOnly.NO);
    }


    public List<Product> findProducts(TopLevelProductsOnly topLevelProductsOnly) {
        return findProducts(AvailableProductsOnly.NO, topLevelProductsOnly);
    }

    public Product findByBusinessKey(String key) {
        return findByPartNumber(key);
    }

    /**
     * General purpose product finder method
     *
     * @param availableProductsOnly
     *
     * @param topLevelProductsOnly
     *
     * @return
     */
    public List<Product> findProducts(@NotNull AvailableProductsOnly availableProductsOnly,
                                      @NotNull TopLevelProductsOnly topLevelProductsOnly) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        List<Predicate> predicateList = new ArrayList<Predicate>();
        cq.distinct(true);

        Root<Product> product = cq.from(Product.class);

        // left join fetches may be required for an application scoped cache of Products.  JPA criteria queries
        // don't seem to do a good job with this and will double select all the fields of the associations: once for the
        // join and again for the fetch.
        // http://stackoverflow.com/questions/4511368/jpa-2-criteria-fetch-path-navigation

//        product.join(Product_.priceItems, JoinType.LEFT);
//        product.fetch(Product_.priceItems, JoinType.LEFT);
//
//        product.join(Product_.addOns, JoinType.LEFT);
//        product.fetch(Product_.addOns, JoinType.LEFT);

        if (availableProductsOnly == AvailableProductsOnly.YES) {
            // there is an availability date
            predicateList.add(cb.isNotNull(product.get(Product_.availabilityDate)));
            // and it is in the past
            predicateList.add(cb.lessThan(product.get(Product_.availabilityDate), Calendar.getInstance().getTime()));

            // and the discontinued date is null or in the future
            predicateList.add(
                cb.or( cb.isNull(product.get(Product_.discontinuedDate)),
                       cb.greaterThan(product.get(Product_.discontinuedDate), Calendar.getInstance().getTime()))
            );
        }

        if (topLevelProductsOnly == TopLevelProductsOnly.YES) {
            predicateList.add(cb.equal(product.get(Product_.topLevelProduct), true));
        }

        Predicate[] predicates = new Predicate[predicateList.size()];
        cq.where(predicateList.toArray(predicates));

        return getEntityManager().createQuery(cq).getResultList();

    }


    /**
     * Find a Product by the specified part number.  Currently not fetching through to addOns or PriceItems since
     * there probably aren't enough of them to really bog things down, but that decision can be revisited if needed.
     *
     * @param partNumber
     *
     * @return
     */
    public Product findByPartNumber(String partNumber) {
        return findSingle(Product.class, Product_.partNumber, partNumber);
    }
}
