package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Date;

/**
 * Database Free unit test of Ledger Entry entity ETL.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class LedgerEntryEtlDbFreeTest {
    private final String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    private static final long ledgerId = 1122334455L;
    private static final long posId = 2233445511L;
    private static final String quoteId = "ABCD9";
    private String datafileDir;
    private LedgerEntryEtl ledgerEntryEtl;

    private final AuditReaderDao auditReader = EasyMock.createMock(AuditReaderDao.class);
    private final LedgerEntry ledgerEntry = EasyMock.createMock(LedgerEntry.class);
    private final LedgerEntryDao ledgerEntryDao = EasyMock.createMock(LedgerEntryDao.class);
    private final ProductOrderSample productOrderSample = EasyMock.createMock(ProductOrderSample.class);
    private final Object[] mocks = new Object[]{auditReader, ledgerEntry, ledgerEntryDao, productOrderSample};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        datafileDir = System.getProperty("java.io.tmpdir");
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        ledgerEntryEtl = new LedgerEntryEtl(ledgerEntryDao);
        ledgerEntryEtl.setAuditReaderDao(auditReader);

        EasyMock.reset(mocks);
    }

    @AfterMethod(groups = TestGroups.DATABASE_FREE)
    public void afterMethod() throws Exception {
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    public void testEtlFlags() throws Exception {
        EasyMock.expect(ledgerEntry.getLedgerId()).andReturn(ledgerId);
        EasyMock.replay(mocks);

        Assert.assertEquals(ledgerEntryEtl.entityClass, LedgerEntry.class);
        Assert.assertEquals(ledgerEntryEtl.baseFilename, "ledger_entry");
        Assert.assertEquals(ledgerEntryEtl.entityId(ledgerEntry), (Long) ledgerId);

        EasyMock.verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        EasyMock.expect(ledgerEntryDao.findById(LedgerEntry.class, -1L)).andReturn(null);

        EasyMock.replay(mocks);

        Assert.assertEquals(ledgerEntryEtl.dataRecords(etlDateStr, false, -1L).size(), 0);

        EasyMock.verify(mocks);
    }

    public void testMakeRecord() throws Exception {
        EasyMock.expect(ledgerEntryDao.findById(LedgerEntry.class, ledgerId)).andReturn(ledgerEntry);
        EasyMock.expect(ledgerEntry.getProductOrderSample()).andReturn(productOrderSample);
        EasyMock.expect(ledgerEntry.getLedgerId()).andReturn(ledgerId);
        EasyMock.expect(productOrderSample.getProductOrderSampleId()).andReturn(posId);
        EasyMock.expect(ledgerEntry.getQuoteId()).andReturn(quoteId);
        EasyMock.replay(mocks);

        Collection<String> records = ledgerEntryEtl.dataRecords(etlDateStr, false, ledgerId);
        Assert.assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        EasyMock.verify(mocks);
    }

    private void verifyRecord(String record) {
        int i = 0;
        String[] parts = record.split(",");
        Assert.assertEquals(parts[i++], etlDateStr);
        Assert.assertEquals(parts[i++], "F");
        Assert.assertEquals(parts[i++], String.valueOf(ledgerId));
        Assert.assertEquals(parts[i++], String.valueOf(posId));
        Assert.assertEquals(parts[i++], quoteId);
        Assert.assertEquals(parts.length, i);
    }

}

