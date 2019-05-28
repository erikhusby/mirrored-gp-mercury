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

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessItem;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessStatus;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
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
    private SAPAccessControlEjb accessController;

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

        // Enable access for all products in Mercury, but keep track of the blacklisted ones
        SAPAccessControl control = accessController.getCurrentControlDefinitions();
        control.setAccessStatus(AccessStatus.ENABLED);
        Set<AccessItem> disabledItems = control.getDisabledItems();
        control.setDisabledItems(Collections.emptySet());
        productDao.persist(control);

        // publish products. will create or update them in SAP
        productEjb.publishProductsToSAP(partNumbersByPartNumberValidity.get(true), true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);



        Map<Boolean, List<Product>> allProductsByPartNumberValidity =
                productDao.findProducts(ProductDao.Availability.ALL, ProductDao.TopLevelOnly.NO,
                        ProductDao.IncludePDMOnly.YES).stream().collect(Collectors.partitioningBy(validPartNumber()));

        List<Product> onlyDisabledProducts = allProductsByPartNumberValidity.get(true)
                .stream()
                .filter(product -> !partNumbersByPartNumberValidity.get(true).contains(product) &&
                                   (product.isClinicalProduct() || product.isExternalProduct()))
                .collect(Collectors.toList());

        productEjb.publishProductsToSAP(onlyDisabledProducts, false, SapIntegrationService.PublishType.UPDATE_ONLY);


        // restore blacklist;
        control.setDisabledItems(disabledItems);
        control.setAccessStatus(AccessStatus.DISABLED);

        System.out.println("The part numbers attempted to Update. " + partNumbersToUpdate.size() + " -- \n" +
                           StringUtils.join(partNumbersToUpdate, "\n"));

        System.out.println("excludedPartNumbers = " + partNumbersByPartNumberValidity.get(false).stream()
            .map(Product::getPartNumber).collect(Collectors.toList()));

        productDao.persist(new FixupCommentary("GPLIM-6183 Syncing products witn SAP"));
        utx.commit();
    }

}
