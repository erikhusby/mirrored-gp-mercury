package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessItem;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductEjbDBFreeTest {

    public void testPublishToSAP() throws Exception {
        final SAPAccessControlEjb mockSapAccessControl = Mockito.mock(SAPAccessControlEjb.class);
        final AttributeArchetypeDao mockAttributeArchetypeDao = Mockito.mock(AttributeArchetypeDao.class);
        final AuditReaderDao mockAuditReaderDao = Mockito.mock(AuditReaderDao.class);
        final SapIntegrationService mockSapService = Mockito.mock(SapIntegrationService.class);
        final ProductDao mockProductDao = Mockito.mock(ProductDao.class);
        ProductEjb testEjb = new ProductEjb(mockProductDao, mockSapService, mockAuditReaderDao,
                mockAttributeArchetypeDao, mockSapAccessControl);

        SAPAccessControl noControl = new SAPAccessControl();
        SAPAccessControl blockControl = new SAPAccessControl();
        blockControl.setDisabledItems(Collections.singleton(new AccessItem("blockThisItem")));

        Mockito.when(mockSapAccessControl.getCurrentControlDefinitions()).thenReturn(blockControl);
        Product testProduct = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "SGM-TEST-SAP");
        testProduct.setPrimaryPriceItem(new PriceItem("qsID", "testPlatform", "testCategory", "blockThisItem"));

        try {
            testEjb.publishProductToSAP(testProduct);
            Assert.fail();
        } catch (SAPIntegrationException e) {

        }

        assertThat(testProduct.isSavedInSAP(), is(false));
        Mockito.verify(mockSapService, Mockito.times(0)).createProductInSAP(testProduct);

        Mockito.when(mockSapAccessControl.getCurrentControlDefinitions()).thenReturn(noControl);
        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(1)).createProductInSAP(testProduct);


        Mockito.when(mockSapAccessControl.getCurrentControlDefinitions()).thenReturn(blockControl);
        try {
            testEjb.publishProductToSAP(testProduct);
            Assert.fail();
        } catch (SAPIntegrationException e) {
        }
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(1)).createProductInSAP(testProduct);
        Mockito.verify(mockSapService, Mockito.times(0)).changeProductInSAP(testProduct);


        Mockito.when(mockSapAccessControl.getCurrentControlDefinitions()).thenReturn(noControl);
        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(1)).createProductInSAP(testProduct);
        Mockito.verify(mockSapService, Mockito.times(1)).changeProductInSAP(testProduct);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(1)).createProductInSAP(testProduct);
        Mockito.verify(mockSapService, Mockito.times(2)).changeProductInSAP(testProduct);

        testEjb.publishProductToSAP(testProduct);
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(1)).createProductInSAP(testProduct);
        Mockito.verify(mockSapService, Mockito.times(3)).changeProductInSAP(testProduct);

        Mockito.when(mockSapAccessControl.getCurrentControlDefinitions()).thenReturn(blockControl);

        try {
            testEjb.publishProductToSAP(testProduct);
            Assert.fail();
        } catch (SAPIntegrationException e) {

        }
        assertThat(testProduct.isSavedInSAP(), is(true));
        Mockito.verify(mockSapService, Mockito.times(1)).createProductInSAP(testProduct);
        Mockito.verify(mockSapService, Mockito.times(3)).changeProductInSAP(testProduct);


        Product testProduct2 = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "P-CLIATEST-SAP");
        testProduct2.setPrimaryPriceItem(new PriceItem("qsID", "testPlatform", "testCategory", "blockThisItem"));
        testProduct2.setExternalOnlyProduct(true);
        Mockito.when(mockSapAccessControl.getCurrentControlDefinitions()).thenReturn(noControl);

        try {
            testEjb.publishProductToSAP(testProduct2);
            Assert.fail();
        } catch (SAPIntegrationException e) {
            assertThat(e.getMessage(), containsString("Cannot be published to SAP since it is either a Clinical or Commercial product"));
        }

    }
}
