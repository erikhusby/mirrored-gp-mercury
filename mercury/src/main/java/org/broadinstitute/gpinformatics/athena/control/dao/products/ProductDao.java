package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.apache.commons.lang3.BooleanUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Stateful
@RequestScoped
/**
 *
 * Dao for {@link Product}s, supporting the browse and CRUD UIs.
 *
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ProductDao extends GenericDao implements Serializable {

    @Inject
    private UserBean userBean;

    /**
     * Preferring strong types to booleans
     */
    public enum Availability {
        ALL,
        CURRENT,
        CURRENT_OR_FUTURE,
        PAST
    }

    public enum TopLevelOnly {
        YES,
        NO
    }

    public enum IncludePDMOnly {
        WITH_GP_PM,
        YES,
        NO;

        public static IncludePDMOnly toIncludePDMOnly(boolean bool, boolean gpPMRole) {
            IncludePDMOnly includePDMOnly = NO;
            if(gpPMRole) {
                includePDMOnly = WITH_GP_PM;
            } else {
                includePDMOnly = IncludePDMOnly.valueOf(BooleanUtils.toStringYesNo(bool).toUpperCase());
            }
            return includePDMOnly;
        }
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
        return findProducts(availability, topLevelOnly, includePDMOnly, Collections.<String>emptyList());
    }

    /**
     * General purpose product finder method
     *
     * @param availability   Do we only want to get available products
     * @param topLevelOnly   Do we only want to get top level products
     * @param includePDMOnly Do we only want PDM products
     * @param searchTerms    Collection of terms to search the product name and part number for.
     *
     * @return The chosen products
     */
    public List<Product> findProducts(@Nonnull Availability availability,
                                      @Nonnull TopLevelOnly topLevelOnly,
                                      @Nonnull IncludePDMOnly includePDMOnly, Collection<String> searchTerms) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        List<Predicate> predicateList = new ArrayList<>();
        cq.distinct(true);

        Root<Product> product = cq.from(Product.class);

        Expression<String> productNameExpression = product.get(Product_.productName).as(String.class);
        Expression<String> partNumberExpression = product.get(Product_.partNumber).as(String.class);

        for (String searchTerm : searchTerms) {
            Predicate searchTermsLike = cb.or(
                    cb.like(cb.lower(productNameExpression), '%' + searchTerm.toLowerCase() + '%'),
                    cb.like(cb.lower(partNumberExpression), '%' + searchTerm.toLowerCase() + '%')
            );
            predicateList.add(cb.and(searchTermsLike));
        }

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

        case PAST:
            predicateList.add(cb.lessThanOrEqualTo(product.get(Product_.discontinuedDate),
                    Calendar.getInstance().getTime()));
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
        } else if(includePDMOnly == IncludePDMOnly.WITH_GP_PM) {
            predicateList.add(cb.or(cb.equal(product.get(Product_.pdmOrderableOnly), false),
                    cb.and(cb.equal(product.get(Product_.pdmOrderableOnly), true),
                            cb.equal(product.get(Product_.externalOnlyProduct), true))
                    )
            );
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

    /** Returns products by name, excluding products that are not available yet or that are discontinued. */
    public List<Product> findAvailableByName(String productName) {
        return findList(Product.class, Product_.productName, productName).stream().
                filter(product -> product.getAvailabilityDate() != null &&
                        product.getAvailabilityDate().before(Calendar.getInstance().getTime())).
                filter(product -> product.getDiscontinuedDate() == null ||
                        product.getDiscontinuedDate().after(Calendar.getInstance().getTime())).
                collect(Collectors.toList());
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
        return findTopLevelProductsForProductOrder(Collections.<String>emptyList());
    }

    /**
     * Products suitable for use as top-level products in a product order.
     *
     * @param searchTerms if provided, also perform case-insensitive search in product names and part numbers.
     *
     * @return The products
     */
    public List<Product> findTopLevelProductsForProductOrder(Collection<String> searchTerms) {
        return findProducts(
                Availability.CURRENT,
                TopLevelOnly.YES,
                IncludePDMOnly.toIncludePDMOnly(canIncludePdmProducts(), userBean.isGPPMUser()), searchTerms);
    }

    public List<Product> findDiscontinuedProducts() {
        return findProducts(Availability.PAST, TopLevelOnly.YES, IncludePDMOnly.YES, Collections.emptyList());
    }


    private boolean canIncludePdmProducts() {
        return userBean.isPDMUser() || userBean.isDeveloperUser();
    }

    public List<Product> findProductsForProductList() {
        // everybody can see everything
        return findProducts(Availability.ALL, TopLevelOnly.NO, IncludePDMOnly.YES);
    }

    /**
     * Support finding add-on products for product definition including PDM-only products, disallowing the
     * top-level product as a search result. If you wish to exclude PDM-only products you need to filter it yourself.
     *
     * @param topLevelProduct product to find the add-ons for.
     * @param searchTerms     Collection of terms to search the product name and part number for.
     *
     * @return The products in the db that matches
     */
    public List<Product> searchProductsForAddOnsInProductEdit(Product topLevelProduct, Collection<String> searchTerms) {
        List<Product> products = findProducts(
                ProductDao.Availability.CURRENT_OR_FUTURE, TopLevelOnly.NO,
                IncludePDMOnly.toIncludePDMOnly(canIncludePdmProducts(), userBean.isGPPMUser()), searchTerms);

        // remove top level product from the list if it's showing up there
        products.remove(topLevelProduct);
        return products;
    }

    public List<Product> findAllWithAnalysisType() {
        return findAll(Product.class, new GenericDaoCallback<Product>() {
            @Override
            public void callback(CriteriaQuery<Product> criteriaQuery, Root<Product> root) {
                criteriaQuery.where(getCriteriaBuilder().isNotNull(root.get(Product_.analysisTypeKey)));
            }
        });
    }

    // todo emp this should be replaced with a call to PipelineDataTypeDao when GPLIM-5521 is deployed.
    public List<String> findAggregationDataTypes() {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        CriteriaQuery<String> criteriaQuery = criteriaBuilder.createQuery(String.class);
        Root<Product> root = criteriaQuery.from(Product.class);
        criteriaQuery.select(root.get(Product_.aggregationDataType)).
                where(criteriaBuilder.isNotNull(root.get(Product_.aggregationDataType))).
                distinct(true).
                orderBy(criteriaBuilder.asc(root.get(Product_.aggregationDataType)));
        return getEntityManager().createQuery(criteriaQuery).getResultList();
    }
}
