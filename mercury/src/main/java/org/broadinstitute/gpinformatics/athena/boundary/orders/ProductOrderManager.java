package org.broadinstitute.gpinformatics.athena.boundary.orders;


import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Stateful
@RequestScoped
public class ProductOrderManager {

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private QuoteService quoteService;


    private void validateUniqueProjectTitle(ProductOrder productOrder) throws DuplicateTitleException {
        if (productOrderDao.findByTitle(productOrder.getTitle()) != null) {
            throw new DuplicateTitleException();
        }
    }

    private void validateQuote(ProductOrder productOrder) throws QuoteNotFoundException {
        try {
            quoteService.getQuoteByAlphaId(productOrder.getQuoteId());
        } catch (QuoteServerException e) {
            throw new RuntimeException(e);
        }
    }


    private void updateSamples(ProductOrder productOrder, List<String> sampleIds) throws NoSamplesException {
        if (sampleIds.isEmpty()) {
            throw new NoSamplesException();
        }

        List<ProductOrderSample> orderSamples = new ArrayList<ProductOrderSample>(sampleIds.size());
        for (String sampleId : sampleIds) {
            orderSamples.add(new ProductOrderSample(sampleId));
        }
        productOrder.setSamples(orderSamples);
    }


    private void createJiraIssue(ProductOrder productOrder) {
        try {
            productOrder.submitProductOrder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void updateAddOnProducts(ProductOrder productOrder, List<String> addOnPartNumbers) {
        List<Product> addOns =
                addOnPartNumbers.isEmpty() ? new ArrayList<Product>() : productDao.findByPartNumbers(addOnPartNumbers);

        productOrder.updateAddOnProducts(addOns);
    }


    private void updateStatus(ProductOrder productOrder) {
        // DRAFT orders not yet supported; force state of new PDOs to Submitted.
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
    }


    /**
     * Including {@link QuoteNotFoundException} since this is an expected failure that may occur in application validation
     *
     * @param productOrder
     * @param productOrderSamplesIds
     * @param addOnPartNumbers
     * @throws QuoteNotFoundException
     */
    public void save(ProductOrder productOrder, List<String> productOrderSamplesIds, List<String> addOnPartNumbers) throws DuplicateTitleException, QuoteNotFoundException, NoSamplesException {

        validateUniqueProjectTitle(productOrder);
        validateQuote(productOrder);
        updateSamples(productOrder, productOrderSamplesIds);
        updateAddOnProducts(productOrder, addOnPartNumbers);
        updateStatus(productOrder);
        // create JIRA before we attempt to persist since that is more likely to fail
        createJiraIssue(productOrder);

        productOrderDao.persist(productOrder);

    }


    private void updateJiraIssue(ProductOrder productOrder) {
        throw new UnsupportedOperationException("updateJiraTicket not yet implemented");
    }


    /**
     * Allow updated quotes, products, and add-ons.
     *
     * @param productOrder
     * @param selectedAddOnPartNumbers
     */
    public void update(final ProductOrder productOrder, final List<String> selectedAddOnPartNumbers) {
        // update JIRA ticket with new quote
        // GPLIM-488
        // updateJiraIssue(productOrder);

        // In the PDO edit UI, if the user goes through and edits the quote and then hits 'Submit', this works
        // without the merge.  But if the user tabs out of the quote field before hitting 'Submit', this merge
        // is required because our method receives a detached ProductOrder instance.
        ProductOrder updatedProductOrder = productOrderDao.getEntityManager().merge(productOrder);

        // update add-ons, first remove old
        for (ProductOrderAddOn productOrderAddOn : updatedProductOrder.getAddOns()) {
            productOrderDao.remove(productOrderAddOn);
        }

        // set new add-ons in
        Set<ProductOrderAddOn> productOrderAddOns = new HashSet<ProductOrderAddOn>();
        for (Product addOn : productDao.findByPartNumbers(selectedAddOnPartNumbers)) {
            ProductOrderAddOn productOrderAddOn = new ProductOrderAddOn(addOn, updatedProductOrder);
            productOrderDao.persist(productOrderAddOn);
            productOrderAddOns.add(productOrderAddOn);
        }
        
        updatedProductOrder.setProductOrderAddOns(productOrderAddOns);

    }
}
