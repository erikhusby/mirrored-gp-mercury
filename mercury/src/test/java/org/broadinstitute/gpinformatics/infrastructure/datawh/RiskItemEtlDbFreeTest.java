package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.products.RiskItemDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.RiskItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Date;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class RiskItemEtlDbFreeTest {
    private String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    private long posId = 2233445511L;
    private String datafileDir;
    private RiskItemEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private RiskItem obj = createMock(RiskItem.class);
    private RiskItemDao dao = createMock(RiskItemDao.class);
    private ProductOrderSample pos = createMock(ProductOrderSample.class);
    private Object[] mocks = new Object[]{auditReader, obj, dao, pos};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        datafileDir = System.getProperty("java.io.tmpdir");
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        reset(mocks);

        tst = new RiskItemEtl();
        tst.setRiskItemDao(dao);
        tst.setAuditReaderDao(auditReader);
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    public void testEtlFlags() throws Exception {
        long entityId = 1122334455L;
        expect(obj.getRiskItemId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.getEntityClass(), RiskItem.class);

        assertEquals(tst.getBaseFilename(), "product_order_sample_risk");

        assertEquals(tst.entityId(obj), (Long) entityId);

        assertNull(tst.entityStatusRecord(etlDateStr, null, null, false));

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(ProductOrderSample.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.entityRecords(etlDateStr, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testMakePosRecord() throws Exception {
        expect(dao.findById(ProductOrderSample.class, posId)).andReturn(pos);
        expect(pos.getProductOrderSampleId()).andReturn(posId);
        expect(pos.isOnRisk()).andReturn(true);
        replay(mocks);

        Collection<String> records = tst.entityRecords(etlDateStr, false, posId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private void verifyRecord(String record) {
        int i = 0;
        String[] parts = record.split(",");
        assertEquals(parts[i++], etlDateStr);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(posId));
        assertEquals(parts[i++], "T");
        assertEquals(parts.length, i);
    }

}

