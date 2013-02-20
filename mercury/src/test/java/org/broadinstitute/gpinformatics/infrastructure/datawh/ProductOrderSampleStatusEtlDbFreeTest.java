package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderSampleStatusEtlDbFreeTest {
    String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    long entityId = 1122334455L;
    long pdoId = 332891L;
    Date revDate = new Date(1350000000000L);
    ProductOrderSample.DeliveryStatus deliveryStatus = ProductOrderSample.DeliveryStatus.DELIVERED;

    AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    ProductOrderSample obj = createMock(ProductOrderSample.class);
    Object[] mocks = new Object[]{auditReader, obj};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        reset(mocks);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getProductOrderSampleId()).andReturn(entityId);
        replay(mocks);

        ProductOrderSampleStatusEtl tst = new ProductOrderSampleStatusEtl();
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.getEntityClass(), ProductOrderSample.class);

        assertEquals(tst.getBaseFilename(), "product_order_sample_status");

        assertEquals(tst.entityId(obj), (Long) entityId);

        assertEquals(tst.entityRecord(etlDateStr, false, null).size(), 0);
        assertEquals(tst.entityRecordsInRange(0, 1, etlDateStr, false).size(), 0);

        assertFalse(tst.isEntityEtl());

        verify(mocks);
    }

    public void testCantMakeEtlRecord1() throws Exception {
        replay(mocks);

        ProductOrderSampleStatusEtl tst = new ProductOrderSampleStatusEtl();
        tst.setAuditReaderDao(auditReader);

        assertNull(tst.entityStatusRecord(etlDateStr, revDate, null, false));

        verify(mocks);
    }

    public void testCantMakeEtlRecord2() throws Exception {
        expect(obj.getDeliveryStatus()).andReturn(null);

        replay(mocks);

        ProductOrderSampleStatusEtl tst = new ProductOrderSampleStatusEtl();
        tst.setAuditReaderDao(auditReader);

        assertNull(tst.entityStatusRecord(etlDateStr, revDate, obj, false));

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(obj.getDeliveryStatus()).andReturn(deliveryStatus);
        expect(obj.getProductOrderSampleId()).andReturn(entityId);
        expect(obj.getDeliveryStatus()).andReturn(deliveryStatus);

        replay(mocks);

        ProductOrderSampleStatusEtl tst = new ProductOrderSampleStatusEtl();
        tst.setAuditReaderDao(auditReader);

        String record = tst.entityStatusRecord(etlDateStr, revDate, obj, false);
        assertTrue(record != null && record.length() > 0);
        verifyRecord(record);

        verify(mocks);
    }

    private void verifyRecord(String record) {
        int i = 0;
        String[] parts = record.split(",");
        assertEquals(parts[i++], etlDateStr);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], ExtractTransform.secTimestampFormat.format(revDate));
        assertEquals(parts[i++], deliveryStatus.name());
        assertEquals(parts.length, i);
    }
}

