package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Database free test of ManifestRecords, entities representing individual samples within a Buick manifest used for
 * sample registration.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestRecordTest {

    private static final String VALUE_1 = "value1";
    private static final String VALUE_2 = "value2";
    private static final String VALUE_3 = "value3";
    private static final ManifestRecord.Status NEW_STATUS = ManifestRecord.Status.ABANDONED;
    private static final ManifestRecord.ErrorStatus NEW_ERROR_STATUS = ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID;

    private ManifestRecord testRecord;

    @BeforeMethod
    public void setUp() throws Exception {
        testRecord = ManifestTestFactory.buildManifestRecord(
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, VALUE_1, Metadata.Key.GENDER, VALUE_2, Metadata.Key.PATIENT_ID, VALUE_3));
    }

    /**
     * Tests basic ManifestRecord creation and Metadata lookup.
     */
    public void createRecord() throws Exception {

        // Test with no specified Status or ErrorStatus.

        // Basic sanity check of retrieving Metadata by key.
        Assert.assertEquals(testRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue(), VALUE_1);
        Assert.assertEquals(testRecord.getMetadataByKey(Metadata.Key.GENDER).getValue(), VALUE_2);
        Assert.assertEquals(testRecord.getMetadataByKey(Metadata.Key.PATIENT_ID).getValue(), VALUE_3);
        // Default status should be UPLOADED.
        Assert.assertEquals(testRecord.getStatus(), ManifestRecord.Status.UPLOADED);
        // Default error status should be null (no error).
        Assert.assertNull(testRecord.getErrorStatus());

        // Test with specified Status and ErrorStatus.
        testRecord.setStatus(NEW_STATUS);
        testRecord.setErrorStatus(NEW_ERROR_STATUS);

        Assert.assertEquals(testRecord.getStatus(), NEW_STATUS);
        Assert.assertEquals(testRecord.getErrorStatus(), NEW_ERROR_STATUS);
    }

    public void testValidationDuplicateInManifest() throws Exception {

        ResearchProject testProject = ResearchProjectTestFactory.createTestResearchProject("RP-334");

        ManifestSession testSession = new ManifestSession(testProject, "DUPLICATE_TEST", new BSPUserList.QADudeUser("LU", 33L));

        testSession.addRecord(testRecord);

        ManifestRecord testRecordWithDupe =
                ManifestTestFactory.buildManifestRecord(ImmutableMap.of(Metadata.Key.SAMPLE_ID, VALUE_1,
                        Metadata.Key.GENDER, "F", Metadata.Key.PATIENT_ID, VALUE_3));

        testSession.addRecord(testRecordWithDupe);

        Assert.assertFalse(testSession.isManifestValid());


        Assert.assertEquals(testSession.getLogEntries().size(), 2);
        Assert.assertEquals(testRecord.getErrorStatus(), ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID);
        Assert.assertEquals(testRecordWithDupe.getErrorStatus(), ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID);

        Assert.assertEquals(testRecord.getLogEntries().size(), 1);
        Assert.assertEquals(testRecordWithDupe.getLogEntries().size(), 1);
    }

}
