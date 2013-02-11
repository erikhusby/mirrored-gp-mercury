package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Container test of ExtractTransform.
 *
 * @author epolk
 */

@Test(enabled =  true, groups = TestGroups.EXTERNAL_INTEGRATION)
public class ExtractTransformTest extends Arquillian {
    private Logger logger = Logger.getLogger(getClass());
    private String datafileDir;
    private Map<String, GenericEntityEtl> auditTables = new HashMap<String, GenericEntityEtl>();

    @Inject
    private ExtractTransform extractTransform;
    @Inject
    private AuditReaderDao auditReaderDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @BeforeClass
    public void beforeClass() throws Exception {
        datafileDir = System.getProperty("java.io.tmpdir");

        auditTables.put("LAB_EVENT_AUD", new EventEtl());
        auditTables.put("LAB_BATCH_AUD", new LabBatchEtl());
        auditTables.put("LAB_VESSEL_AUD", new LabVesselEtl());
        auditTables.put("PRICE_ITEM_AUD", new PriceItemEtl());
        auditTables.put("PRODUCT_AUD", new ProductEtl());
        auditTables.put("PRODUCT_ORDER_AUD", new ProductOrderEtl());
        auditTables.put("PRODUCT_ORDER_ADD_ON_AUD", new ProductOrderAddOnEtl());
        auditTables.put("PRODUCT_ORDER_SAMPLE_AUD", new ProductOrderSampleEtl());
        auditTables.put("RESEARCH_PROJECT_AUD", new ResearchProjectEtl());
        auditTables.put("RESEARCH_PROJECT_COHORT_AUD", new ResearchProjectCohortEtl());
        auditTables.put("RESEARCH_PROJECT_FUNDING_AUD", new ResearchProjectFundingEtl());
        auditTables.put("RESEARCH_PROJECTIRB_AUD", new ResearchProjectIrbEtl());

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

    public void testEtl() throws Exception {
        // Gets the most recent audit rev timestamp for an etl entity.
        long mostRecentTimestamp = 0L;
        GenericEntityEtl mostRecentClass = null;
        for (String auditTable : auditTables.keySet()) {
            long auditTimestamp = auditReaderDao.fetchLatestAuditDate(auditTable);
            if (auditTimestamp > mostRecentTimestamp) {
                mostRecentTimestamp = auditTimestamp;
                mostRecentClass = auditTables.get(auditTable);
            }
        }
        if (mostRecentTimestamp == 0L) {
            logger.warn("No audit entities exist -- cannot test etl");
            return;
        }
        logger.debug("most recent entity: " + mostRecentTimestamp + ", " + mostRecentClass.getEntityClass() + ", " + mostRecentClass.getBaseFilename());
        // Sets up the last etl file to be just before the most recent audit rev.  Sql timestamp resolution is 10 mSec.
        long startEtl = mostRecentTimestamp - 10;
        extractTransform.writeLastEtlRun(startEtl);

        // Does incremental Etl.
        int recordCount = extractTransform.incrementalEtl();

        // Must have at least one etl record.
        Assert.assertTrue(recordCount > 0);

        // Must have is_ready and at least one data file for the most recent class.
        long endEtl = extractTransform.readLastEtlRun();
        Assert.assertFalse(startEtl == endEtl);

        String fileTimestamp = ExtractTransform.secTimestampFormat.format(new Date(endEtl));
        String isReadyFilename = fileTimestamp + ExtractTransform.READY_FILE_SUFFIX;
        Assert.assertTrue((new File(datafileDir, isReadyFilename)).exists());
        String dataFilename = fileTimestamp + "_" + mostRecentClass.getBaseFilename() + ".dat";
        File datafile = new File(datafileDir, dataFilename);
        Assert.assertTrue(datafile.exists());

        // Extracts an entity id from the data file.
        long entityId = 0L;
        Reader reader = new FileReader(datafile);
        List<String> lines = IOUtils.readLines(reader);
        for (String line : lines) {
            // All data records start with: linenumber, etlDate, deletionFlag, entityId.
            String[] parts = line.split(",");
            // Looks for the first non-deletion record.
            if ("T".equals(parts[2])) {
                entityId = Long.parseLong(parts[3]);
                break;
            }
        }
        IOUtils.closeQuietly(reader);
        if (entityId == 0L) {
            logger.warn("Etl records are all deletions -- cannot test backfill etl.");
            return;
        }

        // Deletes all data files.
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        // Runs backfill ETL on a range of entity ids that includes the known entity id.
        Assert.assertEquals(Response.Status.NO_CONTENT,
                extractTransform.backfillEtl(mostRecentClass.getEntityClass().getName() , entityId - 1, entityId + 1));

        // Checks that the new data file contains the known entity id.
        boolean found = false;
        datafile = new File(datafileDir, dataFilename);
        Assert.assertTrue(datafile.exists());
        reader = new FileReader(datafile);
        lines = IOUtils.readLines(reader);
        for (String line : lines) {
            String[] parts = line.split(",");
            if (entityId == Long.parseLong(parts[3])) {
                found = true;
                break;
            }
        }
        IOUtils.closeQuietly(reader);
        Assert.assertTrue(found);


        auditReaderDao.fetchLatestAuditDate("PRODUCT_ORDER_SAMPLE");
    }

}

