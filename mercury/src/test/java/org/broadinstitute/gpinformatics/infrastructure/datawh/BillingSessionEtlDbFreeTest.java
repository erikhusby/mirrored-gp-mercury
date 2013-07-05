package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Date;

@Test(groups = TestGroups.DATABASE_FREE)
public class BillingSessionEtlDbFreeTest {
    private final String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    private static final long BILLING_SESSION_ID = 1122334455;
    private static final String BILLING_SESSION_MESSAGE = "Billing Successful";
    private static final Date BILLED_DATE = new Date();
    private static final BillingSession.BillingSessionType BILLING_SESSION_TYPE = BillingSession.BillingSessionType.ROLLUP_DAILY;
    private String datafileDir;
    private BillingSessionEtl billingSessionEtl;

    private final AuditReaderDao auditReader = EasyMock.createMock(AuditReaderDao.class);
    private final BillingSession billingSession = EasyMock.createMock(BillingSession.class);
    private final BillingSessionDao billingSessionDao = EasyMock.createMock(BillingSessionDao.class);
    private final Object[] mocks = new Object[]{auditReader, billingSessionDao, billingSession};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        datafileDir = System.getProperty("java.io.tmpdir");
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        billingSessionEtl = new BillingSessionEtl(billingSessionDao);
        billingSessionEtl.setAuditReaderDao(auditReader);

        EasyMock.reset(mocks);
    }

    @AfterMethod(groups = TestGroups.DATABASE_FREE)
    public void afterMethod() throws Exception {
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    public void testEtlFlags() throws Exception {
        EasyMock.expect(billingSession.getBillingSessionId()).andReturn(BILLING_SESSION_ID);
        EasyMock.replay(mocks);

        Assert.assertEquals(billingSessionEtl.entityClass, BillingSession.class);
        Assert.assertEquals(billingSessionEtl.baseFilename, "billing_session");
        Assert.assertEquals(billingSessionEtl.entityId(billingSession), (Long) BILLING_SESSION_ID);

        EasyMock.verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        EasyMock.expect(billingSessionDao.findById(BillingSession.class, -1L)).andReturn(null);

        EasyMock.replay(mocks);

        Assert.assertEquals(billingSessionEtl.dataRecords(etlDateStr, false, -1L).size(), 0);

        EasyMock.verify(mocks);
    }

    public void testMakeRecord() throws Exception {
        EasyMock.expect(billingSessionDao.findById(BillingSession.class, BILLING_SESSION_ID)).andReturn(billingSession);
        EasyMock.expect(billingSession.getBillingSessionId()).andReturn(BILLING_SESSION_ID);
        EasyMock.expect(billingSession.getBilledDate()).andReturn(BILLED_DATE);
        EasyMock.expect(billingSession.getBillingSessionType()).andReturn(BILLING_SESSION_TYPE);
        EasyMock.replay(mocks);

        Collection<String> records = billingSessionEtl.dataRecords(etlDateStr, false, BILLING_SESSION_ID);
        Assert.assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next());

        EasyMock.verify(mocks);
    }

    private void verifyRecord(String record) {
        int i = 0;
        String[] parts = record.split(",");
        Assert.assertEquals(parts[i++], etlDateStr);
        Assert.assertEquals(parts[i++], "F");
        Assert.assertEquals(parts[i++], String.valueOf(BILLING_SESSION_ID));
        Assert.assertEquals(parts[i++], EtlTestUtilities.format(BILLED_DATE));
        Assert.assertEquals(parts[i++], String.valueOf(BILLING_SESSION_TYPE));
        Assert.assertEquals(parts.length, i);
    }
}
