package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.*;

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
    public enum AvailableOnly {
        YES,
        NO
    }

    public enum TopLevelOnly {
        YES,
        NO
    }

    public enum IncludePDMOnly {
        YES,
        NO
    }

    public List<Product> findProducts() {
        return findProducts(AvailableOnly.NO, TopLevelOnly.NO, IncludePDMOnly.YES);
    }

    public List<Product> findProducts(AvailableOnly availableOnly) {
        return findProducts(availableOnly, TopLevelOnly.NO, IncludePDMOnly.YES);
    }

    public List<Product> findProducts(TopLevelOnly topLevelOnly) {
        return findProducts(AvailableOnly.NO, topLevelOnly, IncludePDMOnly.YES);
    }

    public List<Product> findProducts(IncludePDMOnly includePDMOnly) {
        return findProducts(AvailableOnly.NO, TopLevelOnly.NO, includePDMOnly);
    }

    public Product findByBusinessKey(String key) {
        return findByPartNumber(key);
    }

    public SortedSet<String> getProductNames() {
        CriteriaBuilder cb = getCriteriaBuilder();

        CriteriaQuery<Object> cq = cb.createQuery();
        Root<Product> productRoot = cq.from(Product.class);
        CriteriaQuery<Object> select = cq.select(productRoot.get(Product_.productName));
        select.distinct(true);

        TypedQuery<Object> typedQuery = getEntityManager().createQuery(select);
        List<Object> listActual = typedQuery.getResultList();

        SortedSet<String> productNames = new TreeSet<String>();
        for (Object result : listActual) {
            productNames.add((String) result);
        }

        return productNames;
    }

    /**
     * General purpose product finder method
     *
     * @param availableOnly Do we only want to get available products
     * @param topLevelOnly Do we only want to get top level products
     * @param includePDMOnly Do we only want PDM products
     *
     * @return The chosen products
     */
    public List<Product> findProducts(@Nonnull AvailableOnly availableOnly,
                                      @Nonnull TopLevelOnly topLevelOnly,
                                      @Nonnull IncludePDMOnly includePDMOnly) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        List<Predicate> predicateList = new ArrayList<Predicate>();
        cq.distinct(true);

        Root<Product> product = cq.from(Product.class);

        if (availableOnly == AvailableOnly.YES) {
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
}
