package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.sap.entity.SAPOrder;
import org.broadinstitute.sap.entity.SAPOrderItem;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;

import javax.inject.Inject;
import java.io.IOException;

@Impl
public class SapIntegrationServiceImpl implements SapIntegrationService {

    @Inject
    private SapConfig sapConfig;

    @Inject
    private QuoteService quoteService;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private PriceListCache priceListCache;

    private SapIntegrationClientImpl wrappedClient;

    private final static Log log = LogFactory.getLog(SapIntegrationServiceImpl.class);

    public SapIntegrationServiceImpl() {
    }

    public SapIntegrationServiceImpl(SapConfig sapConfigIn) {
        initializeClient(sapConfigIn);
    }

    private void initializeClient(SapConfig sapConfigIn) {
        wrappedClient = new SapIntegrationClientImpl(sapConfigIn.getLogin(), sapConfigIn.getPassword(),
                sapConfigIn.getBaseUrl(), sapConfigIn.getWsdlUri(), "/wsdl/sap/dev/sap_test_service.wsdl");
    }

    @Override
    public String submitAge(String age) throws IOException {
        if (wrappedClient == null) {
            initializeClient(this.sapConfig);
        }
        return wrappedClient.ageSubmission(age);
    }

    @Override
    public String createOrder(ProductOrder placedOrder) throws SAPInterfaceException {

        SAPOrder newOrder = initializeSAPOrder(placedOrder);

        return wrappedClient.createSAPOrder(newOrder);
    }

    @Override
    public String updateOrder(ProductOrder placedOrder) throws SAPInterfaceException {

        SAPOrder newOrder = initializeSAPOrder(placedOrder);

        newOrder.setSapOrderNumber(placedOrder.getSapOrderNumber());

        return wrappedClient.createSAPOrder(newOrder);
    }

    private SAPOrder initializeSAPOrder(ProductOrder placedOrder) throws SAPInterfaceException {
        Quote foundQuote = null;
        try {
            foundQuote = quoteService.getQuoteByAlphaId(placedOrder.getQuoteId());
        } catch (QuoteServerException | QuoteNotFoundException e) {
            throw new SAPInterfaceException("Unable to get information for the Quote from the quote server", e);
        }

        String customerNumber = findCustomer(foundQuote);

        SAPOrder newOrder =
                new SAPOrder(SapIntegrationClientImpl.SystemIdentifier.MERCURY, determineCompanyCode(placedOrder),
                        placedOrder.getQuoteId(), bspUserList.getUserFullName(placedOrder.getCreatedBy()));

        newOrder.setExternalOrderNumber(placedOrder.getJiraTicketKey());

        Funding funding = foundQuote.getQuoteFunding().getFundingLevel().iterator().next().getFunding();

        if (funding.getFundingType().equals(Funding.PURCHASE_ORDER)) {
            newOrder.setFundingSource(funding.getPurchaseOrderNumber(), SAPOrder.FundingType.PURCHASE_ORDER);
        } else {
            newOrder.setFundingSource(funding.getFundsReservationNumber(), SAPOrder.FundingType.FUNDS_RESERVATION);
        }

        newOrder.setSapCustomerNumber(customerNumber);

        newOrder.setResearchProjectNumber(placedOrder.getResearchProject().getJiraTicketKey());

        Product primaryProduct = placedOrder.getProduct();
        newOrder.addOrderItem(getOrderItem(placedOrder, primaryProduct));

        for (ProductOrderAddOn addon : placedOrder.getAddOns()) {
            newOrder.addOrderItem(getOrderItem(placedOrder, addon.getAddOn()));
        }

        return newOrder;
    }

    private SAPOrderItem getOrderItem(ProductOrder placedOrder, Product primaryProduct) {
        return new SAPOrderItem(primaryProduct.getPartNumber(),
                priceListCache.findByKeyFields(primaryProduct.getPrimaryPriceItem().getPlatform(),
                        primaryProduct.getPrimaryPriceItem().getCategory(),
                        primaryProduct.getPrimaryPriceItem().getName()).getPrice(),
                placedOrder.getSampleCount());
    }

    private String determineCompanyCode(ProductOrder companyProductOrder) {
        String companyCode = "1000";
        if (companyProductOrder.getResearchProject().getRegulatoryDesignation()
            != ResearchProject.RegulatoryDesignation.RESEARCH_ONLY ||
            companyProductOrder.getProduct().getPartNumber().startsWith("XT")) {
            companyCode = "2000";
        }

        return companyCode;
    }

    @Override
    public String findCustomer(Quote foundQuote) throws SAPInterfaceException {

        String customerEmailAddress;
        String customerNumber = null;
        if (foundQuote.getQuoteFunding().getFundingLevel().size() > 1) {
            // Too many funding sources to allow this to work with SAP.  Keep using the Quote Server as the definition
            // of funding
            throw new SAPInterfaceException(
                    "Unable to continue with SAP.  The associated quote has multiple funding sources");
        } else {

            Funding funding = foundQuote.getQuoteFunding().getFundingLevel().iterator().next().getFunding();
            if (funding.getFundingType().equals("Purchase Order")) {
                /*
                    TODO SGM:  This call will return more than just the customer number in terms of the error conditions.
                    Must account for the potential errors in this call to pass back to the user
                */
                customerNumber = wrappedClient.findCustomerNumber(funding.getPurchaseOrderContact());

            }
        }


        return customerNumber;
    }

    public String billOrder(BillingSession sessionForBilling) {
        return null;
    }


}
