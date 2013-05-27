package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.RiskItem;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtilityKeepScope;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.hibernate.SQLQuery;
import org.hibernate.type.LongType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Container test of ExtractTransform.
 *
 * @author epolk
 */

@Test(enabled = true, groups = TestGroups.EXTERNAL_INTEGRATION, singleThreaded = true)
public class ExtractTransformTest extends Arquillian {
    private static final Log logger = LogFactory.getLog(ExtractTransform.class);
    private String datafileDir;
    public final long MSEC_IN_SEC = 1000L;
    private final String barcode = "TEST" + System.currentTimeMillis();

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
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DEV, SessionContextUtilityKeepScope.class);
    }

    @BeforeClass(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void beforeClass() throws Exception {
        datafileDir = System.getProperty("java.io.tmpdir");
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void beforeMethod() throws Exception {
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    public void testEtl() throws Exception {
        final TwoDBarcodedTube labVessel = new TwoDBarcodedTube(barcode);
        final String datFileEnding = "_lab_vessel.dat";

        // Writes and commits an entity to the db.  Envers requires the transaction to commit.
        long startSec = System.currentTimeMillis() / MSEC_IN_SEC;
        assertNotNull(utx);
        utx.begin();
        labVesselDao.persist(labVessel);
        labVesselDao.flush();
        utx.commit();

        // Incremental etl won't pick up entities in the current second.
        Thread.sleep(MSEC_IN_SEC);

        ExtractTransform.writeLastEtlRun(startSec);
        int recordCount = extractTransform.incrementalEtl("0", "0");
        long endEtlSec = ExtractTransform.readLastEtlRun();
        assertTrue(recordCount > 0);

        // Gets the entity.
        LabVessel entity = labVesselDao.findByIdentifier(barcode);
        assertNotNull(entity);
        long entityId = entity.getLabVesselId();

        // Finds the entity in a data file (may be more than one data file if another commit
        // hit in the small time window between startMsec and the incrementalEtl start).
        boolean found = searchEtlFile(startSec * MSEC_IN_SEC, endEtlSec * MSEC_IN_SEC, datFileEnding, "F", entityId);
        assertTrue(found);

        // Deletes the etl files.
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        // Runs a limited time span incremental etl that covers the entity created above.
        String startEtl = ExtractTransform.secTimestampFormat.format(new Date(startSec * MSEC_IN_SEC));
        String endEtl = ExtractTransform.secTimestampFormat.format(new Date(endEtlSec  * MSEC_IN_SEC));
        recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        assertTrue(recordCount > 0);
        found = searchEtlFile(startSec * MSEC_IN_SEC, endEtlSec * MSEC_IN_SEC, datFileEnding, "F", entityId);
        assertTrue(found);
        // Deletes the etl files.
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        // Runs backfill ETL on a range of entity ids that includes the known entity id.
        long startMsec = System.currentTimeMillis();
        Response.Status status = extractTransform.backfillEtl(LabVessel.class.getName(), entityId, entityId);
        assertEquals(status, Response.Status.NO_CONTENT);
        long endMsec = System.currentTimeMillis();

        // Checks that the new data file contains the known entity id.
        found = searchEtlFile(startMsec, endMsec, datFileEnding, "F", entityId);
        assertTrue(found);

        // Deletes the etl files.
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        // Deletes the entity.
        startSec = System.currentTimeMillis() / MSEC_IN_SEC;
        utx.begin();
        labVesselDao.remove(entity);
        labVesselDao.flush();
        utx.commit();
        Thread.sleep(MSEC_IN_SEC);

        ExtractTransform.writeLastEtlRun(startSec);
        recordCount = extractTransform.incrementalEtl("0", "0");
        endEtlSec = ExtractTransform.readLastEtlRun();
        assertTrue(recordCount > 0);

        // Finds the deletion entity in a data file (may be more than one data file if another commit
        // hit in the small time window between startMsec and the incrementalEtl start).
        found = searchEtlFile(startSec * MSEC_IN_SEC, endEtlSec * MSEC_IN_SEC, datFileEnding, "T", entityId);
        assertTrue(found);

    }

    // Returns existing riskItemId and its productOrderSampleId.  Set deleted=true for a deleted risk item.
    private Long[] getRiskJoin(boolean deleted) {
        String queryString;
        if (deleted) {
            queryString = "select risk_item_id as id1, product_order_sample as id2, rev " +
                    " from ATHENA.PO_SAMPLE_RISK_JOIN_AUD where revtype = 2 and rownum = 1";
        } else {
            queryString = "select risk_item_id as id1, product_order_sample as id2, rev " +
                    " from ATHENA.PO_SAMPLE_RISK_JOIN_AUD p1  where revtype = 0 and rownum = 1 " +
                    " and not exists (select 1 from ATHENA.PO_SAMPLE_RISK_JOIN_AUD p2 " +
                    " where p2.risk_item_id = p1.risk_item_id and p2.revtype = 2)";
        }
        return getJoinedIds(queryString);
    }

    // Returns existing ledgerId and its productOrderSampleId.  Set deleted=true for a deleted ledger item.
    private Long[] getLedgerJoin(boolean deleted) {
        String queryString;
        if (deleted) {
            queryString = "select p1.ledger_id as id1, p2.product_order_sample_id as id2, p1.rev" +
                    " from ATHENA.BILLING_LEDGER_AUD p1, ATHENA.BILLING_LEDGER_AUD p2" +
                    " where p1.revtype = 2 and p2.revtype = 0 and p1.ledger_id = p2.ledger_id and rownum = 1";
        } else {
            queryString = "select ledger_id as id1, product_order_sample_id as id2, rev " +
                    " from ATHENA.BILLING_LEDGER_AUD p1 where revtype = 0 and rownum = 1" +
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
        Long[] ids = new Long[]{(Long)obj[0], (Long)obj[1], (Long)obj[2]};
        return ids;
    }

    public void testUndeletedRiskOnDevDb() throws Exception {
        Long[] ids = getRiskJoin(false);
        if (ids == null || ids.length < 3) {
            logger.info("Skipping test, cannot find undeleted product order risk");
            return;
        }

        // Tests backfill etl.
        long startMsec = System.currentTimeMillis();
        long entityId = ids[0];
        long pdoSampleId = ids[1];
        Response.Status status = extractTransform.backfillEtl(RiskItem.class.getName(), entityId, entityId);
        assertEquals(status, Response.Status.NO_CONTENT);
        long endMsec = System.currentTimeMillis();

        final String datFileEnding = "_product_order_sample_risk.dat";
        assertTrue(searchEtlFile(startMsec, endMsec, datFileEnding, "F", pdoSampleId));

        EtlTestUtilities.deleteEtlFiles(datafileDir);
        assertFalse(searchEtlFile(startMsec, endMsec, datFileEnding, "F", pdoSampleId));

        // Test incremental etl.
        long rev = ids[2];
        RevInfo revInfo = auditReaderDao.findById(RevInfo.class, rev);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.secTimestampFormat.format(revInfo.getRevDate());
        startMsec = ExtractTransform.secTimestampFormat.parse(startEtl).getTime();
        endMsec = startMsec + MSEC_IN_SEC;
        String endEtl = ExtractTransform.secTimestampFormat.format(new Date(endMsec));
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        assertTrue(recordCount > 0);
        // Filename is "back dated".
        assertTrue(searchEtlFile(startMsec, endMsec, datFileEnding, "F", pdoSampleId));
    }

    public void testDeletedRiskOnDevDb() throws Exception {
        Long[] ids = getRiskJoin(true);
        if (ids == null || ids.length < 3) {
            logger.info("Skipping test, cannot find deleted product order risk");
            return;
        }
        long entityId = ids[0];
        long pdoSampleId = ids[1];
        long rev = ids[2];
        RevInfo revInfo = auditReaderDao.findById(RevInfo.class, rev);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.secTimestampFormat.format(revInfo.getRevDate());
        long startMsec = ExtractTransform.secTimestampFormat.parse(startEtl).getTime();
        long endMsec = startMsec + MSEC_IN_SEC;
        String endEtl = ExtractTransform.secTimestampFormat.format(new Date(endMsec));
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        assertTrue(recordCount > 0);

        final String datFileEnding = "_product_order_sample_risk.dat";
        assertTrue(searchEtlFile(startMsec, endMsec, datFileEnding, "T", pdoSampleId));
    }



    public void testaUndeletedLedgerOnDevDb() throws Exception {
        Long[] ids = getLedgerJoin(false);
        if (ids == null || ids.length < 3) {
            logger.info("Skipping test, cannot find undeleted product order ledger entry");
            return;
        }

        // Tests backfill etl.
        long startMsec = System.currentTimeMillis();
        long entityId = ids[0];
        long pdoSampleId = ids[1];
        Response.Status status = extractTransform.backfillEtl(LedgerEntry.class.getName(), entityId, entityId);
        assertEquals(status, Response.Status.NO_CONTENT);
        long endMsec = System.currentTimeMillis();

        final String datFileEnding = "product_order_sample_bill.dat";
        assertTrue(searchEtlFile(startMsec, endMsec, datFileEnding, "F", pdoSampleId));

        EtlTestUtilities.deleteEtlFiles(datafileDir);
        assertFalse(searchEtlFile(startMsec, endMsec, datFileEnding, "F", pdoSampleId));

        // Test incremental etl.
        long rev = ids[2];
        RevInfo revInfo = auditReaderDao.findById(RevInfo.class, rev);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.secTimestampFormat.format(revInfo.getRevDate());
        startMsec = ExtractTransform.secTimestampFormat.parse(startEtl).getTime();
        endMsec = startMsec + MSEC_IN_SEC;
        String endEtl = ExtractTransform.secTimestampFormat.format(new Date(endMsec));
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        assertTrue(recordCount > 0);
        // Filename is "back dated".
        assertTrue(searchEtlFile(startMsec, endMsec, datFileEnding, "F", pdoSampleId));
    }

    public void testDeletedLedgerOnDevDb() throws Exception {
        Long[] ids = getLedgerJoin(true);
        if (ids == null || ids.length < 3 || ids[0] == null) {
            logger.info("Skipping test, cannot find deleted product order ledger entry");
            return;
        }
        long entityId = ids[0];
        long pdoSampleId = ids[1];
        long rev = ids[2];
        RevInfo revInfo = auditReaderDao.findById(RevInfo.class, rev);
        // Brackets the change with interval on whole second boundaries.
        String startEtl = ExtractTransform.secTimestampFormat.format(revInfo.getRevDate());
        long startMsec = ExtractTransform.secTimestampFormat.parse(startEtl).getTime();
        long endMsec = startMsec + MSEC_IN_SEC;
        String endEtl = ExtractTransform.secTimestampFormat.format(new Date(endMsec));
        int recordCount = extractTransform.incrementalEtl(startEtl, endEtl);
        assertTrue(recordCount > 0);

        final String datFileEnding = "_product_order_sample_bill.dat";
        assertTrue(searchEtlFile(startMsec, endMsec, datFileEnding, "T", pdoSampleId));
    }


    /**
     * Looks for etl files having name timestamps in the given range, then searches them for a record having
     * the given isDelete and entityId values.
     */
    private boolean searchEtlFile(long startMsec, long endMsec, String datFileEnding, String isDelete, long entityId)
            throws IOException {

        for (File file : EtlTestUtilities.getDirFiles(datafileDir, startMsec, endMsec)) {
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
