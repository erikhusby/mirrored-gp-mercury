package org.broadinstitute.gpinformatics.athena.control.dao.products;

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
        List<Predicate> predicateList = new ArrayList<Predicate>();
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


    public Product findByBusinessKey(String key) {
        return findByPartNumber(key);
    }


    /**
     * Products suitable for use as top-level products in a product order.
     *
     * @return
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
        return findProducts(Availability.ALL, TopLevelOnly.NO, IncludePDMOnly.NO);
    }
}
