package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
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
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderSampleEtlDbFreeTest {
    String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    long entityId = 1122334455L;
    long pdoId = 98789798L;
    String sampleName = "SM-007";
    ProductOrderSample.DeliveryStatus deliveryStatus = ProductOrderSample.DeliveryStatus.NOT_STARTED;
    int position = 42;

    AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    ProductOrderSampleDao dao = createMock(ProductOrderSampleDao.class);
    ProductOrder pdo = createMock(ProductOrder.class);
    ProductOrderSample obj = createMock(ProductOrderSample.class);
    Object[] mocks = new Object[]{auditReader, dao, pdo, obj};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        reset(mocks);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getProductOrderSampleId()).andReturn(entityId);
        replay(mocks);

        ProductOrderSampleEtl tst = new ProductOrderSampleEtl();
        tst.setProductOrderSampleDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.getEntityClass(), ProductOrderSample.class);

        assertEquals(tst.getBaseFilename(), "product_order_sample");

        assertEquals(tst.entityId(obj), (Long) entityId);

        assertNull(tst.entityStatusRecord(etlDateStr, null, null, false));

        assertTrue(tst.isEntityEtl());

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(ProductOrderSample.class, -1L)).andReturn(null);

        replay(mocks);

        ProductOrderSampleEtl tst = new ProductOrderSampleEtl();
        tst.setProductOrderSampleDao(dao);
        tst.setAuditReaderDao(auditReader);

        assertEquals(tst.entityRecord(etlDateStr, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(ProductOrderSample.class, entityId)).andReturn(obj);
        expect(obj.getProductOrderSampleId()).andReturn(entityId);
        expect(obj.getProductOrder()).andReturn(pdo).times(2);
        expect(pdo.getProductOrderId()).andReturn(pdoId);
        expect(obj.getSampleName()).andReturn(sampleName);
        expect(obj.getDeliveryStatus()).andReturn(deliveryStatus);
        expect(obj.getSamplePosition()).andReturn(position);

        replay(mocks);

        ProductOrderSampleEtl tst = new ProductOrderSampleEtl();
        tst.setProductOrderSampleDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecord(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    public void testBackfillEtl() throws Exception {
        List<ProductOrderSample> list = new ArrayList<ProductOrderSample>();
        list.add(obj);
        expect(dao.findAll(eq(ProductOrderSample.class), (GenericDao.GenericDaoCallback<ProductOrderSample>) anyObject())).andReturn(list);

        expect(obj.getProductOrderSampleId()).andReturn(entityId);
        expect(obj.getProductOrder()).andReturn(pdo).times(2);
        expect(pdo.getProductOrderId()).andReturn(pdoId);
        expect(obj.getSampleName()).andReturn(sampleName);
        expect(obj.getDeliveryStatus()).andReturn(deliveryStatus);
        expect(obj.getSamplePosition()).andReturn(position);

        replay(mocks);

        ProductOrderSampleEtl tst = new ProductOrderSampleEtl();
        tst.setProductOrderSampleDao(dao);
        tst.setAuditReaderDao(auditReader);

        Collection<String> records = tst.entityRecordsInRange(entityId, entityId, etlDateStr, false);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private void verifyRecord(String record) {
	int i = 0;
        String[] parts = record.split(",");
        assertEquals(parts[i++], etlDateStr);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], String.valueOf(pdoId));
        assertEquals(parts[i++], sampleName);
        assertEquals(parts[i++], deliveryStatus.name());
        assertEquals(parts[i++], String.valueOf(position));
    }

}

