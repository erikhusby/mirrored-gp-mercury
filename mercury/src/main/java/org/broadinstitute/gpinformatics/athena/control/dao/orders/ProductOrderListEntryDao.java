package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession_;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry_;
import org.broadinstitute.gpinformatics.athena.entity.orders.*;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateRangeSelector;
import org.hibernate.criterion.Disjunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestScoped
@Stateful
public class ProductOrderListEntryDao extends GenericDao implements Serializable {

    /**
     *
     * @param productOrderListEntries The entries to fill with unbilled ledger entry counts.
     */
    private void fetchUnbilledLedgerEntryCounts(List<ProductOrderListEntry> productOrderListEntries) {
        fetchUnbilledLedgerEntryCounts(productOrderListEntries, null);
    }


    /**
     * Second-pass, ledger aware query that merges its results into the first-pass objects passed as an argument.
     *
     * Fetch the count of unbilled ledger entries for the
     * {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder}s referenced by the
     * ProductOrderListEntry DTOs and set those counts into the DTOs.
     *
     * @param productOrderListEntries The entries to fill with unbilled ledger entry counts.
     */
    private void fetchUnbilledLedgerEntryCounts(List<ProductOrderListEntry> productOrderListEntries, @Nullable String jiraTicketKey) {

        if (CollectionUtils.isEmpty(productOrderListEntries)) {
            return;
        }

        // Build map of input productOrderListEntries jira key to DTO.
        Map<String, ProductOrderListEntry> jiraKeyToProductOrderListEntryMap = new HashMap<String, ProductOrderListEntry>();

        for (ProductOrderListEntry productOrderListEntry : productOrderListEntries) {
            jiraKeyToProductOrderListEntryMap.put(productOrderListEntry.getJiraTicketKey(), productOrderListEntry);
        }

        // Build query to pick out eligible DTOs.  This only returns values for PDOs with ledger entries in open
        // billing sessions or with no associated billing session.
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<ProductOrderListEntry> cq = cb.createQuery(ProductOrderListEntry.class);

        cq.distinct(true);

        Root<ProductOrder> productOrderRoot = cq.from(ProductOrder.class);

        // Left join the samples, inner join the samples to the ledger items.
        ListJoin<ProductOrder, ProductOrderSample> productOrderProductOrderSampleListJoin =
                productOrderRoot.join(ProductOrder_.samples, JoinType.LEFT);

        SetJoin<ProductOrderSample, LedgerEntry> productOrderSampleLedgerEntrySetJoin =
                productOrderProductOrderSampleListJoin.join(ProductOrderSample_.ledgerItems);

        // Even if there are ledger entries there may not be a billing session so left join.
        Join<LedgerEntry, BillingSession> ledgerEntryBillingSessionJoin =
                productOrderSampleLedgerEntrySetJoin.join(LedgerEntry_.billingSession, JoinType.LEFT);

        cq.select(
                cb.construct(ProductOrderListEntry.class,
                        productOrderRoot.get(ProductOrder_.productOrderId),
                        productOrderRoot.get(ProductOrder_.jiraTicketKey),
                        ledgerEntryBillingSessionJoin.get(BillingSession_.billingSessionId),
                        cb.count(productOrderRoot)));

        List<Predicate> predicates = new ArrayList<Predicate>();
        predicates.add(cb.isNull(ledgerEntryBillingSessionJoin.get(BillingSession_.billedDate)));

        if (jiraTicketKey != null) {
            predicates.add(cb.equal(productOrderRoot.get(ProductOrder_.jiraTicketKey), jiraTicketKey));
        }

        //noinspection ToArrayCallWithZeroLengthArrayArgument
        cq.where(predicates.toArray(new Predicate[]{}));

        cq.groupBy(
                productOrderRoot.get(ProductOrder_.productOrderId),
                productOrderRoot.get(ProductOrder_.jiraTicketKey),
                ledgerEntryBillingSessionJoin.get(BillingSession_.billingSessionId));

        List<ProductOrderListEntry> resultList = getEntityManager().createQuery(cq).getResultList();

        // Merge these counts into the objects from the first-pass query.
        for (ProductOrderListEntry result : resultList) {
            if ((result.getJiraTicketKey() != null) &&
                (jiraKeyToProductOrderListEntryMap.containsKey(result.getJiraTicketKey()))) {

                ProductOrderListEntry productOrderListEntry = jiraKeyToProductOrderListEntryMap.get(result.getJiraTicketKey());
                productOrderListEntry.setBillingSessionId(result.getBillingSessionId());
                productOrderListEntry.setUnbilledLedgerEntryCount(result.getUnbilledLedgerEntryCount());
            }
        }

    }


    /**
     * Call the worker method with a null parameter indicating all ProductOrderListEntries should be fetched.
     *
     * @param jiraTicketKey The jira ticket.
     *
     * @return List of all ProductOrderListEntries.
     */
    private List<ProductOrderListEntry> findBaseProductOrderListEntries(@Nullable String jiraTicketKey) {
        return findBaseProductOrderListEntries(jiraTicketKey, null, null, null, null, null);
    }


    /**
     * First pass, ledger-unaware querying.
     *
     * @param jiraTicketKey The nullable JIRA ticket key parameter.  If null, all ProductOrderListEntries are fetched,
     *                      otherwise only the ProductOrderListEntry corresponding to the specific JIRA ticket key
     *                      is fetched.
     *
     * @param productFamilyId The identifier for the product family.
     * @param productBusinessKey The business key for the product (part number).
     * @param orderStatuses The list of order statuses to filter on.
     * @param placedDate The date range selector object for search.
     * @param owner The BSP user to use for placed.
     *
     * @return The order list.
     */
    private List<ProductOrderListEntry> findBaseProductOrderListEntries(
            @Nullable String jiraTicketKey,
            @Nullable Long productFamilyId,
            @Nullable String productBusinessKey,
            @Nullable List<ProductOrder.OrderStatus> orderStatuses,
            @Nullable DateRangeSelector placedDate,
            @Nullable BspUser owner) {

        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<ProductOrderListEntry> cq = cb.createQuery(ProductOrderListEntry.class);

        Root<ProductOrder> productOrderRoot = cq.from(ProductOrder.class);

        // These joins pick out attributes that are selected into the report object. DRAFTS can have any of these be null
        // so left joins are needed.
        Join<ProductOrder, Product> productOrderProductJoin = productOrderRoot.join(ProductOrder_.product, JoinType.LEFT);
        Join<Product, ProductFamily> productProductFamilyJoin = productOrderProductJoin.join(Product_.productFamily, JoinType.LEFT);
        Join<ProductOrder, ResearchProject> productOrderResearchProjectJoin = productOrderRoot.join(ProductOrder_.researchProject, JoinType.LEFT);

        cq.select(
                cb.construct(ProductOrderListEntry.class,
                        productOrderRoot.get(ProductOrder_.productOrderId),
                        productOrderRoot.get(ProductOrder_.title),
                        productOrderRoot.get(ProductOrder_.jiraTicketKey),
                        productOrderRoot.get(ProductOrder_.orderStatus),
                        productOrderProductJoin.get(Product_.productName),
                        productProductFamilyJoin.get(ProductFamily_.name),
                        productOrderResearchProjectJoin.get(ResearchProject_.title),
                        productOrderRoot.get(ProductOrder_.createdBy),
                        productOrderRoot.get(ProductOrder_.placedDate)));

        List<Predicate> queryItems = new ArrayList<Predicate>();

        // Only taking a single JIRA ticket key as I don't want to worry about Splitter-like issues for
        // a varargs array of keys as there is currently no need for that functionality.
        if (jiraTicketKey != null) {
            queryItems.add(cb.equal(productOrderRoot.get(ProductOrder_.jiraTicketKey), jiraTicketKey));
        }

        if (productFamilyId != null) {
            queryItems.add(cb.equal(productProductFamilyJoin.get(ProductFamily_.productFamilyId), productFamilyId));
        }

        if (!StringUtils.isBlank(productBusinessKey)) {
            queryItems.add(cb.equal(productOrderProductJoin.get(Product_.partNumber), productBusinessKey));
        }

        if (!CollectionUtils.isEmpty(orderStatuses)) {
            Predicate statusDisjunction = cb.disjunction();
            for (ProductOrder.OrderStatus status : orderStatuses) {
                statusDisjunction.getExpressions().add(cb.equal(productOrderRoot.get(ProductOrder_.orderStatus), status));
            }

            queryItems.add(statusDisjunction);
        }

        // If there is a placed date range and the range has at least a start or end date, then add a date range.
        if ((placedDate != null) && ((placedDate.getStart() != null) || (placedDate.getEnd() != null))) {
            if (placedDate.getStart() == null) {
                queryItems.add(cb.lessThan(productOrderRoot.get(ProductOrder_.placedDate), placedDate.getEnd()));
            } else if (placedDate.getEnd() == null) {
                queryItems.add(cb.greaterThan(productOrderRoot.get(ProductOrder_.placedDate), placedDate.getStartTime()));
            } else {
                queryItems.add(
                    cb.between(productOrderRoot.get(
                        ProductOrder_.placedDate), placedDate.getStart(), placedDate.getEndTime()));
            }
        }

        if (owner != null) {
            queryItems.add(cb.equal(productOrderRoot.get(ProductOrder_.createdBy), owner.getUserId()));
        }

        if (!CollectionUtils.isEmpty(queryItems)) {
            cq.where(cb.and(queryItems.toArray(new Predicate[queryItems.size()])));
        }

        return getEntityManager().createQuery(cq).getResultList();
    }

    /**
     * Generates reporting object ProductOrderListEntries for efficient Product Order list view.  Merges the
     * results of the first-pass ledger-unaware query with the second-pass ledger aware query.
     *
     * @return The list of order entries.
     */
    public List<ProductOrderListEntry> findProductOrderListEntries() {

        List<ProductOrderListEntry> productOrderListEntries = findBaseProductOrderListEntries(null);
        fetchUnbilledLedgerEntryCounts(productOrderListEntries);

        return productOrderListEntries;
    }

    public List<ProductOrderListEntry> findProductOrderListEntries(
            @Nullable Long productFamilyId,
            @Nullable String productBusinessKey,
            @Nullable List<ProductOrder.OrderStatus> orderStatuses,
            @Nullable DateRangeSelector placedDate,
            @Nullable BspUser owner) {

        List<ProductOrderListEntry> productOrderListEntries =
            findBaseProductOrderListEntries(null, productFamilyId, productBusinessKey, orderStatuses, placedDate, owner);
        fetchUnbilledLedgerEntryCounts(productOrderListEntries);

        return productOrderListEntries;
    }

    /**
     * Find the single ProductOrderListEntry corresponding to the specified JIRA ticket key.
     *
     * @param jiraTicketKey JIRA ticket key of non-DRAFT PDO to be fetched.
     *
     * @return corresponding ProductOrderListEntry.
     */
    public ProductOrderListEntry findSingle(@Nonnull String jiraTicketKey) {

        List<ProductOrderListEntry> productOrderListEntries = findBaseProductOrderListEntries(jiraTicketKey);

        if (productOrderListEntries.isEmpty()) {
            return null;
        }

        if (productOrderListEntries.size() > 1) {
            throw new RuntimeException("Too many results");
        }

        fetchUnbilledLedgerEntryCounts(productOrderListEntries, jiraTicketKey);

        return productOrderListEntries.get(0);
    }
}
