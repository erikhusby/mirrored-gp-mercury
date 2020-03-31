package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;


/**
 * dbfree test of ExtractTransform.
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class ExtractTransformDbFreeTest {
    private String datafileDir;
    private final Date now = new Date();
    private final String nowMsec = String.valueOf(now.getTime());
    private String badDataDir = datafileDir + nowMsec;

    private ExtractTransform extractTransform;
    private final AuditReaderDao auditReaderDao = createMock(AuditReaderDao.class);
    private final LabEventEtl labEventEtl = createMock(LabEventEtl.class);
    private final LabVesselEtl labVesselEtl = createMock(LabVesselEtl.class);
    private final PriceItemEtl priceItemEtl = createMock(PriceItemEtl.class);
    private final ProductEtl productEtl = createMock(ProductEtl.class);
    private final ProductOrderAddOnEtl productOrderAddOnEtl = createMock(ProductOrderAddOnEtl.class);
    private final ProductOrderEtl productOrderEtl = createMock(ProductOrderEtl.class);
    private final ProductOrderSampleEtl productOrderSampleEtl = createMock(ProductOrderSampleEtl.class);
    private final ProjectPersonEtl projectPersonEtl = createMock(ProjectPersonEtl.class);
    private final ResearchProjectCohortEtl researchProjectCohortEtl = createMock(ResearchProjectCohortEtl.class);
    private final ResearchProjectEtl researchProjectEtl = createMock(ResearchProjectEtl.class);
    private final ResearchProjectFundingEtl researchProjectFundingEtl = createMock(ResearchProjectFundingEtl.class);
    private final ResearchProjectIrbEtl researchProjectIrbEtl = createMock(ResearchProjectIrbEtl.class);
    private final WorkflowConfigEtl workflowConfigEtl = createMock(WorkflowConfigEtl.class);
    private final RiskItemEtl riskItemEtl = createMock(RiskItemEtl.class);
    private final LedgerEntryCrossEtl ledgerEntryCrossEtl = createMock(LedgerEntryCrossEtl.class);
    private final LedgerEntryEtl ledgerEntryEtl = createMock(LedgerEntryEtl.class);
    private final SequencingRunEtl sequencingRunEtl = createMock(SequencingRunEtl.class);
    private final SequencingSampleFactEtl sequencingSampleFactEtl = createMock(SequencingSampleFactEtl.class);
    private final BillingSessionEtl billingSessionEtl = createMock(BillingSessionEtl.class);
    private final LabMetricEtl labMetricEtl = createMock(LabMetricEtl.class);

    private Object[] mocks = new Object[]{
            auditReaderDao,
            labEventEtl,
            labVesselEtl,
            priceItemEtl,
            productEtl,
            productOrderAddOnEtl,
            productOrderEtl,
            productOrderSampleEtl,
            projectPersonEtl,
            researchProjectCohortEtl,
            researchProjectEtl,
            researchProjectFundingEtl,
            researchProjectIrbEtl,
            workflowConfigEtl,
            riskItemEtl,
            ledgerEntryCrossEtl,
            ledgerEntryEtl,
            sequencingRunEtl,
            sequencingSampleFactEtl,
            billingSessionEtl,
            labMetricEtl
    };

    @BeforeClass
    public void beforeClass() throws Exception {
        datafileDir = System.getProperty("java.io.tmpdir");
        badDataDir = datafileDir + System.getProperty("file.separator") + nowMsec;

        Collection<GenericEntityEtl> etlInstances = new HashSet<>();
        for (Object mock : mocks) {
            if (mock.getClass().getName().contains("Etl")) {
                etlInstances.add((GenericEntityEtl)mock);
            }
        }

        extractTransform = new ExtractTransform(auditReaderDao,
                new SessionContextUtility(null, null) {
                    @Override
                    public void executeInContext(Function function) {
                        function.apply();
                    }
                },
                etlInstances);
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);
        reset(mocks);
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    public void testInvalidDir1() {
        replay(mocks);
        ExtractTransform.setDatafileDir(null);
        Assert.assertEquals(extractTransform.incrementalEtl("0", "0"), -1);
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 0, Long.MAX_VALUE).getStatus());
        verify(mocks);
    }

    public void testInvalidDir2() {
        replay(mocks);
        ExtractTransform.setDatafileDir("");
        Assert.assertEquals(extractTransform.incrementalEtl("0", "0"), -1);
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 0, Long.MAX_VALUE).getStatus());
        verify(mocks);
    }

    public void testInvalidDir3() {
        replay(mocks);
        ExtractTransform.setDatafileDir(badDataDir);
        Assert.assertEquals(extractTransform.incrementalEtl("0", "0"), -1);
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 0, Long.MAX_VALUE).getStatus());
        verify(mocks);
    }

    public void testNoChanges() {
        long futureSec = 9999999999L;
        replay(mocks);
        ExtractTransform.writeLastEtlRun(futureSec);
        Assert.assertEquals(extractTransform.incrementalEtl("0", "0"), -1);
        Assert.assertTrue(ExtractTransform.getIncrementalRunStartTime() >= 0);
        verify(mocks);
    }

    public void testBadRange1() {
        replay(mocks);
        Assert.assertEquals(
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), -1, Long.MAX_VALUE).getStatus(),
                Response.Status.BAD_REQUEST.getStatusCode());
        verify(mocks);
    }

    public void testBadRange2() {
        replay(mocks);
        Assert.assertEquals(
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 1000, 999).getStatus(),
                Response.Status.BAD_REQUEST.getStatusCode());
        verify(mocks);
    }

    public void testInvalidClassName() {
        replay(mocks);
        Assert.assertEquals(
                extractTransform.backfillEtl("NoSuchClass_Ihope", 0, Long.MAX_VALUE).getStatus(),
                Response.Status.NOT_FOUND.getStatusCode());
        verify(mocks);
    }

    /**
     * Passes a non-existent directory for the last run file.
     */
    public void testInvalidLastEtlBadDir() {
        ExtractTransform.setDatafileDir(badDataDir);
        Assert.assertEquals(ExtractTransform.readLastEtlRun(), 0);
    }

    /**
     * Writes seconds or milliseconds, verify read is always seconds.
     */
    public void testReadLastEtlmSec() throws IOException {
        File file = new File(datafileDir, ExtractTransform.LAST_ETL_FILE);
        final long sec  = 1351112223L;
        final long mSec = 1351112223789L;
        FileUtils.write(file, String.valueOf(sec));
        Assert.assertEquals(ExtractTransform.readLastEtlRun(), sec);

        FileUtils.write(file, String.valueOf(mSec));
        Assert.assertEquals(ExtractTransform.readLastEtlRun(), sec);
    }

    /**
     * Takes the mutex, ETL cannot run.
     */
    public void testMutex() {
        replay(mocks);
        Assert.assertTrue(ExtractTransform.getMutex().tryAcquire());
        try {
            Assert.assertEquals(extractTransform.incrementalEtl("20130327155950", "20130327160200"), -1);
        } finally {
            ExtractTransform.getMutex().release();
        }
        verify(mocks);
    }

    public void testIsReady() {
        String etlDateStr = "20130215091500";
        File f = new File(datafileDir, etlDateStr + ExtractTransform.READY_FILE_SUFFIX);
        Assert.assertFalse(f.exists());
        ExtractTransform.writeIsReadyFile(etlDateStr);
        Assert.assertTrue(f.exists());
    }

    public void testOnDemandIncr1() {
        long startEtl = 1364411920L;

        expect(auditReaderDao.fetchAuditIds(eq(startEtl - ExtractTransform.TRANSACTION_COMPLETION_GUARDBAND), anyLong())).andReturn(new TreeMap<Long, Date>());
        auditReaderDao.clear();
        expectLastCall();
        replay(mocks);

        ExtractTransform.writeLastEtlRun(startEtl);
        Assert.assertEquals(ExtractTransform.readLastEtlRun(), startEtl);

        Assert.assertEquals(extractTransform.incrementalEtl("0", "0"), 0);

        Assert.assertTrue(ExtractTransform.readLastEtlRun() > startEtl);
        verify(mocks);
    }

    public void testOnDemandIncr2() {
        long startEtl = 1364411920L;
        long endEtl = 1364411930L;
        String endEtlStr = ExtractTransform.formatTimestamp(new Date(endEtl * 1000L));

        expect(auditReaderDao.fetchAuditIds(startEtl - ExtractTransform.TRANSACTION_COMPLETION_GUARDBAND, endEtl)).andReturn(new TreeMap<Long, Date>());
        auditReaderDao.clear();
        expectLastCall();
        replay(mocks);

        ExtractTransform.writeLastEtlRun(startEtl);

        Assert.assertEquals(extractTransform.incrementalEtl("0", endEtlStr), 0);
        // Limited range etl should not update lastEtlRun file.
        Assert.assertEquals(ExtractTransform.readLastEtlRun(), startEtl);
        verify(mocks);
    }

    public void testOnDemandBackfill() throws Exception {
        Class testClass = LabBatch.class;
        expect(productEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(priceItemEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(projectPersonEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectIrbEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectFundingEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectCohortEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(productOrderSampleEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(productOrderEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(productOrderAddOnEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(labVesselEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(workflowConfigEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(labEventEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(riskItemEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(ledgerEntryCrossEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(ledgerEntryEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(sequencingRunEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(sequencingSampleFactEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(billingSessionEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(labMetricEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);

        replay(mocks);
        ExtractTransform.writeLastEtlRun(0L);
        new ExtractTransformResource(extractTransform).entityIdRangeEtl(testClass.getName(), 0, 0);
        Assert.assertEquals(0L, ExtractTransform.readLastEtlRun());
        verify(mocks);
    }

    public void testBackfillDefaultEnd() throws Exception {
        long startEtl = System.currentTimeMillis();
        Class testClass = Product.class;
        expect(productEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(1);
        expect(priceItemEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(projectPersonEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectIrbEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectFundingEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectCohortEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(productOrderSampleEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(productOrderEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(productOrderAddOnEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(labVesselEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(workflowConfigEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(labEventEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(riskItemEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(ledgerEntryCrossEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(ledgerEntryEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(sequencingRunEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(sequencingSampleFactEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(billingSessionEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(labMetricEtl.doBackfillEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);

        replay(mocks);
        ExtractTransform.writeLastEtlRun(0L);
        new ExtractTransformResource(extractTransform).entityIdRangeEtl(testClass.getName(), 0, -1);
        long endEtl = System.currentTimeMillis();
        File[] files = EtlTestUtilities.getDirFiles(datafileDir, startEtl, endEtl);
        boolean readyFileFound = false;
        for (File dataFile : files) {
            if (dataFile.getName().endsWith(ExtractTransform.READY_FILE_SUFFIX)) {
                readyFileFound = true;
            }
        }
        Assert.assertTrue(readyFileFound);

        verify(mocks);
    }

    public void testOnDemandIncrementalNoChanges() {
        final long startEtlSec = 1360000000L;
        SortedMap<Long, Date> revs = new TreeMap<>();
        expect(auditReaderDao.fetchAuditIds(eq(startEtlSec - ExtractTransform.TRANSACTION_COMPLETION_GUARDBAND), anyLong())).andReturn(revs);
        auditReaderDao.clear();
        expectLastCall();

        replay(mocks);
        ExtractTransform.writeLastEtlRun(startEtlSec);
        new ExtractTransformResource(extractTransform).auditDateRangeEtl("0", "0");
        Assert.assertTrue(ExtractTransform.readLastEtlRun() > startEtlSec);
        verify(mocks);
    }

    public void testOnDemandIncremental() throws Exception {
        final long startEtlSec = 1360000000L;
        SortedMap<Long, Date> revs = new TreeMap<>();
        revs.put(1L, new Date(startEtlSec));
        expect(auditReaderDao.fetchAuditIds(eq(startEtlSec - ExtractTransform.TRANSACTION_COMPLETION_GUARDBAND), anyLong())).andReturn(revs);
        Set<Long> revIds = revs.keySet();
        expect(productEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(1);
        expect(priceItemEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(researchProjectEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(projectPersonEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(researchProjectIrbEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(researchProjectFundingEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(researchProjectCohortEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(productOrderSampleEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(productOrderEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(productOrderAddOnEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(labVesselEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(workflowConfigEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(labEventEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(riskItemEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(ledgerEntryCrossEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(ledgerEntryEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(sequencingRunEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(sequencingSampleFactEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(billingSessionEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(labMetricEtl.doIncrementalEtl(eq(revIds), (String) anyObject())).andReturn(0);
        auditReaderDao.clear();
        expectLastCall();

        replay(mocks);
        ExtractTransform.writeLastEtlRun(startEtlSec);
        new ExtractTransformResource(extractTransform).auditDateRangeEtl("0", "0");
        long etlEnd = ExtractTransform.readLastEtlRun();
        Assert.assertTrue(etlEnd >= startEtlSec);

        File[] files = EtlTestUtilities.getDirFiles(datafileDir, startEtlSec, System.currentTimeMillis());
        boolean readyFileFound = false;
        for (File dataFile : files) {
            if (dataFile.getName().endsWith(ExtractTransform.READY_FILE_SUFFIX)) {
                readyFileFound = true;
            }
        }
        Assert.assertTrue(readyFileFound);

        verify(mocks);
    }

    public void testNoLimitEmptyBatch() {
        SortedMap<Long, Date> revs = new TreeMap<>();
        long startTimeSec = 136000000;
        long endTimeSec =  startTimeSec + ExtractTransform.ETL_BATCH_SIZE;
        ImmutablePair<SortedMap<Long, Date>, Long> revAndDate = ExtractTransform.limitBatchSize(revs, endTimeSec);
        assertEquals(revAndDate.left.size(), revs.size());
        assertEquals(revAndDate.right.longValue(), endTimeSec);
    }

    public void testNoLimitSmallSameTimeBatch() {
        SortedMap<Long, Date> revs = new TreeMap<>();
        long startTimeSec = 136000000;
        long endTimeSec =  startTimeSec + ExtractTransform.ETL_BATCH_SIZE;
        Date revDate = new Date(startTimeSec * ExtractTransform.MSEC_IN_SEC);
        for (long i = 0; i < ExtractTransform.ETL_BATCH_SIZE; ++i) {
            revs.put(i, revDate);
        }
        ImmutablePair<SortedMap<Long, Date>, Long> revAndDate = ExtractTransform.limitBatchSize(revs, endTimeSec);
        assertEquals(revAndDate.left.size(), revs.size());
        assertEquals(revAndDate.right.longValue(), endTimeSec);
    }

    public void testNoLimitBigSameTimeBatch() {
        SortedMap<Long, Date> revs = new TreeMap<>();
        long startTimeSec = 136000000;
        long endTimeSec =  startTimeSec + ExtractTransform.ETL_BATCH_SIZE + 1001;
        Date revDate = new Date(startTimeSec * ExtractTransform.MSEC_IN_SEC);
        for (long i = 0; i < ExtractTransform.ETL_BATCH_SIZE + 1000; ++i) {
            revs.put(i, revDate);
        }
        ImmutablePair<SortedMap<Long, Date>, Long> revAndDate = ExtractTransform.limitBatchSize(revs, endTimeSec);
        assertEquals(revAndDate.left.size(), revs.size());
        assertEquals(revAndDate.right.longValue(), endTimeSec);
    }

    public void testLimitExcessBigBatch() {
        SortedMap<Long, Date> revs = new TreeMap<>();
        long startTimeSec = 136000000;
        long endTimeSec =  startTimeSec + ExtractTransform.ETL_BATCH_SIZE + 1001;
        for (long i = 0; i < ExtractTransform.ETL_BATCH_SIZE + 1001; ++i) {
            Date revDate = new Date(startTimeSec * ExtractTransform.MSEC_IN_SEC + i);
            revs.put(i, revDate);
        }
        ImmutablePair<SortedMap<Long, Date>, Long> revAndDate = ExtractTransform.limitBatchSize(revs, endTimeSec);
        // Must include all items within the same second, i.e. items 0-999.
        assertEquals(revAndDate.left.size(), 1000);
        assertEquals(revAndDate.right.longValue(), startTimeSec + 1);
    }

    public void testLimitNormalBigBatch() {
        SortedMap<Long, Date> revs = new TreeMap<>();
        long startTimeSec = 136000000;
        long endTimeSec = 0;
        for (long i = 0; i < ExtractTransform.ETL_BATCH_SIZE + 102; ++i) {
            endTimeSec = startTimeSec + i;
            Date revDate = new Date(endTimeSec * ExtractTransform.MSEC_IN_SEC);
            revs.put(i, revDate);
        }
        ImmutablePair<SortedMap<Long, Date>, Long> revAndDate = ExtractTransform.limitBatchSize(revs, endTimeSec);
        assertEquals(revAndDate.left.size(), ExtractTransform.ETL_BATCH_SIZE);
        assertEquals(revAndDate.right.longValue(), startTimeSec + ExtractTransform.ETL_BATCH_SIZE);
        assertNotEquals(revAndDate.left, revs);
    }
}
