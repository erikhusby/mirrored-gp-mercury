package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
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
public class ProductOrderStatusEtlDbFreeTest {
    private String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    private long entityId = 1122334455L;
    private Date revDate = new Date(1350000000000L);
    private ProductOrder.OrderStatus orderStatus = ProductOrder.OrderStatus.Submitted;
    private ProductOrderStatusEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private ProductOrder obj = createMock(ProductOrder.class);
    private Object[] mocks = new Object[]{auditReader, obj};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        reset(mocks);

        tst = new ProductOrderStatusEtl();
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getProductOrderId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.getEntityClass(), ProductOrder.class);

        assertEquals(tst.getBaseFilename(), "product_order_status");

        assertEquals(tst.entityId(obj), (Long) entityId);

        assertEquals(tst.entityRecords(etlDateStr, false, null).size(), 0);
        assertEquals(tst.entityRecordsInRange(0, 1, etlDateStr, false).size(), 0);

        assertFalse(tst.isEntityEtl());

        verify(mocks);
    }

    public void testCantMakeEtlRecord1() throws Exception {
        replay(mocks);

        assertNull(tst.entityStatusRecord(etlDateStr, revDate, null, false));

        verify(mocks);
    }

    public void testCantMakeEtlRecord2() throws Exception {
        expect(obj.getOrderStatus()).andReturn(null);

        replay(mocks);

        assertNull(tst.entityStatusRecord(etlDateStr, revDate, obj, false));

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(obj.getProductOrderId()).andReturn(entityId);
        expect(obj.getOrderStatus()).andReturn(orderStatus).times(2);

        replay(mocks);

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
        assertEquals(parts[i++], orderStatus.getDisplayName());
        assertEquals(parts.length, i);
    }
}

