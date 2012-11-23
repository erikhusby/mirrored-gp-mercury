package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.entity.ProductOrderListEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger_;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RequestScoped
@Stateful
public class ProductOrderListEntryDao extends GenericDao {


    /**
     * Fetch the count of unbilled ledger entries for the {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder}s referenced by the
     * {@link org.broadinstitute.gpinformatics.athena.entity.ProductOrderListEntry} DTOs and set those counts into the DTOs.
     *
     * @param productOrderListEntries
     */
    private void fetchUnbilledLedgerEntryCounts(List<ProductOrderListEntry> productOrderListEntries) {

        // build map of input productOrderListEntries jira key to DTO
        Map<String, ProductOrderListEntry> jiraKeyToProductOrderListEntryMap =
                new HashMap<String, ProductOrderListEntry>();

        for (ProductOrderListEntry productOrderListEntry : productOrderListEntries) {
            jiraKeyToProductOrderListEntryMap.put(productOrderListEntry.getJiraTicketKey(), productOrderListEntry);
        }


        // build query to pick out eligible DTOs
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<ProductOrderListEntry> cq = cb.createQuery(ProductOrderListEntry.class);

        cq.distinct(true);

        Root<ProductOrder> productOrderRoot = cq.from(ProductOrder.class);

        // left join the samples, inner join the samples to the ledger items
        ListJoin<ProductOrder, ProductOrderSample> productOrderProductOrderSampleListJoin =
                productOrderRoot.join(ProductOrder_.samples, JoinType.LEFT);

        SetJoin<ProductOrderSample, BillingLedger> productOrderSampleBillingLedgerSetJoin =
                productOrderProductOrderSampleListJoin.join(ProductOrderSample_.ledgerItems);

        // even if there are ledger entries there may not be a billing session so left join
        Join<BillingLedger, BillingSession> billingLedgerBillingSessionJoin =
                productOrderSampleBillingLedgerSetJoin.join(BillingLedger_.billingSession, JoinType.LEFT);

        cq.select(
                cb.construct(ProductOrderListEntry.class,
                        productOrderRoot.get(ProductOrder_.jiraTicketKey),
                        billingLedgerBillingSessionJoin.get(BillingSession_.billingSessionId),
                        cb.count(productOrderRoot)));

        cq.where(
                cb.isNull(billingLedgerBillingSessionJoin.get(BillingSession_.billedDate)));

        cq.groupBy(
                productOrderRoot.get(ProductOrder_.jiraTicketKey),
                billingLedgerBillingSessionJoin.get(BillingSession_.billingSessionId));

        List<ProductOrderListEntry> resultList = getEntityManager().createQuery(cq).getResultList();

        for (ProductOrderListEntry result : resultList) {
            ProductOrderListEntry productOrderListEntry = jiraKeyToProductOrderListEntryMap.get(result.getJiraTicketKey());
            productOrderListEntry.setBillingSessionId(result.getBillingSessionId());
            productOrderListEntry.setUnbilledLedgerEntryCount(result.getUnbilledLedgerEntryCount());
        }

    }



    /**
     * no counts
     *
     * @return
     */
    private List<ProductOrderListEntry> findBaseProductOrderListEntries() {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<ProductOrderListEntry> cq = cb.createQuery(ProductOrderListEntry.class);

        Root<ProductOrder> productOrderRoot = cq.from(ProductOrder.class);

        // these joins pick out attributes that are selected into the report object
        Join<ProductOrder, Product> productOrderProductJoin = productOrderRoot.join(ProductOrder_.product);
        Join<Product, ProductFamily> productProductFamilyJoin = productOrderProductJoin.join(Product_.productFamily);
        Join<ProductOrder, ResearchProject> productOrderResearchProjectJoin = productOrderRoot.join(ProductOrder_.researchProject);

        cq.select(
                cb.construct(ProductOrderListEntry.class,
                        productOrderRoot.get(ProductOrder_.title),
                        productOrderRoot.get(ProductOrder_.jiraTicketKey),
                        productOrderRoot.get(ProductOrder_.orderStatus),
                        productOrderProductJoin.get(Product_.productName),
                        productProductFamilyJoin.get(ProductFamily_.name),
                        productOrderResearchProjectJoin.get(ResearchProject_.title),
                        productOrderRoot.get(ProductOrder_.createdBy),
                        productOrderRoot.get(ProductOrder_.createdDate)));

        return getEntityManager().createQuery(cq).getResultList();

    }


    /**
     * Generates reporting object {@link ProductOrderListEntry}s for efficient Product Order list view
     *
     * @return
     */
    public List<ProductOrderListEntry> findProductOrderListEntries() {

        List<ProductOrderListEntry> productOrderListEntries = findBaseProductOrderListEntries();
        fetchUnbilledLedgerEntryCounts(productOrderListEntries);

        return productOrderListEntries;

    }


}
