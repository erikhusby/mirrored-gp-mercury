package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
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
import org.broadinstitute.sap.entity.SAPDeliveryDocument;
import org.broadinstitute.sap.entity.SAPDeliveryItem;
import org.broadinstitute.sap.entity.SAPMaterial;
import org.broadinstitute.sap.entity.SAPOrder;
import org.broadinstitute.sap.entity.SAPOrderItem;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;

import javax.inject.Inject;
import java.math.BigDecimal;

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
        log.debug("Am in the default constructor");
    }

    public SapIntegrationServiceImpl(SapConfig sapConfigIn) {
        if(sapConfig == null) {
            this.sapConfig = sapConfigIn;
        }
    }

    protected void initializeClient() {

        SapIntegrationClientImpl.SAPEnvironment environment;

        switch (sapConfig.getDeploymentConfig()) {
        case PROD:
            environment = SapIntegrationClientImpl.SAPEnvironment.PRODUCTION;
            break;
        default:
            environment = SapIntegrationClientImpl.SAPEnvironment.QA;
            break;
        }

        wrappedClient = new SapIntegrationClientImpl(sapConfig.getLogin(), sapConfig.getPassword(),
                environment);
    }

    private SapIntegrationClientImpl getClient() {
        if(wrappedClient == null) {
            initializeClient();
        }
        return wrappedClient;
    }

    @Override
    public String createOrder(ProductOrder placedOrder) throws SAPIntegrationException {

        SAPOrder newOrder = initializeSAPOrder(placedOrder);

        return getClient().createSAPOrder(newOrder);
    }

    @Override
    public String updateOrder(ProductOrder placedOrder) throws SAPIntegrationException {

        SAPOrder newOrder = initializeSAPOrder(placedOrder);

        newOrder.setSapOrderNumber(placedOrder.getSapOrderNumber());

        return getClient().createSAPOrder(newOrder);
    }

    private SAPOrder initializeSAPOrder(ProductOrder placedOrder) throws SAPIntegrationException {
        Quote foundQuote = null;
        try {
            foundQuote = quoteService.getQuoteByAlphaId(placedOrder.getQuoteId());
        } catch (QuoteServerException | QuoteNotFoundException e) {
            throw new SAPIntegrationException("Unable to get information for the Quote from the quote server", e);
        }

        String customerNumber = findCustomer(foundQuote, determineCompanyCode(placedOrder));

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

    @Override
    public String findCustomer(Quote foundQuote, String companyCode) throws SAPIntegrationException {

        String customerNumber = null;
        if (foundQuote.getQuoteFunding().getFundingLevel().size() > 1) {
            // Too many funding sources to allow this to work with SAP.  Keep using the Quote Server as the definition
            // of funding
            throw new SAPIntegrationException(
                    "Unable to continue with SAP.  The associated quote has multiple funding sources");
        } else {

            Funding funding = foundQuote.getQuoteFunding().getFundingLevel().iterator().next().getFunding();
            if (funding.getFundingType().equals(Funding.PURCHASE_ORDER)) {
                try {
                    customerNumber = getClient().findCustomerNumber(funding.getPurchaseOrderContact(), companyCode);
                } catch (SAPIntegrationException e) {
                    if (e.getMessage().equals(SapIntegrationClientImpl.MISSING_CUSTOMER_RESULT)) {
                        throw new SAPIntegrationException(
                                "Your order cannot be placed in SAP because the email address "
                                + "specified on the Quote or Mercury PDO is not attached to "
                                + "any SAP Customer account.\n"
                                + "An email has been sent to Amber Kennedy in AR to initiate "
                                + "this SAP Customer Creation process. Please contact Amber "
                                + "Kennedy to follow this up.\n"
                                + "Once the Customer has been created in SAP you will need "
                                + "to resubmit this order to ensure that your work is "
                                + "properly processed.\n"
                                + "For further questions please contact Mercury support");
                    } else if (e.getMessage().equals(SapIntegrationClientImpl.TOO_MANY_ACCOUNTS_RESULT)) {
                        throw new SAPIntegrationException(
                                "Your order cannot be placed because the email address specified "
                                + "on the Quote or Mercury PDO is associated with more than 1 "
                                + "SAP Customer account.\n"
                                + "An email has been sent to Amber Kennedy in AR to initiate "
                                + "this SAP Customer Creation process. Please contact Amber "
                                + "Kennedy to follow this up.\n"
                                + "Once the SAP Customer account has been corrected you will "
                                + "need to resubmit this order to ensure that your work is "
                                + "properly processed.\n"
                                + "For further questions please contact Mercury support");
                    } else {
                        throw e;
                    }
                }
            }
        }

        return customerNumber;
    }

    @Override
    public String billOrder(QuoteImportItem quoteItemForBilling) throws SAPIntegrationException {

        SAPDeliveryDocument deliveryDocument =
                new SAPDeliveryDocument(SapIntegrationClientImpl.SystemIdentifier.MERCURY,
                        determineCompanyCode(quoteItemForBilling.getProductOrder()),
                        quoteItemForBilling.getProductOrder().getSapOrderNumber());

        SAPDeliveryItem lineItem =
                new SAPDeliveryItem(quoteItemForBilling.getProduct().getPartNumber(),
                        new BigDecimal(quoteItemForBilling.getQuantityForSAP()));
        deliveryDocument.addDeliveryItem(lineItem);


        return getClient().createDeliveryDocument(deliveryDocument);
    }

    @Override
    public void createProductInSAP(Product product) throws SAPIntegrationException {
        SAPMaterial newMaterial = new SAPMaterial(product.getPartNumber(),
                SapIntegrationClientImpl.SystemIdentifier.MERCURY, product.getAvailabilityDate(),
                product.getAvailabilityDate());
        newMaterial.setCompanyCode(
                product.isExternalProduct() ? SapIntegrationClientImpl.BROAD_EXTERNAL_SERVICES_COMPANY_CODE :
                        SapIntegrationClientImpl.BROAD_COMPANY_CODE);
        newMaterial.setDescription(product.getProductName());
        newMaterial.setDeliverables(product.getDeliverables());
        newMaterial.setInputRequirements(product.getInputRequirements());
        newMaterial.setBaseUnitOfMeasure("1");
        BigDecimal minimumOrderQuantity = product.getMinimumOrderSize()!= null?new BigDecimal(product.getMinimumOrderSize()):BigDecimal.ONE;
        newMaterial.setMinimumOrderQuantity(minimumOrderQuantity);


        getClient().createMaterial(newMaterial);
    }

    @Override
    public void changeProductInSAP(Product product) throws SAPIntegrationException {

        SAPMaterial existingMaterial = new SAPMaterial(product.getPartNumber(),
                SapIntegrationClientImpl.SystemIdentifier.MERCURY, product.getAvailabilityDate(),
                product.getAvailabilityDate());
        existingMaterial.setCompanyCode(
                product.isExternalProduct() ? SapIntegrationClientImpl.BROAD_EXTERNAL_SERVICES_COMPANY_CODE :
                        SapIntegrationClientImpl.BROAD_COMPANY_CODE);
        existingMaterial.setDescription(product.getProductName());
        existingMaterial.setDeliverables(product.getDeliverables());
        existingMaterial.setInputRequirements(product.getInputRequirements());

        if(product.isAvailable()) {
            existingMaterial.setStatus(SAPMaterial.MaterialStatus.ENABLED);
        } else {
            existingMaterial.setStatus(SAPMaterial.MaterialStatus.DISABLED);
        }

        getClient().createMaterial(existingMaterial);
    }

    private String determineCompanyCode(ProductOrder companyProductOrder) {
        String companyCode = SapIntegrationClientImpl.BROAD_COMPANY_CODE;
        if (companyProductOrder.getResearchProject().getRegulatoryDesignation()
            != ResearchProject.RegulatoryDesignation.RESEARCH_ONLY ||
            companyProductOrder.getProduct().isExternalProduct()) {
            companyCode = SapIntegrationClientImpl.BROAD_EXTERNAL_SERVICES_COMPANY_CODE;
        }

        return companyCode;
    }
}
