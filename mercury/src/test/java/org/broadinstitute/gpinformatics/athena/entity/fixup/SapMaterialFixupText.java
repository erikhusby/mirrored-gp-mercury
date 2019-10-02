/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.fixup;

import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.sap.entity.material.SAPChangeMaterial;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.entity.quote.QuoteItem;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SAPServiceFailure;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.FIXUP)
public class SapMaterialFixupText extends Arquillian {
    @Inject
    ProductDao productDao;

    @Inject
    private UserBean userBean;

    @Inject
    private ProductEjb productEjb;

    @Inject
    private UserTransaction utx;

    @Inject
    private SapIntegrationService sapIntegrationService;

    @Inject
    private SAPAccessControlEjb accessController;

    private final static Log log = LogFactory.getLog(SapMaterialFixupText.class);
    /*
     * When applying this to Production, change the input to PROD, "prod"
     */
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    private static Predicate<Product> validPartNumber() {
        return p -> p.getPartNumber().length() <= 18 ||
                    !p.getPartNumber().trim().equals(p.getPartNumber());
    }

    @Test(enabled = false)
    public void testUpdateActiveProductsInSap() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        Map<Boolean, List<Product>> partNumbersByPartNumberValidity =
            productDao.findProducts(ProductDao.Availability.CURRENT_OR_FUTURE, ProductDao.TopLevelOnly.NO,
                ProductDao.IncludePDMOnly.YES).stream().collect(Collectors.partitioningBy(validPartNumber()));

        final List<String> partNumbersToUpdate =
                partNumbersByPartNumberValidity.get(Boolean.TRUE).stream().map(Product::getPartNumber)
                        .collect(Collectors.toList());

        // publish products. will create or update them in SAP
        log.debug("Applying first pass of create and update");
        productEjb.publishProductsToSAP(partNumbersByPartNumberValidity.get(true), true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);

        System.out.println("The part numbers attempted to Update. " + partNumbersToUpdate.size() + " -- \n" +
                           StringUtils.join(partNumbersToUpdate, "\n"));

        System.out.println("excludedPartNumbers = " + partNumbersByPartNumberValidity.get(false).stream()
            .map(Product::getPartNumber).collect(Collectors.toList()));

        productDao.persist(new FixupCommentary("GPLIM-6183 Syncing products witn SAP"));
        utx.commit();
    }

    @Test(enabled = false)
    public void testDisablingBadProducts() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        Map<Boolean, List<Product>> partNumbersByPartNumberValidity =
                    productDao.findProducts(ProductDao.Availability.CURRENT_OR_FUTURE, ProductDao.TopLevelOnly.NO,
                        ProductDao.IncludePDMOnly.YES).stream().collect(Collectors.partitioningBy(validPartNumber()));

        final List<String> partNumbersToUpdate =
                partNumbersByPartNumberValidity.get(Boolean.TRUE).stream().map(Product::getPartNumber)
                        .collect(Collectors.toList());

        final EnumSet<SapIntegrationClientImpl.SAPCompanyConfiguration> platformsToExtend =
                EnumSet.of(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD,
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES);

        List<SAPMaterial> productsToDisable = new ArrayList<>();

        ArrayListMultimap<String, SAPMaterial> collectedProductsBySalesOrg = null;
        try {
            final Set<SAPMaterial> productsInSap = sapIntegrationService.findProductsInSap();
            for (SAPMaterial sapMaterial : productsInSap) {
                if(!sapMaterial.getMaterialName().contains(QuoteItem.DOLLAR_LIMIT_MATERIAL_DESCRIPTOR)) {
                    productsToDisable.add(new SAPMaterial(sapMaterial.getMaterialIdentifier(),
                            SapIntegrationClientImpl.SAPCompanyConfiguration
                                    .fromSalesOrgForMaterial(sapMaterial.getSalesOrg()), sapMaterial.getWbs(),
                            sapMaterial.getMaterialName(), sapMaterial.getBasePrice(),
                            sapMaterial.getBaseUnitOfMeasure(), sapMaterial.getMinimumOrderQuantity(), new Date(),
                            new Date(),sapMaterial.getPossibleOrderConditions(),
                            sapMaterial.getPossibleDeliveryConditions(), sapMaterial.getSalesOrgStatus(),
                            null));
                }
            }
            collectedProductsBySalesOrg = productsToDisable.stream().collect(Collector.of(ArrayListMultimap::create, (arrayListMultimap, material) -> {
                arrayListMultimap.put(material.getSalesOrg(), material);
            }, (arrayListMultimap, arrayListMultimap2) -> {
                arrayListMultimap.putAll(arrayListMultimap2);
                return arrayListMultimap;
            }));

        } catch (SAPIntegrationException e) {
            Assert.fail("Failure retrieving products for SAP");
        }

        for (SapIntegrationClientImpl.SAPCompanyConfiguration sapCompanyConfiguration : platformsToExtend) {
            final List<SAPMaterial> sapMaterials =
                    collectedProductsBySalesOrg.get(sapCompanyConfiguration.getSalesOrganization());
            log.debug(String.format("Showing statuses for %d sapMaterials",sapMaterials.size()));
            sapMaterials.stream()
                    .filter(material -> !material.getMaterialName().contains(QuoteItem.DOLLAR_LIMIT_MATERIAL_DESCRIPTOR) &&
                                        !partNumbersToUpdate.contains(material.getMaterialIdentifier()))
                    .forEach(material -> {
                        try {
                            material.setSalesOrgStatus(SAPMaterial.MaterialStatus.DISABLED);
                            log.debug(String.format("Disabling material %s in sales org %s.  Disable status is %s",
                                    material.getMaterialIdentifier(), material.getSalesOrg(), material.getSalesOrgStatus()));
                            log.debug("Material to disable\n"+material);
                            ((SapIntegrationServiceImpl) sapIntegrationService).getClient().changeMaterialDetails(SAPChangeMaterial.fromSAPMaterial(material));
                        } catch (SAPIntegrationException|SAPServiceFailure e) {
                            log.debug(String.format("Failed to UPDATe SAP for material %s in sales org %s:  %s",
                                    material.getMaterialIdentifier(), material.getSalesOrg(), e.getMessage()));
                        }
                    });
        }

        productDao.persist(new FixupCommentary("GPLIM-6183 Syncing products witn SAP"));
        utx.commit();
    }
}
