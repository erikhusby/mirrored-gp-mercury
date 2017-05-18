package org.broadinstitute.gpinformatics.infrastructure.sap;

import clover.org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;
import org.broadinstitute.sap.entity.SAPMaterial;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.springframework.util.CollectionUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO scottmat fill in javadoc!!!
 */
@ApplicationScoped
public class SAPProductPriceCache extends AbstractCache implements Serializable {

    private Set<SAPMaterial> sapMaterials = new HashSet<>();

    private SapIntegrationService sapService;

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

    public SAPMaterial findByPartNumber (String partNumber) {
        SAPMaterial foundMaterial = null;

        for (SAPMaterial sapMaterial : getSapMaterials()) {
            if(StringUtils.equals(partNumber, sapMaterial.getMaterialIdentifier())) {
                foundMaterial = sapMaterial;
                break;
            }
        }

        return foundMaterial;
    }

}
