package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.FileUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.hibernate.envers.RevisionType;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.*;

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
    private Deployment deployment;
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
        createMocks();

    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }


    private void createMocks() {
        deployment = createMock(Deployment.class);
        extractTransform.setDeployment(deployment);

        auditReaderDao = createMock(AuditReaderDao.class);
        extractTransform.setAuditReaderDao(auditReaderDao);

        productEtl = createMock(ProductEtl.class);
        extractTransform.setProductEtl(productEtl);

        productOrderEtl = createMock(ProductOrderEtl.class);
        extractTransform.setProductOrderEtl(productOrderEtl);

        productOrderSampleEtl = createMock(ProductOrderSampleEtl.class);
        extractTransform.setProductOrderSampleEtl(productOrderSampleEtl);

        productOrderSampleStatusEtl = createMock(ProductOrderSampleStatusEtl.class);
        extractTransform.setProductOrderSampleStatusEtl(productOrderSampleStatusEtl);

        productOrderStatusEtl = createMock(ProductOrderStatusEtl.class);
        extractTransform.setProductOrderStatusEtl(productOrderStatusEtl);

        priceItemEtl = createMock(PriceItemEtl.class);
        extractTransform.setPriceItemEtl(priceItemEtl);

        researchProjectEtl = createMock(ResearchProjectEtl.class);
        extractTransform.setResearchProjectEtl(researchProjectEtl);

        researchProjectStatusEtl = createMock(ResearchProjectStatusEtl.class);
        extractTransform.setResearchProjectStatusEtl(researchProjectStatusEtl);

        projectPersonEtl = createMock(ProjectPersonEtl.class);
        extractTransform.setProjectPersonEtl(projectPersonEtl);

        researchProjectIrbEtl = createMock(ResearchProjectIrbEtl.class);
        extractTransform.setResearchProjectIrbEtl(researchProjectIrbEtl);

        researchProjectFundingEtl = createMock(ResearchProjectFundingEtl.class);
        extractTransform.setResearchProjectFundingEtl(researchProjectFundingEtl);

        researchProjectCohortEtl = createMock(ResearchProjectCohortEtl.class);
        extractTransform.setResearchProjectCohortEtl(researchProjectCohortEtl);

        productOrderAddOnEtl = createMock(ProductOrderAddOnEtl.class);
        extractTransform.setProductOrderAddOnEtl(productOrderAddOnEtl);

        eventEtl = createMock(EventEtl.class);
        extractTransform.setEventEtl(eventEtl);

        workflowConfigEtl = createMock(WorkflowConfigEtl.class);
        extractTransform.setWorkflowConfigEtl(workflowConfigEtl);

        labBatchEtl = createMock(LabBatchEtl.class);
        extractTransform.setLabBatchEtl(labBatchEtl);

        labVesselEtl = createMock(LabVesselEtl.class);
        extractTransform.setLabVesselEtl(labVesselEtl);
    }

    private void replayAll() {
        replay(deployment);
        replay(auditReaderDao);
        replay(productEtl);
        replay(productOrderEtl);
        replay(productOrderSampleEtl);
        replay(productOrderSampleStatusEtl);
        replay(productOrderStatusEtl);
        replay(priceItemEtl);
        replay(researchProjectEtl);
        replay(researchProjectStatusEtl);
        replay(projectPersonEtl);
        replay(researchProjectIrbEtl);
        replay(researchProjectFundingEtl);
        replay(researchProjectCohortEtl);
        replay(productOrderAddOnEtl);
        replay(eventEtl);
        replay(workflowConfigEtl);
        replay(labBatchEtl);
        replay(labVesselEtl);
    }

    private void verifyAll() {
        verify(deployment);
        verify(auditReaderDao);
        verify(productEtl);
        verify(productOrderEtl);
        verify(productOrderSampleEtl);
        verify(productOrderSampleStatusEtl);
        verify(productOrderStatusEtl);
        verify(priceItemEtl);
        verify(researchProjectEtl);
        verify(researchProjectStatusEtl);
        verify(projectPersonEtl);
        verify(researchProjectIrbEtl);
        verify(researchProjectFundingEtl);
        verify(researchProjectCohortEtl);
        verify(productOrderAddOnEtl);
        verify(eventEtl);
        verify(workflowConfigEtl);
        verify(labBatchEtl);
        verify(labVesselEtl);
    }

    private void resetAll() {
        reset(deployment);
        reset(auditReaderDao);
        reset(productEtl);
        reset(productOrderEtl);
        reset(productOrderSampleEtl);
        reset(productOrderSampleStatusEtl);
        reset(productOrderStatusEtl);
        reset(priceItemEtl);
        reset(researchProjectEtl);
        reset(researchProjectStatusEtl);
        reset(projectPersonEtl);
        reset(researchProjectIrbEtl);
        reset(researchProjectFundingEtl);
        reset(researchProjectCohortEtl);
        reset(productOrderAddOnEtl);
        reset(eventEtl);
        reset(workflowConfigEtl);
        reset(labBatchEtl);
        reset(labVesselEtl);
    }

    /**
     * Passes a blank and a non-existent directory for datafiles.
     */
    public void testInvalidDir() {
        replayAll();
        ExtractTransform.setDatafileDir(null);
        Assert.assertEquals(-1, extractTransform.incrementalEtl());
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR,
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 0, Long.MAX_VALUE));
        verifyAll();

        resetAll();
        ExtractTransform.setDatafileDir("");
        Assert.assertEquals(-1, extractTransform.incrementalEtl());
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR,
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 0, Long.MAX_VALUE));
        verifyAll();

        resetAll();
        ExtractTransform.setDatafileDir(badDataDir);
        Assert.assertEquals(-1, extractTransform.incrementalEtl());
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR,
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 0, Long.MAX_VALUE));
        verifyAll();
    }

    /**
     * Mathematically excludes changes by setting last ETL version impossibly high.
     */
    public void testNoChanges() {
        resetAll();
        extractTransform.writeLastEtlRun(Long.MAX_VALUE - 1);
        Assert.assertEquals(0, extractTransform.incrementalEtl());
        Assert.assertTrue(ExtractTransform.getIncrementalRunStartTime() >= 0);
        verifyAll();
    }

    public void testBadRange() {
        resetAll();
        Assert.assertEquals(Response.Status.BAD_REQUEST,
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), -1, Long.MAX_VALUE));
        verifyAll();

        resetAll();
        Assert.assertEquals(Response.Status.BAD_REQUEST,
                extractTransform.backfillEtl(ProductOrderSample.class.getName(), 1000, 999));
        verifyAll();
    }

    /**
     * Must supply fully qualified class name
     */
    public void testInvalidClassName() {
        resetAll();
        Assert.assertEquals(Response.Status.NOT_FOUND,
                extractTransform.backfillEtl("ProductOrderSample", 0, Long.MAX_VALUE));
        verifyAll();
    }

    /**
     * Passes a non-existent directory for the last run file.
     */
    public void testInvalidLastEtlBadDir() {
        resetAll();
        extractTransform.setDatafileDir(badDataDir);
        Assert.assertEquals(0L, extractTransform.readLastEtlRun());
        verifyAll();
    }

    /**
     * Writes an unparsable timestamp.
     */
    public void testReadLastEtlUnparsable() throws IOException {
        resetAll();
        File file = new File(datafileDir, ExtractTransform.LAST_ETL_FILE);
        FileUtils.write(file, "abcedfg");

        Assert.assertEquals(0L, extractTransform.readLastEtlRun());
        verifyAll();
    }

    /**
     * Takes the mutex, ETL cannot run.
     */
    public void testMutex() {
        resetAll();
        Assert.assertTrue(ExtractTransform.getMutex().tryAcquire());
        try {
            int recordCount = extractTransform.incrementalEtl();
            Assert.assertEquals(-1, recordCount);
        } finally {
            ExtractTransform.getMutex().release();
        }
        verifyAll();
    }
}


