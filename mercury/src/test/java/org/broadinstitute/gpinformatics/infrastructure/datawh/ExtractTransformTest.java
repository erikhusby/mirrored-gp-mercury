package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.RiskItem;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Container test of ExtractTransform.
 *
 * @author epolk
 */

@Test(enabled = true, groups = TestGroups.EXTERNAL_INTEGRATION, singleThreaded = true)
public class ExtractTransformTest extends Arquillian {
    private String datafileDir;
    public final long MSEC_IN_SEC = 1000L;
    private final String barcode = "TEST" + System.currentTimeMillis();

    @Inject
    private ExtractTransform extractTransform;
    @Inject
    private AuditReaderDao auditReaderDao;
    @Inject
    private LabVesselDao labVesselDao;
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
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

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Deletes any test entity in case the test prematurely ended.
        if (utx != null) {
            if (utx.getStatus() == Status.STATUS_NO_TRANSACTION) {
                utx.begin();
            }
            LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
            if (labVessel != null) {
                labVesselDao.remove(labVessel);
                labVesselDao.flush();
            }
            utx.commit();
        }
    }

    public void testEtl() throws Exception {
        final TwoDBarcodedTube labVessel = new TwoDBarcodedTube(barcode);
        final String datFileEnding = "_lab_vessel.dat";

        // Writes and commits an entity to the db.  Envers requires the transaction to commit.
        long startSec = System.currentTimeMillis() / MSEC_IN_SEC;
        Assert.assertNotNull(utx);
        utx.begin();
        labVesselDao.persist(labVessel);
        labVesselDao.flush();
        utx.commit();

        // Incremental etl won't pick up entities in the current second.
        Thread.sleep(MSEC_IN_SEC);

        extractTransform.writeLastEtlRun(startSec);
        int recordCount = extractTransform.incrementalEtl("0", "0");
        long endEtlSec = extractTransform.readLastEtlRun();
        Assert.assertTrue(recordCount > 0);

        // Gets the entity.
        LabVessel entity = labVesselDao.findByIdentifier(barcode);
        Assert.assertNotNull(entity);
        long entityId = entity.getLabVesselId();

        // Finds the entity in a data file (may be more than one data file if another commit
        // hit in the small time window between startMsec and the incrementalEtl start).
        boolean found = searchEtlFile(startSec * MSEC_IN_SEC, endEtlSec * MSEC_IN_SEC, datFileEnding, "F", entityId);
        Assert.assertTrue(found);

        // Deletes the etl files.
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        // Runs backfill ETL on a range of entity ids that includes the known entity id.
        long startMsec = System.currentTimeMillis();
        Response.Status status = extractTransform.backfillEtl(LabVessel.class.getName(), entityId, entityId);
        Assert.assertEquals(status, Response.Status.NO_CONTENT);
        long endMsec = System.currentTimeMillis();

        // Checks that the new data file contains the known entity id.
        found = searchEtlFile(startMsec, endMsec, datFileEnding, "F", entityId);
        Assert.assertTrue(found);

        // Deletes the etl files.
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        // Deletes the entity.
        startSec = System.currentTimeMillis() / MSEC_IN_SEC;
        utx.begin();
        labVesselDao.remove(entity);
        labVesselDao.flush();
        utx.commit();
        Thread.sleep(MSEC_IN_SEC);

        extractTransform.writeLastEtlRun(startSec);
        recordCount = extractTransform.incrementalEtl("0", "0");
        endEtlSec = extractTransform.readLastEtlRun();
        Assert.assertTrue(recordCount > 0);

        // Finds the deletion entity in a data file (may be more than one data file if another commit
        // hit in the small time window between startMsec and the incrementalEtl start).
        found = searchEtlFile(startSec * MSEC_IN_SEC, endEtlSec * MSEC_IN_SEC, datFileEnding, "T", entityId);
        Assert.assertTrue(found);

    }

    // todo rewrite to not require specific item in db
    @Test(enabled=false, groups=TestGroups.EXTERNAL_INTEGRATION)
    public void testRiskOnDevDb() throws Exception {
        long startMsec = System.currentTimeMillis();
        long entityId = 53338L;
        long pdoSampleId = 116079L;
        Response.Status status = extractTransform.backfillEtl(RiskItem.class.getName(), entityId, entityId);
        Assert.assertEquals(status, Response.Status.NO_CONTENT);
        long endMsec = System.currentTimeMillis();

        final String datFileEnding = "_product_order_sample_risk.dat";
        boolean found = searchEtlFile(startMsec, endMsec, datFileEnding, "F", pdoSampleId);
        Assert.assertTrue(found);
    }

    // todo rewrite to not require specific item in db
    @Test(enabled=false, groups=TestGroups.EXTERNAL_INTEGRATION)
    public void testLedgerOnDevDb() throws Exception {
        long startMsec = System.currentTimeMillis();
        long entityId = 12366L;
        long pdoSampleId = 23912L;
        Response.Status status = extractTransform.backfillEtl(LedgerEntry.class.getName(), entityId, entityId);
        Assert.assertEquals(status, Response.Status.NO_CONTENT);
        long endMsec = System.currentTimeMillis();

        final String datFileEnding = "product_order_sample_bill.dat";
        boolean found = searchEtlFile(startMsec, endMsec, datFileEnding, "F", pdoSampleId);
        Assert.assertTrue(found);
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
