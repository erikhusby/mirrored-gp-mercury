package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private ResearchProject researchProject;
    private ManifestSession manifestSessionI;
    private ManifestRecord manifestRecordI;
    private ManifestRecord manifestRecordIn2;
    private ManifestRecord manifestRecordIn3;
    private ManifestRecord manifestRecordIn4;
    private ManifestRecord manifestRecordIn5;
    private ManifestRecord manifestRecordIn6;
    private ManifestSession manifestSessionII;
    private ManifestRecord manifestRecordII1;
    private ManifestRecord manifestRecordII3;
    private ManifestRecord manifestRecordII4;
    private ManifestRecord manifestRecordII5;
    private ManifestRecord manifestRecordII6;
    private ManifestRecord manifestRecordII2;
    public BSPUserList.QADudeUser testUser;
    public static String UPLOADED_COLLABORATOR_SESSION_1;
    public static String UPLOADED_PATIENT_ID_SESSION_1;

    public Set<Long> sessionsToDelete;
    public List<String> firstUploadedSessionSamples;
    public String firstUplaodedOmittedScan;
    public List<String> secondUploadPatientsWithMismatchedGender;
    public List<String> secondUploadedSamplesGood;
    public List<String> secondUploadedSamplesDupes;
    public Map<String, MercurySample> sourceSampleToMercurySample;
    public Map<String, LabVessel> sourceSampleToTargetVessel;

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

    @BeforeMethod
    public void setUp() throws Exception {
        researchProject =
                ResearchProjectTestFactory.createTestResearchProject(ResearchProject.PREFIX + (new Date()).getTime());
        manifestSessionI = new ManifestSession(researchProject, "BUICK-TEST",
                new BSPUserList.QADudeUser("PM", 5176L));
        manifestRecordI = new ManifestRecord(new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1),
                new Metadata(Metadata.Key.GENDER, GENDER_MALE), new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_1));
        manifestRecordIn2 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_2),
                new Metadata(Metadata.Key.GENDER, GENDER_FEMALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "2"));
        manifestRecordIn3 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_3),
                new Metadata(Metadata.Key.GENDER, GENDER_MALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "3"));
        manifestRecordIn4 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_4),
                new Metadata(Metadata.Key.GENDER, GENDER_FEMALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "4"));
        manifestRecordIn5 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_5),
                new Metadata(Metadata.Key.GENDER, GENDER_FEMALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "5"));
        manifestRecordIn6 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_6),
                new Metadata(Metadata.Key.GENDER, GENDER_MALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "6"));
        manifestSessionI.addRecord(manifestRecordI);
        manifestSessionI.addRecord(manifestRecordIn2);
        manifestSessionI.addRecord(manifestRecordIn3);
        manifestSessionI.addRecord(manifestRecordIn4);
        manifestSessionI.addRecord(manifestRecordIn5);
        manifestSessionI.addRecord(manifestRecordIn6);

        testUser = new BSPUserList.QADudeUser("PM", 5176L);
        manifestSessionII = new ManifestSession(researchProject, "BUICK-TEST2",
                testUser);

        manifestRecordII1 = new ManifestRecord(new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "7"),
                new Metadata(Metadata.Key.GENDER, GENDER_MALE), new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_7));
        manifestRecordII2 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_8),
                new Metadata(Metadata.Key.GENDER, GENDER_FEMALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "8"));
        manifestRecordII3 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_9),
                new Metadata(Metadata.Key.GENDER, GENDER_MALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "9"));
        manifestRecordII4 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_10),
                new Metadata(Metadata.Key.GENDER, GENDER_FEMALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "10"));
        manifestRecordII5 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_11),
                new Metadata(Metadata.Key.GENDER, GENDER_FEMALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "11"));
        manifestRecordII6 = new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, SAMPLE_ID_12),
                new Metadata(Metadata.Key.GENDER, GENDER_MALE),
                new Metadata(Metadata.Key.PATIENT_ID, PATIENT_1 + "12"));

        manifestSessionII.addRecord(manifestRecordII1);
        manifestSessionII.addRecord(manifestRecordII2);
        manifestSessionII.addRecord(manifestRecordII3);
        manifestSessionII.addRecord(manifestRecordII4);
        manifestSessionII.addRecord(manifestRecordII5);
        manifestSessionII.addRecord(manifestRecordII6);

        UPLOADED_COLLABORATOR_SESSION_1 = "03101067213";
        UPLOADED_PATIENT_ID_SESSION_1 = "001-001";

        sessionsToDelete = new HashSet<>();

        Date today = new Date();

        firstUploadedSessionSamples =
                Arrays.asList("03101231193", "03101067213", "03101214167", "03101067211", "03101989209", "03101947686",
                        "03101892406", "03101757212", "03101064137", "03101231191", "03102000417", "03102000418",
                        "03101752021", "03101752020", "03101411324", "03101411323", "03101492492", "03101492495",
                        "03101254358", "03101254357", "03101254356", "03101170867"
                        //, "03101778110" omitting the last one so that there is an error
                );
        firstUplaodedOmittedScan = "03101778110";
//        secondUploadPatientsWithMismatchedGender = Collections.singletonList("005-010");
        secondUploadPatientsWithMismatchedGender = Collections.singletonList("03101492492ZZZ");
        secondUploadedSamplesGood =
                Arrays.asList("03101067213ZZZ", "03101214167ZZZ", "03101067211ZZZ", "03101989209ZZZ",
                        "03101947686ZZZ", "03101892406ZZZ", "03101757212ZZZ", "03101064137ZZZ", "03101231191ZZZ",
                        "03102000417ZZZ", "03102000418ZZZ", "03101752021ZZZ", "03101411324ZZZ",
                        "03101411323ZZZ", "03101492495ZZZ", "03101254358ZZZ", "03101254357ZZZ",
                        "03101254356ZZZ", "03101170867ZZZ", "03101778110ZZZ");
        secondUploadedSamplesDupes = Arrays.asList("03101231193", "03101752020");

        sourceSampleToMercurySample = new HashMap<>();
        sourceSampleToTargetVessel = new HashMap<>();

        for (String sourceSample : firstUploadedSessionSamples) {
            sourceSampleToMercurySample.put(sourceSample, new MercurySample("SM_" + sourceSample + today.getTime(),
                    MercurySample.MetadataSource.MERCURY));
            sourceSampleToTargetVessel.put(sourceSample, new BarcodedTube("A0" + sourceSample + today.getTime(),
                    BarcodedTube.BarcodedTubeType.MatrixTube2mL));
            sourceSampleToTargetVessel.get(sourceSample).addSample(sourceSampleToMercurySample.get(sourceSample));
        }
        for (String sourceSample : secondUploadedSamplesDupes) {
            sourceSampleToMercurySample.put(sourceSample, new MercurySample("SM_" + sourceSample + today.getTime(),
                    MercurySample.MetadataSource.MERCURY));
            sourceSampleToTargetVessel.put(sourceSample, new BarcodedTube("A0" + sourceSample + today.getTime(),
                    BarcodedTube.BarcodedTubeType.MatrixTube2mL));
            sourceSampleToTargetVessel.get(sourceSample).addSample(sourceSampleToMercurySample.get(sourceSample));
        }
        for (String sourceSample : secondUploadedSamplesGood) {
            sourceSampleToMercurySample.put(sourceSample, new MercurySample("SM_" + sourceSample + today.getTime(),
                    MercurySample.MetadataSource.MERCURY));
            sourceSampleToTargetVessel.put(sourceSample, new BarcodedTube("A0" + sourceSample + today.getTime(),
                    BarcodedTube.BarcodedTubeType.MatrixTube2mL));
            sourceSampleToTargetVessel.get(sourceSample).addSample(sourceSampleToMercurySample.get(sourceSample));
        }
        for (String sourceSample : secondUploadPatientsWithMismatchedGender) {
            sourceSampleToMercurySample.put(sourceSample, new MercurySample("SM_" + sourceSample + today.getTime(),
                    MercurySample.MetadataSource.MERCURY));
            sourceSampleToTargetVessel.put(sourceSample, new BarcodedTube("A0" + sourceSample + today.getTime(),
                    BarcodedTube.BarcodedTubeType.MatrixTube2mL));
            sourceSampleToTargetVessel.get(sourceSample).addSample(sourceSampleToMercurySample.get(sourceSample));
        }

        sourceSampleToMercurySample
                .put(firstUplaodedOmittedScan, new MercurySample("SM_" + firstUplaodedOmittedScan + today.getTime(),
                        MercurySample.MetadataSource.MERCURY));
        sourceSampleToTargetVessel
                .put(firstUplaodedOmittedScan, new BarcodedTube("A0" + firstUplaodedOmittedScan + today.getTime(),
                        BarcodedTube.BarcodedTubeType.MatrixTube2mL));
        sourceSampleToTargetVessel.get(firstUplaodedOmittedScan).addSample(
                sourceSampleToMercurySample.get(firstUplaodedOmittedScan));


    }

    /**
     * Round trip test for ManifestSession.
     */
    @Test(groups = TestGroups.STANDARD)
    public void roundTrip() {

        // Create and persist ManifestSession.

        // Create ManifestSessionII.

        //Persist Everything
        manifestSessionDao.persist(manifestSessionI);
        manifestSessionDao.flush();

        assertThat(manifestSessionI.getResearchProject(), is(equalTo(researchProject)));
        assertThat(manifestSessionI.getManifestSessionId(), notNullValue());
        sessionsToDelete.add(manifestSessionI.getManifestSessionId());
        assertThat(manifestSessionI.hasErrors(), is(equalTo(false)));

        assertThat(manifestSessionII.getResearchProject(), is(equalTo(researchProject)));
        assertThat(manifestSessionII.getManifestSessionId(), notNullValue());
        sessionsToDelete.add(manifestSessionII.getManifestSessionId());
        assertThat(manifestSessionII.hasErrors(), is(equalTo(false)));
        assertThat(researchProject.getManifestSessions(), hasItems(manifestSessionI, manifestSessionII));

        // Clear the Session to force retrieval of a persistent instance 'manifestSessionOut' below that is distinct
        // from the detached 'manifestSessionI' instance.
        manifestSessionDao.clear();

        ManifestSession manifestSessionOut =
                manifestSessionDao.findById(ManifestSession.class, manifestSessionI.getManifestSessionId());

        assertThat(manifestSessionI.getRecords().size(), is(equalTo(manifestSessionOut.getRecords().size())));
        assertThat(manifestSessionOut.getRecords().size(), is(equalTo(6)));

        // Get the sole 'out' ManifestRecord for comparison with the sole 'in' ManifestRecord.
        //Just to make sure the annotations are correct
        ManifestRecord manifestRecordOut = manifestSessionOut.getRecords().get(0);
        assertThat(manifestRecordOut.getMetadata().size(), is(equalTo(manifestRecordI.getMetadata().size())));
        for (Metadata metadata : manifestRecordI.getMetadata()) {
            String inValue = metadata.getValue();
            String outValue = manifestRecordOut.getMetadataByKey(metadata.getKey()).getValue();
            assertThat(inValue, is(equalTo(outValue)));
        }

        assertThat(manifestRecordI.isQuarantined(), is(equalTo(false)));
        assertThat(manifestRecordOut.isQuarantined(), is(equalTo(manifestRecordI.isQuarantined())));

    }

    @Test(groups = TestGroups.STANDARD)
    public void endToEnd() throws Exception {

        for (LabVessel targetVessel : sourceSampleToTargetVessel.values()) {
            labVesselDao.persist(targetVessel);
        }


        researchProjectDao.persist(researchProject);

        String excelFilePath = "manifest-upload/duplicates/good-manifest-1.xlsx";
        InputStream testStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(excelFilePath);

        ManifestSession uploadedSession =
                manifestSessionEjb.uploadManifest(researchProject.getBusinessKey(), testStream, excelFilePath,
                        testUser);

        assertThat(uploadedSession, is(notNullValue()));
        assertThat(uploadedSession.getManifestSessionId(), is(notNullValue()));
        sessionsToDelete.add(uploadedSession.getManifestSessionId());
        assertThat(uploadedSession.hasErrors(), is(false));
        assertThat(uploadedSession.getResearchProject(), is(equalTo(researchProject)));

        assertThat(uploadedSession.getRecords().size(), is(23));

        for (ManifestRecord manifestRecord : uploadedSession.getRecords()) {
            assertThat(manifestRecord.getManifestRecordId(), is(notNullValue()));
        }

        List<ManifestSession> openSessions = manifestSessionDao.findOpenSessions();

        assertThat(openSessions, hasItem(uploadedSession));

        assertThat(uploadedSession.findRecordByCollaboratorId(UPLOADED_COLLABORATOR_SESSION_1).getMetadataByKey(
                Metadata.Key.PATIENT_ID).getValue(), is(UPLOADED_PATIENT_ID_SESSION_1));

        manifestSessionDao.clear();

        manifestSessionEjb.acceptManifestUpload(uploadedSession.getManifestSessionId());

        ManifestSession acceptedSession = manifestSessionDao.find(uploadedSession.getManifestSessionId());

        assertThat(acceptedSession, is(notNullValue()));
        assertThat(acceptedSession.getManifestSessionId(), is(notNullValue()));
        assertThat(acceptedSession.hasErrors(), is(false));
        for (ManifestRecord manifestRecord : acceptedSession.getRecords()) {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
            if (!manifestRecord.getSampleId().equals(firstUplaodedOmittedScan)) {
                assertThat(firstUploadedSessionSamples, hasItem(manifestRecord.getSampleId()));
            }
        }

        manifestSessionDao.clear();

        ManifestSession sessionOfScan = manifestSessionDao.find(uploadedSession.getManifestSessionId());
        for (String sourceSampleToTest : firstUploadedSessionSamples) {

            manifestSessionEjb.accessionScan(sessionOfScan.getManifestSessionId(), sourceSampleToTest);


            ManifestRecord sourceRecordToTest = sessionOfScan.findRecordByCollaboratorId(sourceSampleToTest);

            assertThat(sourceRecordToTest.getStatus(), is(ManifestRecord.Status.SCANNED));
            assertThat(sessionOfScan.hasErrors(), is(false));
        }

        ManifestStatus sessionStatus = manifestSessionEjb.getSessionStatus(sessionOfScan.getManifestSessionId());

        assertThat(sessionStatus, is(notNullValue()));
        assertThat(sessionStatus.getSamplesInManifest(), is(23));
        assertThat(sessionStatus.getSamplesSuccessfullyScanned(), is(22));
        assertThat(sessionStatus.getSamplesEligibleInManifest(), is(23));
        assertThat(sessionStatus.getErrorMessages(), is(not(empty())));
        assertThat(sessionStatus.getErrorMessages(),
                hasItem(ManifestRecord.ErrorStatus.MISSING_SAMPLE
                        .formatMessage(ManifestSession.SAMPLE_ID_KEY, firstUplaodedOmittedScan)));


        manifestSessionEjb.closeSession(sessionOfScan.getManifestSessionId());

        ManifestSession closedSession = manifestSessionDao.find(sessionOfScan.getManifestSessionId());

        assertThat(closedSession, is(notNullValue()));

        assertThat(closedSession.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));
        assertThat(closedSession.getManifestEvents().size(), is(1));
        assertThat(closedSession.getManifestEvents(), hasEventError(ManifestRecord.ErrorStatus.MISSING_SAMPLE));

        for (ManifestRecord manifestRecord : closedSession.getRecords()) {
            if (StringUtils.equals(firstUplaodedOmittedScan, manifestRecord.getSampleId())) {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
                assertThat(manifestRecord.getManifestEvents().size(), is(1));
                assertThat(manifestRecord.getManifestEvents(), hasEventError(ManifestRecord.ErrorStatus.MISSING_SAMPLE));
            } else {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
            }
        }

        List<ManifestSession> closedSessions = manifestSessionDao.findClosedSessions();

        assertThat(closedSessions, Matchers.hasItem(closedSession));

        for (String sourceSampleToTest : firstUploadedSessionSamples) {

            ManifestRecord manifestRecord = manifestSessionEjb
                    .validateSourceTubeForTransfer(closedSession.getManifestSessionId(), sourceSampleToTest);
            assertThat(manifestRecord.getManifestSession(), is(closedSession));

            MercurySample targetSample = manifestSessionEjb
                    .validateTargetSample(sourceSampleToMercurySample.get(sourceSampleToTest).getSampleKey());
            assertThat(targetSample, is(notNullValue()));
            LabVessel targetVessel =
                    manifestSessionEjb.validateTargetSampleAndVessel(sourceSampleToMercurySample.get(sourceSampleToTest)
                            .getSampleKey(), sourceSampleToTargetVessel.get(sourceSampleToTest).getLabel());
            assertThat(targetVessel, is(notNullValue()));

            manifestSessionEjb.transferSample(closedSession.getManifestSessionId(), sourceSampleToTest,
                    sourceSampleToMercurySample.get(sourceSampleToTest)
                            .getSampleKey(), sourceSampleToTargetVessel.get(sourceSampleToTest).getLabel(), testUser);

            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE));

        }

        try {
            manifestSessionEjb
                    .validateSourceTubeForTransfer(closedSession.getManifestSessionId(), firstUplaodedOmittedScan);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    containsString(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE.getBaseMessage()));
        }
        MercurySample targetSampleForOmitted = manifestSessionEjb
                .validateTargetSample(sourceSampleToMercurySample.get(firstUplaodedOmittedScan).getSampleKey());
        assertThat(targetSampleForOmitted, is(notNullValue()));
        LabVessel targetVesselForOmitted =
                manifestSessionEjb.validateTargetSampleAndVessel(sourceSampleToMercurySample.get(firstUplaodedOmittedScan)
                        .getSampleKey(), sourceSampleToTargetVessel.get(firstUplaodedOmittedScan).getLabel());
        assertThat(targetVesselForOmitted, is(notNullValue()));

        try {
            manifestSessionEjb.transferSample(closedSession.getManifestSessionId(), firstUplaodedOmittedScan,
                    sourceSampleToMercurySample.get(firstUplaodedOmittedScan)
                            .getSampleKey(), sourceSampleToTargetVessel.get(firstUplaodedOmittedScan).getLabel(), testUser);
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    containsString(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE.getBaseMessage()));
        }

        /*****   Second Upload   ******/


        List<String> secondUploadAll = new ArrayList<>();
        secondUploadAll.addAll(secondUploadedSamplesGood);
        secondUploadAll.addAll(secondUploadedSamplesDupes);
//        secondUploadAll.addAll(secondUploadPatientsWithMismatchedGender);

        manifestSessionDao.clear();

        String pathToTestFile2 = "manifest-upload/duplicates/good-manifest-3.xlsx";
        InputStream testStream2 = Thread.currentThread().getContextClassLoader().getResourceAsStream(pathToTestFile2);
        ResearchProject rpSecondUpload = researchProjectDao.findByBusinessKey(researchProject.getBusinessKey());
        ManifestSession uploadedSession2 =
                manifestSessionEjb.uploadManifest(rpSecondUpload.getBusinessKey(), testStream2, pathToTestFile2,
                        testUser);

        assertThat(uploadedSession2, is(notNullValue()));
        assertThat(uploadedSession2.getManifestSessionId(), is(notNullValue()));
        sessionsToDelete.add(uploadedSession2.getManifestSessionId());
        assertThat(uploadedSession2.hasErrors(), is(true));
        assertThat(uploadedSession2.getResearchProject(), is(equalTo(rpSecondUpload)));

        assertThat(uploadedSession2.getRecords().size(), is(23));

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

        List<ManifestSession> checkOpenSessions = manifestSessionDao.findOpenSessions();

        assertThat(checkOpenSessions, hasItem(uploadedSession2));

        manifestSessionDao.clear();

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
            assertThat(record.getManifestEvents().size(), is(1));
            assertThat(record.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
        }
        for (String collaboratorSampleId : secondUploadedSamplesGood) {
            ManifestRecord record = acceptedSession2.findRecordByCollaboratorId(collaboratorSampleId);
            assertThat(record.getManifestEvents(), is(empty()));
            assertThat(record.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
        }
        manifestSessionDao.clear();

        ManifestSession sessionOfScan2 = manifestSessionDao.find(acceptedSession2.getManifestSessionId());

        for (String sampleId : secondUploadedSamplesGood) {
            manifestSessionEjb.accessionScan(sessionOfScan2.getManifestSessionId(), sampleId);

            ManifestRecord sourceToTest = sessionOfScan2.findRecordByCollaboratorId(sampleId);

            assertThat(sourceToTest.getStatus(), is(ManifestRecord.Status.SCANNED));
            assertThat(sourceToTest.getManifestEvents(), is(empty()));
            assertThat(sessionOfScan2.hasErrors(), is(true));
        }

        for (String sampleId : secondUploadedSamplesDupes) {
            try {
                manifestSessionEjb.accessionScan(sessionOfScan2.getManifestSessionId(), sampleId);
                Assert.fail();
            } catch (Exception e) {
                assertThat(e.getMessage(), Matchers.containsString(
                        ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.getBaseMessage()));
            }
        }

        sessionOfScan2 = manifestSessionDao.find(acceptedSession2.getManifestSessionId());
        for (String sampleId : secondUploadPatientsWithMismatchedGender) {
            manifestSessionEjb.accessionScan(sessionOfScan2.getManifestSessionId(), sampleId);

            ManifestRecord sourceToTest = sessionOfScan2.findRecordByCollaboratorId(sampleId);

            assertThat(sourceToTest.getStatus(), is(ManifestRecord.Status.SCANNED));
            assertThat(sourceToTest.getManifestEvents().size(), is(1));
            assertThat(sessionOfScan2.hasErrors(), is(true));
        }

        ManifestStatus sessionStatus2 = manifestSessionEjb.getSessionStatus(sessionOfScan2.getManifestSessionId());

        assertThat(sessionStatus2, is(notNullValue()));
        assertThat(sessionStatus2.getSamplesInManifest(), is(23));
        assertThat(sessionStatus2.getSamplesSuccessfullyScanned(), is(21));
        assertThat(sessionStatus2.getSamplesEligibleInManifest(), is(21));
        assertThat(sessionStatus2.getErrorMessages().size(), is(3));

        manifestSessionEjb.closeSession(sessionOfScan2.getManifestSessionId());
        ManifestSession closedSession2 = manifestSessionDao.find(sessionOfScan2.getManifestSessionId());

        assertThat(closedSession2, is(notNullValue()));

        assertThat(closedSession2.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));
        assertThat(closedSession2.getManifestEvents().size(), is(3));
        assertThat(closedSession2.getManifestEvents(), hasEventError(ManifestRecord.ErrorStatus.MISMATCHED_GENDER));
        assertThat(closedSession2.getManifestEvents(), hasEventError(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID));

        for (ManifestRecord manifestRecord : closedSession2.getRecords()) {
            if (secondUploadedSamplesDupes.contains(manifestRecord.getSampleId())) {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOADED));
                assertThat(manifestRecord.getManifestEvents().size(), is(1));
                assertThat(manifestRecord.getManifestEvents(), hasEventError(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID));
            } else if (secondUploadPatientsWithMismatchedGender.contains(manifestRecord.getSampleId())) {
                assertThat(manifestRecord.getManifestEvents().size(), is(1));
                assertThat(manifestRecord.getManifestEvents(), hasEventError(ManifestRecord.ErrorStatus.MISMATCHED_GENDER));
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
            } else {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
            }
        }

        List<ManifestSession> closedSessions2 = manifestSessionDao.findClosedSessions();

        assertThat(closedSessions2, Matchers.hasItem(closedSession2));

        for (String sourceSampleToTest : secondUploadedSamplesGood) {

            ManifestRecord manifestRecord = manifestSessionEjb
                    .validateSourceTubeForTransfer(closedSession2.getManifestSessionId(), sourceSampleToTest);
            assertThat(manifestRecord.getManifestSession(), is(closedSession2));

            MercurySample targetSample = manifestSessionEjb
                    .validateTargetSample(sourceSampleToMercurySample.get(sourceSampleToTest).getSampleKey());
            assertThat(targetSample, is(notNullValue()));
            LabVessel targetVessel =
                    manifestSessionEjb.validateTargetSampleAndVessel(sourceSampleToMercurySample.get(sourceSampleToTest)
                            .getSampleKey(), sourceSampleToTargetVessel.get(sourceSampleToTest).getLabel());
            assertThat(targetVessel, is(notNullValue()));

            manifestSessionEjb.transferSample(closedSession2.getManifestSessionId(), sourceSampleToTest,
                    sourceSampleToMercurySample.get(sourceSampleToTest)
                            .getSampleKey(), sourceSampleToTargetVessel.get(sourceSampleToTest).getLabel(), testUser);

            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE));

        }
        for (String sourceSampleToTest : secondUploadPatientsWithMismatchedGender) {

            ManifestRecord manifestRecord = manifestSessionEjb
                    .validateSourceTubeForTransfer(closedSession2.getManifestSessionId(), sourceSampleToTest);
            assertThat(manifestRecord.getManifestSession(), is(closedSession2));

            MercurySample targetSample = manifestSessionEjb
                    .validateTargetSample(sourceSampleToMercurySample.get(sourceSampleToTest).getSampleKey());
            assertThat(targetSample, is(notNullValue()));
            LabVessel targetVessel = manifestSessionEjb
                    .validateTargetSampleAndVessel(sourceSampleToMercurySample.get(sourceSampleToTest).getSampleKey(),
                            sourceSampleToTargetVessel.get(sourceSampleToTest).getLabel());
            assertThat(targetVessel, is(notNullValue()));

            manifestSessionEjb.transferSample(closedSession2.getManifestSessionId(), sourceSampleToTest,
                    sourceSampleToMercurySample.get(sourceSampleToTest).getSampleKey(),
                    sourceSampleToTargetVessel.get(sourceSampleToTest).getLabel(), testUser);

            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE));
        }
        for (String sourceSampleToTest : secondUploadedSamplesDupes) {

            try {
                manifestSessionEjb
                        .validateSourceTubeForTransfer(closedSession2.getManifestSessionId(), sourceSampleToTest);
            } catch (Exception e) {
                assertThat(e.getMessage(), Matchers.containsString(
                        ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE.getBaseMessage()));
            }
        }
    }
}
