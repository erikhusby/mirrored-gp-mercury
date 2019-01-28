package org.broadinstitute.gpinformatics.infrastructure.sap;

import clover.org.apache.commons.lang.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteItem;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class SAPProductPriceCache extends AbstractCache implements Serializable {

    private Set<SAPMaterial> sapMaterials = new HashSet<>();

    private SapIntegrationService sapService;

    private SAPAccessControlEjb accessControlEjb;

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
            SAPAccessControl control = accessControlEjb.getCurrentControlDefinitions();
            if (control.isEnabled()) {
                Set<SAPMaterial> tempSet = sapService.findProductsInSap();

                if(!CollectionUtils.isEmpty(tempSet)) {
                    setMaterials(tempSet);
                }
            }
        } catch (SAPIntegrationException e) {
            logger.error("Could not refresh the SAP Product Price Cache", e);
        }
    }

    private void setMaterials(Set<SAPMaterial> tempSet) {
        sapMaterials = tempSet;
    }


    private Collection<SAPMaterial> getSapMaterials()
    {
        if(CollectionUtils.isEmpty(sapMaterials)) {
            refreshCache();
        }
        return sapMaterials;
    }

    public SAPMaterial findByPartNumber(String partNumber,
                                         SapIntegrationClientImpl.SAPCompanyConfiguration companyCode) {
        SAPMaterial foundMaterial = null;

        for (SAPMaterial sapMaterial : getSapMaterials()) {
            if(StringUtils.equalsIgnoreCase(partNumber, sapMaterial.getMaterialIdentifier()) &&
               sapMaterial.getCompanyCode() == companyCode) {
                foundMaterial = sapMaterial;
                break;
            }
        }

        return foundMaterial;
    }

    public SAPMaterial findByProduct(Product product, SapIntegrationClientImpl.SAPCompanyConfiguration companyCode) {
        SAPMaterial foundMaterial = findByPartNumber(product.getPartNumber(), companyCode);
        
        return foundMaterial;
    }

    public static PriceItem determinePriceItemByCompanyCode(Product product,
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

        final PriceItem determinePriceItemByCompanyCode = determinePriceItemByCompanyCode(product, companyCode);
        String price = cachedMaterial.getBasePrice();
        QuoteItem foundMatchingQuoteItem = orderQuote.findCachedQuoteItem(determinePriceItemByCompanyCode.getPlatform(),
                determinePriceItemByCompanyCode.getCategory(), determinePriceItemByCompanyCode.getName());

        if (foundMatchingQuoteItem  != null && !orderQuote.getExpired()) {
            if (new BigDecimal(foundMatchingQuoteItem.getPrice()).compareTo(new BigDecimal(cachedMaterial.getBasePrice())) < 0) {
                price = foundMatchingQuoteItem .getPrice();
            }
        }
        return price;
    }

    public List<String> getEffectivePricesForProducts(List<Product> products, ProductOrder productOrder,
                                                      Quote orderQuote)
            throws InvalidProductException, SAPIntegrationException {
        List<String> orderedPrices = new ArrayList<>();
        final SapIntegrationClientImpl.SAPCompanyConfiguration configuration =
                productOrder.getSapCompanyConfigurationForProductOrder();
        for (Product product : products) {
            orderedPrices.add(getEffectivePrice(product, configuration, orderQuote));
        }

        return orderedPrices;
    }

    public boolean productExists(String partNumber) {
        boolean result = false;
        for (SAPMaterial sapMaterial : getSapMaterials()) {
            if (StringUtils.equalsIgnoreCase(sapMaterial.getMaterialIdentifier(), partNumber)) {
                result = true;
                break;
            }
        }
        return result;
    }

    @Inject
    public void setAccessControlEjb(
            SAPAccessControlEjb accessControlEjb) {
        this.accessControlEjb = accessControlEjb;
    }

}
