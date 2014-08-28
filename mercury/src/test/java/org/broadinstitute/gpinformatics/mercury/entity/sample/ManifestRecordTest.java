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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Database free test of ManifestRecords, entities representing individual samples within a Buick manifest used for
 * sample registration.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestRecordTest {

    private static final String COLLABORATOR_SAMPLE_ID_1 = "COLLABORATOR_SAMPLE_ID_1";
    private static final String VALUE_2 = "value2";
    private static final String VALUE_3 = "value3";
    private static final ManifestRecord.Status NEW_STATUS = ManifestRecord.Status.ABANDONED;
    private static final ManifestRecord.ErrorStatus NEW_ERROR_STATUS = ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID;

    private ManifestRecord testRecord;

    @BeforeMethod
    public void setUp() throws Exception {
        testRecord = buildManifestRecord(COLLABORATOR_SAMPLE_ID_1);
    }

    private ManifestRecord buildManifestRecord(String sampleId) {
        return ManifestTestFactory.buildManifestRecord(
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, sampleId, Metadata.Key.GENDER, VALUE_2, Metadata.Key.PATIENT_ID,
                        VALUE_3));
    }

    /**
     * Tests basic ManifestRecord creation and Metadata lookup.
     */
    public void createRecord() throws Exception {

        // Test with no specified Status or ErrorStatus.

        // Basic sanity check of retrieving Metadata by key.
        Assert.assertEquals(testRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue(), COLLABORATOR_SAMPLE_ID_1);
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

    public void validManifest() {
        ManifestSession testSession = buildTestSession();

        ManifestSession secondSession = getManifestSession(testSession.getResearchProject(),
                buildManifestRecord("COLLABORATOR_SAMPLE_ID_3"));

        String COLLABORATOR_SAMPLE_ID_2 = "COLLABORATOR_SAMPLE_ID_2";
        ManifestRecord secondManifestRecord = buildManifestRecord(COLLABORATOR_SAMPLE_ID_2);
        testSession.addRecord(secondManifestRecord);

        testSession.validateManifest();
        Assert.assertTrue(testSession.isManifestValid());

        Assert.assertEquals(testSession.getLogEntries().size(), 0);
        assertThat(testRecord.getErrorStatus(), is(nullValue()));
        assertThat(secondManifestRecord.getErrorStatus(), is(nullValue()));

        assertThat(testRecord.getLogEntries(), is(empty()));
        assertThat(secondManifestRecord.getLogEntries(), is(empty()));

        secondSession.validateManifest();
        Assert.assertTrue(secondSession.isManifestValid());

        Assert.assertEquals(secondSession.getLogEntries().size(), 0);
    }


    public void duplicateInManifest() throws Exception {

        ManifestSession testSession = buildTestSession();

        ManifestRecord testRecordWithDupe =
                ManifestTestFactory.buildManifestRecord(ImmutableMap.of(Metadata.Key.SAMPLE_ID,
                        COLLABORATOR_SAMPLE_ID_1,
                        Metadata.Key.GENDER, "F", Metadata.Key.PATIENT_ID, VALUE_3));

        testSession.addRecord(testRecordWithDupe);

        testSession.validateManifest();

        Assert.assertFalse(testSession.isManifestValid());

        Assert.assertEquals(testSession.getLogEntries().size(), 2);
        Assert.assertEquals(testRecord.getErrorStatus(), ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID);
        Assert.assertEquals(testRecordWithDupe.getErrorStatus(), ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID);

        Assert.assertEquals(testRecord.getLogEntries().size(), 1);
        Assert.assertEquals(testRecordWithDupe.getLogEntries().size(), 1);
    }

    /**
     * TODO
     * <p/>
     * Awaiting decision on question asked in Confluence decision
     * <a href=https://labopsconfluence.broadinstitute.org/display/GPI/What+is+the+scope+limit+with+marking+records+as+bad \>
     * <p/>
     * Till then, this test method tests that a sample in a manifest record must be unique across manifest sessions
     * in a research project, but marking the duplicate record is limited to the current manifest session being
     * validated.
     *
     * @throws Exception
     */
    public void duplicateAcrossRP() throws Exception {

        ManifestSession testSession = buildTestSession();

        ManifestSession secondSession = getManifestSession(testSession.getResearchProject(),
                buildManifestRecord(COLLABORATOR_SAMPLE_ID_1));

        testSession.validateManifest();
        Assert.assertFalse(testSession.isManifestValid());
        assertThat(testSession.getLogEntries(), is(not(empty())));
        assertThat(testSession.getLogEntries().size(), is(equalTo(1)));

        assertThat(secondSession.isManifestValid(), is(equalTo(true)));
        assertThat(secondSession.getLogEntries(), is(empty()));

    }

    /**
     * TODO
     * <p/>
     * Awaiting decision on question asked in Confluence decision
     * <a href=https://labopsconfluence.broadinstitute.org/display/GPI/Sample+ID+uniqueness+in+Research+Project+hierarchies \>
     * <p/>
     * Till then, this test method tests that a sample in a manifest record does not have to be unique when
     * compared to another session in either the parent or child research project of its own research project.
     *
     * @throws Exception
     */
    public void duplicateAcrossRPHierarchy() throws Exception {

        ManifestSession testSession = buildTestSession();

        ResearchProject parentResearchProject = ResearchProjectTestFactory.createTestResearchProject("RP-334SIS");
        ManifestSession secondSession = getManifestSession(parentResearchProject,
                buildManifestRecord(COLLABORATOR_SAMPLE_ID_1));
        buildTestSession().getResearchProject().setParentResearchProject(parentResearchProject);

        testSession.validateManifest();
        Assert.assertTrue(testSession.isManifestValid());
        assertThat(testSession.getLogEntries(), is(empty()));
        assertThat(testSession.getLogEntries().size(), is(equalTo(0)));

        assertThat(secondSession.isManifestValid(), is(equalTo(true)));
        assertThat(secondSession.getLogEntries(), is(empty()));

    }

    public void mismatchedGenderTest() throws Exception {
        ManifestSession testSession = buildTestSession();

        ManifestRecord testRecordWrongGender = ManifestTestFactory.buildManifestRecord(ImmutableMap.of(
                Metadata.Key.SAMPLE_ID, "989282484", Metadata.Key.GENDER, "M", Metadata.Key.PATIENT_ID, VALUE_3));
        testSession.addRecord(testRecordWrongGender);

        testSession.validateManifest();
        assertThat(testSession.isManifestValid(), is(equalTo(false)));
    }


    public ManifestSession buildTestSession() {
        ResearchProject testProject = ResearchProjectTestFactory.createTestResearchProject("RP-334");

        return getManifestSession(testProject, testRecord);
    }

    public ManifestSession getManifestSession(ResearchProject testProject, ManifestRecord testRecord1) {
        ManifestSession testSession =
                new ManifestSession(testProject, "DUPLICATE_TEST", new BSPUserList.QADudeUser("LU", 33L));

        testSession.addRecord(testRecord1);
        return testSession;
    }

}
