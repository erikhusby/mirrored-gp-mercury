package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.RiskItem;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ProjectPerson;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectCohort;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtilityKeepScope;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.hibernate.SQLQuery;
import org.hibernate.type.LongType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.transaction.UserTransaction;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Container test of ExtractTransform.
 *
 * @author epolk
 */

@Test(enabled = true, groups = TestGroups.ALTERNATIVES, singleThreaded = true)
public class ExtractTransformTest extends Arquillian {
    private static final Log logger = LogFactory.getLog(ExtractTransform.class);
    private String datafileDir;
    public final long MSEC_IN_SEC = 1000L;
    private final String barcode = "TEST" + System.currentTimeMillis();

    // This list should contain every class that can be backfilled.
    private final Class[] backfillClasses = new Class[] {
            BillingSession.class,
            LabEvent.class,
            LabMetric.class,
            LabVessel.class,
            LedgerEntry.class,
            PriceItem.class,
            Product.class,
            ProductOrder.class,
            ProductOrderAddOn.class,
            ProductOrderSample.class,
            ProjectPerson.class,
            ResearchProject.class,
            ResearchProjectCohort.class,
            ResearchProjectFunding.class,
            ResearchProjectIRB.class,
            RiskItem.class,
            SequencingRun.class,
            WorkflowConfig.class,
    };

    @Inject
    private ExtractTransform extractTransform;
    @Inject
    private AuditReaderDao auditReaderDao;
    @Inject
    private ProductOrderSampleDao pdoSampleDao;
    @Inject
    private LabVesselDao labVesselDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DEV, "dev", SessionContextUtilityKeepScope.class);
    }

    @BeforeClass(groups = TestGroups.ALTERNATIVES)
    public void beforeClass() throws Exception {
        datafileDir = System.getProperty("java.io.tmpdir");
    }

    @BeforeMethod(groups = TestGroups.ALTERNATIVES)
    public void beforeMethod() throws Exception {
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    @Test(enabled = true, groups = TestGroups.ALTERNATIVES)
    public void testEtl() throws Exception {
        final BarcodedTube labVessel = new BarcodedTube(barcode);
        final String datFileEnding = "_lab_vessel.dat";

        // Writes and commits an entity to the db.  Envers requires the transaction to commit.
        final long startSec = System.currentTimeMillis() / MSEC_IN_SEC;
        final long startMSec = startSec * MSEC_IN_SEC;
        final String startEtl = ExtractTransform.formatTimestamp(new Date(startMSec));
        Assert.assertNotNull(utx);
        utx.begin();
        labVesselDao.persist(labVessel);
        labVesselDao.flush();
        utx.commit();

        // Pick up the ID
        final long entityId = labVessel.getLabVesselId();

        // Wait since incremental etl won't pick up entities in the current second.
        Thread.sleep(MSEC_IN_SEC);

        ExtractTransform.writeLastEtlRun(startSec);
        // Runs incremental etl from last_etl_run (i.e. startSec) to now.
        int recordCount = extractTransform.incrementalEtl("0", "0");
        final long endEtlMSec = ExtractTransform.readLastEtlRun() * MSEC_IN_SEC;
        Assert.assertTrue(recordCount > 0);

        // Finds the entity in a data file (may be more than one data file if another commit
        // hit in the small time window between startMsec and the incrementalEtl start).
        boolean found = searchEtlFile(datafileDir, datFileEnding, "F", entityId);
        Assert.assertTrue(found);
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        // Runs an incremental etl that starts after the entity was created.
        // Entity create should not be in the etl file, if any was created.
        String endEtl = ExtractTransform.formatTimestamp(new Date(endEtlMSec));
        extractTransform.incrementalEtl(endEtl, "0");
        Assert.assertFalse(searchEtlFile(datafileDir, datFileEnding, "F", entityId));
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        // Runs backfill ETL on a range of entity ids that includes the known entity id.
        // Checks that the new data file contains the known entity id.

        Response response = extractTransform.backfillEtl(LabVessel.class.getName(), entityId, entityId);
        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Assert.assertTrue(searchEtlFile(datafileDir, datFileEnding, "F", entityId));
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        // Deletes the entity.
        utx.begin();
        // Gets the entity.
        LabVessel entity = labVesselDao.findByIdentifier(barcode);
        Assert.assertNotNull(entity);
        labVesselDao.remove(entity);
        labVesselDao.flush();
        utx.commit();
        Thread.sleep(MSEC_IN_SEC);

        // Incremental etl should pick up the delete and not the earlier create.
        recordCount = extractTransform.incrementalEtl(startEtl, "0");
        Assert.assertTrue(recordCount > 0);
        Assert.assertFalse(searchEtlFile(datafileDir, datFileEnding, "F", entityId));
        Assert.assertTrue(searchEtlFile(datafileDir, datFileEnding, "T", entityId));
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    // Returns existing riskItemId and its productOrderSampleId.  Set deleted=true for a deleted risk item.
    private Long[] getRiskJoin(boolean deleted) {
        // The join with revchanges is needed to make audits visible to ETL.  Currently the audits before
        // 8/6/14 (rev 330051) do not have joins with revchanges, so this test must not pick them up.
        String queryString;
        if (deleted) {
            queryString = "select risk_item_id as id1, product_order_sample as id2, rev " +
                    " from ATHENA.PO_SAMPLE_RISK_JOIN_AUD where revtype = 2 and rownum = 1 " +
                    " and exists (select 1 from revchanges rc where rc.rev = ATHENA.PO_SAMPLE_RISK_JOIN_AUD.rev)";
        } else {
            queryString = "select risk_item_id as id1, product_order_sample as id2, rev " +
                    " from ATHENA.PO_SAMPLE_RISK_JOIN_AUD p1  where revtype = 0 and rownum = 1 " +
                    " and exists (select 1 from revchanges rc where rc.rev = p1.rev)" +
                    " and not exists (select 1 from ATHENA.PO_SAMPLE_RISK_JOIN_AUD p2 " +
                    " where p2.risk_item_id = p1.risk_item_id and p2.revtype = 2)";
        }
        return getJoinedIds(queryString);
    }

    // Returns existing ledgerId and its productOrderSampleId.  Set deleted=true for a deleted ledger item.
    private Long[] getLedgerJoin(boolean deleted) {
        // The join with revchanges is needed to make audits visible to ETL.  Currently the audits before
        // 8/6/14 (rev 330051) do not have joins with revchanges, so this test must not pick them up.
        String queryString;
        if (deleted) {
            queryString = "select p1.ledger_id as id1, p2.product_order_sample_id as id2, p1.rev" +
                    " from ATHENA.BILLING_LEDGER_AUD p1, ATHENA.BILLING_LEDGER_AUD p2" +
                    " where p1.revtype = 2 and p2.revtype = 0 and p1.ledger_id = p2.ledger_id " +
                    " and exists (select 1 from revchanges rc where rc.rev = p1.rev)" +
                    " and rownum = 1";
        } else {
            queryString = "select ledger_id as id1, product_order_sample_id as id2, rev " +
                    " from ATHENA.BILLING_LEDGER_AUD p1 where revtype = 0 and rownum = 1" +
                    " and exists (select 1 from revchanges rc where rc.rev = p1.rev)" +
                    " and not exists (select 1 from ATHENA.BILLING_LEDGER_AUD p2" +
                    " where p2.ledger_id = p1.ledger_id and p2.revtype = 2)";
        }
        return getJoinedIds(queryString);
    }

    private Long[] getJoinedIds(String queryString) {
        Query query = pdoSampleDao.getEntityManager().createNativeQuery(queryString);
        query.unwrap(SQLQuery.class)
                .addScalar("id1", LongType.INSTANCE)
                .addScalar("id2", LongType.INSTANCE)
                .addScalar("rev", LongType.INSTANCE);
        Object[] obj = (Object[])query.getSingleResult();
        return new Long[]{(Long)obj[0], (Long)obj[1], (Long)obj[2]};
    }

    @Test(enabled = true, groups = TestGroups.ALTERNATIVES)
    public void testUndeletedRiskOnDevDb() throws Exception {
        Long[] ids = getRiskJoin(false);
        if (ids == null || ids.length < 3) {
            logger.info("Skipping test, cannot find undeleted product order risk");
            return;
        }

        // Tests backfill etl.
        long entityId = ids[0];
        long pdoSampleId = ids[1];
        Response response = extractTransform.backfillEtl(RiskItem.class.getName(), entityId, entityId);
        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());

        final String datFileEnding = "_product_order_sample_risk.dat";
        Assert.assertTrue(searchEtlFile(datafileDir, datFileEnding, "F", pdoSampleId));
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        // Test incremental etl.
        long rev = ids[2];
        RevInfo revInfo = auditReaderDao.findById(RevInfo.class, rev);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.formatTimestamp(revInfo.getRevDate());
        long startMsec = ExtractTransform.parseTimestamp(startEtl).getTime();
        long endMsec = startMsec + MSEC_IN_SEC;
        String endEtl = ExtractTransform.formatTimestamp(new Date(endMsec));
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        Assert.assertTrue(recordCount > 0);
        // Filename is "back dated".
        Assert.assertTrue(searchEtlFile(datafileDir, datFileEnding, "F", pdoSampleId));
    }

    @Test(enabled = true, groups = TestGroups.ALTERNATIVES)
    public void testDeletedRiskOnDevDb() throws Exception {
        Long[] ids = getRiskJoin(true);
        if (ids == null || ids.length < 3) {
            logger.info("Skipping test, cannot find deleted product order risk");
            return;
        }
        // entityId = ids[0];
        long pdoSampleId = ids[1];
        long rev = ids[2];
        RevInfo revInfo = auditReaderDao.findById(RevInfo.class, rev);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.formatTimestamp(revInfo.getRevDate());
        long startMsec = ExtractTransform.parseTimestamp(startEtl).getTime();
        long endMsec = startMsec + MSEC_IN_SEC;
        String endEtl = ExtractTransform.formatTimestamp(new Date(endMsec));
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        Assert.assertTrue(recordCount > 0);

        final String datFileEnding = "_product_order_sample_risk.dat";
        Assert.assertTrue(searchEtlFile(datafileDir, datFileEnding, "T", pdoSampleId));
    }


    @Test(enabled = true, groups = TestGroups.ALTERNATIVES)
    public void testUndeletedLedgerOnDevDb() throws Exception {
        Long[] ids = getLedgerJoin(false);
        if (ids == null || ids.length < 3) {
            logger.info("Skipping test, cannot find undeleted product order ledger entry");
            return;
        }

        // Tests backfill etl.
        long entityId = ids[0];
        long pdoSampleId = ids[1];
        Response response = extractTransform.backfillEtl(LedgerEntry.class.getName(), entityId, entityId);
        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());

        final String datFileEnding = "product_order_sample_bill.dat";
        Assert.assertTrue(searchEtlFile(datafileDir, datFileEnding, "F", pdoSampleId));

        EtlTestUtilities.deleteEtlFiles(datafileDir);
        Assert.assertFalse(searchEtlFile(datafileDir, datFileEnding, "F", pdoSampleId));

        // Test incremental etl.
        long rev = ids[2];
        RevInfo revInfo = auditReaderDao.findById(RevInfo.class, rev);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.formatTimestamp(revInfo.getRevDate());
        long startMsec = ExtractTransform.parseTimestamp(startEtl).getTime();
        long endMsec = startMsec + MSEC_IN_SEC;
        String endEtl = ExtractTransform.formatTimestamp(new Date(endMsec));
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        Assert.assertTrue(recordCount > 0);
        // Filename is "back dated".
        Assert.assertTrue(searchEtlFile(datafileDir, datFileEnding, "F", pdoSampleId));
    }

    @Test(enabled = true, groups = TestGroups.ALTERNATIVES)
    public void testDeletedLedgerOnDevDb() throws Exception {
        Long[] ids = getLedgerJoin(true);
        if (ids == null || ids.length < 3 || ids[0] == null) {
            logger.info("Skipping test, cannot find deleted product order ledger entry");
            return;
        }
        // entityId = ids[0];
        long pdoSampleId = ids[1];
        long rev = ids[2];
        RevInfo revInfo = auditReaderDao.findById(RevInfo.class, rev);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.formatTimestamp(revInfo.getRevDate());
        long startMsec = ExtractTransform.parseTimestamp(startEtl).getTime();
        long endMsec = startMsec + MSEC_IN_SEC;
        String endEtl = ExtractTransform.formatTimestamp(new Date(endMsec));
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        Assert.assertTrue(recordCount > 0);

        final String datFileEnding = "_product_order_sample_bill.dat";
        Assert.assertTrue(searchEtlFile(datafileDir, datFileEnding, "T", pdoSampleId));
    }

    @Test(enabled = true, groups = TestGroups.ALTERNATIVES)
    public void testBackfill() throws Exception {
        for (Class backfillClass : backfillClasses) {
            Response response = extractTransform.backfillEtl(backfillClass.getName(), 1, 1);
            Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            Assert.assertTrue(((String)response.getEntity()).toLowerCase().contains("created"));
        }
    }


    /**
     * Looks for etl files having name timestamps in the given range, then searches them for a record having
     * the given isDelete and entityId values.
     */
    public static boolean searchEtlFile(String datafileDir, String datFileEnding, String isDelete, long entityId)
            throws IOException {

        for (File file : EtlTestUtilities.getEtlFiles(datafileDir)) {
            if (file.getName().endsWith(datFileEnding)) {
                Reader reader = new FileReader(file);
                List<String> lines = IOUtils.readLines(reader);
                IOUtils.closeQuietly(reader);
                for (String line : lines) {
                    // All data records start with: lineNumber, etlDate, deletionFlag, entityId.
                    String[] parts = line.split(",");
                    if (isDelete.equals(parts[2]) && entityId == Long.parseLong(parts[3])) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
