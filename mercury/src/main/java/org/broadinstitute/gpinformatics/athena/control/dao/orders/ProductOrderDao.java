package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.CriteriaInClauseCreator;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.jpa.JPASplitter;
import org.hibernate.SQLQuery;
import org.hibernate.type.StandardBasicTypes;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateful
@RequestScoped
public class ProductOrderDao extends GenericDao {

    public enum FetchSpec {
        Product,
        ProductFamily,
        ResearchProject,
        Samples
    }

    private static class ProductOrderDaoCallback implements GenericDaoCallback<ProductOrder> {

        private Set<FetchSpec> fetchSpecs;

        ProductOrderDaoCallback(FetchSpec... fs) {
            if (fs.length != 0) {
                fetchSpecs = EnumSet.copyOf(Arrays.asList(fs));
            } else {
                fetchSpecs = EnumSet.noneOf(FetchSpec.class);
            }
        }

        @Override
        public void callback(CriteriaQuery<ProductOrder> criteriaQuery, Root<ProductOrder> productOrder) {
            if (fetchSpecs.contains(FetchSpec.Samples)) {
                // This is one to many so set it to be distinct.
                criteriaQuery.distinct(true);
                productOrder.fetch(ProductOrder_.samples, JoinType.LEFT);
            }

            if (fetchSpecs.contains(FetchSpec.Product) || fetchSpecs.contains(FetchSpec.ProductFamily)) {
                productOrder.fetch(ProductOrder_.product);
            }

            if (fetchSpecs.contains(FetchSpec.ProductFamily)) {
                Join<ProductOrder, Product> productOrderProductJoin = productOrder.join(ProductOrder_.product);
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
    public ProductOrder findByBusinessKey(@Nonnull String key) {
        ProductOrder.JiraOrId jiraOrId = ProductOrder.convertBusinessKeyToJiraOrId(key);
        if (jiraOrId.jiraTicketKey != null) {
            return findSingle(ProductOrder.class, ProductOrder_.jiraTicketKey, jiraOrId.jiraTicketKey);
        }

        return findSingle(ProductOrder.class, ProductOrder_.productOrderId, jiraOrId.productOrderId);
    }

    /**
     * Find the order using the unique title
     *
     * @param title Title.
     *
     * @return Corresponding Product Order.
     */
    public ProductOrder findByTitle(String title) {
        return findSingle(ProductOrder.class, ProductOrder_.title, title);
    }

    /**
     * Return the {@link ProductOrder}s specified by the {@link List} of business keys, applying optional fetches.
     *
     * @param businessKeyList List of business keys.
     * @param fs Varargs array of Fetch Specs.
     *
     * @return List of ProductOrders.
     */
    public List<ProductOrder> findListByBusinessKeyList(List<String> businessKeyList, FetchSpec... fs) {
        return findListByList(ProductOrder.class, ProductOrder_.jiraTicketKey, businessKeyList,
                new ProductOrderDaoCallback(fs));
    }

    public List<ProductOrder> findByWorkflowName(@Nonnull String workflowName) {

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ProductOrder> criteriaQuery = criteriaBuilder.createQuery(ProductOrder.class);

        List<Predicate> predicates = new ArrayList<Predicate>();

        Root<ProductOrder> productOrderRoot = criteriaQuery.from(ProductOrder.class);
        Join<ProductOrder, Product> productJoin = productOrderRoot.join(ProductOrder_.product);

        predicates.add(criteriaBuilder.equal(productJoin.get(Product_.workflowName), workflowName));

        criteriaQuery.where(predicates.toArray(new Predicate[predicates.size()]));

        try {
            return entityManager.createQuery(criteriaQuery).getResultList();
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
     * Find a ProductOrder by its primary key identifier
     *
     * @param orderId The order id to look up
     *
     * @return the order that matches
     */
    public ProductOrder findById(@Nonnull Long orderId) {

        return findSingle(ProductOrder.class, ProductOrder_.productOrderId, orderId);
    }

    @SuppressWarnings("unchecked")
    public Map<String, ProductOrderCompletionStatus> getAllProgressByBusinessKey() {
        return getProgressByBusinessKey(null);
    }

    /**
     * This gets the completion status for the product orders. There are four pieces of information returned for each
     * order:
     * <dl>
     * <dt>name</dt><dd>Simply the jira ticket key</dd>
     * <dt>completed</dt><dd>The number of NOT abandoned samples that have ledger items for any primary or optional price item</dd>
     * <dt>abandoned</dt><dd>The number of samples that ARE abandoned</dd>
     * <dt>total</dt><dd>The total number of samples</dd>
     * </dl>
     * @param productOrderKeys null if returning info for all orders, the list of keys for specific ones
     *
     * @return The mapping of business keys to the completion status object for each order
     */
    @SuppressWarnings("unchecked")
    public Map<String, ProductOrderCompletionStatus> getProgressByBusinessKey(Collection<String> productOrderKeys) {
        if (productOrderKeys != null && productOrderKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        /**
         * This SQL query looks up the product orders specified by the order keys (all, if the keys are null) and then
         * projects out three count queries along with the jira ticket and the order id:
         *
         *       1. The number of non abandoned samples that have ledger items with price items that
         *          were primary or replacement on each pdo. Note that this could include items that are billed
         *          positive and negative, leaving a 0 billed total. We still are calling this complete and that
         *          was deemed OK.
         *       2. The number of abandoned samples on each pdo.
         *       3. The total number of samples on each pdo.
         */
        String sqlString = "SELECT JIRA_TICKET_KEY AS name, PRODUCT_ORDER_ID as ID, " +
                           "    ( SELECT count( DISTINCT pos.PRODUCT_ORDER_SAMPLE_ID) " +
                           "      FROM athena.product_order_sample pos " +
                           "           INNER JOIN athena.BILLING_LEDGER ledger " +
                           "           ON ledger.PRODUCT_ORDER_SAMPLE_ID = pos.PRODUCT_ORDER_SAMPLE_ID" +
                           "      WHERE pos.product_order = ord.product_order_id " +
                           "      AND pos.DELIVERY_STATUS != 'ABANDONED' " +
                           "      AND (ledger.price_item_type = '" + LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM.name() + "'" +
                           "             OR " +
                           "           ledger.price_item_type = '" + LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM.name() + "')" +
                           "    ) AS completed, " +
                           "    (SELECT count(pos.PRODUCT_ORDER_SAMPLE_ID) FROM athena.product_order_sample pos" +
                           "        WHERE pos.product_order = ord.product_order_id AND pos.DELIVERY_STATUS = 'ABANDONED' " +
                           "    ) AS abandoned, " +
                           "    (SELECT count(pos.PRODUCT_ORDER_SAMPLE_ID) FROM athena.product_order_sample pos" +
                           "        WHERE pos.product_order = ord.product_order_id" +
                           "    ) AS total" +
                           " FROM athena.PRODUCT_ORDER ord ";

        // Add the business key, if we are only doing one.
        if (productOrderKeys != null) {
            sqlString += " WHERE ord.JIRA_TICKET_KEY in (:businessKeys)";
        }

        Query query = getThreadEntityManager().getEntityManager().createNativeQuery(sqlString);
        query.unwrap(SQLQuery.class).addScalar("name", StandardBasicTypes.STRING)
             .addScalar("id", StandardBasicTypes.LONG)
             .addScalar("completed", StandardBasicTypes.INTEGER).addScalar("abandoned", StandardBasicTypes.INTEGER)
             .addScalar("total", StandardBasicTypes.INTEGER);

        List<Object> results;
        if (productOrderKeys != null) {
            results = JPASplitter.runQuery(query, "businessKeys", productOrderKeys);
        } else {
            results = (List<Object>) query.getResultList();
        }

        Map<String, ProductOrderCompletionStatus> progressCounterMap =
                new HashMap<String, ProductOrderCompletionStatus>(results.size());
        for (Object resultObject : results) {
            Object[] result = (Object[]) resultObject;
            String businessKey = ProductOrder.createBusinessKey((Long) result[1], (String) result[0]);
            progressCounterMap.put(businessKey,
                    new ProductOrderCompletionStatus((Integer) result[3], (Integer) result[2], (Integer) result[4]));
        }

        return progressCounterMap;
    }

    /**
     * Find all PDOs modified after a specified date.
     * @param modifiedAfter date to compare.
     * @return list of PDOs.
     */
    public List<ProductOrder> findModifiedAfter(final Date modifiedAfter) {
        return findAll(ProductOrder.class, new GenericDaoCallback<ProductOrder>() {
            @Override
            public void callback(CriteriaQuery<ProductOrder> criteriaQuery, Root<ProductOrder> root) {
                criteriaQuery.where(getCriteriaBuilder().greaterThan(root.get(ProductOrder_.modifiedDate), modifiedAfter));
            }
        });
    }

    /**
     * Find all ProductOrders for the specified varargs array of barcodes, will throw {@link IllegalArgumentException}
     * if given more than 1000 barcodes.
     *
     * @param barcodes One or more sample barcodes.
     *
     * @return All Product Orders containing samples with these barcodes.
     */
    public List<ProductOrder> findBySampleBarcodes(@Nonnull String... barcodes) throws IllegalArgumentException {

        CriteriaBuilder cb = getCriteriaBuilder();

        final CriteriaQuery<ProductOrder> query = cb.createQuery(ProductOrder.class);
        query.distinct(true);

        Root<ProductOrder> root = query.from(ProductOrder.class);
        final ListJoin<ProductOrder,ProductOrderSample> sampleListJoin = root.join(ProductOrder_.samples);

        return JPASplitter.runCriteriaQuery(
                Arrays.asList(barcodes),
                new CriteriaInClauseCreator<String>() {
                    @Override
                    public Query createCriteriaInQuery(Collection<String> parameterList) {
                        query.where(sampleListJoin.get(ProductOrderSample_.sampleName).in(parameterList));
                        return getEntityManager().createQuery(query);
                    }
                }
        );
    }
}
