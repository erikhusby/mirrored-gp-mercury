package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductEtlDbFreeTest {
    String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    long entityId = 1122334455L;
    long productId = 998877L;
    String productName = "Test Product";
    String partNumber = "TestNumber-5544";
    Date availabilityDate = new Date(1350000000000L);
    Date discontinuedDate = new Date(1380000000000L);
    int expectedCycleTimeSeconds = 5400;
    int guaranteedCycleTimeSeconds = 99999;
    int samplesPerWeek = 200;
    boolean isTopLevelProduct = true;
    String workflowName = "Test Workflow 1";
    String productFamilyName = "Test ProductFamily";

    AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    Product obj = createMock(Product.class);
    ProductDao dao = createMock(ProductDao.class);
    ProductFamily family = createMock(ProductFamily.class);
    Object[] mocks = new Object[]{auditReader, dao, obj, family};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getProductId()).andReturn(entityId);
        replay(mocks);

        ProductEtl tst = new ProductEtl();
        tst.setProductDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.getEntityClass(), Product.class);

        assertEquals(tst.getBaseFilename(), "product");

        assertEquals(tst.entityId(obj), (Long) entityId);

        assertNull(tst.entityStatusRecord(etlDateStr, null, null, false));

        assertTrue(tst.isEntityEtl());

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(Product.class, -1L)).andReturn(null);

        replay(mocks);

        ProductEtl tst = new ProductEtl();
        tst.setProductDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.entityRecord(etlDateStr, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(Product.class, entityId)).andReturn(obj);

        expect(obj.getProductId()).andReturn(entityId);
        expect(obj.getProductName()).andReturn(productName);
        expect(obj.getPartNumber()).andReturn(partNumber);
        expect(obj.getAvailabilityDate()).andReturn(availabilityDate);
        expect(obj.getDiscontinuedDate()).andReturn(discontinuedDate);
        expect(obj.getExpectedCycleTimeSeconds()).andReturn(expectedCycleTimeSeconds);
        expect(obj.getGuaranteedCycleTimeSeconds()).andReturn(guaranteedCycleTimeSeconds);
        expect(obj.getSamplesPerWeek()).andReturn(samplesPerWeek);
        expect(obj.isTopLevelProduct()).andReturn(isTopLevelProduct);
        expect(obj.getWorkflowName()).andReturn(workflowName);
        expect(obj.getProductFamily()).andReturn(family).times(2);

        expect(family.getName()).andReturn(productFamilyName);

        replay(mocks);

        ProductEtl tst = new ProductEtl();
        tst.setProductDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecord(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    public void testBackfillEtl() throws Exception {
        List<Product> list = new ArrayList<Product>();
        list.add(obj);
        expect(dao.findAll(eq(Product.class), (GenericDao.GenericDaoCallback<Product>) anyObject())).andReturn(list);

        expect(obj.getProductId()).andReturn(entityId);
        expect(obj.getProductName()).andReturn(productName);
        expect(obj.getPartNumber()).andReturn(partNumber);
        expect(obj.getAvailabilityDate()).andReturn(availabilityDate);
        expect(obj.getDiscontinuedDate()).andReturn(discontinuedDate);
        expect(obj.getExpectedCycleTimeSeconds()).andReturn(expectedCycleTimeSeconds);
        expect(obj.getGuaranteedCycleTimeSeconds()).andReturn(guaranteedCycleTimeSeconds);
        expect(obj.getSamplesPerWeek()).andReturn(samplesPerWeek);
        expect(obj.isTopLevelProduct()).andReturn(isTopLevelProduct);
        expect(obj.getWorkflowName()).andReturn(workflowName);
        expect(obj.getProductFamily()).andReturn(family).times(2);

        expect(family.getName()).andReturn(productFamilyName);

        replay(mocks);

        ProductEtl tst = new ProductEtl();
        tst.setProductDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecordsInRange(entityId, entityId, etlDateStr, false);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private void verifyRecord(String record) {
        String[] parts = record.split(",");
        int i = 0;
        assertEquals(parts[i++], etlDateStr);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], productName);
        assertEquals(parts[i++], partNumber);
        assertEquals(parts[i++], EtlTestUtilities.format(availabilityDate));
        assertEquals(parts[i++], EtlTestUtilities.format(discontinuedDate));
        assertEquals(parts[i++], String.valueOf(expectedCycleTimeSeconds));
        assertEquals(parts[i++], String.valueOf(guaranteedCycleTimeSeconds));
        assertEquals(parts[i++], String.valueOf(samplesPerWeek));
        assertEquals(parts[i++], EtlTestUtilities.format(isTopLevelProduct));
        assertEquals(parts[i++], workflowName);
        assertEquals(parts[i++], productFamilyName);
        assertEquals(parts.length, i);
    }

}

