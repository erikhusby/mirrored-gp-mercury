package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
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
import org.jetbrains.annotations.NotNull;

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
    }

    public SapIntegrationServiceImpl(SapConfig sapConfigIn) {
        if(sapConfig == null) {
            this.sapConfig = sapConfigIn;
        }
    }

    /**
     * Helper method to initialize the common client this service will utilize in order to communicate to SAP
     */
    protected void initializeClient() {

        SapIntegrationClientImpl.SAPEnvironment environment;

        switch (sapConfig.getExternalDeployment()) {
        case PROD:
            environment = SapIntegrationClientImpl.SAPEnvironment.PRODUCTION;
            break;
        case DEV:
            environment = SapIntegrationClientImpl.SAPEnvironment.DEV;
            break;
        case QA:
        default:
            environment = SapIntegrationClientImpl.SAPEnvironment.QA;
            break;
        }

        log.debug("Config environment is: " + sapConfig.getExternalDeployment().name());
        log.debug("Sending to SAP environment of: " + environment.name());

        wrappedClient = new SapIntegrationClientImpl(sapConfig.getLogin(), sapConfig.getPassword(),
                environment);
    }

    /**
     * Getter for the common sap client utilized to communicate to SAP
     * @return
     */
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
    public void updateOrder(ProductOrder placedOrder) throws SAPIntegrationException {

        SAPOrder newOrder = initializeSAPOrder(placedOrder);

        if(placedOrder.getSapOrderNumber() == null) {
            throw new SAPIntegrationException("Cannot update an order in SAP since this product order does not have "
                                              + "an SAP Order number with which to reference an order.");
        }
        newOrder.setSapOrderNumber(placedOrder.getSapOrderNumber());

        getClient().updateSAPOrder(newOrder);
    }

    /**
     * Helper method to compile the order object which will be transmitted to SAP for either creation up to be
     * updated
     * @param placedOrder The ProductOrder from which a JAXB representation of an SAP order will be created
     * @return JAXB representation of a Product Order
     * @throws SAPIntegrationException
     */
    protected SAPOrder initializeSAPOrder(ProductOrder placedOrder) throws SAPIntegrationException {
        Quote foundQuote = null;
        try {
            foundQuote = quoteService.getQuoteByAlphaId(placedOrder.getQuoteId());
        } catch (QuoteServerException | QuoteNotFoundException e) {
            throw new SAPIntegrationException("Unable to get information for the Quote from the quote server", e);
        }

        SAPOrder newOrder =
                new SAPOrder(SapIntegrationClientImpl.SystemIdentifier.MERCURY, determineCompanyCode(placedOrder),
                        placedOrder.getQuoteId(), bspUserList.getUserFullName(placedOrder.getCreatedBy()));

        newOrder.setExternalOrderNumber(placedOrder.getJiraTicketKey());

        Funding funding = foundQuote.getQuoteFunding().getFundingLevel().iterator().next().getFunding();

        if (funding.getFundingType().equals(Funding.PURCHASE_ORDER)) {
            newOrder.setFundingSource(funding.getPurchaseOrderNumber(), SAPOrder.FundingType.PURCHASE_ORDER);
            String customerNumber = findCustomer(foundQuote, determineCompanyCode(placedOrder));

            newOrder.setSapCustomerNumber(customerNumber);
        } else {
            newOrder.setFundingSource(funding.getFundsReservationNumber(), SAPOrder.FundingType.FUNDS_RESERVATION);
        }

        newOrder.setResearchProjectNumber(placedOrder.getResearchProject().getJiraTicketKey());

        Product primaryProduct = placedOrder.getProduct();
        newOrder.addOrderItem(getOrderItem(placedOrder, primaryProduct));

        for (ProductOrderAddOn addon : placedOrder.getAddOns()) {
            newOrder.addOrderItem(getOrderItem(placedOrder, addon.getAddOn()));
        }

        return newOrder;
    }

    /**
     * Helper method to extract the sub elements of a Product order to represent a product and the quantity of which
     * is expected to be charged
     * @param placedOrder Order from which the quantities are defined
     * @param product Product that is to be eventually charged when work on the product order is completed
     * @return JAXB sub element of the SAP order to represent the Product that will be charged and the quantity that
     * is expected of it.
     */
    protected SAPOrderItem getOrderItem(ProductOrder placedOrder, Product product) {
        return new SAPOrderItem(product.getPartNumber(),
                priceListCache.findByKeyFields(product.getPrimaryPriceItem().getPlatform(),
                        product.getPrimaryPriceItem().getCategory(),
                        product.getPrimaryPriceItem().getName()).getPrice(),
                (product.getProductFamily().getName().equals(ProductFamily.ProductFamilyName.SEQUENCE_ONLY.name()) &&
                placedOrder.getLaneCount()>0)
                        ?placedOrder.getNonAbandonedCount()*placedOrder.getLaneCount()
                        :placedOrder.getNonAbandonedCount());
    }

    @Override
    public String findCustomer(Quote foundQuote,
                               SapIntegrationClientImpl.SAPCompanyConfiguration companyCode) throws SAPIntegrationException {

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
                                + "specified on the Quote is not attached to any SAP Customer account.\n"
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
                                + "on the Quote is associated with more than 1 SAP Customer account.\n"
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
        SAPMaterial newMaterial = initializeSapMaterialObject(product);

        getClient().createMaterial(newMaterial);
    }

    @NotNull
    protected SAPMaterial initializeSapMaterialObject(Product product) {
        SAPMaterial newMaterial = new SAPMaterial(product.getPartNumber(),
                SapIntegrationClientImpl.SystemIdentifier.MERCURY, product.getAvailabilityDate(),
                product.getAvailabilityDate());
        newMaterial.setCompanyCode(
                product.isExternalProduct() ? SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES :
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        newMaterial.setMaterialName(product.getProductName());
        newMaterial.setDescription(product.getDescription());
        newMaterial.setDeliverables(product.getDeliverables());
        newMaterial.setInputRequirements(product.getInputRequirements());
        newMaterial.setBaseUnitOfMeasure("EA");
        BigDecimal minimumOrderQuantity = product.getMinimumOrderSize() != null?new BigDecimal(product.getMinimumOrderSize()):BigDecimal.ONE;
        newMaterial.setMinimumOrderQuantity(minimumOrderQuantity);

        if(product.isDiscontinued()) {
            newMaterial.setStatus(SAPMaterial.MaterialStatus.DISABLED);
        } else {
            newMaterial.setStatus(SAPMaterial.MaterialStatus.ENABLED);
        }

        return newMaterial;
    }

    @Override
    public void changeProductInSAP(Product product) throws SAPIntegrationException {

        SAPMaterial existingMaterial = initializeSapMaterialObject(product);

        getClient().changeMaterialDetails(existingMaterial);
    }

    /**
     * Helper method to figure out which company code to associate with a given product order
     * @param companyProductOrder Product Order from which the company code is to be determined
     * @return an indicator that represents one of the configured companies within SAP
     */
    private SapIntegrationClientImpl.SAPCompanyConfiguration determineCompanyCode(ProductOrder companyProductOrder) {
        SapIntegrationClientImpl.SAPCompanyConfiguration companyCode = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
        if (companyProductOrder.getResearchProject().getRegulatoryDesignation()
            != ResearchProject.RegulatoryDesignation.RESEARCH_ONLY ||
            companyProductOrder.getProduct().isExternalProduct()) {
            companyCode = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES;
        }

        return companyCode;
    }

    protected void setQuoteService(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    protected void setBspUserList(BSPUserList bspUserList) {
        this.bspUserList = bspUserList;
    }

    protected void setWrappedClient(SapIntegrationClientImpl wrappedClient) {
        this.wrappedClient = wrappedClient;
    }

    protected void setPriceListCache(PriceListCache priceListCache) {
        this.priceListCache = priceListCache;
    }
}
