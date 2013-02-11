package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.hibernate.SQLQuery;
import org.hibernate.type.StandardBasicTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.*;
import java.util.*;

/**
 * Dao for {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder}s.
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/5/12
 * Time: 6:17 PM
 */

@Stateful
@RequestScoped
public class ProductOrderDao extends GenericDao {

    public enum FetchSpec {
        Product,
        ProductFamily,
        ResearchProject,
        Samples
    }


    private class ProductOrderDaoCallback implements GenericDaoCallback<ProductOrder> {

        private Set<FetchSpec> fetchSpecs;

        ProductOrderDaoCallback(FetchSpec... fs) {

           fetchSpecs = new HashSet<FetchSpec>(Arrays.asList(fs));
        }


        @Override
        public void callback(CriteriaQuery<ProductOrder> criteriaQuery, Root<ProductOrder> productOrder) {
            if (fetchSpecs.contains(FetchSpec.Samples)) {
                // one to many so set distinct
                criteriaQuery.distinct(true);
                productOrder.fetch(ProductOrder_.samples, JoinType.LEFT);
            }

            if (fetchSpecs.contains(FetchSpec.Product) || fetchSpecs.contains(FetchSpec.ProductFamily)) {
                productOrder.fetch(ProductOrder_.product);
            }

            if (fetchSpecs.contains(FetchSpec.ProductFamily)) {
                Join<ProductOrder,Product> productOrderProductJoin = productOrder.join(ProductOrder_.product);
                productOrderProductJoin.fetch(Product_.productFamily);
            }

            if (fetchSpecs.contains(FetchSpec.ResearchProject)) {
                productOrder.fetch(ProductOrder_.researchProject);
            }
        }
    }

    /**
     * Find the order using the business key (the jira ticket number).
     *
     * @param key The business key to look up
     *
     * @return The matching order
     */
    public ProductOrder findByBusinessKey(String key) {
        if (key.startsWith(ProductOrder.DRAFT_PREFIX)) {
            Long idPartOfKey = Long.parseLong(key.substring(BillingSession.ID_PREFIX.length() + 1));
            return findSingle(ProductOrder.class, ProductOrder_.productOrderId, idPartOfKey);
        }

        return findSingle(ProductOrder.class, ProductOrder_.jiraTicketKey, key);
    }


    /**
     * Find the order using the unique title
     *
     * @param title
     *
     * @return
     */
    public ProductOrder findByTitle(String title) {
        return findSingle(ProductOrder.class, ProductOrder_.title, title);
    }


    /**
     * Return the {@link ProductOrder}s specified by the {@link List} of business keys, applying optional fetches.
     *
     * @param businessKeyList
     * @param fs
     *
     * @return
     */
    public List<ProductOrder> findListByBusinessKeyList(List<String> businessKeyList, FetchSpec... fs) {

        return findListByList(ProductOrder.class, ProductOrder_.jiraTicketKey, businessKeyList, new ProductOrderDaoCallback(fs));
    }


    /**
     * Find ProductOrders by Research Project
     *
     * @param researchProject The project
     *
     * @return The matching list of orders
     */
    public List<ProductOrder> findByResearchProject(ResearchProject researchProject) {
        return findList(ProductOrder.class, ProductOrder_.researchProject, researchProject);
    }


    /**
     * Find a productOrder by its containing ResearchProject and title
     *
     * @param orderTitle The title to look up
     * @param researchProject The project
     *
     * @return The order that matches the project and title
     */
    public ProductOrder findByResearchProjectAndTitle(@Nonnull ResearchProject researchProject, @Nonnull String orderTitle) {
        if (researchProject == null) {
            throw new NullPointerException("Null Research Project.");
        }

        if (orderTitle == null) {
            throw new NullPointerException("Null Product Order Title.");
        }

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ProductOrder> criteriaQuery =
                criteriaBuilder.createQuery(ProductOrder.class);

        List<Predicate> predicateList = new ArrayList<Predicate>();

        Root<ProductOrder> productOrderRoot = criteriaQuery.from(ProductOrder.class);
        predicateList.add(criteriaBuilder.equal(productOrderRoot.get(ProductOrder_.researchProject), researchProject));
        predicateList.add(criteriaBuilder.equal(productOrderRoot.get(ProductOrder_.title), orderTitle));

        Predicate[] predicates = new Predicate[predicateList.size()];
        criteriaQuery.where(predicateList.toArray(predicates));

        try {
            return entityManager.createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    /**
     * Find all ProductOrders, not initializing any associations
     *
     * @return All the orders
     */
    public List<ProductOrder> findAll() {
        return findAll(ProductOrder.class);
    }


    public List<ProductOrder> findAll(FetchSpec... fetchSpecs) {
        return findAll(ProductOrder.class, new ProductOrderDaoCallback(fetchSpecs));
    }


    /**
     * Find all the ProductOrders for a person who is
     * associated ( as the creator ) with the ProductOrders
     *
     * @param personId The person to filter on
     *
     * @return The products for this person
     */
    public List<ProductOrder> findByCreatedPersonId(@Nonnull Long personId) {
        if (personId == null) {
            throw new NullPointerException("Null Person Id.");
        }

        return findList(ProductOrder.class, ProductOrder_.createdBy, personId);
    }

    /**
     * Find all the ProductOrders for a person who is
     * associated ( as the modifier ) with the ProductOrders
     *
     * @param personId The person to filter on
     *
     * @return The products for this person
     */
    public List<ProductOrder> findByModifiedPersonId(@Nonnull Long personId) {
        if (personId == null) {
            throw new NullPointerException("Null Person Id.");
        }

        return findList(ProductOrder.class, ProductOrder_.modifiedBy, personId);
    }

    /**
     * Find a ProductOrder by its primary key identifier
     *
     * @param orderId The order id to look up
     *
     * @return the order that matches
     */
    public ProductOrder findById(@Nonnull Long orderId) {
        if (orderId == null) {
            throw new NullPointerException("Null ProductOrder Id.");
        }

        return findSingle(ProductOrder.class, ProductOrder_.productOrderId, orderId);
    }

    /**
     * @return Get the completion status information for all product orders
     */
    public Map<String, ProductOrderCompletionStatus> getProgressByBusinessKey() {
        return getProgressByBusinessKey(null);
    }

    /**
     * This gets the completion status for the product orders. There are four pieces of information returned for each order:
     *
     *      'name' - Simply the jira ticket key
     *      'completed' - The number of NOT abandoned samples that have ledger items for any primary or optional price item
     *      'abandoned' - The number of samples that ARE abandoned
     *      'total' - The total number of samples
     *
     * @param productOrderKeys null if returning info for all orders, the list of keys for specific ones
     *
     * @return The mapping of business keys to the completion status object for each order
     */
    @SuppressWarnings("unchecked")
    public Map<String, ProductOrderCompletionStatus> getProgressByBusinessKey(@Nullable Collection<String> productOrderKeys) {
        String sqlString =
                "SELECT JIRA_TICKET_KEY AS name, " +
                "    ( SELECT count( DISTINCT pos.PRODUCT_ORDER_SAMPLE_ID) " +
                "      FROM athena.product_order_sample pos " +
                "           INNER JOIN athena.BILLING_LEDGER ledger ON ledger.PRODUCT_ORDER_SAMPLE_ID = pos.PRODUCT_ORDER_SAMPLE_ID" +
                "      WHERE pos.product_order = ord.product_order_id " +
                "      AND pos.DELIVERY_STATUS != 'ABANDONED' " +
                "      AND (ledger.PRICE_ITEM_ID = prod.PRIMARY_PRICE_ITEM " +
                "           OR ledger.price_item_id IN ( " +
                "               SELECT OPTIONAL_PRICE_ITEMS FROM athena.PRODUCT_OPT_PRICE_ITEMS opt WHERE opt.PRODUCT = prod.PRODUCT_ID " +
                "           ) " +
                "      ) " +
                "    ) AS completed, " +
                "    (SELECT count(pos.PRODUCT_ORDER_SAMPLE_ID) FROM athena.product_order_sample pos" +
                "        WHERE pos.product_order = ord.product_order_id AND pos.DELIVERY_STATUS = 'ABANDONED' " +
                "    ) AS abandoned, " +
                "    (SELECT count(pos.PRODUCT_ORDER_SAMPLE_ID) FROM athena.product_order_sample pos" +
                "        WHERE pos.product_order = ord.product_order_id" +
                "    ) AS total" +
                " FROM athena.PRODUCT_ORDER ord LEFT JOIN athena.PRODUCT prod ON ord.PRODUCT = prod.PRODUCT_ID " +
                " WHERE ord.JIRA_TICKET_KEY is not null ";

        // Add the business key, if we are only doing one
        if ((productOrderKeys != null) && (!productOrderKeys.isEmpty())) {
            sqlString += " AND ord.JIRA_TICKET_KEY in (:businessKeys)";
        }

        Query query = getThreadEntityManager().getEntityManager().createNativeQuery(sqlString);
        query.unwrap(SQLQuery.class)
                .addScalar("name", StandardBasicTypes.STRING)
                .addScalar("completed", StandardBasicTypes.INTEGER)
                .addScalar("abandoned", StandardBasicTypes.INTEGER)
                .addScalar("total", StandardBasicTypes.INTEGER);

        if ((productOrderKeys != null) && (!productOrderKeys.isEmpty())) {
            query.setParameter("businessKeys", productOrderKeys);
        }

        List<Object> results = (List<Object>) query.getResultList();

        Map<String, ProductOrderCompletionStatus> progressCounterMap =
            new HashMap<String, ProductOrderCompletionStatus> (results.size());
        for (Object resultObject : results) {
            Object[] result = (Object[]) resultObject;
            if (result[0] != null) {
                progressCounterMap.put((String) result[0],
                    new ProductOrderCompletionStatus((Integer) result[2], (Integer) result[1], (Integer) result[3]));
            }
        }

        return progressCounterMap;
    }

}
