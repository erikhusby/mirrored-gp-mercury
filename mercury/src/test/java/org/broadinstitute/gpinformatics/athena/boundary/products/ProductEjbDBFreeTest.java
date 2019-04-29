package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessItem;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

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
        Mockito.verify(mockSapService, Mockito.times(1)).publishProductInSAP(testProduct);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(2)).publishProductInSAP(testProduct);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(3)).publishProductInSAP(testProduct);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(4)).publishProductInSAP(testProduct);
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
        Mockito.verify(mockSapService, Mockito.times(1)).publishProductInSAP(testProduct);


        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(2)).publishProductInSAP(testProduct);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(3)).publishProductInSAP(testProduct);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(4)).publishProductInSAP(testProduct);


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
        Mockito.verify(mockSapService, Mockito.times(1)).publishProductInSAP(testProduct);


        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(2)).publishProductInSAP(testProduct);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(3)).publishProductInSAP(testProduct);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(4)).publishProductInSAP(testProduct);


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
        SAPProductPriceCache productPriceCache = Mockito.mock(SAPProductPriceCache.class);
        ProductEjb testEjb = new ProductEjb(mockProductDao, sapService, mockAuditReaderDao,
                mockAttributeArchetypeDao, mockSapAccessControl, productPriceCache);
        sapService.setProductPriceCache(productPriceCache);

        SAPAccessControl noControl = new SAPAccessControl();
        SAPAccessControl blockControl = new SAPAccessControl();
        blockControl.setDisabledItems(Collections.singleton(new AccessItem("blockThisItem")));

        Product testProduct = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "SGM-TEST-SAP");
        testProduct.setPrimaryPriceItem(new PriceItem("qsID", "testPlatform", "testCategory", "blockThisItem"));


        Mockito.when(mockSapAccessControl.getCurrentControlDefinitions()).thenThrow(new RuntimeException());
        testEjb.publishProductToSAP(testProduct);

        assertThat(testProduct.isSavedInSAP(), is(true));
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(2)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(0)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));


        SapIntegrationClientImpl.SAPCompanyConfiguration broad = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;

        final SAPChangeMaterial primaryProductMaterial =
            new SAPChangeMaterial("test", broad, broad.getDefaultWbs(), "test description", "50",
                SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "",
                new Date(), new Date(), Collections.emptyMap(), Collections.emptyMap(),
                SAPMaterial.MaterialStatus.ENABLED, "");

        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD))).thenReturn(
                primaryProductMaterial);


        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        /*
            Since we are mimicking that the product has been published to only one Platform, it will call ChangeMaterial
            for that one, and create material for the other 12
         */
        Mockito.verify(mockWrappedClient, Mockito.times(3)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(1)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        Mockito.when(productPriceCache.productExists(Mockito.anyString())).thenReturn(Boolean.TRUE);
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES)))
                .thenReturn(new SAPMaterial("", SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES, "", "", "", "", BigDecimal.ONE, "", "", "", new Date(), new Date(), Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, ""));


        SAPMaterial otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.DSP, "", "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.PRISM, "", "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.BITSTORE, "", "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.PROTEOMICS, "", "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.GPP, "", "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.CDOT, "", "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.METABOLOMICS, "", "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.IMAGING, "", "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.CMAP, "", "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.MICROBIAL, "", "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.GENOME_METHYLATION, "", "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);




        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(3)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(3)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(3)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(5)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));
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
        SAPProductPriceCache productPriceCache = Mockito.mock(SAPProductPriceCache.class);
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

        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(2)).createMaterial(Mockito.any(SAPMaterial.class));


        SapIntegrationClientImpl.SAPCompanyConfiguration broad = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;

        final SAPChangeMaterial primaryProductMaterial =
            new SAPChangeMaterial("test", broad, broad.getDefaultWbs(), "test description", "50",
                                    SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                                    Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");

        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD))).thenReturn(
                primaryProductMaterial);

        final SAPMaterial primaryProductMaterialExternal =
            new SAPChangeMaterial("test", broad, broad.getDefaultWbs(), "test description", "50",
                                    SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                                    Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");

        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                primaryProductMaterialExternal);

        SAPMaterial otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.DSP, SapIntegrationClientImpl.SAPCompanyConfiguration.DSP.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.PRISM, SapIntegrationClientImpl.SAPCompanyConfiguration.PRISM.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.BITSTORE, SapIntegrationClientImpl.SAPCompanyConfiguration.BITSTORE.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.PROTEOMICS, SapIntegrationClientImpl.SAPCompanyConfiguration.PROTEOMICS.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.GPP, SapIntegrationClientImpl.SAPCompanyConfiguration.GPP.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.CDOT, SapIntegrationClientImpl.SAPCompanyConfiguration.CDOT.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.METABOLOMICS, SapIntegrationClientImpl.SAPCompanyConfiguration.METABOLOMICS.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.IMAGING, SapIntegrationClientImpl.SAPCompanyConfiguration.IMAGING.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.CMAP, SapIntegrationClientImpl.SAPCompanyConfiguration.CMAP.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.MICROBIAL, SapIntegrationClientImpl.SAPCompanyConfiguration.MICROBIAL.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.GENOME_METHYLATION, SapIntegrationClientImpl.SAPCompanyConfiguration.GENOME_METHYLATION.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);



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
        SAPProductPriceCache productPriceCache = Mockito.mock(SAPProductPriceCache.class);
        ProductEjb testEjb = new ProductEjb(mockProductDao, sapService, mockAuditReaderDao,
                mockAttributeArchetypeDao, mockSapAccessControl, productPriceCache);
        sapService.setProductPriceCache(productPriceCache);

        SAPAccessControl noControl = new SAPAccessControl();
        SAPAccessControl blockControl = new SAPAccessControl();
        blockControl.setDisabledItems(Collections.singleton(new AccessItem("blockThisItem")));

        Product testProduct = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "SGM-TEST-SAP");
        testProduct.setClinicalProduct(Boolean.TRUE);
        testProduct.setPrimaryPriceItem(new PriceItem("qsID", "testPlatform", "testCategory", "blockThisItem"));

        Mockito.when(mockSapAccessControl.getCurrentControlDefinitions()).thenThrow(new RuntimeException());
        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(2)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(0)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));


        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockWrappedClient, Mockito.times(4)).createMaterial(Mockito.any(SAPMaterial.class));
        Mockito.verify(mockWrappedClient, Mockito.times(0)).changeMaterialDetails(Mockito.any(SAPChangeMaterial.class));

        SapIntegrationClientImpl.SAPCompanyConfiguration broadInternal =
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
        SapIntegrationClientImpl.SAPCompanyConfiguration broadExternal=
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES;

        final SAPChangeMaterial primaryProductMaterial =
                new SAPChangeMaterial("test", broadInternal, broadInternal.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD))).thenReturn(
                primaryProductMaterial);

        final SAPMaterial primaryProductMaterialExternal =
                new SAPChangeMaterial("test", broadExternal, broadExternal.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                primaryProductMaterialExternal);

        SAPMaterial otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.DSP, broadExternal.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.PRISM, broadExternal.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.BITSTORE, broadExternal.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.PROTEOMICS, broadExternal.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.GPP, broadExternal.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.CDOT, broadExternal.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.METABOLOMICS, broadExternal.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.IMAGING, broadExternal.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.CMAP, broadExternal.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.MICROBIAL, broadExternal.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);

        otherPlatformMaterial =
                new SAPChangeMaterial("test", SapIntegrationClientImpl.SAPCompanyConfiguration.GENOME_METHYLATION, broadExternal.getDefaultWbs(), "test description", "50",
                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.eq(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES))).thenReturn(
                otherPlatformMaterial);


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

