package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
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

    private ManifestRecord testRecord;
    private ManifestSession testSession;

    @BeforeMethod
    public void setUp() throws Exception {
        testSession = buildTestSession(ManifestSessionEjb.AccessioningProcessType.CRSP);

        testRecord = buildManifestRecord(testSession, COLLABORATOR_SAMPLE_ID_1);
    }

    private ManifestRecord buildManifestRecord(ManifestSession manifestSession, String sampleId) {
        ManifestRecord manifestRecord = buildManifestRecord(manifestSession,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, sampleId,
                        Metadata.Key.GENDER, VALUE_2, Metadata.Key.PATIENT_ID, VALUE_3));
        manifestSession.addRecord(manifestRecord);
        return manifestRecord;
    }

    /**
     * Tests basic ManifestRecord creation and Metadata lookup.
     */
    public void createRecord() throws Exception {

        // Test with no specified Status or ErrorStatus.
        ManifestSession sessionIn = new ManifestSession();
        ManifestRecord testRecord = new ManifestRecord(
                new Metadata(Metadata.Key.SAMPLE_ID, COLLABORATOR_SAMPLE_ID_1),
                new Metadata(Metadata.Key.GENDER, VALUE_2), new Metadata(Metadata.Key.PATIENT_ID, VALUE_3));
        sessionIn.addRecord(testRecord);

        // Basic sanity check of retrieving Metadata by key.
        Assert.assertEquals(testRecord.getValueByKey(Metadata.Key.SAMPLE_ID), COLLABORATOR_SAMPLE_ID_1);
        Assert.assertEquals(testRecord.getValueByKey(Metadata.Key.GENDER), VALUE_2);
        Assert.assertEquals(testRecord.getValueByKey(Metadata.Key.PATIENT_ID), VALUE_3);
        // Default status should be UPLOADED.
        Assert.assertEquals(testRecord.getStatus(), ManifestRecord.Status.UPLOADED);
        // Default error status should be null (no error).
        Assert.assertFalse(testRecord.isQuarantined());

    }

    public void testStatusUpdate() throws Exception {
        // Test with no specified Status or ErrorStatus.
        ManifestRecord testRecord = new ManifestRecord();

        // Test with specified Status and ErrorStatus.
        testRecord.setStatus(NEW_STATUS);

        Assert.assertEquals(testRecord.getStatus(), NEW_STATUS);
    }

    public void validManifest() {

        ManifestSession secondSession = buildTestSession(testSession.getResearchProject(),
                ManifestSessionEjb.AccessioningProcessType.CRSP,"COLLABORATOR_SAMPLE_ID_3");

        String COLLABORATOR_SAMPLE_ID_2 = "COLLABORATOR_SAMPLE_ID_2";
        ManifestRecord secondManifestRecord = buildManifestRecord(secondSession, COLLABORATOR_SAMPLE_ID_2);

        testSession.validateManifest();
        assertThat(testSession.hasErrors(), is(false));

        assertThat(testSession.getManifestEvents().size(), is(0));
        assertThat(testRecord.isQuarantined(), is(false));
        assertThat(secondManifestRecord.isQuarantined(), is(false));

        assertThat(testRecord.getManifestEvents(), is(empty()));
        assertThat(secondManifestRecord.getManifestEvents(), is(empty()));

        secondSession.validateManifest();
        assertThat(secondSession.hasErrors(), is(false));

        assertThat(secondSession.getManifestEvents().size(), is(0));
    }


    public void duplicateInManifest() throws Exception {

        ManifestRecord testRecordWithDupe = buildManifestRecord(testSession,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, COLLABORATOR_SAMPLE_ID_1,
                        Metadata.Key.GENDER, VALUE_2, Metadata.Key.PATIENT_ID, VALUE_3));
        testSession.addRecord(testRecordWithDupe);

        testSession.validateManifest();

        assertThat(testSession.hasErrors(), is(true));

        assertThat(testSession.getManifestEvents().size(), is(2));
        assertThat(testRecord.isQuarantined(), is(true));
        assertThat(testRecordWithDupe.isQuarantined(), is(true));

        assertThat(testRecord.getManifestEvents().size(), is(1));
        assertThat(testRecordWithDupe.getManifestEvents().size(), is(1));
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

        ManifestSession secondSession = buildTestSession(testSession.getResearchProject(),
                ManifestSessionEjb.AccessioningProcessType.CRSP, COLLABORATOR_SAMPLE_ID_1);

        testSession.validateManifest();
        assertThat(testSession.hasErrors(), is(true));
        assertThat(testSession.getManifestEvents(), is(not(empty())));
        assertThat(testSession.getManifestEvents().size(), is(1));

        assertThat(secondSession.hasErrors(), is(false));
        assertThat(secondSession.getManifestEvents(), is(empty()));

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
        ManifestSession secondSession = buildTestSession(parentResearchProject, ManifestSessionEjb.AccessioningProcessType.CRSP, COLLABORATOR_SAMPLE_ID_1);
        buildTestSession(ManifestSessionEjb.AccessioningProcessType.CRSP).getResearchProject().setParentResearchProject(parentResearchProject);

        testSession.validateManifest();
        assertThat(testSession.hasErrors(), is(false));
        assertThat(testSession.getManifestEvents(), is(empty()));
        assertThat(testSession.getManifestEvents().size(), is(0));

        assertThat(secondSession.hasErrors(), is(false));
        assertThat(secondSession.getManifestEvents(), is(empty()));

    }

    public void mismatchedGenderTest() throws Exception {
        ManifestRecord manifestRecord = buildManifestRecord(testSession, ImmutableMap.of(
                Metadata.Key.SAMPLE_ID, "989282484", Metadata.Key.GENDER, "M", Metadata.Key.PATIENT_ID, VALUE_3));
        testSession.addRecord(manifestRecord);
        testSession.validateManifest();
        assertThat(testSession.hasErrors(), is(true));
    }

    private ManifestRecord buildManifestRecord(ManifestSession manifestSession, Map<Metadata.Key, String> metadata) {
        ManifestRecord manifestRecord = new ManifestRecord(ManifestTestFactory.buildMetadata(metadata));
        manifestRecord.setManifestSession(manifestSession);
        return manifestRecord;
    }

    public void mixedValidationErrorTest() throws Exception {

        ManifestRecord duplicateSampleRecord = buildManifestRecord(testSession,
            ImmutableMap.of(Metadata.Key.SAMPLE_ID, COLLABORATOR_SAMPLE_ID_1,
                    Metadata.Key.GENDER, VALUE_2, Metadata.Key.PATIENT_ID, "PI-3234"));
        testSession.addRecord(duplicateSampleRecord);

        ManifestRecord genderMisMatch = buildManifestRecord(testSession,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, "229249239",
                                Metadata.Key.GENDER, "M", Metadata.Key.PATIENT_ID, "PI-3234"));
        testSession.addRecord(genderMisMatch);
        testSession.validateManifest();

        assertThat(testSession.hasErrors(), is(true));

        assertThat(testSession.getManifestEvents(), is(not(empty())));
        assertThat(testSession.getManifestEvents().size(), is(4));
        assertThat(testRecord.getManifestEvents(), is(not(empty())));
        assertThat(testRecord.getManifestEvents().size(), is(1));

        assertThat(duplicateSampleRecord.getManifestEvents(), is(not(empty())));
        assertThat(duplicateSampleRecord.getManifestEvents().size(), is(2));

        assertThat(genderMisMatch.getManifestEvents(), is(not(empty())));
        assertThat(genderMisMatch.getManifestEvents().size(), is(1));
    }

    private ManifestSession buildTestSession(ManifestSessionEjb.AccessioningProcessType accessioningProcessType) {
        ResearchProject testProject = null;
        if(accessioningProcessType != ManifestSessionEjb.AccessioningProcessType.COVID) {
            testProject = ResearchProjectTestFactory.createTestResearchProject("RP-334");
        }

        return buildTestSession(testProject, accessioningProcessType);
    }

    private ManifestSession buildTestSession(ResearchProject testProject,
                                             ManifestSessionEjb.AccessioningProcessType accessioningProcessType,
                                             String... testRecords) {
        ManifestSession newTestSession =
                new ManifestSession(testProject, "DUPLICATE_TEST", new BSPUserList.QADudeUser("LU", 33L), false,
                        accessioningProcessType);
        for (String recordSampleId : testRecords) {
            buildManifestRecord(newTestSession, recordSampleId);
        }
        return newTestSession;
    }
}
