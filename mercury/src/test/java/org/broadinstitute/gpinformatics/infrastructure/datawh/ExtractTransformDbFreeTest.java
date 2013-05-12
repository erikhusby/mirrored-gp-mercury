package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.*;


/**
 * dbfree test of ExtractTransform.
 *
 * @author epolk
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
    private final LabBatchEtl labBatchEtl = createMock(LabBatchEtl.class);
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

    private Object[] mocks = new Object[]{
            auditReaderDao,
            labEventEtl,
            labBatchEtl,
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
            ledgerEntryEtl
    };

    @BeforeClass(groups = TestGroups.DATABASE_FREE)
    public void beforeClass() throws Exception {
        datafileDir = System.getProperty("java.io.tmpdir");
        badDataDir = datafileDir + System.getProperty("file.separator") + nowMsec;

        Collection<GenericEntityEtl> etlInstances = new HashSet<GenericEntityEtl>();
        for (Object mock : mocks) {
            if (mock != auditReaderDao) {
                etlInstances.add((GenericEntityEtl)mock);
            }
        }
        extractTransform = new ExtractTransform(auditReaderDao, etlInstances);
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void beforeMethod() throws Exception {
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);
        reset(mocks);
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void afterMethod() throws Exception {
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    public void testInvalidDir1() {
        replay(mocks);
        ExtractTransform.setDatafileDir(null);
        Assert.assertEquals(extractTransform.incrementalEtl("0", "0"), -1);
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR,
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 0, Long.MAX_VALUE));
        verify(mocks);
    }

    public void testInvalidDir2() {
        replay(mocks);
        ExtractTransform.setDatafileDir("");
        Assert.assertEquals(extractTransform.incrementalEtl("0", "0"), -1);
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR,
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 0, Long.MAX_VALUE));
        verify(mocks);
    }

    public void testInvalidDir3() {
        replay(mocks);
        ExtractTransform.setDatafileDir(badDataDir);
        Assert.assertEquals(extractTransform.incrementalEtl("0", "0"), -1);
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR,
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 0, Long.MAX_VALUE));
        verify(mocks);
    }

    public void testNoLastRun() {
        replay(mocks);
        Assert.assertEquals(extractTransform.incrementalEtl("0", "0"), -1);
        Assert.assertTrue(ExtractTransform.getIncrementalRunStartTime() >= 0);
        verify(mocks);
    }

    public void testNoChanges() {
        long futureSec = 9999999999L;
        replay(mocks);
        extractTransform.writeLastEtlRun(futureSec);
        Assert.assertEquals(extractTransform.incrementalEtl("0", "0"), -1);
        Assert.assertTrue(ExtractTransform.getIncrementalRunStartTime() >= 0);
        verify(mocks);
    }

    public void testBadRange1() {
        replay(mocks);
        Assert.assertEquals(
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), -1, Long.MAX_VALUE),
                Response.Status.BAD_REQUEST);
        verify(mocks);
    }

    public void testBadRange2() {
        replay(mocks);
        Assert.assertEquals(
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 1000, 999),
                Response.Status.BAD_REQUEST);
        verify(mocks);
    }

    public void testInvalidClassName() {
        replay(mocks);
        Assert.assertEquals(
                extractTransform.backfillEtl("NoSuchClass_Ihope", 0, Long.MAX_VALUE),
                Response.Status.NOT_FOUND);
        verify(mocks);
    }

    /**
     * Passes a non-existent directory for the last run file.
     */
    public void testInvalidLastEtlBadDir() {
        ExtractTransform.setDatafileDir(badDataDir);
        Assert.assertEquals(extractTransform.readLastEtlRun(), 0);
    }

    /**
     * Writes seconds or milliseconds, verify read is always seconds.
     */
    public void testReadLastEtlmSec() throws IOException {
        File file = new File(datafileDir, ExtractTransform.LAST_ETL_FILE);
        final long sec  = 1351112223L;
        final long mSec = 1351112223789L;
        FileUtils.write(file, String.valueOf(sec));
        Assert.assertEquals(extractTransform.readLastEtlRun(), sec);

        FileUtils.write(file, String.valueOf(mSec));
        Assert.assertEquals(extractTransform.readLastEtlRun(), sec);
    }

    /**
     * Takes the mutex, ETL cannot run.
     */
    public void testMutex() {
        replay(mocks);
        Assert.assertTrue(ExtractTransform.getMutex().tryAcquire());
        try {
            int recordCount = extractTransform.incrementalEtl("20130327155950", "20130327160200");
            Assert.assertEquals(recordCount, -1);
        } finally {
            ExtractTransform.getMutex().release();
        }
        verify(mocks);
    }

    public void testIsReady() {
        String etlDateStr = "20130215091500";
        File f = new File(datafileDir, etlDateStr + ExtractTransform.READY_FILE_SUFFIX);
        Assert.assertFalse(f.exists());
        extractTransform.writeIsReadyFile(etlDateStr);
        Assert.assertTrue(f.exists());
    }

    public void testOnDemandIncr1() {
        long startEtl = 1364411920L;

        expect(auditReaderDao.fetchAuditIds(eq(startEtl), anyLong())).andReturn(Collections.EMPTY_LIST);
        replay(mocks);

        extractTransform.writeLastEtlRun(startEtl);
        Assert.assertEquals(extractTransform.readLastEtlRun(), startEtl);

        Assert.assertEquals(extractTransform.incrementalEtl("0", "0"), 0);

        Assert.assertTrue(extractTransform.readLastEtlRun() > startEtl);
        verify(mocks);
    }

    public void testOnDemandIncr2() {
        long startEtl = 1364411920L;
        long endEtl = 1364411930L;
        String endEtlStr = ExtractTransform.secTimestampFormat.format(new Date(endEtl * 1000L));

        expect(auditReaderDao.fetchAuditIds(startEtl, endEtl)).andReturn(Collections.EMPTY_LIST);
        replay(mocks);

        extractTransform.writeLastEtlRun(startEtl);

        Assert.assertEquals(extractTransform.incrementalEtl("0", endEtlStr), 0);
        // Limited range etl should not update lastEtlRun file.
        Assert.assertEquals(extractTransform.readLastEtlRun(), startEtl);
        verify(mocks);
    }

    public void testOnDemandBackfill() {
        Class testClass = LabBatch.class;
        expect(productEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(priceItemEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(projectPersonEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectIrbEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectFundingEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectCohortEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(productOrderSampleEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(productOrderEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(productOrderAddOnEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(labBatchEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(labVesselEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(workflowConfigEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(labEventEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(riskItemEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(ledgerEntryCrossEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(ledgerEntryEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);

        replay(mocks);
        extractTransform.writeLastEtlRun(0L);
        extractTransform.entityIdRangeEtl(testClass.getName(), 0, 0);
        Assert.assertEquals(0L, extractTransform.readLastEtlRun());
        verify(mocks);
    }

    public void testBackfillDefaultEnd() {
        long startEtl = System.currentTimeMillis();
        Class testClass = LabBatch.class;
        expect(productEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(1);
        expect(priceItemEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(projectPersonEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectIrbEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectFundingEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(researchProjectCohortEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(productOrderSampleEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(productOrderEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(productOrderAddOnEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(labBatchEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(labVesselEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(workflowConfigEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(labEventEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(riskItemEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(ledgerEntryCrossEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);
        expect(ledgerEntryEtl.doEtl(eq(testClass), anyLong(), anyLong(), (String) anyObject())).andReturn(0);

        replay(mocks);
        extractTransform.writeLastEtlRun(0L);
        extractTransform.entityIdRangeEtl(testClass.getName(), 0, -1);
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
        Collection<Long> revIds = new ArrayList<Long>();
        expect(auditReaderDao.fetchAuditIds(eq(startEtlSec), anyLong())).andReturn(revIds);

        replay(mocks);
        extractTransform.writeLastEtlRun(startEtlSec);
        extractTransform.auditDateRangeEtl("0", "0");
        Assert.assertTrue(extractTransform.readLastEtlRun() > startEtlSec);
        verify(mocks);
    }

    public void testOnDemandIncremental() {
        final long startEtlSec = 1360000000L;
        Collection<Long> revIds = new ArrayList<Long>();
        revIds.add(1L);
        expect(auditReaderDao.fetchAuditIds(eq(startEtlSec), anyLong())).andReturn(revIds);
        expect(productEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(1);
        expect(priceItemEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(researchProjectEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(projectPersonEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(researchProjectIrbEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(researchProjectFundingEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(researchProjectCohortEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(productOrderSampleEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(productOrderEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(productOrderAddOnEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(labBatchEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(labVesselEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(workflowConfigEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(labEventEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(riskItemEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(ledgerEntryCrossEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);
        expect(ledgerEntryEtl.doEtl(eq(revIds), (String) anyObject())).andReturn(0);

        replay(mocks);
        extractTransform.writeLastEtlRun(startEtlSec);
        extractTransform.auditDateRangeEtl("0", "0");
        long etlEnd = extractTransform.readLastEtlRun();
        Assert.assertTrue(etlEnd >= startEtlSec);
        String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date(TimeUnit.SECONDS.toMillis(etlEnd)));
        Assert.assertTrue((new File(datafileDir, etlDateStr + ExtractTransform.READY_FILE_SUFFIX)).exists());

        verify(mocks);
    }
}



