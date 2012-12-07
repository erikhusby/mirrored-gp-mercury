package org.broadinstitute.gpinformatics.athena.boundary.orders;


import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
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
import java.util.List;

@Stateful
@RequestScoped
public class ProductOrderManager {

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private QuoteService quoteService;


    private void validateUniqueProjectTitle(ProductOrder productOrder) {
        if (productOrderDao.findByTitle(productOrder.getTitle()) != null) {
            throw new RuntimeException("Product order with this title already exists, please choose a different title");
        }
    }

    private void validateQuote(ProductOrder productOrder) {
        try {
            quoteService.getQuoteByAlphaId(productOrder.getQuoteId());
        } catch (QuoteServerException e) {
            throw new RuntimeException(e);
        } catch (QuoteNotFoundException e) {
            throw new RuntimeException("Invalid quote: " + productOrder.getQuoteId());
        }
    }


    private void updateSamples(ProductOrder productOrder, List<String> sampleIds) {
        if (sampleIds.isEmpty()) {
            throw new RuntimeException("You must add at least one sample before placing an order");
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


    public void save(ProductOrder productOrder, List<String> productOrderSamplesIds, List<String> addOnPartNumbers) {

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
     * Initially the only change we support will be an updated quote, which is simply a field on the {@link ProductOrder}
     * @param productOrder
     */
    public void update(ProductOrder productOrder) {
        // update JIRA ticket with new quote
        updateJiraIssue(productOrder);
        // persist updated productOrder
        productOrderDao.persist(productOrder);
    }
}
