package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.collect.Iterables;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.UpdateData;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestEventMatcher.hasEventError;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Container tests for ManifestSessions.
 */
@Test(groups = TestGroups.STANDARD)
public class ManifestSessionContainerTest extends Arquillian {

    private static final String PATIENT_1 = "PATIENT 1";

    private static final String GENDER_MALE = "Male";
    private static final String GENDER_FEMALE = "Female";
    private static final String SAMPLE_ID_1 = "2482938";
    private static final String SAMPLE_ID_2 = "2342332";
    private static final String SAMPLE_ID_3 = "8482833";
    private static final String SAMPLE_ID_4 = "8284233";
    private static final String SAMPLE_ID_5 = "4772733";
    private static final String SAMPLE_ID_6 = "2244838";
    private static final String SAMPLE_ID_7 = "24829338";
    private static final String SAMPLE_ID_8 = "23423232";
    private static final String SAMPLE_ID_9 = "84821833";
    private static final String SAMPLE_ID_10 = "82844233";
    private static final String SAMPLE_ID_11 = "47732733";
    private static final String SAMPLE_ID_12 = "22424838";
    public static final int NUM_RECORDS_IN_SPREADSHEET = 23;

    private ResearchProject researchProject;
    private ManifestSession manifestSessionI;
    private ManifestRecord manifestRecordI;
    private ManifestSession manifestSessionII;
    private BSPUserList.QADudeUser testUser = new BSPUserList.QADudeUser("PM", 5176L);

    private List<String> firstUploadedScannedSamples =
            Arrays.asList("03101231193", "03101067213", "03101214167", "03101067211", "03101989209", "03101947686",
                    "03101892406", "03101757212", "03101064137", "03101231191", "03102000417", "03102000418",
                    "03101752021", "03101752020", "03101411324", "03101411323", "03101492492", "03101492495",
                    "03101254358", "03101254357", "03101254356", "03101170867"
                    //, "03101778110" omitting the last one so that there is an error
            );
    private String firstUploadedOmittedScan = "03101778110";
    private List<String> secondUploadPatientsWithMismatchedGender = Collections.singletonList("03101492492ZZZ");
    private List<String> secondUploadedSamplesGood =
            Arrays.asList("03101067213ZZZ", "03101214167ZZZ", "03101067211ZZZ", "03101989209ZZZ",
                    "03101947686ZZZ", "03101892406ZZZ", "03101757212ZZZ", "03101064137ZZZ", "03101231191ZZZ",
                    "03102000417ZZZ", "03102000418ZZZ", "03101752021ZZZ", "03101411324ZZZ",
                    "03101411323ZZZ", "03101492495ZZZ", "03101254358ZZZ", "03101254357ZZZ",
                    "03101254356ZZZ", "03101170867ZZZ", "03101778110ZZZ");
    private List<String> secondUploadedSamplesDupes = Arrays.asList("03101231193", "03101752020");
    private Map<String, MercurySample> sourceSampleToMercurySample;
    private Map<String, LabVessel> sourceSampleToTargetVessel;
    @Inject
    private MercurySampleDao mercurySampleDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ManifestSessionDao manifestSessionDao;

    @Inject
    private ManifestSessionEjb manifestSessionEjb;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private UserBean userBean;

    @BeforeMethod
    public void setUp() throws Exception {

        if(userBean == null) {
            return;
        }

        userBean.loginTestUser();

        Date researchProjectCreateTime = new Date();
        researchProject =
                ResearchProjectTestFactory.createTestResearchProject(ResearchProject.PREFIX + researchProjectCreateTime.getTime());
        researchProject.setTitle("Buick test Project" + researchProjectCreateTime.getTime());

        manifestSessionI = new ManifestSession(researchProject, "BUICK-TEST", testUser);
        manifestRecordI = createManifestRecord(Metadata.Key.PATIENT_ID, PATIENT_1, Metadata.Key.GENDER, GENDER_MALE,
                Metadata.Key.SAMPLE_ID, SAMPLE_ID_1);
        manifestSessionI.addRecord(manifestRecordI);

        manifestSessionI.addRecord(
                createManifestRecord(Metadata.Key.SAMPLE_ID, SAMPLE_ID_2, Metadata.Key.GENDER, GENDER_FEMALE,
                        Metadata.Key.PATIENT_ID, PATIENT_1 + "2"));
        manifestSessionI.addRecord(
                createManifestRecord(Metadata.Key.SAMPLE_ID, SAMPLE_ID_3, Metadata.Key.GENDER, GENDER_MALE,
                        Metadata.Key.PATIENT_ID, PATIENT_1 + "3"));
        manifestSessionI.addRecord(
                createManifestRecord(Metadata.Key.SAMPLE_ID, SAMPLE_ID_4, Metadata.Key.GENDER, GENDER_FEMALE,
                        Metadata.Key.PATIENT_ID, PATIENT_1 + "4"));
        manifestSessionI.addRecord(
                createManifestRecord(Metadata.Key.SAMPLE_ID, SAMPLE_ID_5, Metadata.Key.GENDER, GENDER_FEMALE,
                        Metadata.Key.PATIENT_ID, PATIENT_1 + "5"));
        manifestSessionI.addRecord(
                createManifestRecord(Metadata.Key.SAMPLE_ID, SAMPLE_ID_6, Metadata.Key.GENDER, GENDER_MALE,
                        Metadata.Key.PATIENT_ID, PATIENT_1 + "6"));

        manifestSessionII = new ManifestSession(researchProject, "BUICK-TEST2", testUser);

        manifestSessionII.addRecord(
                createManifestRecord(Metadata.Key.PATIENT_ID, PATIENT_1 + "7", Metadata.Key.GENDER, GENDER_MALE,
                        Metadata.Key.SAMPLE_ID, SAMPLE_ID_7));
        manifestSessionII.addRecord(
                createManifestRecord(Metadata.Key.SAMPLE_ID, SAMPLE_ID_8, Metadata.Key.GENDER, GENDER_FEMALE,
                        Metadata.Key.PATIENT_ID, PATIENT_1 + "8"));
        manifestSessionII.addRecord(
                createManifestRecord(Metadata.Key.SAMPLE_ID, SAMPLE_ID_9, Metadata.Key.GENDER, GENDER_MALE,
                        Metadata.Key.PATIENT_ID, PATIENT_1 + "9"));
        manifestSessionII.addRecord(
                createManifestRecord(Metadata.Key.SAMPLE_ID, SAMPLE_ID_10, Metadata.Key.GENDER, GENDER_FEMALE,
                        Metadata.Key.PATIENT_ID, PATIENT_1 + "10"));
        manifestSessionII.addRecord(
                createManifestRecord(Metadata.Key.SAMPLE_ID, SAMPLE_ID_11, Metadata.Key.GENDER, GENDER_FEMALE,
                        Metadata.Key.PATIENT_ID, PATIENT_1 + "11"));
        manifestSessionII.addRecord(
                createManifestRecord(Metadata.Key.SAMPLE_ID, SAMPLE_ID_12, Metadata.Key.GENDER, GENDER_MALE,
                        Metadata.Key.PATIENT_ID, PATIENT_1 + "12"));

        Date today = new Date();

        sourceSampleToMercurySample = new HashMap<>();
        sourceSampleToTargetVessel = new HashMap<>();

        // Generics warning.
        @SuppressWarnings("unchecked")
        Iterable<String> allSourceSamples = Iterables
                .concat(firstUploadedScannedSamples, secondUploadedSamplesDupes, secondUploadedSamplesGood,
                        secondUploadPatientsWithMismatchedGender, Collections.singleton(firstUploadedOmittedScan));

        for (String sourceSample : allSourceSamples) {
            sourceSampleToMercurySample.put(sourceSample, new MercurySample("SM_" + sourceSample + today.getTime(),
                    MercurySample.MetadataSource.MERCURY));
            sourceSampleToTargetVessel.put(sourceSample, new BarcodedTube("A0" + sourceSample + today.getTime(),
                    BarcodedTube.BarcodedTubeType.MatrixTube2mL));
            sourceSampleToTargetVessel.get(sourceSample).addSample(sourceSampleToMercurySample.get(sourceSample));
        }
    }

    private ManifestRecord createManifestRecord(Metadata.Key key1, String value1, Metadata.Key key2, String value2,
                                                Metadata.Key key3, String value3) {
        return new ManifestRecord(new Metadata(key1, value1), new Metadata(key2, value2), new Metadata(key3, value3));
    }

    /**
     * Round trip test for ManifestSession used in initial development to work out issues in mappings, but this
     * should still have value.
     */
    @Test(groups = TestGroups.STANDARD)
    public void roundTrip() {

        // Persist everything.
        manifestSessionDao.persist(manifestSessionI);
        manifestSessionDao.flush();

        assertThat(manifestSessionI.getResearchProject(), is(equalTo(researchProject)));
        assertThat(manifestSessionI.getManifestSessionId(), notNullValue());
        assertThat(manifestSessionI.hasErrors(), is(equalTo(false)));

        assertThat(manifestSessionII.getResearchProject(), is(equalTo(researchProject)));
        assertThat(manifestSessionII.getManifestSessionId(), notNullValue());
        assertThat(manifestSessionII.hasErrors(), is(equalTo(false)));
        assertThat(researchProject.getManifestSessions(), hasItems(manifestSessionI, manifestSessionII));

        assertThat(manifestSessionI.getUpdateData().getModifiedDate(), is(equalTo(
                manifestSessionI.getUpdateData().getCreatedDate())));

        for (ManifestRecord manifestRecord : manifestSessionI.getRecords()) {
            assertThat(manifestRecord.getUpdateData().getModifiedDate(), is(equalTo(manifestRecord.getUpdateData().getCreatedDate())));
        }


        // Clear the Session to force retrieval of a persistent instance 'manifestSessionOut' below that is distinct
        // from the detached 'manifestSessionI' instance.
        manifestSessionDao.clear();

        ManifestSession manifestSessionOut =
                manifestSessionDao.findById(ManifestSession.class, manifestSessionI.getManifestSessionId());

        assertThat(manifestSessionI.getRecords(), hasSize(equalTo(manifestSessionOut.getRecords().size())));
        assertThat(manifestSessionOut.getRecords(), hasSize(equalTo(6)));

        // Get the sole 'out' ManifestRecord for comparison with the sole 'in' ManifestRecord.
        // Just to make sure the annotations are correct
        ManifestRecord manifestRecordOut = manifestSessionOut.getRecords().get(0);
        assertThat(manifestRecordOut.getMetadata(), hasSize(equalTo(manifestRecordI.getMetadata().size())));
        for (Metadata metadata : manifestRecordI.getMetadata()) {
            String inValue = metadata.getValue();
            String outValue = manifestRecordOut.getValueByKey(metadata.getKey());
            assertThat(inValue, is(equalTo(outValue)));
        }

        assertThat(manifestRecordI.isQuarantined(), is(equalTo(false)));
        assertThat(manifestRecordOut.isQuarantined(), is(equalTo(manifestRecordI.isQuarantined())));

        manifestSessionOut.setStatus(ManifestSession.SessionStatus.COMPLETED);
        for (ManifestRecord manifestRecord : manifestSessionOut.getRecords()) {
            manifestRecord.setStatus(ManifestRecord.Status.UPLOAD_ACCEPTED);
        }

        manifestSessionDao.persist(manifestSessionOut);

        manifestSessionDao.flush();
        manifestSessionDao.clear();

        ManifestSession sessionClosed = manifestSessionDao.find(manifestSessionOut.getManifestSessionId());

        assertThat(sessionClosed.getUpdateData().getModifiedDate(), is(not(equalTo(manifestSessionI.getUpdateData().getModifiedDate()))));
        assertThat(sessionClosed.getUpdateData().getCreatedDate(), is(not(equalTo(sessionClosed.getUpdateData().getModifiedDate()))));
        for (ManifestRecord manifestRecord : sessionClosed.getRecords()) {
            assertThat(manifestRecord.getUpdateData().getCreatedDate(), is(not(equalTo(manifestRecord.getUpdateData().getModifiedDate()))));
        }

    }

    @Test(groups = TestGroups.STANDARD)
    public void endToEnd() throws Exception {
        /*
         * Setup required preliminary entities
         */
        for (LabVessel targetVessel : sourceSampleToTargetVessel.values()) {
            labVesselDao.persist(targetVessel);
        }
        researchProjectDao.persist(researchProject);

        /*
         * Mimic user upload of manifest
         */
        String excelFilePath = "manifest-upload/duplicates/good-manifest-1.xlsx";
        InputStream testStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(excelFilePath);

        ManifestSession uploadedSession =
                manifestSessionEjb.uploadManifest(researchProject.getBusinessKey(), testStream, excelFilePath,
                        testUser);
        UpdateData updateData = uploadedSession.getUpdateData();
        assertThat(updateData.getModifiedBy(), is(not(equalTo(updateData.getCreatedBy()))));
        assertThat(updateData.getModifiedDate(), is(equalTo(updateData.getCreatedDate())));

        assertThat(uploadedSession, is(notNullValue()));
        assertThat(uploadedSession.getManifestSessionId(), is(notNullValue()));
        assertThat(uploadedSession.hasErrors(), is(false));
        assertThat(uploadedSession.getResearchProject(), is(equalTo(researchProject)));
        assertThat(uploadedSession.getRecords(), hasSize(NUM_RECORDS_IN_SPREADSHEET));

        for (ManifestRecord manifestRecord : uploadedSession.getRecords()) {
            assertThat(manifestRecord.getManifestRecordId(), is(notNullValue()));
        }

        /*
         *  Test that the session is listed in all open sessions and not listed in the closed sessions
         */
        List<ManifestSession> openSessions = manifestSessionDao.findOpenSessions();

        assertThat(openSessions, hasItem(uploadedSession));
        assertThat(manifestSessionDao.findClosedSessions(), not(hasItem(uploadedSession)));

        String UPLOADED_COLLABORATOR_SESSION_1 = "03101067213";
        String UPLOADED_PATIENT_ID_SESSION_1 = "001-001";
        assertThat(uploadedSession.findRecordByCollaboratorId(UPLOADED_COLLABORATOR_SESSION_1)
                .getValueByKey(Metadata.Key.PATIENT_ID), is(UPLOADED_PATIENT_ID_SESSION_1));

        manifestSessionDao.clear();

        /*
         *  Mimic user accepting the upload
         */
        manifestSessionEjb.acceptManifestUpload(uploadedSession.getManifestSessionId());

        ManifestSession acceptedSession = manifestSessionDao.find(uploadedSession.getManifestSessionId());

        assertThat(acceptedSession, is(notNullValue()));
        assertThat(acceptedSession.getManifestSessionId(), is(notNullValue()));
        assertThat(acceptedSession.hasErrors(), is(false));

        Iterable<String> allFirstUploadedSamples =
                Iterables.concat(firstUploadedScannedSamples, Collections.singleton(firstUploadedOmittedScan));

        for (ManifestRecord manifestRecord : acceptedSession.getRecords()) {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
            // Make sure nothing comes back that wasn't in the list of samples uploaded.  firstUploadedOmittedScan
            // is in the manifest, but isn't going to be scanned.
            assertThat(allFirstUploadedSamples, hasItem(manifestRecord.getSampleId()));
        }

        // Clear the session to reload the ManifestSession.
        manifestSessionDao.clear();
        ManifestSession sessionOfScan = manifestSessionDao.find(uploadedSession.getManifestSessionId());

        /*
         *  Mimic the scan of all samples in the manifest... except One (to test closing and validating with an
         *  omitted scan.
         */
        for (String sourceSampleToTest : firstUploadedScannedSamples) {

            manifestSessionEjb.accessionScan(sessionOfScan.getManifestSessionId(), sourceSampleToTest);

            ManifestRecord sourceRecordToTest = sessionOfScan.findRecordByCollaboratorId(sourceSampleToTest);

            assertThat(sourceRecordToTest.getStatus(), is(ManifestRecord.Status.SCANNED));
            assertThat(sessionOfScan.hasErrors(), is(false));
        }

        /*
         * Mimic the user getting a "Pre-close" validation
         */
        ManifestStatus sessionStatus = manifestSessionEjb.getSessionStatus(sessionOfScan.getManifestSessionId());

        assertThat(sessionStatus, is(notNullValue()));
        assertThat(sessionStatus.getSamplesInManifest(), is(NUM_RECORDS_IN_SPREADSHEET));
        // Deliberately missed one scan.
        assertThat(sessionStatus.getSamplesSuccessfullyScanned(), is(NUM_RECORDS_IN_SPREADSHEET - 1));
        assertThat(sessionStatus.getSamplesEligibleInManifest(), is(NUM_RECORDS_IN_SPREADSHEET));
        assertThat(sessionStatus.getErrorMessages(), is(not(empty())));
        assertThat(sessionStatus.getErrorMessages(), hasItem(ManifestRecord.ErrorStatus.MISSING_SAMPLE
                .formatMessage(ManifestSession.SAMPLE_ID_KEY, firstUploadedOmittedScan)));

        /*
         * Mimic a user closing the session, and the ramifications of that.
         */
        manifestSessionEjb.closeSession(sessionOfScan.getManifestSessionId());

        // Clear to force a reload.
        manifestSessionDao.clear();
        ManifestSession closedSession = manifestSessionDao.find(sessionOfScan.getManifestSessionId());

        assertThat(closedSession.getUpdateData().getModifiedDate(), is(not(equalTo(updateData.getModifiedDate()))));

        assertThat(closedSession, is(notNullValue()));

        assertThat(closedSession.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));
        assertThat(closedSession.getManifestEvents(), hasSize(1));
        assertThat(closedSession.getManifestEvents(), hasEventError(ManifestRecord.ErrorStatus.MISSING_SAMPLE));

        for (ManifestRecord manifestRecord : closedSession.getRecords()) {
            if (firstUploadedOmittedScan.equals(manifestRecord.getSampleId())) {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
                assertThat(manifestRecord.getManifestEvents(), hasSize(1));
                assertThat(manifestRecord.getManifestEvents(),
                        hasEventError(ManifestRecord.ErrorStatus.MISSING_SAMPLE));
            } else {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
            }
        }

        /*
         * Validate that the session is in the list of closed sessions.
         */
        List<ManifestSession> closedSessions = manifestSessionDao.findClosedSessions();
        assertThat(closedSessions, hasItem(closedSession));

        /*
         * Mimic the user actually performing the tube scans for transferring the material to Broad tubes
         */
        for (String sourceSampleToTest : firstUploadedScannedSamples) {

            ManifestRecord manifestRecord = manifestSessionEjb
                    .validateSourceTubeForTransfer(closedSession.getManifestSessionId(), sourceSampleToTest);
            assertThat(manifestRecord.getManifestSession().getManifestSessionId(), is(equalTo(
                    closedSession.getManifestSessionId())));

            String sourceSampleKey = sourceSampleToMercurySample.get(sourceSampleToTest).getSampleKey();

            MercurySample targetSample = manifestSessionEjb.validateTargetSample(sourceSampleKey);
            assertThat(targetSample, is(notNullValue()));

            String sourceSampleLabel = sourceSampleToTargetVessel.get(sourceSampleToTest).getLabel();

            LabVessel targetVessel =
                    manifestSessionEjb.validateTargetSampleAndVessel(sourceSampleKey, sourceSampleLabel);
            assertThat(targetVessel, is(notNullValue()));

            MercurySample mercurySample = sourceSampleToMercurySample.get(sourceSampleToTest);
            manifestSessionEjb.transferSample(closedSession.getManifestSessionId(), sourceSampleToTest,
                    mercurySample.getSampleKey(), sourceSampleLabel, testUser);

            // Make sure the metadata was persisted with the sample.
            manifestSessionDao.clear();
            MercurySample otherSample =
                    mercurySampleDao.findById(MercurySample.class, mercurySample.getMercurySampleId());
            assertThat(otherSample.getMetadata(), is(not(empty())));

            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE));
        }

        /*
         * Mimic the user accidentally attempting to transfer the tube that was never scanned before session was
         * closed
         */
        try {
            manifestSessionEjb
                    .validateSourceTubeForTransfer(closedSession.getManifestSessionId(), firstUploadedOmittedScan);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    containsString(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE.getBaseMessage()));
        }
        String omittedScanSampleKey = sourceSampleToMercurySample.get(firstUploadedOmittedScan).getSampleKey();
        String omittedScanSampleLabel = sourceSampleToTargetVessel.get(firstUploadedOmittedScan).getLabel();

        MercurySample targetSampleForOmittedScan = manifestSessionEjb.validateTargetSample(omittedScanSampleKey);
        assertThat(targetSampleForOmittedScan, is(notNullValue()));

        LabVessel targetVesselForOmittedScan =
                manifestSessionEjb.validateTargetSampleAndVessel(omittedScanSampleKey, omittedScanSampleLabel);

        assertThat(targetVesselForOmittedScan, is(notNullValue()));

        try {
            manifestSessionEjb.transferSample(closedSession.getManifestSessionId(), firstUploadedOmittedScan,
                    omittedScanSampleKey, omittedScanSampleLabel, testUser);
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    containsString(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE.getBaseMessage()));
        }

        /*
         *   ============  Second Upload   =============
         *
         *  This upload will contain 2 records that are duplicates of samples found in the first manifest.  it will
         *  also contain one record that shares a patient ID with the first manifest but the gender is listed
         *  differently
         */
        int NUM_DUPLICATES_IN_SESSION_2 = 2;
        int NUM_MISMATCHED_GENDERS_IN_SESSION_2 = 1;
        manifestSessionDao.clear();

        /*
         * Mimic the user uploading the manifest
         */
        String pathToTestFile2 = "manifest-upload/duplicates/good-manifest-3.xlsx";
        InputStream testStream2 = Thread.currentThread().getContextClassLoader().getResourceAsStream(pathToTestFile2);
        ResearchProject rpSecondUpload = researchProjectDao.findByBusinessKey(researchProject.getBusinessKey());
        ManifestSession uploadedSession2 =
                manifestSessionEjb.uploadManifest(rpSecondUpload.getBusinessKey(), testStream2, pathToTestFile2,
                        testUser);

        assertThat(uploadedSession2, is(notNullValue()));
        assertThat(uploadedSession2.getManifestSessionId(), is(notNullValue()));
        assertThat(uploadedSession2.hasErrors(), is(true));
        assertThat(uploadedSession2.getResearchProject(), is(equalTo(rpSecondUpload)));

        assertThat(uploadedSession2.getRecords(), hasSize(NUM_RECORDS_IN_SPREADSHEET));

        for (ManifestRecord manifestRecord : uploadedSession2.getRecords()) {
            assertThat(manifestRecord.getManifestRecordId(), is(notNullValue()));
        }

        for (String dupeSampleId : secondUploadedSamplesDupes) {
            ManifestRecord dupeRecord = uploadedSession2.findRecordByCollaboratorId(dupeSampleId);
            assertThat(dupeRecord.isQuarantined(), is(true));
            assertThat(dupeRecord.getSampleId(), is(dupeSampleId));
        }
        for (String genderSample : secondUploadPatientsWithMismatchedGender) {
            ManifestRecord dupeRecord = uploadedSession2.findRecordByCollaboratorId(genderSample);
            assertThat(dupeRecord.isQuarantined(), is(false));
            assertThat(dupeRecord.getSampleId(), is(genderSample));
        }

        /*
         * Check that the new upload is in the list of uploaded sessions and not in the list of closed sessions
         */
        List<ManifestSession> checkOpenSessions = manifestSessionDao.findOpenSessions();
        List<ManifestSession> checkClosedSessions = manifestSessionDao.findClosedSessions();

        assertThat(checkOpenSessions, hasItem(uploadedSession2));
        assertThat(checkClosedSessions, not(hasItem(uploadedSession2)));

        manifestSessionDao.clear();

        /*
         * mimic the user accepting the second manifest
         */
        manifestSessionEjb.acceptManifestUpload(uploadedSession2.getManifestSessionId());

        ManifestSession acceptedSession2 = manifestSessionDao.find(uploadedSession2.getManifestSessionId());

        assertThat(acceptedSession2, is(notNullValue()));
        assertThat(acceptedSession2.getManifestSessionId(), is(notNullValue()));
        assertThat(acceptedSession2.hasErrors(), is(true));

        for (String collaboratorSampleId : secondUploadedSamplesDupes) {
            ManifestRecord record = acceptedSession2.findRecordByCollaboratorId(collaboratorSampleId);
            assertThat(record.isQuarantined(), is(true));
            assertThat(record.getStatus(), is(ManifestRecord.Status.UPLOADED));
        }

        for (String collaboratorSampleId : secondUploadPatientsWithMismatchedGender) {
            ManifestRecord record = acceptedSession2.findRecordByCollaboratorId(collaboratorSampleId);
            assertThat(record.isQuarantined(), is(false));
            assertThat(record.getManifestEvents(), hasSize(1));
            assertThat(record.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
        }
        for (String collaboratorSampleId : secondUploadedSamplesGood) {
            ManifestRecord record = acceptedSession2.findRecordByCollaboratorId(collaboratorSampleId);
            assertThat(record.getManifestEvents(), is(empty()));
            assertThat(record.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
        }
        manifestSessionDao.clear();

        ManifestSession sessionOfScan2 = manifestSessionDao.find(acceptedSession2.getManifestSessionId());

        /*
         * Mimic the user Scanning the tubes to complete accessioning
         */
        for (String sampleId : secondUploadedSamplesGood) {
            manifestSessionEjb.accessionScan(sessionOfScan2.getManifestSessionId(), sampleId);

            ManifestRecord sourceToTest = sessionOfScan2.findRecordByCollaboratorId(sampleId);

            assertThat(sourceToTest.getStatus(), is(ManifestRecord.Status.SCANNED));
            assertThat(sourceToTest.getManifestEvents(), is(empty()));
            assertThat(sessionOfScan2.hasErrors(), is(true));
        }

        /*
         * Mimic the user attempting to scan the dupes accidentally
         */
        for (String sampleId : secondUploadedSamplesDupes) {
            try {
                manifestSessionEjb.accessionScan(sessionOfScan2.getManifestSessionId(), sampleId);
                Assert.fail();
            } catch (Exception e) {
                assertThat(e.getMessage(), containsString(
                        ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.getBaseMessage()));
            }
        }

        /*
         * mimic the user scanning the tube(s) for the record(s) that contain mismatched gender
         *
         */
        sessionOfScan2 = manifestSessionDao.find(acceptedSession2.getManifestSessionId());
        for (String sampleId : secondUploadPatientsWithMismatchedGender) {
            manifestSessionEjb.accessionScan(sessionOfScan2.getManifestSessionId(), sampleId);

            ManifestRecord sourceToTest = sessionOfScan2.findRecordByCollaboratorId(sampleId);

            assertThat(sourceToTest.getStatus(), is(ManifestRecord.Status.SCANNED));
            assertThat(sourceToTest.getManifestEvents(), hasSize(NUM_MISMATCHED_GENDERS_IN_SESSION_2));
            assertThat(sessionOfScan2.hasErrors(), is(true));
        }

        /*
         * mimic the user preparing to close the session and getting a summary of events that occurred.
         */
        ManifestStatus sessionStatus2 = manifestSessionEjb.getSessionStatus(sessionOfScan2.getManifestSessionId());

        assertThat(sessionStatus2, is(notNullValue()));
        assertThat(sessionStatus2.getSamplesInManifest(), is(NUM_RECORDS_IN_SPREADSHEET));
        assertThat(sessionStatus2.getSamplesSuccessfullyScanned(), is(NUM_RECORDS_IN_SPREADSHEET - NUM_DUPLICATES_IN_SESSION_2));
        assertThat(sessionStatus2.getSamplesEligibleInManifest(), is(NUM_RECORDS_IN_SPREADSHEET - NUM_DUPLICATES_IN_SESSION_2));
        assertThat(sessionStatus2.getErrorMessages(), hasSize(NUM_DUPLICATES_IN_SESSION_2 + NUM_MISMATCHED_GENDERS_IN_SESSION_2));

        /*
         * Mimic the user closing the session
         */
        manifestSessionEjb.closeSession(sessionOfScan2.getManifestSessionId());
        ManifestSession closedSession2 = manifestSessionDao.find(sessionOfScan2.getManifestSessionId());

        assertThat(closedSession2, is(notNullValue()));

        assertThat(closedSession2.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));
        assertThat(closedSession2.getManifestEvents(), hasSize(NUM_DUPLICATES_IN_SESSION_2 + NUM_MISMATCHED_GENDERS_IN_SESSION_2));
        assertThat(closedSession2.getManifestEvents(), hasEventError(ManifestRecord.ErrorStatus.MISMATCHED_GENDER));
        assertThat(closedSession2.getManifestEvents(), hasEventError(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID));

        for (ManifestRecord manifestRecord : closedSession2.getRecords()) {
            if (secondUploadedSamplesDupes.contains(manifestRecord.getSampleId())) {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOADED));
                assertThat(manifestRecord.getManifestEvents(), hasSize(1));
                assertThat(manifestRecord.getManifestEvents(),
                        hasEventError(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID));
            } else {
                if (secondUploadPatientsWithMismatchedGender.contains(manifestRecord.getSampleId())) {
                    assertThat(manifestRecord.getManifestEvents(), hasSize(1));
                    assertThat(manifestRecord.getManifestEvents(),
                            hasEventError(ManifestRecord.ErrorStatus.MISMATCHED_GENDER));
                }
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
            }
        }

        List<ManifestSession> closedSessions2 = manifestSessionDao.findClosedSessions();

        assertThat(closedSessions2, hasItem(closedSession2));

        /*
         * mimic the user performing the tube transfer
         */
        for (String sourceSampleToTest : secondUploadedSamplesGood) {

            ManifestRecord manifestRecord = manifestSessionEjb
                    .validateSourceTubeForTransfer(closedSession2.getManifestSessionId(), sourceSampleToTest);
            assertThat(manifestRecord.getManifestSession(), is(closedSession2));

            String sourceSampleKey = sourceSampleToMercurySample.get(sourceSampleToTest).getSampleKey();
            MercurySample targetSample = manifestSessionEjb.validateTargetSample(sourceSampleKey);
            assertThat(targetSample, is(notNullValue()));
            String sourceSampleLabel = sourceSampleToTargetVessel.get(sourceSampleToTest).getLabel();

            LabVessel targetVessel =
                    manifestSessionEjb.validateTargetSampleAndVessel(sourceSampleKey, sourceSampleLabel);
            assertThat(targetVessel, is(notNullValue()));

            manifestSessionEjb.transferSample(closedSession2.getManifestSessionId(), sourceSampleToTest,
                    sourceSampleKey, sourceSampleLabel, testUser);

            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE));
        }

        for (String sourceSampleToTest : secondUploadPatientsWithMismatchedGender) {
            ManifestRecord manifestRecord = manifestSessionEjb
                    .validateSourceTubeForTransfer(closedSession2.getManifestSessionId(), sourceSampleToTest);
            assertThat(manifestRecord.getManifestSession(), is(closedSession2));

            String sourceSampleKey = sourceSampleToMercurySample.get(sourceSampleToTest).getSampleKey();
            MercurySample targetSample = manifestSessionEjb.validateTargetSample(sourceSampleKey);
            assertThat(targetSample, is(notNullValue()));
            String sourceSampleLabel = sourceSampleToTargetVessel.get(sourceSampleToTest).getLabel();
            LabVessel targetVessel =
                    manifestSessionEjb.validateTargetSampleAndVessel(sourceSampleKey, sourceSampleLabel);
            assertThat(targetVessel, is(notNullValue()));

            manifestSessionEjb.transferSample(closedSession2.getManifestSessionId(), sourceSampleToTest,
                    sourceSampleKey, sourceSampleLabel, testUser);

            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE));
        }

        /*
         * mimic the user accidentally performing tube transfer on the duplicate tubes
         */
        for (String sourceSampleToTest : secondUploadedSamplesDupes) {
            try {
                manifestSessionEjb
                        .validateSourceTubeForTransfer(closedSession2.getManifestSessionId(), sourceSampleToTest);
            } catch (Exception e) {
                assertThat(e.getMessage(), containsString(
                        ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE.getBaseMessage()));
            }
        }
    }
}
