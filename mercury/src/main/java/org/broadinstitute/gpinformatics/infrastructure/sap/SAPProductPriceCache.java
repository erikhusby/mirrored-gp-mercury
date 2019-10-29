package org.broadinstitute.gpinformatics.infrastructure.sap;

import clover.org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
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
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.MoreCollectors.toOptional;

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
            Set<SAPMaterial> tempSet = sapService.findProductsInSap();

            if(!CollectionUtils.isEmpty(tempSet)) {
                setMaterials(tempSet);
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
                                        String salesOrg) {
        SAPMaterial foundMaterial = null;

        final Optional<SAPMaterial> optionalMaterial = getSapMaterials().stream()
                .filter(material -> StringUtils.equalsIgnoreCase(partNumber, material.getMaterialIdentifier()) &&
                                    StringUtils.equals(material.getSalesOrg(),salesOrg)).collect(toOptional());

        if(optionalMaterial.isPresent()) {
            foundMaterial = optionalMaterial.get();
        }

        return foundMaterial;
    }

    public SAPMaterial findByProduct(Product product, String salesOrganization) {

        return findByPartNumber(product.getPartNumber(), salesOrganization);
    }


    public void determineIfProductsExist(Collection<Product> products, String salesOrganization)
            throws InvalidProductException {
        List<String> missingProducts = new ArrayList<>();

        for( Product product:products) {
            SAPMaterial foundMaterial = findByProduct(product, salesOrganization);
            if(foundMaterial == null) {
                missingProducts.add(product.getPartNumber());
            }
        }

        if(!missingProducts.isEmpty()) {
            throw new InvalidProductException("The following products have not been setup in SAP: " +
                                              StringUtils.join(missingProducts));
        }
    }

    public String getEffectivePrice(Product product, SapIntegrationClientImpl.SAPCompanyConfiguration companyCode,
                                    Quote orderQuote) throws InvalidProductException {

        final SAPMaterial cachedMaterial = findByProduct(product, companyCode.getSalesOrganization());
        if(cachedMaterial == null) {
            throw new InvalidProductException("The material "+product.getName()+" does not exist in SAP");
        }

        final PriceItem determinePriceItemByCompanyCode = product.getPrimaryPriceItem();
        String price = cachedMaterial.getBasePrice();
        if(determinePriceItemByCompanyCode != null) {
            QuoteItem foundMatchingQuoteItem =
                    orderQuote.findCachedQuoteItem(determinePriceItemByCompanyCode.getPlatform(),
                            determinePriceItemByCompanyCode.getCategory(), determinePriceItemByCompanyCode.getName());

            if (foundMatchingQuoteItem != null && !orderQuote.getExpired()) {
                if (new BigDecimal(foundMatchingQuoteItem.getPrice())
                            .compareTo(new BigDecimal(cachedMaterial.getBasePrice())) < 0) {
                    price = foundMatchingQuoteItem.getPrice();
                }
            }
        }
        return price;
    }

    /**
     * This is only used in a fixup test.  There doesnt' appear to be a case for this anymore.
     *
     */
    @Deprecated
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
