package org.broadinstitute.gpinformatics.infrastructure.datawh;

import com.google.common.io.LineReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.RiskItem;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtilityKeepScope;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.hibernate.SQLQuery;
import org.hibernate.type.LongType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.Query;
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

    /* Periodically Oracle execution of queries in beforeMethod() takes over an hour despite fresh statistics.
     * If these get stale, replace with new valid values */
    private Long[] deletedRiskIds = new Long[]{788678L, 715899L, 336011L};
    private Long[] riskIds = new Long[]{770277L, 698382L, 331234L};
    private Long[] deletedLedgerIds = new Long[]{529053L, 676741L, 330162L};
    private Long[] ledgerIds = new Long[]{529366L, 572925L, 330229L};

    @Inject
    private ExtractTransform extractTransform;

    @Inject
    private AuditReaderDao auditReaderDao;

    @Inject
    private FixUpEtl fixUpEtl;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DEV, "dev", SessionContextUtilityKeepScope.class);
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        datafileDir = System.getProperty("java.io.tmpdir");
        ExtractTransform.setDatafileDir(datafileDir);
        EtlTestUtilities.deleteEtlFiles(datafileDir);

        /** These queries in some cases take over an hour despite Oracle fresh statistics
         *  Values are hardcoded above  - see comment at bottom of file for risk id retrieval **/
        if (false) {

            deletedRiskIds =  getJoinedIds("select risk_item_id as id1, product_order_sample as id2, rev  from "
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

            deletedLedgerIds = getJoinedIds(
                    "select * " +
                    "  from ( select p1.ledger_id as id1 " +
                    "              , p2.product_order_sample_id as id2 " +
                    "	           , p1.rev " +
                    "           from ATHENA.BILLING_LEDGER_AUD p1 " +
                    "              , ATHENA.BILLING_LEDGER_AUD p2 " +
                    "          where p1.revtype = 2 " +
                    "            and p2.revtype = 0 " +
                    "            and p1.ledger_id = p2.ledger_id  " +
                    "            and exists (select 1 from revchanges rc where rc.rev = p1.rev) " +
                    "         order by p1.ledger_id desc ) " +
                    " where rownum = 1 ");
            Assert.assertFalse(deletedLedgerIds == null || deletedLedgerIds.length < 3);

            ledgerIds = getJoinedIds(
                    "select * " +
                    "  from ( select ledger_id as id1 " +
                    "              , product_order_sample_id as id2 " +
                    "              , rev  " +
                    "           from ATHENA.BILLING_LEDGER_AUD p1 " +
                    "          where revtype = 0 " +
                    "   and exists (select 1 from revchanges rc where rc.rev = p1.rev) " +
                    "   and not exists (select 1 " +
                    "                     from ATHENA.BILLING_LEDGER_AUD p2 " +
                    "                    where p2.ledger_id = p1.ledger_id " +
                    "                      and p2.revtype = 2 ) " +
                    "order by ledger_id desc ) " +
                    "where rownum = 1 ");
            Assert.assertFalse(ledgerIds == null || ledgerIds.length < 3);
        }
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

    @Test
    public void testUndeletedRiskOnDevDb() throws Exception {
        // Tests backfill etl.
        long entityId = riskIds[0];
        long pdoSampleId = riskIds[1];
        Response response = extractTransform.backfillEtl(RiskItem.class.getName(), entityId, entityId);
        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());

        final String datFileEnding = "_product_order_sample_risk.dat";
        Assert.assertTrue(searchEtlFile(datafileDir, datFileEnding, "F", pdoSampleId));

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
        Assert.assertTrue(searchEtlFile(datafileDir, datFileEnding, "F", pdoSampleId));
    }

    @Test
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
        Assert.assertTrue(searchEtlFile(datafileDir, datFileEnding, "T", pdoSampleId));
    }

    @Test
    public void testUndeletedLedgerOnDevDb() throws Exception {
        // Tests backfill etl.
        long entityId = ledgerIds[0];
        long pdoSampleId = ledgerIds[1];
        Response response = extractTransform.backfillEtl(LedgerEntry.class.getName(), entityId, entityId + 2000);
        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());

        final String datFileEnding = "product_order_sample_bill.dat";
        Assert.assertTrue(searchEtlFile(datafileDir, datFileEnding, "F", pdoSampleId));

        EtlTestUtilities.deleteEtlFiles(datafileDir);
        Assert.assertFalse(searchEtlFile(datafileDir, datFileEnding, "F", pdoSampleId));

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
        Assert.assertTrue(searchEtlFile(datafileDir, datFileEnding, "F", pdoSampleId));
    }

    @Test
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
        Assert.assertTrue(searchEtlFile(datafileDir, datFileEnding, "T", pdoSampleId));
    }

    @Test
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

    @Test
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
    static boolean searchEtlFile(String datafileDir, String datFileEnding, String isDelete, long entityId)
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
    @Test
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
    @Test
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
    @TransactionAttribute(TransactionAttributeType.NEVER)
    @Test
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
    @TransactionAttribute(TransactionAttributeType.NEVER)
    @Test
    public void testBatchFixUpEtl() throws Exception {

        // Use existing fixup commentary revision ID for a lab batch starting vessel modification
        Long fixupCommentaryRevId = new Long(1096251);
        String etlDateStr = ExtractTransform.formatTimestamp(new Date());

        String eventFileName = etlDateStr + "_event_fact.dat";
        String seqSampleFileName = etlDateStr + "_sequencing_sample_fact.dat";

        // Not going to re-test audit revision pickup logic, just force one
        int recordCount = fixUpEtl.doIncrementalEtl(Collections.singleton(fixupCommentaryRevId), etlDateStr);

        // Batch vessel change is way back in workflow so there's many downstream events * samples
        Assert.assertTrue(recordCount > 38000, "Extracted row count " + recordCount + " is less than expected 38000");
        Assert.assertTrue(new File(datafileDir, eventFileName).exists(), "Event fact ETL file does not exist");
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
/** **** This gets risk delete and risk create if the database hangs on the above queries
 * SET SERVEROUTPUT ON;
 *
 * DECLARE
 *   -- Get some matched risk item and PDO sample changes
 *   CURSOR cur_risk_pairs IS
 *     select rc.rev
 *       from revchanges rc
 *      where rc.entityname in ( 'org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample', 'org.broadinstitute.gpinformatics.athena.entity.orders.RiskItem' )
 *     group by rc.rev
 *     having count(*) = 2;
 *
 *   -- Find some with deletes and some with no deletes
 *   CURSOR cur_risk_crud( a_rev revchanges.rev%TYPE ) IS
 *     select rev, MAX(revtype) as crud_max
 *       from ATHENA.PO_SAMPLE_RISK_JOIN_AUD
 *      where rev = a_rev
 *     group by rev;
 *
 *   rec_risk_crud cur_risk_crud%ROWTYPE;
 *
 *   -- Now get ones with between 10 and 100 PDO sample changes
 *   CURSOR cur_risk_multi( a_rev revchanges.rev%TYPE ) IS
 *     select rev
 *       from ATHENA.product_order_sample_aud
 *      where rev = a_rev
 *     group by rev having count(*) between 10 and 100;
 *
 *   rec_risk_multi cur_risk_multi%ROWTYPE;
 *
 *   -- Pick any single one:
 *   CURSOR cur_test_seed( a_rev revchanges.rev%TYPE ) IS
 *     select risk_item_id as id1, product_order_sample as id2, rev
 *       from ATHENA.PO_SAMPLE_RISK_JOIN_AUD
 *       where rev = a_rev;
 *
 *   rec_test_seed cur_test_seed%ROWTYPE;
 *
 *   v_rev revchanges.rev%TYPE;
 *   v_found_del CHAR;
 *   v_found_create CHAR;
 *
 * BEGIN
 *
 *   v_found_del := 'N';
 *   v_found_create := 'N';
 *
 *   OPEN cur_risk_pairs;
 *
 *   LOOP
 *     FETCH cur_risk_pairs INTO v_rev;
 *     EXIT WHEN cur_risk_pairs%NOTFOUND;
 *
 *     OPEN cur_risk_crud( v_rev );
 *
 *     LOOP
 *       FETCH cur_risk_crud INTO rec_risk_crud;
 *       EXIT WHEN cur_risk_crud%NOTFOUND;
 *
 *       v_rev := rec_risk_crud.rev;
 *
 *       IF rec_risk_crud.crud_max = 2 AND v_found_del = 'N' THEN
 *         OPEN cur_risk_multi( v_rev );
 *         FETCH cur_risk_multi INTO rec_risk_multi;
 *         IF cur_risk_multi%NOTFOUND THEN
 *           NULL;
 *         ELSE
 *           v_found_del := 'Y';
 *           OPEN cur_test_seed( v_rev );
 *           FETCH cur_test_seed INTO rec_test_seed;
 *           DBMS_OUTPUT.PUT_LINE('Bulk risk item deletes: risk item ID-' || rec_test_seed.id1 || ', PDO sample ID-' || rec_test_seed.id2 || ', rev-' || v_rev );
 *           CLOSE cur_test_seed;
 *         END IF;
 *         CLOSE cur_risk_multi;
 *       END IF;
 *
 *       IF rec_risk_crud.crud_max = 0 AND v_found_create = 'N' THEN
 *         OPEN cur_risk_multi( v_rev );
 *         FETCH cur_risk_multi INTO rec_risk_multi;
 *         IF cur_risk_multi%NOTFOUND THEN
 *           NULL;
 *         ELSE
 *           v_found_create := 'Y';
 *           OPEN cur_test_seed( v_rev );
 *           FETCH cur_test_seed INTO rec_test_seed;
 *           DBMS_OUTPUT.PUT_LINE('Bulk risk item creates: risk item ID-' || rec_test_seed.id1 || ', PDO sample ID-' || rec_test_seed.id2 || ', rev-' || v_rev );
 *           CLOSE cur_test_seed;
 *         END IF;
 *         CLOSE cur_risk_multi;
 *       END IF;
 *
 *       EXIT WHEN v_found_create = 'Y' AND v_found_del = 'Y';
 *
 *     END LOOP;
 *
 *     CLOSE cur_risk_crud;
 *
 *   END LOOP;
 *
 *   CLOSE cur_risk_pairs;
 *
 * END;
 * /

 */
