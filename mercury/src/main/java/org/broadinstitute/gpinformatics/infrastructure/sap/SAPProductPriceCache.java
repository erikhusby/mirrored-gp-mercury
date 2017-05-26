package org.broadinstitute.gpinformatics.infrastructure.sap;

import clover.org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.sap.entity.Condition;
import org.broadinstitute.sap.entity.DeliveryCondition;
import org.broadinstitute.sap.entity.SAPMaterial;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.springframework.util.CollectionUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO scottmat fill in javadoc!!!
 */
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
        this.sapMaterials = sapMaterials;
    }


    @Override
    public synchronized void refreshCache() {
        try {
            Set<SAPMaterial> tempSet = sapService.findProductsInSap();

            if(!CollectionUtils.isEmpty(tempSet)) {
                sapMaterials = tempSet;
            }
        } catch (SAPIntegrationException e) {
            logger.error("Could not refresh the SAP Product Price Cache", e);
        }

    }

    public Collection<SAPMaterial> getSapMaterials()
    {
        if(CollectionUtils.isEmpty(sapMaterials)) {
            refreshCache();
        }
        return sapMaterials;
    }

    private SAPMaterial findByPartNumber (String partNumber) {
        SAPMaterial foundMaterial = null;

        for (SAPMaterial sapMaterial : getSapMaterials()) {
            if(StringUtils.equals(partNumber, sapMaterial.getMaterialIdentifier())) {
                foundMaterial = sapMaterial;
                break;
            }
        }

        return foundMaterial;
    }

    public SAPMaterial findByProduct (Product product) {
        SAPMaterial foundMaterial = findByPartNumber(product.getPartNumber());

        //  A temporary short circuit until actual implementation of the service to retrieve all materials is in place
        // When it is, this condition and it's contents will be removed
        if(foundMaterial == null) {
            final QuotePriceItem priceListItem = quotesPriceListCache.findByKeyFields(product.getPrimaryPriceItem());
            foundMaterial = new SAPMaterial(product.getPartNumber(), priceListItem.getPrice(), Collections.<Condition>emptySet(), Collections.<DeliveryCondition>emptySet());
        }

        return foundMaterial;
    }

    //  this additon is for the temporary support of current processing of recognizing a valid Product.
    //  When the full implementation of the fetchMatarials interface for SAP is completed, this will be removed
    public void setQuotesPriceListCache(
            PriceListCache quotesPriceListCache) {
        this.quotesPriceListCache = quotesPriceListCache;
    }
}
