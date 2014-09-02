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
    private ManifestSession testSession;

    @BeforeMethod
    public void setUp() throws Exception {
        testSession = buildTestSession();
        testRecord = buildManifestRecord(COLLABORATOR_SAMPLE_ID_1);
    }

    private ManifestRecord buildManifestRecord(String sampleId) {
        ManifestSession testSession1 = testSession;
        return buildManifestRecord(sampleId, testSession1);
    }

    private ManifestRecord buildManifestRecord(String sampleId, ManifestSession testSession1) {
        return new ManifestRecord(testSession1, ManifestTestFactory.buildMetadata(
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, sampleId, Metadata.Key.GENDER, VALUE_2, Metadata.Key.PATIENT_ID,
                        VALUE_3)));
    }

    /**
     * Tests basic ManifestRecord creation and Metadata lookup.
     */
    public void createRecord() throws Exception {

        // Test with no specified Status or ErrorStatus.
        ManifestRecord testRecord = new ManifestRecord(new ManifestSession(),
                new Metadata(Metadata.Key.SAMPLE_ID, COLLABORATOR_SAMPLE_ID_1),
                new Metadata(Metadata.Key.GENDER, VALUE_2), new Metadata(Metadata.Key.PATIENT_ID, VALUE_3));

        // Basic sanity check of retrieving Metadata by key.
        Assert.assertEquals(testRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue(), COLLABORATOR_SAMPLE_ID_1);
        Assert.assertEquals(testRecord.getMetadataByKey(Metadata.Key.GENDER).getValue(), VALUE_2);
        Assert.assertEquals(testRecord.getMetadataByKey(Metadata.Key.PATIENT_ID).getValue(), VALUE_3);
        // Default status should be UPLOADED.
        Assert.assertEquals(testRecord.getStatus(), ManifestRecord.Status.UPLOADED);
        // Default error status should be null (no error).
        Assert.assertFalse(testRecord.quarantinedErrorExists());

        // Test with specified Status and ErrorStatus.
        testRecord.setStatus(NEW_STATUS);
        testRecord.addLogEntry(new ManifestEvent(NEW_ERROR_STATUS.formatMessage("Sample ID", COLLABORATOR_SAMPLE_ID_1),
                ManifestEvent.Type.QUARANTINED));

        Assert.assertEquals(testRecord.getStatus(), NEW_STATUS);
        Assert.assertTrue(testRecord.quarantinedErrorExists());
    }

    public void validManifest() {

        ManifestSession secondSession = buildTestSession(testSession.getResearchProject(),"COLLABORATOR_SAMPLE_ID_3");

        String COLLABORATOR_SAMPLE_ID_2 = "COLLABORATOR_SAMPLE_ID_2";
        ManifestRecord secondManifestRecord = buildManifestRecord(COLLABORATOR_SAMPLE_ID_2);

        testSession.validateManifest();
        assertThat(testSession.didSomethingGetLogged(), is(equalTo(false)));

        assertThat(testSession.getLogEntries().size(), is(equalTo(0)));
        assertThat(testRecord.quarantinedErrorExists(), is(equalTo(false)));
        assertThat(secondManifestRecord.quarantinedErrorExists(), is(equalTo(false)));

        assertThat(testRecord.getLogEntries(), is(empty()));
        assertThat(secondManifestRecord.getLogEntries(), is(empty()));

        secondSession.validateManifest();
        assertThat(secondSession.didSomethingGetLogged(), is(equalTo(false)));

        assertThat(secondSession.getLogEntries().size(), is(equalTo(0)));
    }


    public void duplicateInManifest() throws Exception {

        ManifestRecord testRecordWithDupe =
                new ManifestRecord(testSession,
                        ManifestTestFactory.buildMetadata(ImmutableMap.of(Metadata.Key.SAMPLE_ID,
                                COLLABORATOR_SAMPLE_ID_1,
                                Metadata.Key.GENDER, VALUE_2, Metadata.Key.PATIENT_ID, VALUE_3)));

        testSession.validateManifest();

        assertThat(testSession.didSomethingGetLogged(), is(equalTo(true)));

        assertThat(testSession.getLogEntries().size(), is(equalTo(2)));
        assertThat(testRecord.quarantinedErrorExists(), is(equalTo(true)));
        assertThat(testRecordWithDupe.quarantinedErrorExists(), is(equalTo(true)));

        assertThat(testRecord.getLogEntries().size(), is(equalTo(1)));
        assertThat(testRecordWithDupe.getLogEntries().size(), is(equalTo(1)));
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

        ManifestSession secondSession = buildTestSession(testSession.getResearchProject(), COLLABORATOR_SAMPLE_ID_1);

        testSession.validateManifest();
        assertThat(testSession.didSomethingGetLogged(), is(equalTo(true)));
        assertThat(testSession.getLogEntries(), is(not(empty())));
        assertThat(testSession.getLogEntries().size(), is(equalTo(1)));

        assertThat(secondSession.didSomethingGetLogged(), is(equalTo(false)));
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

        ResearchProject parentResearchProject = ResearchProjectTestFactory.createTestResearchProject("RP-334SIS");
        ManifestSession secondSession = buildTestSession(parentResearchProject, COLLABORATOR_SAMPLE_ID_1);
        buildTestSession().getResearchProject().setParentResearchProject(parentResearchProject);

        testSession.validateManifest();
        assertThat(testSession.didSomethingGetLogged(), is(equalTo(false)));
        assertThat(testSession.getLogEntries(), is(empty()));
        assertThat(testSession.getLogEntries().size(), is(equalTo(0)));

        assertThat(secondSession.didSomethingGetLogged(), is(equalTo(false)));
        assertThat(secondSession.getLogEntries(), is(empty()));

    }

    public void mismatchedGenderTest() throws Exception {
        ManifestRecord testRecordWrongGender =
                new ManifestRecord(testSession, ManifestTestFactory.buildMetadata(ImmutableMap.of(
                        Metadata.Key.SAMPLE_ID, "989282484", Metadata.Key.GENDER, "M", Metadata.Key.PATIENT_ID,
                        VALUE_3)));

        testSession.validateManifest();
        assertThat(testSession.didSomethingGetLogged(), is(equalTo(true)));
    }

    public void mixedValidationErrorTest() throws Exception {

        ManifestRecord duplicateSampleRecord =
                new ManifestRecord(testSession,
                        ManifestTestFactory.buildMetadata(ImmutableMap.of(Metadata.Key.SAMPLE_ID,
                                COLLABORATOR_SAMPLE_ID_1, Metadata.Key.GENDER, VALUE_2, Metadata.Key.PATIENT_ID,
                                "PI-3234")));

        ManifestRecord genderMisMatch =
                new ManifestRecord(testSession,
                        ManifestTestFactory.buildMetadata(ImmutableMap.of(Metadata.Key.SAMPLE_ID, "229249239",
                                Metadata.Key.GENDER, "M", Metadata.Key.PATIENT_ID, "PI-3234")));

        testSession.validateManifest();

        assertThat(testSession.didSomethingGetLogged(), is(equalTo(true)));

        assertThat(testSession.getLogEntries(), is(not(empty())));
        assertThat(testSession.getLogEntries().size(), is(equalTo(4)));
        assertThat(testRecord.getLogEntries(), is(not(empty())));
        assertThat(testRecord.getLogEntries().size(), is(equalTo(1)));

        assertThat(duplicateSampleRecord.getLogEntries(), is(not(empty())));
        assertThat(duplicateSampleRecord.getLogEntries().size(), is(equalTo(2)));

        assertThat(genderMisMatch.getLogEntries(), is(not(empty())));
        assertThat(genderMisMatch.getLogEntries().size(), is(equalTo(1)));
    }

    private ManifestSession buildTestSession() {
        ResearchProject testProject = ResearchProjectTestFactory.createTestResearchProject("RP-334");

        return buildTestSession(testProject);
    }

    private ManifestSession buildTestSession(ResearchProject testProject, String... testRecords) {
        ManifestSession newTestSession =
                new ManifestSession(testProject, "DUPLICATE_TEST", new BSPUserList.QADudeUser("LU", 33L));
        for (String recordSampleId : testRecords) {
            buildManifestRecord(recordSampleId, newTestSession);
        }

        return newTestSession;
    }

}
