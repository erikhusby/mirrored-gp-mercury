package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.Date;

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
    private String etlDateStr = "20130215091500";

    private ExtractTransform extractTransform;
    private AuditReaderDao auditReaderDao;
    private ProductEtl productEtl;
    private ProductOrderEtl productOrderEtl;
    private ProductOrderSampleEtl productOrderSampleEtl;
    private ProductOrderSampleStatusEtl productOrderSampleStatusEtl;
    private ProductOrderStatusEtl productOrderStatusEtl;
    private PriceItemEtl priceItemEtl;
    private ResearchProjectEtl researchProjectEtl;
    private ResearchProjectStatusEtl researchProjectStatusEtl;
    private ProjectPersonEtl projectPersonEtl;
    private ResearchProjectIrbEtl researchProjectIrbEtl;
    private ResearchProjectFundingEtl researchProjectFundingEtl;
    private ResearchProjectCohortEtl researchProjectCohortEtl;
    private ProductOrderAddOnEtl productOrderAddOnEtl;
    private EventEtl eventEtl;
    private WorkflowConfigEtl workflowConfigEtl;
    private LabBatchEtl labBatchEtl;
    private LabVesselEtl labVesselEtl;

    @BeforeClass
    public void beforeClass() throws Exception {
        datafileDir = System.getProperty("java.io.tmpdir");
        badDataDir = datafileDir + System.getProperty("file.separator") + nowMsec;
        extractTransform = new ExtractTransform();
        auditReaderDao = createMock(AuditReaderDao.class);
        extractTransform.setAuditReaderDao(auditReaderDao);
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);
        reset(auditReaderDao);
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    public void testInvalidDir1() {
        replay(auditReaderDao);
        ExtractTransform.setDatafileDir(null);
        Assert.assertEquals(-1, extractTransform.incrementalEtl());
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR,
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 0, Long.MAX_VALUE));
        verify(auditReaderDao);
    }

    public void testInvalidDir2() {
        replay(auditReaderDao);
        ExtractTransform.setDatafileDir("");
        Assert.assertEquals(-1, extractTransform.incrementalEtl());
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR,
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 0, Long.MAX_VALUE));
        verify(auditReaderDao);
    }

    public void testInvalidDir3() {
        replay(auditReaderDao);
        ExtractTransform.setDatafileDir(badDataDir);
        Assert.assertEquals(-1, extractTransform.incrementalEtl());
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR,
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 0, Long.MAX_VALUE));
        verify(auditReaderDao);
    }

    public void testNoChanges() {
        long futureMsec = 9999999999000L;
        replay(auditReaderDao);
        extractTransform.writeLastEtlRun(futureMsec);
        Assert.assertEquals(0, extractTransform.incrementalEtl());
        Assert.assertTrue(ExtractTransform.getIncrementalRunStartTime() >= 0);
        verify(auditReaderDao);
    }

    public void testBadRange1() {
        replay(auditReaderDao);
        Assert.assertEquals(Response.Status.BAD_REQUEST,
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), -1, Long.MAX_VALUE));
        verify(auditReaderDao);
    }

    public void testBadRange2() {
        replay(auditReaderDao);
        Assert.assertEquals(Response.Status.BAD_REQUEST,
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 1000, 999));
        verify(auditReaderDao);
    }

    public void testInvalidClassName() {
        replay(auditReaderDao);
        Assert.assertEquals(Response.Status.NOT_FOUND,
                extractTransform.backfillEtl("NoSuchClass_Ihope", 0, Long.MAX_VALUE));
        verify(auditReaderDao);
    }

    /** Passes a non-existent directory for the last run file. */
    public void testInvalidLastEtlBadDir() {
        replay(auditReaderDao);
        extractTransform.setDatafileDir(badDataDir);
        Assert.assertEquals(0L, extractTransform.readLastEtlRun());
        verify(auditReaderDao);
    }

    /** Writes an unparsable timestamp. */
    public void testReadLastEtlUnparsable() throws IOException {
        replay(auditReaderDao);
        File file = new File(datafileDir, ExtractTransform.LAST_ETL_FILE);
        FileUtils.write(file, "abcedfg");

        Assert.assertEquals(0L, extractTransform.readLastEtlRun());
        verify(auditReaderDao);
    }

    /** Takes the mutex, ETL cannot run. */
    public void testMutex() {
        replay(auditReaderDao);
        Assert.assertTrue(ExtractTransform.getMutex().tryAcquire());
        try {
            int recordCount = extractTransform.incrementalEtl();
            Assert.assertEquals(-1, recordCount);
        } finally {
            ExtractTransform.getMutex().release();
        }
        verify(auditReaderDao);
    }
}


