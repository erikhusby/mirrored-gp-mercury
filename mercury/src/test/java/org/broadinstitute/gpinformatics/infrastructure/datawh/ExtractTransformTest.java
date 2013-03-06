package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.Query;
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

@Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
public class ExtractTransformTest extends Arquillian {
    private Log logger = LogFactory.getLog(getClass());
    private String datafileDir;
    private Map<String, GenericEntityEtl> auditTables = new HashMap<String, GenericEntityEtl>();
    public final long MSEC_IN_SEC = 1000L;

    @Inject
    private ExtractTransform extractTransform;
    @Inject
    private AuditReaderDao auditReaderDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @BeforeClass(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void beforeClass() throws Exception {
        datafileDir = System.getProperty("java.io.tmpdir");

        // Uncomment these when they reliably create etl events.
        //auditTables.put("MERCURY.LAB_EVENT_AUD", new EventEtl());
        //auditTables.put("MERCURY.LAB_BATCH_AUD", new LabBatchEtl());
        //auditTables.put("MERCURY.LAB_VESSEL_AUD", new LabVesselEtl());
        auditTables.put("ATHENA.BILLING_LEDGER_AUD", new BillingLedgerEtl());
        auditTables.put("ATHENA.RISK_ITEM_AUD", new RiskItemEtl());
        auditTables.put("ATHENA.PRICE_ITEM_AUD", new PriceItemEtl());
        auditTables.put("ATHENA.PRODUCT_AUD", new ProductEtl());
        auditTables.put("ATHENA.PRODUCT_ORDER_AUD", new ProductOrderEtl());
        auditTables.put("ATHENA.PRODUCT_ORDER_ADD_ON_AUD", new ProductOrderAddOnEtl());
        auditTables.put("ATHENA.PRODUCT_ORDER_SAMPLE_AUD", new ProductOrderSampleEtl());
        auditTables.put("ATHENA.RESEARCH_PROJECT_AUD", new ResearchProjectEtl());
        auditTables.put("ATHENA.RESEARCH_PROJECT_COHORT_AUD", new ResearchProjectCohortEtl());
        auditTables.put("ATHENA.RESEARCH_PROJECT_FUNDING_AUD", new ResearchProjectFundingEtl());
        auditTables.put("ATHENA.RESEARCH_PROJECTIRB_AUD", new ResearchProjectIrbEtl());
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void beforeMethod() throws Exception {
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    public void testEtl() throws Exception {
        // Gets the most recent audit rev timestamp for an etl entity.
        long mostRecentTimeSec = 0L;
        GenericEntityEtl mostRecentClass = null;
        for (String auditTable : auditTables.keySet()) {
            long auditTimeSec = fetchLatestAuditDate(auditTable);
            if (auditTimeSec > mostRecentTimeSec) {
                mostRecentTimeSec = auditTimeSec;
                mostRecentClass = auditTables.get(auditTable);
            }
        }
        if (mostRecentTimeSec == 0L) {
            logger.warn("No audit entities exist -- cannot test etl");
            return;
        }
        logger.debug("most recent entity: " + mostRecentTimeSec + ", " + mostRecentClass.getEntityClass() + ", " + mostRecentClass.getBaseFilename());

        extractTransform.writeLastEtlRun(mostRecentTimeSec);

        // Does incremental Etl.
        if (System.currentTimeMillis() / MSEC_IN_SEC == mostRecentTimeSec) {
            Thread.sleep(MSEC_IN_SEC);
        }
        int recordCount = extractTransform.incrementalEtl();

        // Must have at least one etl record.
        Assert.assertTrue(recordCount > 0);

        // Must have is_ready and at least one data file for the most recent class.
        long endEtlSec = extractTransform.readLastEtlRun();
        Assert.assertFalse(mostRecentTimeSec == endEtlSec);

        String fileTimestamp = ExtractTransform.secTimestampFormat.format(new Date(endEtlSec * MSEC_IN_SEC));
        final String datFileEnding = "_" + mostRecentClass.getBaseFilename() + ".dat";
        String isReadyFilename = fileTimestamp + ExtractTransform.READY_FILE_SUFFIX;

        Assert.assertTrue((new File(datafileDir, isReadyFilename)).exists());
        String dataFilename = fileTimestamp + datFileEnding;
        File datafile = new File(datafileDir, dataFilename);
        logger.info("expecting file " + datafile.getName());
        Assert.assertTrue(datafile.exists());

        // Extracts an entity id from the data file.
        long entityId = 0L;
        Reader reader = new FileReader(datafile);
        List<String> lines = IOUtils.readLines(reader);
        for (String line : lines) {
            // All data records start with: linenumber, etlDate, deletionFlag, entityId.
            String[] parts = line.split(",");
            // Looks for the first non-deletion record.
            if ("F".equals(parts[2])) {
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
        long backfillEtlStart = System.currentTimeMillis();
        Assert.assertEquals(Response.Status.NO_CONTENT,
                extractTransform.backfillEtl(mostRecentClass.getEntityClass().getName() , entityId - 1, entityId + 1));
        long backfillEtlEnd = System.currentTimeMillis();
        // Checks that the new data file contains the known entity id.
        boolean found = false;
        File[] files = EtlTestUtilities.getDirFiles(datafileDir, backfillEtlStart, backfillEtlEnd);
        for (File dataFile : files) {
            logger.debug("Found backfill etl file " + dataFile.getName());
            if (dataFile.getName().endsWith(datFileEnding)) {
                reader = new FileReader(dataFile);
                lines = IOUtils.readLines(reader);
                IOUtils.closeQuietly(reader);
                for (String line : lines) {
                    String[] parts = line.split(",");
                    if (entityId == Long.parseLong(parts[3])) {
                        found = true;
                        break;
                    }
                }
            }
        }
        Assert.assertTrue(found);
    }


    /**
     * Finds the date a recent audit rev, typically the most recent.
     * @return timestamp in seconds since start of the epoch
     */
    private long fetchLatestAuditDate(String audTableName) throws Exception {
        String queryString = "SELECT TO_CHAR(rev_date,'YYYYMMDDHH24MISS') FROM REV_INFO " +
                " WHERE rev_info_id = (SELECT MAX(rev) FROM " + audTableName + ")";
        Query query = auditReaderDao.getEntityManager().createNativeQuery(queryString);
        String result = (String)query.getSingleResult();
        long timeSec = ExtractTransform.secTimestampFormat.parse(result).getTime() / MSEC_IN_SEC;
        return timeSec;
    }

}

