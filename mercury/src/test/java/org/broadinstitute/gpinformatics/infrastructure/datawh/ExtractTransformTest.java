package org.broadinstitute.gpinformatics.infrastructure.datawh;

import com.google.common.io.LineReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtilityKeepScope;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.SQLQuery;
import org.hibernate.type.LongType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.transaction.UserTransaction;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Container test of ExtractTransform.
 */

@Test(enabled = true, groups = TestGroups.ALTERNATIVES, singleThreaded = true)
@Dependent
public class ExtractTransformTest extends Arquillian {

    public ExtractTransformTest(){}

    private String datafileDir;
    private final long MSEC_IN_SEC = 1000L;
    private final String barcode = "TEST" + System.currentTimeMillis();

    @Inject
    private ExtractTransform extractTransform;
    @Inject
    private AuditReaderDao auditReaderDao;
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private FixUpEtl fixUpEtl;

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
        final String distantEnd = ExtractTransform.formatTimestamp(new Date(startMSec + 600000));
        Assert.assertNotNull(utx);
        utx.begin();
        labVesselDao.persist(labVessel);
        labVesselDao.flush();
        utx.commit();

        // Pick up the ID
        final long entityId = labVessel.getLabVesselId();

        // Wait since incremental etl won't pick up entities in the current second.
        Thread.sleep((ExtractTransform.TRANSACTION_COMPLETION_GUARDBAND + 1) * MSEC_IN_SEC);

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
        extractTransform.incrementalEtl(endEtl, distantEnd);
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
        recordCount = extractTransform.incrementalEtl(startEtl, distantEnd);
        Assert.assertTrue(recordCount > 0);
        Assert.assertFalse(searchEtlFile(datafileDir, datFileEnding, "F", entityId));
        Assert.assertTrue(searchEtlFile(datafileDir, datFileEnding, "T", entityId));
        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }


    private Long[] getJoinedIds(String queryString) {
        Query query = auditReaderDao.getEntityManager().createNativeQuery(queryString);
        query.unwrap(SQLQuery.class)
                .addScalar("id1", LongType.INSTANCE)
                .addScalar("id2", LongType.INSTANCE)
                .addScalar("rev", LongType.INSTANCE);
        Object[] obj = (Object[])query.getSingleResult();
        return new Long[]{(Long)obj[0], (Long)obj[1], (Long)obj[2]};
    }

    @Test(enabled = true, groups = TestGroups.ALTERNATIVES)
    public void testBackfill() throws Exception {
        // Tests every class that can be backfilled.
        for (String genericEntityEtlName : extractTransform.getEtlInstanceNames()) {
            try {
                Response response = extractTransform.backfillEtl(genericEntityEtlName, 1, 1);
                Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
                Assert.assertTrue(((String) response.getEntity()).toLowerCase().contains("created"));
            } catch (Exception e) {
                Assert.fail("Backfill on " + genericEntityEtlName + " failed.", e);
            }
        }
    }

    @Test(enabled = true, groups = TestGroups.ALTERNATIVES)
    public void testVesselBackfill() throws Exception {
        // Tests vessel backfill gets event_fact and sequencing_sample_fact entries
        // Normalization tube 0182870410
        // 5 downstream events, 968 total event_fact entries
        // 1 sequencing run id, 184 sequencing_sample_fact entries
        try {
            Response response = extractTransform.backfillEtlForVessel("0182870410");
            Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
            Assert.assertTrue(((String) response.getEntity()).toLowerCase().contains("created"));
        } catch (Exception e) {
            Assert.fail("TestVesselBackfill call failed.", e);
        }

        // First and last events
        long eventId;
        eventId = 993627L;
        Assert.assertTrue(
                searchEtlFile( datafileDir, "event_fact.dat", "F", eventId), "Expected event id " + eventId );
        eventId = 994556L;
        Assert.assertTrue(
                searchEtlFile( datafileDir, "event_fact.dat", "F", eventId), "Expected event id " + eventId );

        // Only sequencing run
        long runID = 81038L;
        Assert.assertTrue(
            searchEtlFile( datafileDir, "sequencing_sample_fact.dat", "F", runID), "Expected run id " + runID );
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

    /**
     * Every audit table must index rev as the primary column.
     */
    @Test(enabled = true, groups = TestGroups.ALTERNATIVES)
    public void testAudIndexes() throws Exception {
        Query query = auditReaderDao.getEntityManager().createNativeQuery(
                "select table_name from all_tables " +
                        "where table_name like '%_AUD' " +
                        "and not exists (select 1 from all_ind_columns " +
                        "where table_name = all_tables.table_name " +
                        "and column_name = 'REV' " +
                        "and column_position = 1)");
        String missing = StringUtils.join(query.getResultList(), ", ");
        Assert.assertTrue(StringUtils.isBlank(missing),
                missing + " must have an index with REV in the first position.");
    }

    /**
     * Every non-empty audit table should index the entity id as the primary column.
     * E.g. if LAB_EVENT_AUD has PK on (REV, LAB_EVENT_ID) there should be an index on (LAB_EVENT_ID).
     */
    @Test(enabled = true, groups = TestGroups.ALTERNATIVES)
    public void testAudEntityIndexes() throws Exception {
        Query query = auditReaderDao.getEntityManager().createNativeQuery(
                "select m1.table_name||'('||m1.column_name||')' " +
                        "from all_ind_columns m1 " +
                        "join all_ind_columns m2 on m2.index_name = m1.index_name " +
                        "  and m2.column_name = 'REV' " +
                        "  and m2.column_position = 1 " +
                        "where m1.table_name like '%_AUD' " +
                        "and m1.column_position = 2 " +
                        "and not exists (select 1 from all_ind_columns s1 " +
                        "  where s1.table_name = m1.table_name " +
                        "  and s1.column_name = m1.column_name " +
                        "  and s1.column_position = 1) " +
                        "and exists (select 1 from all_tables ss1 " +
                        "  where ss1.table_name = m1.table_name " +
                        "  and ss1.num_rows > 0)");
        String missing = StringUtils.join(query.getResultList(), ", ");
        Assert.assertTrue(StringUtils.isBlank(missing), "Indexes should exist on " + missing + ".");
    }


    /**
     * Tests fix-up logic - certain fixups have an effect on any downstream fact tables
     *    (event_fact, sequencing_sample_fact) which may have been ETL'ed
     */
    @Test(enabled = true)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void testEventFixUpEtl() throws Exception {

       // Use existing fixup commentary revision ID for a deleted shearing transfer event
        Long fixupCommentaryRevId = new Long(1145601);
        String etlDateStr = ExtractTransform.formatTimestamp(new Date());

        String eventFileName = etlDateStr + "_event_fact.dat";
        String seqSampleFileName = etlDateStr + "_sequencing_sample_fact.dat";
        String libraryAncestryFileName = etlDateStr + "_library_ancestry.dat";

        // Not going to re-test audit revision pickup logic, just force one
        int recordCount = fixUpEtl.doIncrementalEtl(Collections.singleton(fixupCommentaryRevId), etlDateStr);

        // Event is way back in workflow so there's many downstream events * samples
        Assert.assertTrue(recordCount > 29000, "Extracted row count is less than expected");
        Assert.assertTrue( new File(datafileDir, eventFileName ).exists(), "Event fact ETL file does not exist");
        Assert.assertTrue( new File(datafileDir, libraryAncestryFileName ).exists(), "Library ancestry ETL file does not exist");
        Assert.assertTrue( new File(datafileDir, seqSampleFileName ).exists(), "Sequencing sample fact ETL file does not exist");

        // Check for a handful of events
        Map<String, Boolean> expectedIds = new HashMap<>();
        expectedIds.put("1928131", Boolean.FALSE);  // PicoMicrofluorTransfer
        expectedIds.put("1935708", Boolean.FALSE);  // AdapterLigationCleanup
        expectedIds.put("1940869", Boolean.FALSE);  // IceCatchEnrichmentSetup
        expectedIds.put("1941567", Boolean.FALSE);  // CatchPico
        expectedIds.put("1945775", Boolean.FALSE);  // FlowcellTransfer

        FileReader reader = new FileReader( new File(datafileDir, eventFileName ));
        LineReader lineReader = new LineReader(reader);
        String line;
        Set<String> ids = expectedIds.keySet();
        while( (line = lineReader.readLine()) != null ) {
            // All data records start with: lineNumber, etlDate, deletionFlag, entityId.
            String[] parts = line.split(",");
            if( ids.contains(parts[3]) ) {
                expectedIds.put(parts[3],Boolean.TRUE);
            }
        }
        IOUtils.closeQuietly(reader);
        Assert.assertFalse(expectedIds.containsValue(Boolean.FALSE), "Expected event missing from ETL");

        expectedIds = new HashMap<>();
        expectedIds.put("151161", Boolean.FALSE);
        expectedIds.put("151162", Boolean.FALSE);

        reader = new FileReader( new File(datafileDir, seqSampleFileName ));
        lineReader = new LineReader(reader);
        ids = expectedIds.keySet();
        while( (line = lineReader.readLine()) != null ) {
            // All data records start with: lineNumber, etlDate, deletionFlag, entityId.
            String[] parts = line.split(",");
            if( ids.contains(parts[3]) ) {
                expectedIds.put(parts[3],Boolean.TRUE);
            }
        }
        IOUtils.closeQuietly(reader);
        Assert.assertFalse(expectedIds.containsValue(Boolean.FALSE), "Expected sequencing sample missing from ETL");

        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }

    /**
     * Tests fix-up logic - certain fixups have an effect on any downstream fact tables
     *    (event_fact, sequencing_sample_fact) which may have been ETL'ed
     */
    @Test(enabled = true)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void testBatchFixUpEtl() throws Exception {

        // Use existing fixup commentary revision ID for a lab batch starting vessel modification
        Long fixupCommentaryRevId = new Long(1096251);
        String etlDateStr = ExtractTransform.formatTimestamp(new Date());

        String eventFileName = etlDateStr + "_event_fact.dat";
        String seqSampleFileName = etlDateStr + "_sequencing_sample_fact.dat";

        // Not going to re-test audit revision pickup logic, just force one
        int recordCount = fixUpEtl.doIncrementalEtl(Collections.singleton(fixupCommentaryRevId), etlDateStr);

        // Batch vessel change is way back in workflow so there's many downstream events * samples
        Assert.assertTrue(recordCount > 38000, "Extracted row count is less than expected");
        Assert.assertTrue( new File(datafileDir, eventFileName ).exists(), "Event fact ETL file does not exist");
        Assert.assertTrue( new File(datafileDir, seqSampleFileName ).exists(), "Sequencing sample fact ETL file does not exist");

        // Check for a handful of events
        Map<String, Boolean> expectedIds = new HashMap<>();
        expectedIds.put("1741406", Boolean.FALSE);  // SampleReceipt
        expectedIds.put("1741479", Boolean.FALSE);  // CollaboratorTransfer
        expectedIds.put("1775757", Boolean.FALSE);  // PicoPlatingBucket
        expectedIds.put("1875227", Boolean.FALSE);  // IceCatchEnrichmentSetup
        expectedIds.put("1876473", Boolean.FALSE);  // EcoTransfer
        expectedIds.put("1877421", Boolean.FALSE);  // DilutionToFlowcellTransfer

        FileReader reader = new FileReader( new File(datafileDir, eventFileName ));
        LineReader lineReader = new LineReader(reader);
        String line;
        Set<String> ids = expectedIds.keySet();
        while( (line = lineReader.readLine()) != null ) {
            // All data records start with: lineNumber, etlDate, deletionFlag, entityId.
            String[] parts = line.split(",");
            if( ids.contains(parts[3]) ) {
                expectedIds.put(parts[3],Boolean.TRUE);
            }
        }
        IOUtils.closeQuietly(reader);
        Assert.assertFalse(expectedIds.containsValue(Boolean.FALSE), "Expected event missing from ETL");

        expectedIds = new HashMap<>();
        expectedIds.put("143173", Boolean.FALSE);

        reader = new FileReader( new File(datafileDir, seqSampleFileName ));
        lineReader = new LineReader(reader);
        ids = expectedIds.keySet();
        while( (line = lineReader.readLine()) != null ) {
            // All data records start with: lineNumber, etlDate, deletionFlag, entityId.
            String[] parts = line.split(",");
            if( ids.contains(parts[3]) ) {
                expectedIds.put(parts[3],Boolean.TRUE);
            }
        }
        IOUtils.closeQuietly(reader);
        Assert.assertFalse(expectedIds.containsValue(Boolean.FALSE), "Expected sequencing sample missing from ETL");

        EtlTestUtilities.deleteEtlFiles(datafileDir);
    }



}
