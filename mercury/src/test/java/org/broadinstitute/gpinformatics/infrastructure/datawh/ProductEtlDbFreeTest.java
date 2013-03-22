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
    private String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    private long entityId = 1122334455L;
    private long productId = 998877L;
    private String productName = "Test Product";
    private String partNumber = "TestNumber-5544";
    private Date availabilityDate = new Date(1350000000000L);
    private Date discontinuedDate = new Date(1380000000000L);
    private int expectedCycleTimeSeconds = 5400;
    private int guaranteedCycleTimeSeconds = 99999;
    private int samplesPerWeek = 200;
    private boolean isTopLevelProduct = true;
    private String workflowName = "Test Workflow 1";
    private String productFamilyName = "Test ProductFamily";
    private ProductEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private Product obj = createMock(Product.class);
    private ProductDao dao = createMock(ProductDao.class);
    private ProductFamily family = createMock(ProductFamily.class);
    private Object[] mocks = new Object[]{auditReader, dao, obj, family};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);

        tst = new ProductEtl(dao);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getProductId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, Product.class);
        assertEquals(tst.baseFilename, "product");
        assertEquals(tst.entityId(obj), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(Product.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateStr, false, -1L).size(), 0);

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

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
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

