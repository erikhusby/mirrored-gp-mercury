package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession_;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.CriteriaInClauseCreator;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.jpa.JPASplitter;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateRangeSelector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequestScoped
@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ProductOrderListEntryDao extends GenericDao implements Serializable {

    private static final long serialVersionUID = -3433442117288051562L;

    /**
     * Universal first pass, ledger-unaware querying method used by both PDO view and PDO list queries.
     *
     * @param jiraTicketKey       Optional, specific JIRA ticket value to search, this is used to support billing data
     *                            in PDO view.
     * @param productFamilyId     The identifier for the product family.
     * @param productBusinessKeys The business key for the product (part number).
     * @param orderStatuses       The list of order statuses to filter on.
     * @param placedDate          The date range selector object for search.
     * @param ownerIds            The BSP user to use for placed.
     *
     * @return The order list.
     */
    private List<ProductOrderListEntry> findBaseProductOrderListEntries(
            @Nullable String jiraTicketKey,
            @Nullable Long productFamilyId,
            @Nullable List<String> productBusinessKeys,
            @Nullable Set<ProductOrder.OrderStatus> orderStatuses,
            @Nullable DateRangeSelector placedDate,
            @Nullable List<Long> ownerIds) {

        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<ProductOrderListEntry> cq = cb.createQuery(ProductOrderListEntry.class);

        Root<ProductOrder> productOrderRoot = cq.from(ProductOrder.class);

        // These joins pick out attributes that are selected into the report object. DRAFTS can have any of these be null
        // so left joins are needed.
        Join<ProductOrder, Product> productOrderProductJoin =
                productOrderRoot.join(ProductOrder_.product, JoinType.LEFT);
        Join<Product, ProductFamily> productProductFamilyJoin =
                productOrderProductJoin.join(Product_.productFamily, JoinType.LEFT);
        Join<ProductOrder, ResearchProject> productOrderResearchProjectJoin =
                productOrderRoot.join(ProductOrder_.researchProject, JoinType.LEFT);

        cq.select(
                cb.construct(ProductOrderListEntry.class,
                        productOrderRoot.get(ProductOrder_.productOrderId),
                        productOrderRoot.get(ProductOrder_.title),
                        productOrderRoot.get(ProductOrder_.jiraTicketKey),
                        productOrderRoot.get(ProductOrder_.orderStatus),
                        productOrderProductJoin,
                        productOrderResearchProjectJoin.get(ResearchProject_.title),
                        productOrderRoot.get(ProductOrder_.createdBy),
                        productOrderRoot.get(ProductOrder_.placedDate),
                        productOrderRoot.get(ProductOrder_.laneCount),
                        productOrderRoot.get(ProductOrder_.quoteId),
                        productOrderRoot.get(ProductOrder_.orderType)));

        List<Predicate> listOfAndTerms = new ArrayList<>();

        // This is to support the PDO view use case, fetching the "Can Bill?" and "Billing Session" data.
        if (jiraTicketKey != null) {
            listOfAndTerms.add(cb.equal(productOrderRoot.get(ProductOrder_.jiraTicketKey), jiraTicketKey));
        }

        if (productFamilyId != null) {
            listOfAndTerms.add(cb.equal(productProductFamilyJoin.get(ProductFamily_.productFamilyId), productFamilyId));
        }

        if (!CollectionUtils.isEmpty(productBusinessKeys)) {
            listOfAndTerms.add(productOrderProductJoin.get(Product_.partNumber).in(productBusinessKeys));
        }

        listOfAndTerms.add(createStatusTerms(cb, placedDate, orderStatuses, productOrderRoot));

        if (!CollectionUtils.isEmpty(ownerIds)) {
            listOfAndTerms.add(productOrderRoot.get(ProductOrder_.createdBy).in(ownerIds));
        }

        if (!CollectionUtils.isEmpty(listOfAndTerms)) {
            cq.where(listOfAndTerms.toArray(new Predicate[listOfAndTerms.size()]));
        }

        return getEntityManager().createQuery(cq).getResultList();
    }


    // PDOs with these statuses don't have a Placed Date, and will always be excluded from date range searches.
    private static final Set<ProductOrder.OrderStatus> NO_DATE_STATUSES =
            EnumSet.of(ProductOrder.OrderStatus.Draft, ProductOrder.OrderStatus.Pending);


    /**
     * To get draft to work, we must run the statuses with the dates as an AND query and then OR in all draft
     * and pending orders.
     *
     * @param cb The criteria builder object.
     * @param placedDate The data placed range object.
     * @param orderStatuses The statuses.
     * @param productOrderRoot The product order root query object.
     *
     * @return The predicate that represents this whole query.
     */
    private Predicate createStatusTerms(
        CriteriaBuilder cb, DateRangeSelector placedDate,
        Set<ProductOrder.OrderStatus> orderStatuses, Root<ProductOrder> productOrderRoot) {

        // If there are no order statuses, add them all so that the inner query on data/draft will work.
        Set<ProductOrder.OrderStatus> fixedOrderStatuses = orderStatuses;
        if (CollectionUtils.isEmpty(fixedOrderStatuses)) {
            fixedOrderStatuses = EnumSet.allOf(ProductOrder.OrderStatus.class);
        }

        // create the and terms for the status and the dates
        List<Predicate> listOfAndTerms = new ArrayList<>();

        Collection<ProductOrder.OrderStatus> filteredStatuses =
                CollectionUtils.removeAll(fixedOrderStatuses, NO_DATE_STATUSES);

        Predicate statusAndDate;

        if (!filteredStatuses.isEmpty()) {
            // Match for the statuses that support date ranges.
            listOfAndTerms.add(productOrderRoot.get(ProductOrder_.orderStatus).in(filteredStatuses));

            // If there is a placed date range and the range has at least a start or end date, then add a date range.
            if ((placedDate != null) && ((placedDate.getStart() != null) || (placedDate.getEnd() != null))) {
                if (placedDate.getStart() == null) {
                    listOfAndTerms
                            .add(cb.lessThan(productOrderRoot.get(ProductOrder_.placedDate), placedDate.getEndTime()));
                } else if (placedDate.getEnd() == null) {
                    listOfAndTerms.add(
                            cb.greaterThan(productOrderRoot.get(ProductOrder_.placedDate), placedDate.getStartTime()));
                } else {
                    listOfAndTerms
                            .add(cb.between(productOrderRoot.get(ProductOrder_.placedDate), placedDate.getStartTime(),
                                    placedDate.getEndTime()));
                }
            }
            // AND the order statuses with the date ranges.
            statusAndDate = cb.and(listOfAndTerms.toArray(new Predicate[listOfAndTerms.size()]));
        } else {
            // Create an expression for FALSE.
            statusAndDate = cb.or();
        }

        // If there were any non-date range statuses, handle them here.
        Collection<ProductOrder.OrderStatus> remainingStatuses =
                CollectionUtils.retainAll(fixedOrderStatuses, NO_DATE_STATUSES);
        if (!remainingStatuses.isEmpty()) {
            // Now add the orders by doing an in query with the statuses.
            statusAndDate = cb.or(statusAndDate, productOrderRoot.get(ProductOrder_.orderStatus).in(remainingStatuses));
        }

        return statusAndDate;
    }

    /**
     * This fetches the count information in some subqueries and then populates the transient counts for the appropriate
     * orders. The makes the ledger status field work in the view.
     *
     * @param productOrderListEntries The order entry list to fill up with data.
     */
    private void fetchUnbilledLedgerEntryCounts(List<ProductOrderListEntry> productOrderListEntries) {

        // Build query to pick out eligible DTOs.  This only returns values for PDOs with ledger entries in open
        // billing sessions or with no associated billing session.
        final CriteriaBuilder cb = getCriteriaBuilder();
        final CriteriaQuery<ProductOrderListEntry> cq = cb.createQuery(ProductOrderListEntry.class);
        cq.distinct(true);

        if (CollectionUtils.isEmpty(productOrderListEntries)) {
            return;
        }

        final Root<ProductOrder> productOrderRoot = cq.from(ProductOrder.class);

        // Left join the samples, inner join the samples to the ledger items.
        ListJoin<ProductOrder, ProductOrderSample> productOrderProductOrderSampleListJoin =
                productOrderRoot.join(ProductOrder_.samples, JoinType.LEFT);

        final SetJoin<ProductOrderSample, LedgerEntry> productOrderSampleLedgerEntrySetJoin =
                productOrderProductOrderSampleListJoin.join(ProductOrderSample_.ledgerItems);

        // Even if there are ledger entries there may not be a billing session so left join.
        final Join<LedgerEntry, BillingSession> ledgerEntryBillingSessionJoin =
                productOrderSampleLedgerEntrySetJoin.join(LedgerEntry_.billingSession, JoinType.LEFT);

        cq.select(
                cb.construct(ProductOrderListEntry.class,
                        productOrderRoot.get(ProductOrder_.productOrderId),
                        productOrderRoot.get(ProductOrder_.jiraTicketKey),
                        ledgerEntryBillingSessionJoin.get(BillingSession_.billingSessionId),
                        cb.count(productOrderRoot)));

        cq.groupBy(
                productOrderRoot.get(ProductOrder_.productOrderId),
                productOrderRoot.get(ProductOrder_.jiraTicketKey),
                ledgerEntryBillingSessionJoin.get(BillingSession_.billingSessionId));


        List<String> jiraTicketKeys = new ArrayList<>();
        for (ProductOrderListEntry productOrderListEntry : productOrderListEntries) {
            if (productOrderListEntry.getJiraTicketKey() != null) {
                jiraTicketKeys.add(productOrderListEntry.getJiraTicketKey());
            }
        }

        for (final ProductOrderListEntry.LedgerStatus ledgerStatus : ProductOrderListEntry.LedgerStatus.values()) {
            if (!ledgerStatus.canCreateQuery) {
                continue;
            }
            // Only query for the JIRA ticket keys of interest, using Splitter.
            List<ProductOrderListEntry> resultList = JPASplitter.runCriteriaQuery(
                    jiraTicketKeys,
                    new CriteriaInClauseCreator<String>() {
                        @Override
                        public Query createCriteriaInQuery(Collection<String> parameterList) {
                            cq.where(ledgerStatus.buildPredicate(cb, productOrderSampleLedgerEntrySetJoin, ledgerEntryBillingSessionJoin),
                                    productOrderRoot.get(ProductOrder_.jiraTicketKey).in(parameterList));
                            return getEntityManager().createQuery(cq);
                        }
                    }
            );

            // Build map of input productOrderListEntries jira key to DTO.
            Map<String, ProductOrderListEntry> jiraKeyToProductOrderListEntryMap = new HashMap<>();
            for (ProductOrderListEntry productOrderListEntry : productOrderListEntries) {
                jiraKeyToProductOrderListEntryMap.put(productOrderListEntry.getJiraTicketKey(), productOrderListEntry);
            }

            // Merge these counts into the objects from the first-pass query.
            for (ProductOrderListEntry result : resultList) {
                if ((result.getJiraTicketKey() != null) &&
                    (jiraKeyToProductOrderListEntryMap.containsKey(result.getJiraTicketKey()))) {

                    ProductOrderListEntry productOrderListEntry =
                            jiraKeyToProductOrderListEntryMap.get(result.getJiraTicketKey());
                    productOrderListEntry.setBillingSessionId(result.getBillingSessionId());

                    ledgerStatus.updateEntryCount(productOrderListEntry, result.getConstructedCount());
                }
            }
        }
    }

    /**
     * Find the ProductOrderListEntries for the PDO list page, honoring any optionally supplied server-side filtering
     * criteria.
     */
    public List<ProductOrderListEntry> findProductOrderListEntries(
            @Nullable Long productFamilyId,
            @Nullable List<String> productKeys,
            @Nullable Set<ProductOrder.OrderStatus> orderStatuses,
            @Nullable DateRangeSelector placedDate,
            @Nullable List<Long> ownerIds,
            @Nullable List<ProductOrderListEntry.LedgerStatus> ledgerStatuses) {

        List<ProductOrderListEntry> productOrderListEntries =
                findBaseProductOrderListEntries(null, productFamilyId, productKeys, orderStatuses, placedDate,
                        ownerIds);

        // Populate the ledger entry counts by doing queries.
        fetchUnbilledLedgerEntryCounts(productOrderListEntries);

        // Since the querying here is already multi-pass and a bit confusing, adding the ledger status as a post filter.
        // Not ideal, but with the number of orders that we will typically have on these filters, it should not pose a
        // problem for a long time. Before needing to make this work, we will probably want scrolling and paging. This
        // returns the whole list if there is nothing selected or if all statuses are selected.
        if (CollectionUtils.isEmpty(ledgerStatuses) ||
            (ledgerStatuses.size() == ProductOrderListEntry.LedgerStatus.values().length)) {
            // If there is nothing selected or all statuses are selected, just return the full list.
            return productOrderListEntries;
        }

        // The following code filters out only the items that have the appropriate ledger status that the user wants.
        List<ProductOrderListEntry> filteredList = new ArrayList<>();
        for (ProductOrderListEntry entry : productOrderListEntries) {
            if (entry.matchStatuses(ledgerStatuses)) {
                filteredList.add(entry);
            }
        }

        return filteredList;
    }

    /**
     * Find the single ProductOrderListEntry corresponding to the specified JIRA ticket key for the PDO view page.
     *
     * @param jiraTicketKey JIRA ticket key of non-DRAFT PDO to be fetched.
     *
     * @return Corresponding ProductOrderListEntry.
     */
    public ProductOrderListEntry findSingle(@Nonnull String jiraTicketKey) {

        List<ProductOrderListEntry> productOrderListEntries =
                findBaseProductOrderListEntries(jiraTicketKey, null, null, null, null, null);

        if (productOrderListEntries.isEmpty()) {
            return null;
        }

        if (productOrderListEntries.size() > 1) {
            throw new RuntimeException("Too many results");
        }

        fetchUnbilledLedgerEntryCounts(productOrderListEntries);

        return productOrderListEntries.get(0);
    }
}
