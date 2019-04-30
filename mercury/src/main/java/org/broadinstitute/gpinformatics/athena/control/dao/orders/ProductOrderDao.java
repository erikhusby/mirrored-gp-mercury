package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.boundary.billing.AutomatedBiller;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.CriteriaInClauseCreator;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.jpa.JPASplitter;
import org.hibernate.SQLQuery;
import org.hibernate.type.StandardBasicTypes;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ProductOrderDao extends GenericDao {

    /**
     * Calculate whether the schedule is processing messages. This will be used to lock out tracker uploads.
     *
     * @return the state of the schedule
     */
    public boolean isAutoProcessing() {
        int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        // Even though this is a constant expression, we want to leave it in for the time when these values change.
        //noinspection ConstantConditions
        if (AutomatedBiller.PROCESSING_START_HOUR < AutomatedBiller.PROCESSING_END_HOUR) {
            return hourOfDay >= AutomatedBiller.PROCESSING_START_HOUR
                   && hourOfDay < AutomatedBiller.PROCESSING_END_HOUR;
        }

        return hourOfDay >= AutomatedBiller.PROCESSING_START_HOUR || hourOfDay < AutomatedBiller.PROCESSING_END_HOUR;
    }

    /**
     * @return all product orders with ORSP having a given identifier
     */
    public List<ProductOrder> findOrdersByRegulatoryInfoIdentifier(String identifier) {
        return findAll(ProductOrder.class, (criteriaQuery, root) -> {
            CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
            CollectionJoin<ProductOrder, RegulatoryInfo> join = root.join(ProductOrder_.regulatoryInfos);
            criteriaQuery.where(criteriaBuilder.equal(join.get(RegulatoryInfo_.identifier), identifier));
        });
    }

    /**
     * Use this to specify which tables should be fetched (joined) with ProductOrder when it's loaded from the
     * database. Using it can have a major performance benefit when retrieving many PDOs that will in turn need
     * a related table loaded.
     */
    public enum FetchSpec {
        PRODUCT,
        PRODUCT_FAMILY,
        RESEARCH_PROJECT,
        SAMPLES,
        RISK_ITEMS,
        LEDGER_ITEMS,
        PRODUCT_ORDER_KIT,
        CHILD_ORDERS,
        SAP_ORDER_INFO
    }

    private static class ProductOrderDaoCallback implements GenericDaoCallback<ProductOrder> {

        private final Set<FetchSpec> fetchSpecs;

        ProductOrderDaoCallback(FetchSpec... fs) {
            if (fs.length != 0) {
                fetchSpecs = EnumSet.copyOf(Arrays.asList(fs));
            } else {
                fetchSpecs = EnumSet.noneOf(FetchSpec.class);
            }
        }

        @Override
        public void callback(CriteriaQuery<ProductOrder> criteriaQuery, Root<ProductOrder> productOrder) {
            // Risk Item fetching requires sample fetching so don't require the user to know that.
            if (fetchSpecs.contains(FetchSpec.SAMPLES) || fetchSpecs.contains(FetchSpec.RISK_ITEMS)) {
                // This is one to many so set it to be distinct.
                criteriaQuery.distinct(true);
                Fetch<ProductOrder, ProductOrderSample> pdoSampleFetch =
                        productOrder.fetch(ProductOrder_.samples, JoinType.LEFT);

                if (fetchSpecs.contains(FetchSpec.RISK_ITEMS)) {
                    pdoSampleFetch.fetch(ProductOrderSample_.riskItems, JoinType.LEFT);
                }
                if (fetchSpecs.contains(FetchSpec.LEDGER_ITEMS)) {
                    pdoSampleFetch.fetch(ProductOrderSample_.ledgerItems, JoinType.LEFT);
                }
            }

            if (fetchSpecs.contains(FetchSpec.PRODUCT) || fetchSpecs.contains(FetchSpec.PRODUCT_FAMILY)) {
                productOrder.fetch(ProductOrder_.product);
            }

            if (fetchSpecs.contains(FetchSpec.PRODUCT_FAMILY)) {
                Join<ProductOrder, Product> productOrderProductJoin = productOrder.join(ProductOrder_.product);
                productOrderProductJoin.fetch(Product_.productFamily);
            }

            if (fetchSpecs.contains(FetchSpec.RESEARCH_PROJECT)) {
                productOrder.fetch(ProductOrder_.researchProject);
            }

            if (fetchSpecs.contains(FetchSpec.PRODUCT_ORDER_KIT)) {
                productOrder.fetch(ProductOrder_.productOrderKit, JoinType.LEFT);
            }

            if (fetchSpecs.contains(FetchSpec.CHILD_ORDERS)) {
                productOrder.fetch(ProductOrder_.childOrders, JoinType.LEFT);
            }

            if (fetchSpecs.contains(FetchSpec.SAP_ORDER_INFO)) {
                productOrder.fetch(ProductOrder_.sapReferenceOrders, JoinType.LEFT);
            }
        }
    }

    /**
     * Find the order using the business key.
     *
     * @param key The business key to look up, this should have the form of PDO-XYZ for placed orders or
     *            Draft-ABC for draft orders.
     *
     * @return The matching order
     */
    public ProductOrder findByBusinessKey(@Nonnull final String key, LockModeType lockModeType,
                                          FetchSpec... fetchSpecs) {

        return findSingle(ProductOrder.class, new ProductOrderDaoCallback(fetchSpecs) {
            @Override
            public void callback(CriteriaQuery<ProductOrder> criteriaQuery, Root<ProductOrder> productOrder) {

                super.callback(criteriaQuery, productOrder);

                ProductOrder.JiraOrId jiraOrId = ProductOrder.convertBusinessKeyToJiraOrId(key);

                CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();

                Predicate predicate = (jiraOrId.jiraTicketKey == null) ?
                        criteriaBuilder.equal(productOrder.get(ProductOrder_.productOrderId), jiraOrId.productOrderId) :
                        criteriaBuilder.equal(productOrder.get(ProductOrder_.jiraTicketKey), jiraOrId.jiraTicketKey);

                criteriaQuery.where(predicate);
            }
        }, lockModeType);
    }

    /**
     * Wraps a call to the main findByBusinessKey with a lock mode of NONE for generic calls
     */
    public ProductOrder findByBusinessKey(@Nonnull String key, FetchSpec... fetchSpecs) {
        return findByBusinessKey(key, LockModeType.NONE, fetchSpecs);
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
     * Return the {@link ProductOrder}s specified by a list of business keys, applying optional fetches.
     *
     * @param businessKeys the business keys.
     * @param fetchSpecs   the Fetch Specs.
     *
     * @return List of ProductOrders.
     */
    public List<ProductOrder> findListByBusinessKeys(Collection<String> businessKeys, FetchSpec... fetchSpecs) {
        return findListByList(ProductOrder.class, ProductOrder_.jiraTicketKey, businessKeys,
                              new ProductOrderDaoCallback(fetchSpecs));
    }

    /**
     * Calls {@link #findListByBusinessKeys(Collection, ProductOrderDao.FetchSpec...)}
     * with a hardcoded set of fetch specs that are tuned for the billing tracker downloads.  See GPLIM-832 for details.
     */
    public List<ProductOrder> findListForBilling(Collection<String> businessKeys) {
        return findListByBusinessKeys(businessKeys, FetchSpec.PRODUCT, FetchSpec.RESEARCH_PROJECT, FetchSpec.SAMPLES,
                                      FetchSpec.RISK_ITEMS, FetchSpec.LEDGER_ITEMS);
    }


    // Used by tests only.
    public List<ProductOrder> findByWorkflow(@Nonnull String workflowName) {

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ProductOrder> criteriaQuery = criteriaBuilder.createQuery(ProductOrder.class);

        List<Predicate> predicates = new ArrayList<>();

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

    public ProductOrder findByIdSafely(@Nonnull Long orderId, LockModeType lockModeType) {
        return findSingleSafely(ProductOrder.class, ProductOrder_.productOrderId, orderId, lockModeType);
    }

    public Map<String, ProductOrderCompletionStatus> getAllProgress() {
        return getProgress(null);
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
     *
     * @param productOrderIds null if returning info for all orders, the list of keys for specific ones
     *
     * @return The mapping of business keys to the completion status object for each order
     */
    public Map<String, ProductOrderCompletionStatus> getProgress(Collection<Long> productOrderIds) {
        if (productOrderIds != null && productOrderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        /*
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
        // @formatter:off
        String sqlString = "SELECT JIRA_TICKET_KEY AS name, PRODUCT_ORDER_ID as ID, " +
                           "    ( SELECT count(nullif(sum(ledger.QUANTITY), 0)) " +
                           "      FROM athena.product_order_sample pos " +
                           "           INNER JOIN athena.BILLING_LEDGER ledger " +
                           "           ON ledger.PRODUCT_ORDER_SAMPLE_ID = pos.PRODUCT_ORDER_SAMPLE_ID " +
                           "      WHERE pos.product_order = ord.product_order_id " +
                           "      AND pos.DELIVERY_STATUS != 'ABANDONED' " +
                           "      AND (ledger.price_item_type = '" + LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM.name() + "' " +
                           "       OR  ledger.price_item_type = '" + LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM.name() + "') " +
                           "      GROUP BY pos.product_order_sample_id " +
                           "    ) AS completed, " +
                           "    (SELECT count(pos.PRODUCT_ORDER_SAMPLE_ID) FROM athena.product_order_sample pos " +
                           "        WHERE pos.product_order = ord.product_order_id AND pos.DELIVERY_STATUS = 'ABANDONED' " +
                           "    ) AS abandoned, " +
                           "    (SELECT count(pos.PRODUCT_ORDER_SAMPLE_ID) FROM athena.product_order_sample pos " +
                           "        WHERE pos.product_order = ord.product_order_id " +
                           "    ) AS total " +
                           " FROM athena.PRODUCT_ORDER ord ";
        // @formatter:on

        // Handle searching by ID.
        if (productOrderIds != null) {
            sqlString += " WHERE ord.PRODUCT_ORDER_ID in (:productOrderIds) ";
        }

        Query query = getEntityManager().createNativeQuery(sqlString);
        query.unwrap(SQLQuery.class).addScalar("name", StandardBasicTypes.STRING)
             .addScalar("id", StandardBasicTypes.LONG)
             .addScalar("completed", StandardBasicTypes.INTEGER).addScalar("abandoned", StandardBasicTypes.INTEGER)
             .addScalar("total", StandardBasicTypes.INTEGER);

        List<Object> results;
        if (productOrderIds != null) {
            results = JPASplitter.runQuery(query, "productOrderIds", productOrderIds);
        } else {
            //noinspection unchecked
            results = (List<Object>) query.getResultList();
        }

        Map<String, ProductOrderCompletionStatus> progressCounterMap =
                new HashMap<>(results.size());
        for (Object resultObject : results) {
            Object[] result = (Object[]) resultObject;
            String businessKey = ProductOrder.createBusinessKey((Long) result[1], (String) result[0]);
            progressCounterMap.put(businessKey,
                                   new ProductOrderCompletionStatus((Integer) result[3], (Integer) result[2],
                                                                    (Integer) result[4]));
        }

        return progressCounterMap;
    }

    /**
     * Find all PDOs modified after a specified date.
     *
     * @param modifiedAfter date to compare.
     *
     * @return list of PDOs.
     */
    public List<ProductOrder> findModifiedAfter(final Date modifiedAfter) {
        return findAll(ProductOrder.class, new GenericDaoCallback<ProductOrder>() {
            @Override
            public void callback(CriteriaQuery<ProductOrder> criteriaQuery, Root<ProductOrder> root) {
                criteriaQuery
                        .where(getCriteriaBuilder().greaterThan(root.get(ProductOrder_.modifiedDate), modifiedAfter));
            }
        });
    }

    /**
     * Find all ProductOrders for the specified list of barcodes.
     *
     * @param barcodes One or more sample barcodes.
     *
     * @return All Product Orders containing samples with these barcodes.
     */
    public List<ProductOrder> findBySampleBarcodes(@Nonnull String... barcodes) {

        CriteriaBuilder cb = getCriteriaBuilder();

        final CriteriaQuery<ProductOrder> query = cb.createQuery(ProductOrder.class);
        query.distinct(true);

        Root<ProductOrder> root = query.from(ProductOrder.class);
        final ListJoin<ProductOrder, ProductOrderSample> sampleListJoin = root.join(ProductOrder_.samples);

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

    /**
     * Helper method for a sample initiation fixup.  Will find all sample initiation PDOs that do not
     *
     * @return
     */
    public List<ProductOrder> findSampleInitiationPDOsNotConverted() {

        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();

        CriteriaQuery<ProductOrder> productOrderCriteriaQuery = criteriaBuilder.createQuery(ProductOrder.class);

        List<Predicate> predicates = new ArrayList<>();

        Root<ProductOrder> productOrderRoot = productOrderCriteriaQuery.from(ProductOrder.class);
        Join<ProductOrder, Product> productJoin = productOrderRoot.join(ProductOrder_.product);


        Join<ProductOrder, ProductOrderKit> productOrderKitJoin = productOrderRoot.join(ProductOrder_.productOrderKit);
        Join<ProductOrderKit, ProductOrderKitDetail> kitDetailJoin = productOrderKitJoin.join(
                ProductOrderKit_.kitOrderDetails, JoinType.LEFT);

        predicates.add(criteriaBuilder
                               .equal(productJoin.get(Product_.partNumber), Product.SAMPLE_INITIATION_PART_NUMBER));
        predicates.add(criteriaBuilder.isNull(kitDetailJoin.get(ProductOrderKitDetail_.numberOfSamples)));

        productOrderCriteriaQuery.where(predicates.toArray(new Predicate[predicates.size()]));

        try {
            return getEntityManager().createQuery(productOrderCriteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    /**
     * Every product order is associated with a Quote ID.  This method allows a consumer to find all product orders,
     * which are in a Submitted state, that have the same common quote id stored in the quoteId field.
     *
     * @param quoteId Main criteria with which to base the query.  This value will determine which product orders will
     *                be returned
     * @return a List of product orders, all of which have the same quoteId as the one passed in as a parameter to this
     * method
     */
    public List<ProductOrder> findOrdersWithCommonQuote(String quoteId) {
        return findListByList(ProductOrder.class, ProductOrder_.quoteId, Collections.singleton(quoteId),
                new ProductOrderDaoCallback(new ProductOrderDao.FetchSpec[]{ProductOrderDao.FetchSpec.SAMPLES}) {
                    @Override
                    public void callback(CriteriaQuery<ProductOrder> criteriaQuery, Root<ProductOrder> root) {

                        super.callback(criteriaQuery, root);

                        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();

                        Predicate predicate =
                                builder.equal(root.get(ProductOrder_.orderStatus), ProductOrder.OrderStatus.Submitted);
                        criteriaQuery.where(predicate);
                    }
                }
        );
    }

    public List<ProductOrder> findOrdersWithCommonProduct(String productPartNumber) {
        return findAll(ProductOrder.class, new ProductOrderDaoCallback() {
            @Override
            public void callback(CriteriaQuery<ProductOrder> criteriaQuery, Root<ProductOrder> productOrderRoot) {
                super.callback(criteriaQuery, productOrderRoot);
                criteriaQuery.distinct(true);

                CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();

                Join<ProductOrder, Product> productJoin = productOrderRoot.join(ProductOrder_.product);
                Predicate predicate = builder.equal(productJoin.get(Product_.partNumber),productPartNumber);
                criteriaQuery.where(predicate);
            }
        });
    }

    public List<ProductOrder> findOrdersWithSAPOrdersAndBilledSamples() {
        return findAll(ProductOrder.class,new ProductOrderDaoCallback(FetchSpec.SAMPLES, FetchSpec.LEDGER_ITEMS,
                FetchSpec.SAP_ORDER_INFO){
            @Override
            public void callback(CriteriaQuery<ProductOrder> criteriaQuery, Root<ProductOrder> productOrderRoot) {
                super.callback(criteriaQuery, productOrderRoot);

                criteriaQuery.distinct(true);

                CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();

                final ListJoin<ProductOrder, SapOrderDetail> join =
                        productOrderRoot.join(ProductOrder_.sapReferenceOrders);
            }
        }) ;
    }
}


