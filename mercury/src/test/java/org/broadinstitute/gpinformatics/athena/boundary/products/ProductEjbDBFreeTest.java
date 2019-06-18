package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessItem;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.sap.entity.material.SAPChangeMaterial;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductEjbDBFreeTest {

    public void testPublishToSAP() throws Exception {
        final SAPAccessControlEjb mockSapAccessControl = Mockito.mock(SAPAccessControlEjb.class);
        final AttributeArchetypeDao mockAttributeArchetypeDao = Mockito.mock(AttributeArchetypeDao.class);
        final AuditReaderDao mockAuditReaderDao = Mockito.mock(AuditReaderDao.class);
        final SapIntegrationService mockSapService = Mockito.mock(SapIntegrationService.class);
        final ProductDao mockProductDao = Mockito.mock(ProductDao.class);
        SAPProductPriceCache productPriceCache = Mockito.mock(SAPProductPriceCache.class);
        ProductEjb testEjb = new ProductEjb(mockProductDao, mockSapService, mockAuditReaderDao,
                mockAttributeArchetypeDao, mockSapAccessControl, productPriceCache);

        SAPAccessControl noControl = new SAPAccessControl();
        SAPAccessControl blockControl = new SAPAccessControl();
        blockControl.setDisabledItems(Collections.singleton(new AccessItem("blockThisItem")));

        Product testProduct = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "SGM-TEST-SAP");
        testProduct.setPrimaryPriceItem(new PriceItem("qsID", "testPlatform", "testCategory", "blockThisItem"));


        Mockito.when(mockSapAccessControl.getCurrentControlDefinitions()).thenThrow(new RuntimeException());
        testEjb.publishProductToSAP(testProduct);

        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(1)).publishProductInSAP(testProduct,
                true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(2)).publishProductInSAP(testProduct,
                true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(3)).publishProductInSAP(testProduct,
                true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(4)).publishProductInSAP(testProduct,
                true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);
    }

    public void testPublishClinicalToSAP() throws Exception {
        final SAPAccessControlEjb mockSapAccessControl = Mockito.mock(SAPAccessControlEjb.class);
        final AttributeArchetypeDao mockAttributeArchetypeDao = Mockito.mock(AttributeArchetypeDao.class);
        final AuditReaderDao mockAuditReaderDao = Mockito.mock(AuditReaderDao.class);
        final SapIntegrationService mockSapService = Mockito.mock(SapIntegrationService.class);
        final ProductDao mockProductDao = Mockito.mock(ProductDao.class);
        SAPProductPriceCache productPriceCache = Mockito.mock(SAPProductPriceCache.class);
        ProductEjb testEjb = new ProductEjb(mockProductDao, mockSapService, mockAuditReaderDao,
                mockAttributeArchetypeDao, mockSapAccessControl, productPriceCache);

        SAPAccessControl noControl = new SAPAccessControl();
        SAPAccessControl blockControl = new SAPAccessControl();
        blockControl.setDisabledItems(Collections.singleton(new AccessItem("blockThisItem")));

        Product testProduct = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "SGM-TEST-SAP");
        testProduct.setClinicalProduct(true);
        testProduct.setPrimaryPriceItem(new PriceItem("qsID", "testPlatform", "testCategory", "blockThisItem"));


        Mockito.when(mockSapAccessControl.getCurrentControlDefinitions()).thenThrow(new RuntimeException());
        testEjb.publishProductToSAP(testProduct);

        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(1)).publishProductInSAP(testProduct,
                true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);


        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(2)).publishProductInSAP(testProduct,
                true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(3)).publishProductInSAP(testProduct,
                true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(4)).publishProductInSAP(testProduct,
                true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);


        Product testProduct2 = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "P-CLIATEST-SAP");
        testProduct2.setPrimaryPriceItem(new PriceItem("qsID", "testPlatform", "testCategory", "blockThisItem"));
        testProduct2.setClinicalProduct(true);
    }

    public void testPublishExternalOnlyToSAP() throws Exception {
        final SAPAccessControlEjb mockSapAccessControl = Mockito.mock(SAPAccessControlEjb.class);
        final AttributeArchetypeDao mockAttributeArchetypeDao = Mockito.mock(AttributeArchetypeDao.class);
        final AuditReaderDao mockAuditReaderDao = Mockito.mock(AuditReaderDao.class);
        final SapIntegrationService mockSapService = Mockito.mock(SapIntegrationService.class);
        final ProductDao mockProductDao = Mockito.mock(ProductDao.class);
        SAPProductPriceCache productPriceCache = Mockito.mock(SAPProductPriceCache.class);
        ProductEjb testEjb = new ProductEjb(mockProductDao, mockSapService, mockAuditReaderDao,
                mockAttributeArchetypeDao, mockSapAccessControl, productPriceCache);

        SAPAccessControl noControl = new SAPAccessControl();
        SAPAccessControl blockControl = new SAPAccessControl();
        blockControl.setDisabledItems(Collections.singleton(new AccessItem("blockThisItem")));

        Product testProduct = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "SGM-TEST-SAP");
        testProduct.setClinicalProduct(true);
        testProduct.setPrimaryPriceItem(new PriceItem("qsID", "testPlatform", "testCategory", "blockThisItem"));


        Mockito.when(mockSapAccessControl.getCurrentControlDefinitions()).thenThrow(new RuntimeException());
        testEjb.publishProductToSAP(testProduct);

        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(1)).publishProductInSAP(testProduct,
                true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);


        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(2)).publishProductInSAP(testProduct,
                true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(3)).publishProductInSAP(testProduct,
                true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(4)).publishProductInSAP(testProduct,
                true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);


        Product testProduct2 = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "P-CLIATEST-SAP");
        testProduct2.setPrimaryPriceItem(new PriceItem("qsID", "testPlatform", "testCategory", "blockThisItem"));
        testProduct2.setExternalOnlyProduct(true);
    }


    public void testPublishSSFToSAPCheckClientCalls() throws Exception {

        final SAPAccessControlEjb mockSapAccessControl = Mockito.mock(SAPAccessControlEjb.class);
        final AttributeArchetypeDao mockAttributeArchetypeDao = Mockito.mock(AttributeArchetypeDao.class);
        final AuditReaderDao mockAuditReaderDao = Mockito.mock(AuditReaderDao.class);
        final SapIntegrationServiceImpl sapService = new SapIntegrationServiceImpl();
        final SapIntegrationClientImpl mockWrappedClient = Mockito.mock(SapIntegrationClientImpl.class);
        sapService.setWrappedClient(mockWrappedClient);
        final ProductDao mockProductDao = Mockito.mock(ProductDao.class);
        Set<SAPMaterial> materialSet = new HashSet<>();
        SAPProductPriceCache productPriceCache = new SAPProductPriceCache(sapService);
        sapService.setProductPriceCache(productPriceCache);
        ProductEjb testEjb = new ProductEjb(mockProductDao, sapService, mockAuditReaderDao,
                mockAttributeArchetypeDao, mockSapAccessControl, productPriceCache);
        sapService.setProductPriceCache(productPriceCache);

        Product testProduct = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "SGM-TEST-SAP");
        testProduct.setPrimaryPriceItem(new PriceItem("qsID", "testPlatform", "testCategory", "blockThisItem"));

        Mockito.when(mockSapAccessControl.getCurrentControlDefinitions()).thenThrow(new RuntimeException());
        testEjb.publishProductToSAP(testProduct);
        Mockito.when(mockWrappedClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(materialSet);

        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(3)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(0)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        SapIntegrationClientImpl.SAPCompanyConfiguration broad = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;

        // Mimic creating the product in Sales org GP01 for GP Platform
        materialSet.add(TestUtils.mockMaterialSearch(broad, testProduct));

        testProduct.setOfferedAsCommercialProduct(true);
        productPriceCache.refreshCache();
        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        /*
            Since we are mimicking that the product has been published to only one Platform, it will call ChangeMaterial
            for that one, and create material for the other 12
         */
        Mockito.verify(mockWrappedClient, Mockito.times(6)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(1)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        //Loop through all company codes and mimic extending the product to those platforms
        final List<SapIntegrationClientImpl.SAPCompanyConfiguration> extendedPlatforms =
                new ArrayList<>(SapIntegrationServiceImpl.EXTENDED_PLATFORMS);
        extendedPlatforms.add(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        extendedPlatforms.forEach(configuration1 -> {
            materialSet.add(TestUtils.mockMaterialSearch(configuration1, testProduct));
        });
        productPriceCache.refreshCache();
        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(6)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(5)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(6)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(9)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));
    }

    public void testPublishSSFToSAPCheckClientCollectionCalls() throws Exception {

        final SAPAccessControlEjb mockSapAccessControl = Mockito.mock(SAPAccessControlEjb.class);
        final AttributeArchetypeDao mockAttributeArchetypeDao = Mockito.mock(AttributeArchetypeDao.class);
        final AuditReaderDao mockAuditReaderDao = Mockito.mock(AuditReaderDao.class);
        final SapIntegrationServiceImpl sapService = new SapIntegrationServiceImpl();
        final SapIntegrationClientImpl mockWrappedClient = Mockito.mock(SapIntegrationClientImpl.class);
        sapService.setWrappedClient(mockWrappedClient);
        final ProductDao mockProductDao = Mockito.mock(ProductDao.class);
        Set<SAPMaterial> materialSet = new HashSet<>();
        SAPProductPriceCache productPriceCache = new SAPProductPriceCache(sapService);
        sapService.setProductPriceCache(productPriceCache);
        ProductEjb testEjb = new ProductEjb(mockProductDao, sapService, mockAuditReaderDao,
                mockAttributeArchetypeDao, mockSapAccessControl, productPriceCache);
        sapService.setProductPriceCache(productPriceCache);

        Product testProduct = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "SGM-TEST-SAP");
        testProduct.setPrimaryPriceItem(new PriceItem("qsID", "testPlatform", "testCategory", "blockThisItem"));

        Mockito.when(mockSapAccessControl.getCurrentControlDefinitions()).thenThrow(new RuntimeException());
        Mockito.when(mockWrappedClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(materialSet);

        testEjb.publishProductsToSAP(Collections.singleton(testProduct), true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(3)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(0)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        testEjb.publishProductsToSAP(Collections.singleton(testProduct), true, SapIntegrationService.PublishType.CREATE_ONLY);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(6)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(0)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        testEjb.publishProductsToSAP(Collections.singleton(testProduct), false, SapIntegrationService.PublishType.CREATE_ONLY);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(7)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(0)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        testEjb.publishProductsToSAP(Collections.singleton(testProduct), true, SapIntegrationService.PublishType.UPDATE_ONLY);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(7)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(0)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        SapIntegrationClientImpl.SAPCompanyConfiguration broad = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;

        // Mimic creating the product in Sales org GP01 for GP Platform
        materialSet.add(TestUtils.mockMaterialSearch(broad, testProduct));
        testProduct.setOfferedAsCommercialProduct(true);

        productPriceCache.refreshCache();
        /*
            Since we are mimicking that the product has been published to only one Platform, it will call ChangeMaterial
            for that one, and create material for the other 3
         */
        testEjb.publishProductsToSAP(Collections.singleton(testProduct), true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(10)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(1)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        testEjb.publishProductsToSAP(Collections.singleton(testProduct), true, SapIntegrationService.PublishType.CREATE_ONLY);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(13)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(1)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        testEjb.publishProductsToSAP(Collections.singleton(testProduct), false, SapIntegrationService.PublishType.CREATE_ONLY);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(14)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(1)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        testEjb.publishProductsToSAP(Collections.singleton(testProduct), true, SapIntegrationService.PublishType.UPDATE_ONLY);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(14)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(2)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        //Loop through all company codes and mimic extending the product to those platforms
        final List<SapIntegrationClientImpl.SAPCompanyConfiguration> extendedPlatforms =
                new ArrayList<>(SapIntegrationServiceImpl.EXTENDED_PLATFORMS);
        extendedPlatforms.add(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        extendedPlatforms.forEach(configuration1 -> {
            materialSet.add(TestUtils.mockMaterialSearch(configuration1, testProduct));
        });
        productPriceCache.refreshCache();
        testEjb.publishProductsToSAP(Collections.singleton(testProduct), true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(14)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(6)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        testEjb.publishProductsToSAP(Collections.singleton(testProduct), true, SapIntegrationService.PublishType.UPDATE_ONLY);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(14)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(10)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));
        testEjb.publishProductsToSAP(Collections.singleton(testProduct), true, SapIntegrationService.PublishType.CREATE_ONLY);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(14)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(10)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));
        testEjb.publishProductsToSAP(Collections.singleton(testProduct), false, SapIntegrationService.PublishType.UPDATE_ONLY);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(14)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(12)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));
        testEjb.publishProductsToSAP(Collections.singleton(testProduct), false, SapIntegrationService.PublishType.CREATE_AND_UPDATE);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(14)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(14)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));
    }

    public void testPublishClinicalToSAPCheckClientCalls() throws Exception {
        SapIntegrationClientImpl.SAPCompanyConfiguration configuration =
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES;
        final SAPAccessControlEjb mockSapAccessControl = Mockito.mock(SAPAccessControlEjb.class);
        final AttributeArchetypeDao mockAttributeArchetypeDao = Mockito.mock(AttributeArchetypeDao.class);
        final AuditReaderDao mockAuditReaderDao = Mockito.mock(AuditReaderDao.class);
        final SapIntegrationServiceImpl sapService = new SapIntegrationServiceImpl();
        final SapIntegrationClientImpl mockWrappedClient = Mockito.mock(SapIntegrationClientImpl.class);
        sapService.setWrappedClient(mockWrappedClient);
        final ProductDao mockProductDao = Mockito.mock(ProductDao.class);
        Set<SAPMaterial> materialSet = new HashSet<>();
        SAPProductPriceCache productPriceCache = new SAPProductPriceCache(sapService);
        sapService.setProductPriceCache(productPriceCache);
        Mockito.when(mockWrappedClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(materialSet);
        ProductEjb testEjb = new ProductEjb(mockProductDao, sapService, mockAuditReaderDao,
                mockAttributeArchetypeDao, mockSapAccessControl, productPriceCache);
        sapService.setProductPriceCache(productPriceCache);
        SAPAccessControl noControl = new SAPAccessControl();
        SAPAccessControl blockControl = new SAPAccessControl();
        blockControl.setDisabledItems(Collections.singleton(new AccessItem("blockThisItem")));

        Product testProduct = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "SGM-TEST-SAP");
        testProduct.setClinicalProduct(Boolean.TRUE);
        testProduct.setPrimaryPriceItem(new PriceItem("qsID", "testPlatform", "testCategory", "blockThisItem"));
        if(configuration == SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES) {
            testProduct.setExternalOnlyProduct(true);
        }

        Mockito.when(mockSapAccessControl.getCurrentControlDefinitions()).thenThrow(new RuntimeException());
        testEjb.publishProductToSAP(testProduct);
        Mockito.when(mockWrappedClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(materialSet);


        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(2)).createMaterial(Mockito.any(SAPMaterial.class));

        //Loop through all company codes and mimic extending the product to those platforms
        final List<SapIntegrationClientImpl.SAPCompanyConfiguration> extendedPlatforms =
                new ArrayList<>(SapIntegrationServiceImpl.EXTENDED_PLATFORMS);
        extendedPlatforms.add(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        extendedPlatforms.forEach(configuration1 -> {
            materialSet.add(TestUtils.mockMaterialSearch(configuration1, testProduct));
        });
        productPriceCache.refreshCache();

        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(2)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(0)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(2)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(2)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(2)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(4)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(2)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(6)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

    }

    public void testPublishExternalToSAPCheckClientCalls() throws Exception {
        final SAPAccessControlEjb mockSapAccessControl = Mockito.mock(SAPAccessControlEjb.class);
        final AttributeArchetypeDao mockAttributeArchetypeDao = Mockito.mock(AttributeArchetypeDao.class);
        final AuditReaderDao mockAuditReaderDao = Mockito.mock(AuditReaderDao.class);
        final SapIntegrationServiceImpl sapService = new SapIntegrationServiceImpl();
        final SapIntegrationClientImpl mockWrappedClient = Mockito.mock(SapIntegrationClientImpl.class);
        sapService.setWrappedClient(mockWrappedClient);
        final ProductDao mockProductDao = Mockito.mock(ProductDao.class);
        Set<SAPMaterial> materialSet = new HashSet<>();
        SAPProductPriceCache productPriceCache = new SAPProductPriceCache(sapService);
        sapService.setProductPriceCache(productPriceCache);
        Mockito.when(mockWrappedClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(materialSet);
        ProductEjb testEjb = new ProductEjb(mockProductDao, sapService, mockAuditReaderDao,
                mockAttributeArchetypeDao, mockSapAccessControl, productPriceCache);
        sapService.setProductPriceCache(productPriceCache);

        SAPAccessControl noControl = new SAPAccessControl();
        SAPAccessControl blockControl = new SAPAccessControl();
        blockControl.setDisabledItems(Collections.singleton(new AccessItem("blockThisItem")));

        Product testProduct = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "SGM-TEST-SAP");
        testProduct.setClinicalProduct(Boolean.TRUE);
        testProduct.setPrimaryPriceItem(new PriceItem("qsID", "testPlatform", "testCategory", "blockThisItem"));
        Mockito.when(mockWrappedClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(materialSet);


        Mockito.when(mockSapAccessControl.getCurrentControlDefinitions()).thenThrow(new RuntimeException());
        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(2)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(0)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));


        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(4)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(0)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        //Loop through all company codes and mimic extending the product to those platforms
        final List<SapIntegrationClientImpl.SAPCompanyConfiguration> extendedPlatforms =
                new ArrayList<>(SapIntegrationServiceImpl.EXTENDED_PLATFORMS);
        extendedPlatforms.add(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        extendedPlatforms.forEach(configuration1 -> {
            materialSet.add(TestUtils.mockMaterialSearch(configuration1, testProduct));
        });
        productPriceCache.refreshCache();
        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(4)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(2)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(4)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(4)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));
    }
}

