package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.RiskItem;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtilityKeepScope;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
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
import javax.ws.rs.core.Response;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Container test of ExtractTransform for the long running tests excluded from continuous integration.
 */

@Test(groups = TestGroups.LONG_RUNNING, singleThreaded = true)
public class ExtractTransformLongRuningTest extends Arquillian {
    private static final Log logger = LogFactory.getLog(ExtractTransform.class);
    private String datafileDir;
    public final long MSEC_IN_SEC = 1000L;
    private Long[] longLabEventIds = null;
    private Long[] longPdoSampleIds = null;
    private Long[] longBackfillPdoSampleIds = null;
    private Long[] deletedRiskIds = null;
    private Long[] riskIds = null;
    private Long[] deletedLedgerIds = null;
    private Long[] ledgerIds = null;

    @Inject
    private ExtractTransform extractTransform;
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private AuditReaderDao auditReaderDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DEV, "dev", SessionContextUtilityKeepScope.class);
    }

    @BeforeClass
    public void beforeClass() throws Exception {
        datafileDir = System.getProperty("java.io.tmpdir");
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);
        // One time setup.
        if (labVesselDao != null && longLabEventIds == null) {
            // The join with revchanges is needed to make audits visible to ETL.  Currently the audits before
            // 8/6/14 (rev 330051) do not have joins with revchanges, so this test must not pick them up.
            longLabEventIds = getJoinedIds("select id1, id2, rev from "
                    + "(select count(*), min(lab_event_id) id1, max(lab_event_id) id2, rev from lab_event_aud "
                    + "where lab_event_type = 'INFINIUM_AUTOCALL_ALL_STARTED' and revtype != 2 "
                    + "and exists (select 1 from revchanges rc where rc.rev = lab_event_aud.rev) "
                    + "group by rev having count(*) > 1000 "
                    + "order by 1) where rownum = 1");
            Assert.assertFalse(longLabEventIds == null || longLabEventIds.length < 3);

            longPdoSampleIds = getJoinedIds("select id1, id2, rev from "
                    + "(select count(*), min(product_order_sample_id) id1, max(product_order_sample_id) id2, rev "
                    + "from athena.product_order_sample_aud "
                    + "where revtype = 1 and delivery_status != 'ABANDONED' "
                    + "and exists (select 1 from revchanges rc where rc.rev = athena.product_order_sample_aud.rev) "
                    + "group by rev having count(*) > 8250 "
                    + "order by 1, 4 desc) where rownum = 1");
            Assert.assertFalse(longPdoSampleIds == null || longPdoSampleIds.length < 3);

            longBackfillPdoSampleIds = getJoinedIds(
                    "select min(product_order_sample_id) id1, max(product_order_sample_id) id2, min(rev) rev from "
                            + "(select product_order_sample_id, rev "
                            + "from athena.product_order_sample_aud where rev = 570528 "
                            + "and rownum < 6500 order by 1)");
            Assert.assertFalse(longPdoSampleIds == null || longPdoSampleIds.length < 3);

        }
        // One time setup.
        if (auditReaderDao != null && deletedRiskIds == null) {

            deletedRiskIds = getJoinedIds("select risk_item_id as id1, product_order_sample as id2, rev  from "
                    + "(select risk_item_id, product_order_sample, rev "
                    + " from ATHENA.PO_SAMPLE_RISK_JOIN_AUD "
                    + " where revtype = 2 "
                    + " and exists (select 1 from revchanges rc where rc.rev = ATHENA.PO_SAMPLE_RISK_JOIN_AUD.rev) "
                    + " and rev in (select rev from ATHENA.product_order_sample_aud "
                    + "             group by rev having count(*) between 10 and 100) "
                    + ") where rownum = 1");
            Assert.assertFalse(deletedRiskIds == null || deletedRiskIds.length < 3);

            riskIds = getJoinedIds("select risk_item_id as id1, product_order_sample as id2, rev  from " +
                    " (select risk_item_id, product_order_sample, rev " +
                    "  from ATHENA.PO_SAMPLE_RISK_JOIN_AUD p1 " +
                    "  where revtype = 0 " +
                    "  and exists (select 1 from revchanges rc where rc.rev = p1.rev)" +
                    "  and not exists (select 1 from ATHENA.PO_SAMPLE_RISK_JOIN_AUD p2 " +
                    "                  where p2.risk_item_id = p1.risk_item_id and p2.revtype = 2) " +
                    "  and rev in (select rev from ATHENA.product_order_sample_aud " +
                    "              group by rev having count(*) between 10 and 100) " +
                    " ) where rownum = 1");
            Assert.assertFalse(riskIds == null || riskIds.length < 3);

            deletedLedgerIds = getJoinedIds("select p1.ledger_id as id1, p2.product_order_sample_id as id2, p1.rev" +
                    " from ATHENA.BILLING_LEDGER_AUD p1, ATHENA.BILLING_LEDGER_AUD p2" +
                    " where p1.revtype = 2 and p2.revtype = 0 and p1.ledger_id = p2.ledger_id " +
                    " and exists (select 1 from revchanges rc where rc.rev = p1.rev)" +
                    " and rownum = 1");
            Assert.assertFalse(deletedLedgerIds == null || deletedLedgerIds.length < 3);

            ledgerIds = getJoinedIds("select ledger_id as id1, product_order_sample_id as id2, rev " +
                    " from ATHENA.BILLING_LEDGER_AUD p1 where revtype = 0 and rownum = 1" +
                    " and exists (select 1 from revchanges rc where rc.rev = p1.rev)" +
                    " and not exists (select 1 from ATHENA.BILLING_LEDGER_AUD p2" +
                    " where p2.ledger_id = p1.ledger_id and p2.revtype = 2)");
            Assert.assertFalse(ledgerIds == null || ledgerIds.length < 3);
        }
    }

    private Long[] getJoinedIds(String queryString) {
        Query query = labVesselDao.getEntityManager().createNativeQuery(queryString);
        query.unwrap(SQLQuery.class)
                .addScalar("id1", LongType.INSTANCE)
                .addScalar("id2", LongType.INSTANCE)
                .addScalar("rev", LongType.INSTANCE);
        Object[] obj = (Object[])query.getSingleResult();
        return new Long[]{(Long)obj[0], (Long)obj[1], (Long)obj[2]};
    }

    @Test
    public void testLongRunningLabEventBackfillEtl() throws Exception {
        // Tests lab event etl which has more complex processing.
        long startRun = System.currentTimeMillis();
        Response response1 = extractTransform.backfillEtl(LabEvent.class.getName(), longLabEventIds[0],
                longLabEventIds[1]);
        logger.info("LabEvent backfill etl ran " + (System.currentTimeMillis() - startRun) / 60000 + " minutes");
        Assert.assertEquals(response1.getStatus(), Response.Status.OK.getStatusCode());
        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, "_event_fact.dat", "F", longLabEventIds[0]));
    }

    @Test
    public void testLongRunningLabEventIncrementalEtl() throws Exception {
        // Tests lab event etl which has more complex processing.
        RevInfo revInfo = labVesselDao.findById(RevInfo.class, longLabEventIds[2]);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.formatTimestamp(revInfo.getRevDate());
        String endEtl = ExtractTransform.formatTimestamp(
                new Date(ExtractTransform.parseTimestamp(startEtl).getTime() + MSEC_IN_SEC));
        long startRun = System.currentTimeMillis();
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        logger.info("LabEvent incremental etl ran " + (System.currentTimeMillis() - startRun) / 60000 + " minutes");
        Assert.assertTrue(recordCount > 0);
        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, "_event_fact.dat", "F", longLabEventIds[0]));
    }

    @Test
    public void testLongRunningPdoSampleBackfillEtl() throws Exception {
        long startRun = System.currentTimeMillis();
        Response response = extractTransform.backfillEtl(ProductOrderSample.class.getName(),
                longBackfillPdoSampleIds[0], longBackfillPdoSampleIds[1]);
        logger.info("PdoSample backfill etl ran " + (System.currentTimeMillis() - startRun) / 60000 + " minutes");
        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, "_product_order_sample.dat", "F",
                longBackfillPdoSampleIds[1]));
    }

    @Test
    public void testLongRunningPdoSampleIncrementalEtl() throws Exception {
        RevInfo revInfo = labVesselDao.findById(RevInfo.class, longPdoSampleIds[2]);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.formatTimestamp(revInfo.getRevDate());
        String endEtl = ExtractTransform.formatTimestamp(
                new Date(ExtractTransform.parseTimestamp(startEtl).getTime() + MSEC_IN_SEC));
        long startRun = System.currentTimeMillis();
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        logger.info("PdoSample incremental etl ran " + (System.currentTimeMillis() - startRun) / 60000 + " minutes");
        Assert.assertTrue(recordCount > 0);
        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, "_product_order_sample.dat", "F",
                longPdoSampleIds[0]));
    }

    @Test(enabled = true, groups = TestGroups.ALTERNATIVES)
    public void testUndeletedRiskOnDevDb() throws Exception {
        // Tests backfill etl.
        long entityId = riskIds[0];
        long pdoSampleId = riskIds[1];
        Response response = extractTransform.backfillEtl(RiskItem.class.getName(), entityId, entityId);
        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());

        final String datFileEnding = "_product_order_sample_risk.dat";
        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, datFileEnding, "F", pdoSampleId));

        // Test incremental etl.
        EtlTestUtilities.deleteEtlFiles(datafileDir);
        long rev = riskIds[2];
        RevInfo revInfo = auditReaderDao.findById(RevInfo.class, rev);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.formatTimestamp(revInfo.getRevDate());
        long startMsec = ExtractTransform.parseTimestamp(startEtl).getTime();
        long endMsec = startMsec + MSEC_IN_SEC;
        String endEtl = ExtractTransform.formatTimestamp(new Date(endMsec));
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        Assert.assertTrue(recordCount > 0);
        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, datFileEnding, "F", pdoSampleId));
    }

    @Test(enabled = true, groups = TestGroups.ALTERNATIVES)
    public void testDeletedRiskOnDevDb() throws Exception {
        long pdoSampleId = deletedRiskIds[1];
        long rev = deletedRiskIds[2];
        RevInfo revInfo = auditReaderDao.findById(RevInfo.class, rev);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.formatTimestamp(revInfo.getRevDate());
        long startMsec = ExtractTransform.parseTimestamp(startEtl).getTime();
        long endMsec = startMsec + MSEC_IN_SEC;
        String endEtl = ExtractTransform.formatTimestamp(new Date(endMsec));
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        Assert.assertTrue(recordCount > 0);
        final String datFileEnding = "_product_order_sample_risk.dat";
        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, datFileEnding, "T", pdoSampleId));
    }


    @Test(enabled = true, groups = TestGroups.ALTERNATIVES)
    public void testUndeletedLedgerOnDevDb() throws Exception {
        // Tests backfill etl.
        long entityId = ledgerIds[0];
        long pdoSampleId = ledgerIds[1];
        Response response = extractTransform.backfillEtl(LedgerEntry.class.getName(), entityId, entityId + 2000);
        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());

        final String datFileEnding = "product_order_sample_bill.dat";
        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, datFileEnding, "F", pdoSampleId));

        EtlTestUtilities.deleteEtlFiles(datafileDir);
        Assert.assertFalse(ExtractTransformTest.searchEtlFile(datafileDir, datFileEnding, "F", pdoSampleId));

        // Test incremental etl.
        long rev = ledgerIds[2];
        RevInfo revInfo = auditReaderDao.findById(RevInfo.class, rev);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.formatTimestamp(revInfo.getRevDate());
        long startMsec = ExtractTransform.parseTimestamp(startEtl).getTime();
        long endMsec = startMsec + MSEC_IN_SEC;
        String endEtl = ExtractTransform.formatTimestamp(new Date(endMsec));
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        Assert.assertTrue(recordCount > 0);
        // Filename is "back dated".
        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, datFileEnding, "F", pdoSampleId));
    }

    @Test(enabled = true, groups = TestGroups.ALTERNATIVES)
    public void testDeletedLedgerOnDevDb() throws Exception {
        long pdoSampleId = deletedLedgerIds[1];
        long rev = deletedLedgerIds[2];
        RevInfo revInfo = auditReaderDao.findById(RevInfo.class, rev);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.formatTimestamp(revInfo.getRevDate());
        long startMsec = ExtractTransform.parseTimestamp(startEtl).getTime();
        long endMsec = startMsec + MSEC_IN_SEC;
        String endEtl = ExtractTransform.formatTimestamp(new Date(endMsec));
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        Assert.assertTrue(recordCount > 0);

        final String datFileEnding = "_product_order_sample_bill.dat";
        Assert.assertTrue(ExtractTransformTest.searchEtlFile(datafileDir, datFileEnding, "T", pdoSampleId));
    }
}
