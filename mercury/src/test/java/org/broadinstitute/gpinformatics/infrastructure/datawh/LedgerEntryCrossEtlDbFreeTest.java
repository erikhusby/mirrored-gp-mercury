package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class LedgerEntryCrossEtlDbFreeTest {
    private String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    private long posId = 2233445511L;
    private String datafileDir;
    private Set<LedgerEntry> ledgerItems = new HashSet<LedgerEntry>();
    private LedgerEntryCrossEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private LedgerEntry obj = createMock(LedgerEntry.class);
    private ProductOrderSampleDao dao = createMock(ProductOrderSampleDao.class);
    private ProductOrderSample pos = createMock(ProductOrderSample.class);
    private Object[] mocks = new Object[]{auditReader, obj, dao, pos};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        datafileDir = System.getProperty("java.io.tmpdir");
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        tst = new LedgerEntryCrossEtl(dao);
        tst.setAuditReaderDao(auditReader);

        reset(mocks);
    }

    @AfterMethod(groups = TestGroups.DATABASE_FREE)
    public void afterMethod() throws Exception {
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    public void testEtlFlags() throws Exception {
        long entityId = 1122334455L;
        expect(obj.getLedgerId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, LedgerEntry.class);
        assertEquals(tst.baseFilename, "product_order_sample_bill");
        assertEquals(tst.entityId(obj), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(ProductOrderSample.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateStr, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testMakePosRecord() throws Exception {
        expect(dao.findById(ProductOrderSample.class, posId)).andReturn(pos);
        expect(pos.getProductOrderSampleId()).andReturn(posId);
        expect(pos.getBillableLedgerItems()).andReturn(ledgerItems);
        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, posId);
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

