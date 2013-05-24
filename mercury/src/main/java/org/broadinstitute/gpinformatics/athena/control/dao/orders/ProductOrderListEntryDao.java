package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.apache.commons.collections.CollectionUtils;
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
import java.util.*;

@RequestScoped
@Stateful
public class ProductOrderListEntryDao extends GenericDao implements Serializable {

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
            @Nullable List<ProductOrder.OrderStatus> orderStatuses,
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
                        productOrderProductJoin.get(Product_.productName),
                        productProductFamilyJoin.get(ProductFamily_.name),
                        productOrderResearchProjectJoin.get(ResearchProject_.title),
                        productOrderRoot.get(ProductOrder_.createdBy),
                        productOrderRoot.get(ProductOrder_.placedDate),
                        productOrderRoot.get(ProductOrder_.quoteId)));

        List<Predicate> listOfAndTerms = new ArrayList<Predicate>();

        // This is to support the PDO view use case, fetching the "Can Bill?" and "Billing Session" data.
        if (jiraTicketKey != null) {
            listOfAndTerms.add(cb.equal(productOrderRoot.get(ProductOrder_.jiraTicketKey), jiraTicketKey));
        }

        if (productFamilyId != null) {
            listOfAndTerms.add(cb.equal(productProductFamilyJoin.get(ProductFamily_.productFamilyId), productFamilyId));
        }

        if (!CollectionUtils.isEmpty(productBusinessKeys)) {
            listOfAndTerms
                    .add(createOrTerms(cb, productOrderProductJoin.get(Product_.partNumber), productBusinessKeys));
        }

        listOfAndTerms.add(createStatusTerms(cb, placedDate, orderStatuses, productOrderRoot));

        if (!CollectionUtils.isEmpty(ownerIds)) {
            listOfAndTerms.add(createOrTerms(cb, productOrderRoot.get(ProductOrder_.createdBy), ownerIds));
        }

        if (!CollectionUtils.isEmpty(listOfAndTerms)) {
            cq.where(listOfAndTerms.toArray(new Predicate[listOfAndTerms.size()]));
        }

        return getEntityManager().createQuery(cq).getResultList();
    }

    /**
     * To get draft to work, we must run the statuses with the dates as an AND query and then OR in all drafts.
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
        List<ProductOrder.OrderStatus> orderStatuses, Root<ProductOrder> productOrderRoot) {

        // If there are no order statuses, add them all so that the inner query on data/draft will work.
        List<ProductOrder.OrderStatus> fixedOrderStatuses = orderStatuses;
        if (CollectionUtils.isEmpty(fixedOrderStatuses)) {
            fixedOrderStatuses = Arrays.asList(ProductOrder.OrderStatus.values());
        }

        // create the and terms for the status and the dates
        List<Predicate> listOfAndTerms = new ArrayList<Predicate>();

        // No matter what and the order statuses with the date rage
        listOfAndTerms.add(createOrTerms(cb, productOrderRoot.get(ProductOrder_.orderStatus), fixedOrderStatuses));

        // If there is a placed date range and the range has at least a start or end date, then add a date range.
        if ((placedDate != null) && ((placedDate.getStart() != null) || (placedDate.getEnd() != null))) {
            if (placedDate.getStart() == null) {
                listOfAndTerms
                        .add(cb.lessThan(productOrderRoot.get(ProductOrder_.placedDate), placedDate.getEndTime()));
            } else if (placedDate.getEnd() == null) {
                listOfAndTerms
                        .add(cb.greaterThan(productOrderRoot.get(ProductOrder_.placedDate), placedDate.getStartTime()));
            } else {
                listOfAndTerms.add(
                        cb.between(productOrderRoot.get(
                                ProductOrder_.placedDate), placedDate.getStart(), placedDate.getEndTime()));
            }
        }

        Predicate statusAndDate = cb.and(listOfAndTerms.toArray(new Predicate[listOfAndTerms.size()]));

        // return the status and date, if not looking for Drafts
        if (!fixedOrderStatuses.contains(ProductOrder.OrderStatus.Draft)) {
            return statusAndDate;
        }

        // Now add all the drafts by doing an OR query with just drafts
        return cb.or(
            statusAndDate, cb.equal(productOrderRoot.get(ProductOrder_.orderStatus), ProductOrder.OrderStatus.Draft));
    }


    /**
     * Second-pass, ledger aware query that merges its results into the first-pass objects passed as an argument.
     * <p/>
     * Fetch the count of unbilled ledger entries for the
     * {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder}s referenced by the
     * ProductOrderListEntry DTOs and set those counts into the DTOs.
     *
     * @param productOrderListEntries The entries to fill with unbilled ledger entry counts.
     */
    private void fetchUnbilledLedgerEntryCounts(List<ProductOrderListEntry> productOrderListEntries) {

        if (CollectionUtils.isEmpty(productOrderListEntries)) {
            return;
        }

        // Build query to pick out eligible DTOs.  This only returns values for PDOs with ledger entries in open
        // billing sessions or with no associated billing session.
        final CriteriaBuilder cb = getCriteriaBuilder();
        final CriteriaQuery<ProductOrderListEntry> cq = cb.createQuery(ProductOrderListEntry.class);

        cq.distinct(true);

        final Root<ProductOrder> productOrderRoot = cq.from(ProductOrder.class);

        // Left join the samples, inner join the samples to the ledger items.
        ListJoin<ProductOrder, ProductOrderSample> productOrderProductOrderSampleListJoin =
                productOrderRoot.join(ProductOrder_.samples, JoinType.LEFT);

        SetJoin<ProductOrderSample, LedgerEntry> productOrderSampleLedgerEntrySetJoin =
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


        List<String> jiraTicketKeys = new ArrayList<String>();
        for (ProductOrderListEntry productOrderListEntry : productOrderListEntries) {
            if (productOrderListEntry.getJiraTicketKey() != null) {
                jiraTicketKeys.add(productOrderListEntry.getJiraTicketKey());
            }
        }

        // Only query for the JIRA ticket keys of interest, using Splitter.
        List<ProductOrderListEntry> resultList = JPASplitter.runCriteriaQuery(
                jiraTicketKeys,
                new CriteriaInClauseCreator<String>() {
                    @Override
                    public Query createCriteriaInQuery(Collection<String> parameterList) {
                        CriteriaQuery<ProductOrderListEntry> query = cq.where(
                                cb.isNull(ledgerEntryBillingSessionJoin.get(BillingSession_.billedDate)),
                                productOrderRoot.get(ProductOrder_.jiraTicketKey).in(parameterList));

                        return getEntityManager().createQuery(query);
                    }
                }
        );

        // Build map of input productOrderListEntries jira key to DTO.
        Map<String, ProductOrderListEntry> jiraKeyToProductOrderListEntryMap =
                new HashMap<String, ProductOrderListEntry>();
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
                productOrderListEntry.setUnbilledLedgerEntryCount(result.getUnbilledLedgerEntryCount());
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
            @Nullable List<ProductOrder.OrderStatus> orderStatuses,
            @Nullable DateRangeSelector placedDate,
            @Nullable List<Long> ownerIds) {

        List<ProductOrderListEntry> productOrderListEntries =
                findBaseProductOrderListEntries(null, productFamilyId, productKeys, orderStatuses, placedDate,
                        ownerIds);
        fetchUnbilledLedgerEntryCounts(productOrderListEntries);

        return productOrderListEntries;
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
