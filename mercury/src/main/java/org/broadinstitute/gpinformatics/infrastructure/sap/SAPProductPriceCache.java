package org.broadinstitute.gpinformatics.infrastructure.sap;

import clover.org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.sap.entity.Condition;
import org.broadinstitute.sap.entity.DeliveryCondition;
import org.broadinstitute.sap.entity.SAPMaterial;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.springframework.util.CollectionUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class SAPProductPriceCache extends AbstractCache implements Serializable {

    private Set<SAPMaterial> sapMaterials = new HashSet<>();

    private SapIntegrationService sapService;

    //  A temporary short circuit until actual implementation of the service to retrieve all materials is in place
    //  When it is, this inclusion of the priceListCache will be removed.
    @Inject
    private PriceListCache quotesPriceListCache;

    private static final Log logger = LogFactory.getLog(SAPProductPriceCache.class);

    public SAPProductPriceCache() {
    }

    @Inject
    public SAPProductPriceCache(SapIntegrationService sapService) {
        this.sapService = sapService;
    }

    public SAPProductPriceCache(Set<SAPMaterial> sapMaterials) {
        setMaterials(sapMaterials);
    }


    @Override
    public synchronized void refreshCache() {
        try {
            Set<SAPMaterial> tempSet = sapService.findProductsInSap();

            if(!CollectionUtils.isEmpty(tempSet)) {
                setMaterials(tempSet);
            }
        } catch (SAPIntegrationException e) {
            logger.error("Could not refresh the SAP Product Price Cache", e);
        }

    }

    public void setMaterials(Set<SAPMaterial> tempSet) {
        sapMaterials = tempSet;
    }

    public Collection<SAPMaterial> getSapMaterials()
    {
        if(CollectionUtils.isEmpty(sapMaterials)) {
            refreshCache();
        }
        return sapMaterials;
    }

    private SAPMaterial findByPartNumber(String partNumber,
                                         SapIntegrationClientImpl.SAPCompanyConfiguration companyCode) {
        SAPMaterial foundMaterial = null;

        for (SAPMaterial sapMaterial : getSapMaterials()) {
            if(StringUtils.equals(partNumber, sapMaterial.getMaterialIdentifier()) &&
               sapMaterial.getCompanyCode() == companyCode) {
                foundMaterial = sapMaterial;
                break;
            }
        }

        return foundMaterial;
    }

    public SAPMaterial findByProduct(Product product, SapIntegrationClientImpl.SAPCompanyConfiguration companyCode) {
        SAPMaterial foundMaterial = findByPartNumber(product.getPartNumber(), companyCode);

        //  A temporary short circuit until actual implementation of the service to retrieve all materials is in place
        // When it is, this condition and it's contents will be removed
        if(foundMaterial == null) {
            PriceItem priceItem = getDeterminePriceItemByCompanyCode(product, companyCode);
            final QuotePriceItem priceListItem = quotesPriceListCache.findByKeyFields(priceItem);
            foundMaterial = new SAPMaterial(product.getPartNumber(), priceListItem.getPrice(), Collections.<Condition>emptySet(), Collections.<DeliveryCondition>emptySet());
        }

        return foundMaterial;
    }

    public static PriceItem getDeterminePriceItemByCompanyCode(Product product,
                                                                SapIntegrationClientImpl.SAPCompanyConfiguration companyCode) {
        PriceItem priceItem = product.getPrimaryPriceItem();
        if(companyCode == SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES &&
                product.getExternalPriceItem() != null) {
            priceItem = product.getExternalPriceItem();
        }
        return priceItem;
    }

    public String getEffectivePrice(Product product, SapIntegrationClientImpl.SAPCompanyConfiguration companyCode,
                                    Quote orderQuote) throws InvalidProductException {

        final SAPMaterial cachedMaterial = findByProduct(product, companyCode);
        if(cachedMaterial == null) {
            throw new InvalidProductException("The material "+product.getName()+" does not exist in SAP");
        }

        final PriceItem determinePriceItemByCompanyCode = getDeterminePriceItemByCompanyCode(product, companyCode);
        String price = cachedMaterial.getBasePrice();
        QuoteItem foundMatchingQuoteItem = orderQuote.findCachedQuoteItem(determinePriceItemByCompanyCode.getPlatform(),
                determinePriceItemByCompanyCode.getCategory(), determinePriceItemByCompanyCode.getName());

        if (foundMatchingQuoteItem  != null && !orderQuote.getExpired()) {
            if (new BigDecimal(foundMatchingQuoteItem.getPrice()).compareTo(new BigDecimal(cachedMaterial.getBasePrice())) < 0) {
                price = foundMatchingQuoteItem .getPrice();
            }
        }
        return price;

//        return getEffectivePrice(cachedMaterial, orderQuote);
    }

    public List<String> getEffectivePricesForProducts(List<Product> products, ProductOrder productOrder,
                                                      Quote orderQuote)
            throws InvalidProductException, SAPIntegrationException {
        List<String> orderedPrices = new ArrayList<>();
        final SapIntegrationClientImpl.SAPCompanyConfiguration configuration =
                SapIntegrationServiceImpl.determineCompanyCode(productOrder);
        for (Product product : products) {
            orderedPrices.add(getEffectivePrice(product,configuration, orderQuote));
        }

        return orderedPrices;
    }

    public boolean productExists(Product productToFind) {
        final String partNumber = productToFind.getPartNumber();
        return productExists(partNumber);

    }

    public boolean productExists(String partNumber) {
        boolean result = false;
        for (SAPMaterial sapMaterial : sapMaterials) {
            if (StringUtils.equals(sapMaterial.getMaterialIdentifier(), partNumber)) {
                result = true;
                break;
            }
        }
        return result;
    }

    //  this additon is for the temporary support of current processing of recognizing a valid Product.
    //  When the full implementation of the fetchMatarials interface for SAP is completed, this will be removed
    public void setQuotesPriceListCache(
            PriceListCache quotesPriceListCache) {
        this.quotesPriceListCache = quotesPriceListCache;
    }

}
