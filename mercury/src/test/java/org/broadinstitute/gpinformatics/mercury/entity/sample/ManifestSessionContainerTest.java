package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ResearchProjectEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.CovidManifestBucketConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.ClinicalSampleTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleBucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.UpdateData;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.matchers.ExceptionMessageMatcher.containsMessage;
import static org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestEventMatcher.hasEventError;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Container tests for ManifestSessions.
 */
@Test(groups = TestGroups.ALTERNATIVES)
@Dependent
public class ManifestSessionContainerTest extends Arquillian {

    public ManifestSessionContainerTest(){}

    private static Log logger = LogFactory.getLog(ManifestSessionContainerTest.class);

    private static final String PATIENT_1 = "PATIENT 1";

    private static final String GENDER_MALE = "Male";
    private static final String GENDER_FEMALE = "Female";
    private static final String COLLAB_PREFIX = "collab_";
    private static final int NUM_RECORDS_IN_SPREADSHEET = 23;

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
    private ManifestSession uploadedSession;
    private ManifestSession uploadedSession2;

    @Inject
    private ManifestSessionEjb manifestSessionEjb;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ManifestSessionDao manifestSessionDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private UserBean userBean;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private LabVesselFactory labVesselFactory;

    @Inject
    private JiraService jiraService;

    @Inject
    private ResearchProjectEjb researchProjectEjb;

    @Inject
    private GoogleBucketDao googleBucketDao;

    @Alternative
    @Dependent
    public static class BSPCohortListProducer {

        public BSPCohortListProducer(){}

        @Produces
        @Alternative
        public static BSPCohortList produce() {
            BSPCohortList bspCohortList = Mockito.mock(BSPCohortList.class);
            Mockito.when(bspCohortList.getCohortListString(Mockito.any(String[].class))).thenReturn("");
            return bspCohortList;
        }
    }

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DEV, BSPCohortListProducer.class);
    }

    @BeforeMethod
    public void setUp() throws Exception {

        if (userBean == null) {
            return;
        }
        Date today = new Date();
        userBean.loginTestUser();

        Date researchProjectCreateTime = new Date();
        researchProject = ResearchProjectTestFactory.createTestResearchProject();
        researchProject.setTitle("Buick test Project" + researchProjectCreateTime.getTime());
        researchProject.setRegulatoryDesignation(ResearchProject.RegulatoryDesignation.CLINICAL_DIAGNOSTICS);
        researchProjectEjb.submitToJira(researchProject);

        String SAMPLE_ID_1 = COLLAB_PREFIX + today.getTime() + "1";
        String SAMPLE_ID_2 = COLLAB_PREFIX + today.getTime() + "2";
        String SAMPLE_ID_3 = COLLAB_PREFIX + today.getTime() + "3";
        String SAMPLE_ID_4 = COLLAB_PREFIX + today.getTime() + "4";
        String SAMPLE_ID_5 = COLLAB_PREFIX + today.getTime() + "5";
        String SAMPLE_ID_6 = COLLAB_PREFIX + today.getTime() + "6";
        String SAMPLE_ID_7 = COLLAB_PREFIX + today.getTime() + "7";
        String SAMPLE_ID_8 = COLLAB_PREFIX + today.getTime() + "8";
        String SAMPLE_ID_9 = COLLAB_PREFIX + today.getTime() + "9";
        String SAMPLE_ID_10 = COLLAB_PREFIX + today.getTime() + "10";
        String SAMPLE_ID_11 = COLLAB_PREFIX + today.getTime() + "11";
        String SAMPLE_ID_12 = COLLAB_PREFIX + today.getTime() + "12";


        manifestSessionI = new ManifestSession(researchProject, "BUICK-TEST", testUser, false,
                ManifestSessionEjb.AccessioningProcessType.CRSP);
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

        manifestSessionII = new ManifestSession(researchProject, "BUICK-TEST2", testUser, false,
                ManifestSessionEjb.AccessioningProcessType.CRSP);

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

        sourceSampleToMercurySample = new HashMap<>();
        sourceSampleToTargetVessel = new HashMap<>();

        // Generics warning.
        @SuppressWarnings("unchecked")
        Iterable<String> allSourceSamples = Iterables
                .concat(firstUploadedScannedSamples, secondUploadedSamplesDupes, secondUploadedSamplesGood,
                        secondUploadPatientsWithMismatchedGender, Collections.singleton(firstUploadedOmittedScan),
                        Arrays.asList(SAMPLE_ID_1, SAMPLE_ID_2, SAMPLE_ID_3, SAMPLE_ID_4, SAMPLE_ID_5, SAMPLE_ID_6,
                                SAMPLE_ID_7, SAMPLE_ID_8, SAMPLE_ID_9, SAMPLE_ID_10, SAMPLE_ID_11, SAMPLE_ID_12));

        for (String sourceSample : allSourceSamples) {
            sourceSampleToMercurySample.put(sourceSample, new MercurySample("SM_" + sourceSample + today.getTime(),
                    MercurySample.MetadataSource.MERCURY));
            sourceSampleToTargetVessel.put(sourceSample, new BarcodedTube("A0" + sourceSample + today.getTime(),
                    BarcodedTube.BarcodedTubeType.MatrixTube2mL));
            sourceSampleToTargetVessel.get(sourceSample).addSample(sourceSampleToMercurySample.get(sourceSample));
        }

        // This config is from the yaml file.
        CovidManifestBucketConfig covidManifestBucketConfig =
                (CovidManifestBucketConfig) MercuryConfiguration.getInstance().
                        getConfig(CovidManifestBucketConfig.class,
                                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV);
        googleBucketDao.setConfigGoogleStorageConfig(covidManifestBucketConfig);
    }

    /**
     * Previously this test would leave some sessions, and the records in them, in an uncompleted state.  This affects
     * the front end on the mercury dev instance by building a lfong list of "to be worked on" sessions.  This clean up
     * will ensure that the to be worked on session list does not grow.
     * @throws Exception
     */
    @AfterMethod
    public void tearDown() throws Exception {
        if(userBean == null) {
            return;
        }

        for(ManifestSession sessionToCleanup :
                Arrays.asList(manifestSessionI, manifestSessionII, uploadedSession, uploadedSession2)) {
            cleanupSession(sessionToCleanup);
        }

    }

    private void cleanupSession(ManifestSession sessionToCleanup) {
        if (sessionToCleanup != null && sessionToCleanup.getManifestSessionId() != null) {
            sessionToCleanup = manifestSessionDao.find(sessionToCleanup.getManifestSessionId());
            sessionToCleanup.setStatus(ManifestSession.SessionStatus.COMPLETED);
            for (ManifestRecord manifestRecord : sessionToCleanup.getNonQuarantinedRecords()) {
                manifestRecord.setStatus(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE);
            }
            manifestSessionDao.flush();
        }
    }

    private ManifestRecord createManifestRecord(Metadata.Key key1, String value1, Metadata.Key key2, String value2,
                                                Metadata.Key key3, String value3) {
        return new ManifestRecord(new Metadata(key1, value1), new Metadata(key2, value2),
                new Metadata(key3, value3));
    }

    /**
     * Round trip test for ManifestSession used in initial development to work out issues in mappings, but this
     * should still have value.
     */
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
            assertThat(manifestRecord.getUpdateData().getModifiedDate(),
                    is(equalTo(manifestRecord.getUpdateData().getCreatedDate())));
        }


        // Clear the Session to force retrieval of a persistent instance 'manifestSessionOut' below that is distinct
        // from the detached 'manifestSessionI' instance.
        manifestSessionDao.flush();
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

        assertThat(sessionClosed.getUpdateData().getModifiedDate(),
                is(not(equalTo(manifestSessionI.getUpdateData().getModifiedDate()))));
        assertThat(sessionClosed.getUpdateData().getCreatedDate(),
                is(not(equalTo(sessionClosed.getUpdateData().getModifiedDate()))));
        for (ManifestRecord manifestRecord : sessionClosed.getRecords()) {
            assertThat(manifestRecord.getUpdateData().getCreatedDate(),
                    is(not(equalTo(manifestRecord.getUpdateData().getModifiedDate()))));
        }
    }

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

        uploadedSession =
                manifestSessionEjb.uploadManifest(researchProject.getBusinessKey(), testStream, excelFilePath,
                        ManifestSessionEjb.AccessioningProcessType.CRSP, false);
        UpdateData updateData = uploadedSession.getUpdateData();
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
        assertThat(manifestSessionDao.findSessionsEligibleForTubeTransfer(), not(hasItem(uploadedSession)));

        String UPLOADED_COLLABORATOR_SESSION_1 = "03101067213";
        String UPLOADED_PATIENT_ID_SESSION_1 = "001-001";
        assertThat(uploadedSession.findRecordByKey(UPLOADED_COLLABORATOR_SESSION_1, Metadata.Key.SAMPLE_ID)
                .getValueByKey(Metadata.Key.PATIENT_ID), is(UPLOADED_PATIENT_ID_SESSION_1));

        manifestSessionDao.flush();
        manifestSessionDao.clear();

        /*
         *  Mimic user accepting the upload
         */
        manifestSessionEjb.acceptManifestUpload(uploadedSession.getManifestSessionId());

        manifestSessionDao.flush();
        manifestSessionDao.clear();

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
        manifestSessionDao.flush();
        manifestSessionDao.clear();
        ManifestSession sessionOfScan = manifestSessionDao.find(uploadedSession.getManifestSessionId());

        /*
         *  Mimic the scan of all samples in the manifest... except One (to test closing and validating with an
         *  omitted scan.
         */
        for (String sourceSampleToTest : firstUploadedScannedSamples) {

            manifestSessionEjb.accessionScan(sessionOfScan.getManifestSessionId(), sourceSampleToTest,
                    sourceSampleToTest);
            manifestSessionDao.flush();
            manifestSessionDao.clear();

            ManifestSession reFetchedSessionOfScan = manifestSessionDao.find(sessionOfScan.getManifestSessionId());

            ManifestRecord sourceRecordToTest = reFetchedSessionOfScan.findRecordByKey(sourceSampleToTest,
                    Metadata.Key.SAMPLE_ID);

            assertThat(sourceRecordToTest.getStatus(), is(ManifestRecord.Status.SCANNED));
            assertThat(reFetchedSessionOfScan.hasErrors(), is(false));
        }

        /*
         * Mimic the user getting a "Pre-close" validation
         */
        ManifestStatus sessionStatus = manifestSessionEjb.getSessionStatus(sessionOfScan.getManifestSessionId());

        assertThat(sessionStatus, is(notNullValue()));
        assertThat(sessionStatus.getSamplesInManifest(), is(NUM_RECORDS_IN_SPREADSHEET));
        // Deliberately missed one scan.
        assertThat(sessionStatus.getSamplesSuccessfullyScanned(), is(NUM_RECORDS_IN_SPREADSHEET - 1));
        // All records (except for one) have been scanned so there is only 1 considered eligible for scanning
        assertThat(sessionStatus.getSamplesEligibleForAccessioningInManifest(), is(1));
        assertThat(sessionStatus.getErrorMessages(), is(not(empty())));
        assertThat(sessionStatus.getErrorMessages(), hasItem(ManifestRecord.ErrorStatus.MISSING_SAMPLE
                .formatMessage(Metadata.Key.SAMPLE_ID, firstUploadedOmittedScan)));

        /*
         * Mimic a user closing the session, and the ramifications of that.
         */
        JiraIssue receiptTicket = createReceiptTicket();
        manifestSessionEjb.updateReceiptInfo(sessionOfScan.getManifestSessionId(), receiptTicket.getKey());
        manifestSessionEjb.closeSession(sessionOfScan.getManifestSessionId());

        // Clear to force a reload.
        manifestSessionDao.flush();
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
        List<ManifestSession> closedSessions = manifestSessionDao.findSessionsEligibleForTubeTransfer();
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

            MercurySample targetSample = manifestSessionEjb.findAndValidateTargetSample(sourceSampleKey);
            assertThat(targetSample, is(notNullValue()));

            String sourceSampleLabel = sourceSampleToTargetVessel.get(sourceSampleToTest).getLabel();

            LabVessel targetVessel =
                    manifestSessionEjb.findAndValidateTargetSampleAndVessel(sourceSampleKey, sourceSampleLabel);
            assertThat(targetVessel, is(notNullValue()));

            MercurySample mercurySample = sourceSampleToMercurySample.get(sourceSampleToTest);
            manifestSessionEjb.transferSample(closedSession.getManifestSessionId(), sourceSampleToTest,
                    mercurySample.getSampleKey(), sourceSampleLabel);

            // Make sure the metadata was persisted with the sample.
            manifestSessionDao.flush();
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
            assertThat(e,
                    containsMessage(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE.getBaseMessage()));
        }
        String omittedScanSampleKey = sourceSampleToMercurySample.get(firstUploadedOmittedScan).getSampleKey();
        String omittedScanSampleLabel = sourceSampleToTargetVessel.get(firstUploadedOmittedScan).getLabel();

        MercurySample targetSampleForOmittedScan = manifestSessionEjb.findAndValidateTargetSample(omittedScanSampleKey);
        assertThat(targetSampleForOmittedScan, is(notNullValue()));

        LabVessel targetVesselForOmittedScan =
                manifestSessionEjb.findAndValidateTargetSampleAndVessel(omittedScanSampleKey, omittedScanSampleLabel);

        assertThat(targetVesselForOmittedScan, is(notNullValue()));

        try {
            manifestSessionEjb.transferSample(closedSession.getManifestSessionId(), firstUploadedOmittedScan,
                    omittedScanSampleKey, omittedScanSampleLabel);
        } catch (Exception e) {
            assertThat(e,
                    containsMessage(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE.getBaseMessage()));
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
        manifestSessionDao.flush();
        manifestSessionDao.clear();

        /*
         * Mimic the user uploading the manifest
         */
        String pathToTestFile2 = "manifest-upload/duplicates/good-manifest-3.xlsx";
        InputStream testStream2 = Thread.currentThread().getContextClassLoader().getResourceAsStream(pathToTestFile2);
        ResearchProject rpSecondUpload = researchProjectDao.findByBusinessKey(researchProject.getBusinessKey());
        uploadedSession2 =
                manifestSessionEjb.uploadManifest(rpSecondUpload.getBusinessKey(), testStream2, pathToTestFile2,
                        ManifestSessionEjb.AccessioningProcessType.CRSP, false);

        assertThat(uploadedSession2, is(notNullValue()));
        assertThat(uploadedSession2.getManifestSessionId(), is(notNullValue()));
        assertThat(uploadedSession2.hasErrors(), is(true));
        assertThat(uploadedSession2.getResearchProject(), is(equalTo(rpSecondUpload)));

        assertThat(uploadedSession2.getRecords(), hasSize(NUM_RECORDS_IN_SPREADSHEET));

        for (ManifestRecord manifestRecord : uploadedSession2.getRecords()) {
            assertThat(manifestRecord.getManifestRecordId(), is(notNullValue()));
        }

        for (String dupeSampleId : secondUploadedSamplesDupes) {
            ManifestRecord dupeRecord = uploadedSession2.findRecordByKey(dupeSampleId,
                    Metadata.Key.SAMPLE_ID);
            assertThat(dupeRecord.isQuarantined(), is(true));
            assertThat(dupeRecord.getSampleId(), is(dupeSampleId));
        }
        for (String genderSample : secondUploadPatientsWithMismatchedGender) {
            ManifestRecord dupeRecord = uploadedSession2.findRecordByKey(genderSample,
                    Metadata.Key.SAMPLE_ID);
            assertThat(dupeRecord.isQuarantined(), is(false));
            assertThat(dupeRecord.getSampleId(), is(genderSample));
        }

        /*
         * Check that the new upload is in the list of uploaded sessions and not in the list of closed sessions
         */
        List<ManifestSession> checkOpenSessions = manifestSessionDao.findOpenSessions();
        List<ManifestSession> checkClosedSessions = manifestSessionDao.findSessionsEligibleForTubeTransfer();

        assertThat(checkOpenSessions, hasItem(uploadedSession2));
        assertThat(checkClosedSessions, not(hasItem(uploadedSession2)));

        manifestSessionDao.flush();
        manifestSessionDao.clear();

        /*
         * mimic the user accepting the second manifest
         */
        manifestSessionEjb.acceptManifestUpload(uploadedSession2.getManifestSessionId());

        manifestSessionDao.flush();
        manifestSessionDao.clear();

        ManifestSession acceptedSession2 = manifestSessionDao.find(uploadedSession2.getManifestSessionId());

        assertThat(acceptedSession2, is(notNullValue()));
        assertThat(acceptedSession2.getManifestSessionId(), is(notNullValue()));
        assertThat(acceptedSession2.hasErrors(), is(true));

        for (String collaboratorSampleId : secondUploadedSamplesDupes) {
            ManifestRecord record = acceptedSession2.findRecordByKey(collaboratorSampleId,
                    Metadata.Key.SAMPLE_ID);
            assertThat(record.isQuarantined(), is(true));
            assertThat(record.getStatus(), is(ManifestRecord.Status.UPLOADED));
        }

        for (String collaboratorSampleId : secondUploadPatientsWithMismatchedGender) {
            ManifestRecord record = acceptedSession2.findRecordByKey(collaboratorSampleId,
                    Metadata.Key.SAMPLE_ID);
            assertThat(record.isQuarantined(), is(false));
            assertThat(record.getManifestEvents(), hasSize(1));
            assertThat(record.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
        }
        for (String collaboratorSampleId : secondUploadedSamplesGood) {
            ManifestRecord record = acceptedSession2.findRecordByKey(collaboratorSampleId,
                    Metadata.Key.SAMPLE_ID);
            assertThat(record.getManifestEvents(), is(empty()));
            assertThat(record.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
        }
        manifestSessionDao.flush();
        manifestSessionDao.clear();

        ManifestSession sessionOfScan2 = manifestSessionDao.find(acceptedSession2.getManifestSessionId());

        /*
         * Mimic the user Scanning the tubes to complete accessioning
         */
        for (String sampleId : secondUploadedSamplesGood) {
            manifestSessionEjb.accessionScan(sessionOfScan2.getManifestSessionId(), sampleId, sampleId
            );

            manifestSessionDao.flush();
            manifestSessionDao.clear();
            ManifestSession reFetchedSessionOfScan2 = manifestSessionDao.find(sessionOfScan2.getManifestSessionId());
            ManifestRecord sourceToTest = reFetchedSessionOfScan2.findRecordByKey(sampleId,
                    Metadata.Key.SAMPLE_ID);

            assertThat(sourceToTest.getStatus(), is(ManifestRecord.Status.SCANNED));
            assertThat(sourceToTest.getManifestEvents(), is(empty()));
            assertThat(reFetchedSessionOfScan2.hasErrors(), is(true));
        }

        /*
         * Mimic the user attempting to scan the dupes accidentally
         */
        for (String sampleId : secondUploadedSamplesDupes) {
            try {
                manifestSessionEjb.accessionScan(sessionOfScan2.getManifestSessionId(), sampleId, sampleId
                );
                Assert.fail();
            } catch (Exception e) {
                assertThat(e, containsMessage(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.getBaseMessage()));
            }
        }

        /*
         * mimic the user scanning the tube(s) for the record(s) that contain mismatched gender
         *
         */
        sessionOfScan2 = manifestSessionDao.find(acceptedSession2.getManifestSessionId());
        for (String sampleId : secondUploadPatientsWithMismatchedGender) {
            manifestSessionEjb.accessionScan(sessionOfScan2.getManifestSessionId(), sampleId, sampleId
            );

            manifestSessionDao.flush();
            manifestSessionDao.clear();
            ManifestSession reFetchedSessionOfScan2 = manifestSessionDao.find(sessionOfScan2.getManifestSessionId());
            ManifestRecord sourceToTest = reFetchedSessionOfScan2.findRecordByKey(sampleId,
                    Metadata.Key.SAMPLE_ID);

            assertThat(sourceToTest.getStatus(), is(ManifestRecord.Status.SCANNED));
            assertThat(sourceToTest.getManifestEvents(), hasSize(NUM_MISMATCHED_GENDERS_IN_SESSION_2));
            assertThat(reFetchedSessionOfScan2.hasErrors(), is(true));
        }

        /*
         * mimic the user preparing to close the session and getting a summary of events that occurred.
         */
        ManifestStatus sessionStatus2 = manifestSessionEjb.getSessionStatus(sessionOfScan2.getManifestSessionId());

        assertThat(sessionStatus2, is(notNullValue()));
        assertThat(sessionStatus2.getSamplesInManifest(), is(NUM_RECORDS_IN_SPREADSHEET));
        assertThat(sessionStatus2.getSamplesSuccessfullyScanned(),
                is(NUM_RECORDS_IN_SPREADSHEET - NUM_DUPLICATES_IN_SESSION_2));
        // All records have been scanned so there are none currently eligible for scanning
        assertThat(sessionStatus2.getSamplesEligibleForAccessioningInManifest(), is(0));
        assertThat(sessionStatus2.getErrorMessages(), is(empty()));

        /*
         * Mimic the user closing the session
         */
        manifestSessionEjb.closeSession(sessionOfScan2.getManifestSessionId());
        manifestSessionDao.flush();
        manifestSessionDao.clear();
        ManifestSession closedSession2 = manifestSessionDao.find(sessionOfScan2.getManifestSessionId());

        assertThat(closedSession2, is(notNullValue()));

        assertThat(closedSession2.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));
        assertThat(closedSession2.getManifestEvents(),
                hasSize(NUM_DUPLICATES_IN_SESSION_2 + NUM_MISMATCHED_GENDERS_IN_SESSION_2));
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

        List<ManifestSession> closedSessions2 = manifestSessionDao.findSessionsEligibleForTubeTransfer();

        assertThat(closedSessions2, hasItem(closedSession2));

        /*
         * mimic the user performing the tube transfer
         */
        for (String sourceSampleToTest : secondUploadedSamplesGood) {

            ManifestRecord manifestRecord = manifestSessionEjb
                    .validateSourceTubeForTransfer(closedSession2.getManifestSessionId(), sourceSampleToTest);
            assertThat(manifestRecord.getManifestSession().getManifestSessionId(),
                    is(closedSession2.getManifestSessionId()));

            String sourceSampleKey = sourceSampleToMercurySample.get(sourceSampleToTest).getSampleKey();
            MercurySample targetSample = manifestSessionEjb.findAndValidateTargetSample(sourceSampleKey);
            assertThat(targetSample, is(notNullValue()));
            String sourceSampleLabel = sourceSampleToTargetVessel.get(sourceSampleToTest).getLabel();

            LabVessel targetVessel =
                    manifestSessionEjb.findAndValidateTargetSampleAndVessel(sourceSampleKey, sourceSampleLabel);
            assertThat(targetVessel, is(notNullValue()));

            manifestSessionEjb.transferSample(closedSession2.getManifestSessionId(), sourceSampleToTest,
                    sourceSampleKey, sourceSampleLabel);

            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE));

            try {
                manifestSessionEjb.transferSample(closedSession2.getManifestSessionId(), sourceSampleToTest,
                        sourceSampleKey, sourceSampleLabel);
                Assert.fail();
            } catch (TubeTransferException e) {
                assertThat(e, containsMessage(ManifestRecord.ErrorStatus.INVALID_TARGET.getBaseMessage()));
            }
        }

        for (String sourceSampleToTest : secondUploadPatientsWithMismatchedGender) {
            ManifestRecord manifestRecord = manifestSessionEjb
                    .validateSourceTubeForTransfer(closedSession2.getManifestSessionId(), sourceSampleToTest);
            assertThat(manifestRecord.getManifestSession().getManifestSessionId(),
                    is(closedSession2.getManifestSessionId()));

            String sourceSampleKey = sourceSampleToMercurySample.get(sourceSampleToTest).getSampleKey();
            MercurySample targetSample = manifestSessionEjb.findAndValidateTargetSample(sourceSampleKey);
            assertThat(targetSample, is(notNullValue()));
            String sourceSampleLabel = sourceSampleToTargetVessel.get(sourceSampleToTest).getLabel();
            LabVessel targetVessel =
                    manifestSessionEjb.findAndValidateTargetSampleAndVessel(sourceSampleKey, sourceSampleLabel);
            assertThat(targetVessel, is(notNullValue()));

            manifestSessionEjb.transferSample(closedSession2.getManifestSessionId(), sourceSampleToTest,
                    sourceSampleKey, sourceSampleLabel);

            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE));
            try {
                manifestSessionEjb.transferSample(closedSession2.getManifestSessionId(), sourceSampleToTest,
                        sourceSampleKey, sourceSampleLabel);
                Assert.fail();
            } catch (TubeTransferException e) {
                assertThat(e, containsMessage(ManifestRecord.ErrorStatus.INVALID_TARGET.getBaseMessage()));
            }
        }

        /*
         * mimic the user accidentally performing tube transfer on the duplicate tubes
         */
        for (String sourceSampleToTest : secondUploadedSamplesDupes) {
            try {
                manifestSessionEjb
                        .validateSourceTubeForTransfer(closedSession2.getManifestSessionId(), sourceSampleToTest);
            } catch (Exception e) {
                assertThat(e, containsMessage(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE
                        .getBaseMessage()));
            }
        }
    }

    private JiraIssue createReceiptTicket() throws IOException {
        String summary = "Sample Receipt " + System.currentTimeMillis();
        String shipmentCondition = "Shipment Condition";
        String materialTypeCounts = "Material Type Counts";
        String storageLocation = "Storage Location";
        String kitDeliveryMethod = "Kit Delivery Method";
        String trackingNumber = "Tracking Number";

        Map<String, CustomFieldDefinition> receiptFields =
                jiraService.getCustomFields(shipmentCondition, materialTypeCounts, storageLocation, kitDeliveryMethod,
                        trackingNumber);

        Collection<CustomField> customFields = new ArrayList<>(receiptFields.size());
        customFields.add(new CustomField(receiptFields.get(shipmentCondition), "A-OK"));
        customFields.add(new CustomField(receiptFields.get(materialTypeCounts), "6 DNA"));
        customFields.add(new CustomField(receiptFields.get(storageLocation), "LOC" + System.currentTimeMillis()));

        customFields
                .add(new CustomField(receiptFields.get(kitDeliveryMethod), new CustomField.ValueContainer("FedEx")));

        customFields.add(new CustomField(receiptFields.get(trackingNumber),
                String.format("%12d", System.currentTimeMillis())));

        return jiraService.createIssue(CreateFields.ProjectType.RECEIPT_PROJECT, "QADude",
                CreateFields.IssueType.RECEIPT,
                summary, customFields);
    }

    public void testFindClosedSessionQuery() throws Exception {

        for (LabVessel targetVessel : sourceSampleToTargetVessel.values()) {
            labVesselDao.persist(targetVessel);
        }
        researchProjectDao.persist(researchProject);

        manifestSessionI.setStatus(ManifestSession.SessionStatus.COMPLETED);

        manifestSessionDao.persist(manifestSessionI);

        List<ManifestSession> closedSessions = manifestSessionDao.findSessionsEligibleForTubeTransfer();

        assertThat(closedSessions, hasItem(manifestSessionI));

        for (ManifestRecord manifestRecord : manifestSessionI.getRecords()) {
            manifestRecord.setStatus(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE);
        }
        manifestSessionDao.persist(manifestSessionI);


        List<ManifestSession> closedSessions2 = manifestSessionDao.findSessionsEligibleForTubeTransfer();
        assertThat(closedSessions2, not(hasItem(manifestSessionI)));
    }

    public void testNonQuarantineRecordCount() throws Exception {

        researchProjectDao.persist(researchProject);
        int counter = 0;
        manifestSessionDao.flush();
        manifestSessionDao.clear();
        ManifestSession retrievedSession = manifestSessionDao.find(manifestSessionI.getManifestSessionId());

        int totalRecords = retrievedSession.getRecords().size();

        while(counter < retrievedSession.getRecords().size()) {

            assertThat(retrievedSession.getNumberOfTubesAvailableForTransfer(), is(equalTo(totalRecords-counter)));
            retrievedSession.addManifestEvent(
                    new ManifestEvent(ManifestEvent.Severity.QUARANTINED, "testing Formula",
                            retrievedSession.getRecords().get(counter)));

            manifestSessionDao.flush();
            manifestSessionDao.clear();
            retrievedSession = manifestSessionDao.find(retrievedSession.getManifestSessionId());
            retrievedSession.getRecords();
            counter++;
        }
        assertThat(retrievedSession.getNumberOfTubesAvailableForTransfer(), is(equalTo(totalRecords - counter)));
    }

    public void testNumberTubesTransferred() throws Exception {
        researchProjectDao.persist(researchProject);
        int counter = 0;
        manifestSessionDao.flush();
        manifestSessionDao.clear();
        ManifestSession retrievedSession = manifestSessionDao.find(manifestSessionI.getManifestSessionId());

        int numRecords = retrievedSession.getRecords().size();
        if (numRecords < 2) {
            String message =
                    "At least 2 records required to test quarantine logic, one record will be quarantined and at" +
                    " least one non-quarantined record is required to test tube transfer";
            throw new RuntimeException(message);
        }

        int indexOfRecordToQuarantine = numRecords / 2;
        int transferredTubeCounter = 0;

        while (counter < numRecords) {

            assertThat(retrievedSession.getNumberOfTubesTransferred(), is(equalTo(transferredTubeCounter)));

            ManifestRecord manifestRecord = retrievedSession.getRecords().get(counter);

            if (counter == indexOfRecordToQuarantine) {
                retrievedSession.addManifestEvent(new ManifestEvent(ManifestEvent.Severity.QUARANTINED,
                        "No good reason, just for testing", manifestRecord));
            } else {
                manifestRecord.setStatus(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE);
                transferredTubeCounter++;
            }

            manifestSessionDao.flush();
            manifestSessionDao.clear();
            retrievedSession = manifestSessionDao.find(retrievedSession.getManifestSessionId());
            counter++;
        }
        assertThat(retrievedSession.getNumberOfTubesTransferred(), is(equalTo(transferredTubeCounter)));
        // One record should have been quarantined.
        assertThat(counter, is(equalTo(transferredTubeCounter + 1)));
    }

    public void endToEndFromSampleKit() throws Exception {
        /*
         * Setup required preliminary entities
         */
        for (LabVessel targetVessel : sourceSampleToTargetVessel.values()) {
            labVesselDao.persist(targetVessel);
        }
        researchProjectDao.persist(researchProject);

        /*
         * Mimic portal initial creation of manifest
         */
        uploadedSession = new ManifestSession(researchProject, "With Sample Kit Manifest", testUser, true,
                ManifestSessionEjb.AccessioningProcessType.CRSP);
        manifestSessionDao.persist(uploadedSession);

        UpdateData updateData = uploadedSession.getUpdateData();
        assertThat(updateData.getModifiedBy(), is(not(equalTo(updateData.getCreatedBy()))));
        assertThat(updateData.getModifiedDate(), is(equalTo(updateData.getCreatedDate())));

        assertThat(uploadedSession, is(notNullValue()));
        assertThat(uploadedSession.getManifestSessionId(), is(notNullValue()));
        assertThat(uploadedSession.hasErrors(), is(false));
        assertThat(uploadedSession.getResearchProject(), is(equalTo(researchProject)));
        assertThat(uploadedSession.getStatus(), is(equalTo(ManifestSession.SessionStatus.PENDING_SAMPLE_INFO)));

        /*
         *  Test that the session is listed in all open sessions and not listed in the closed sessions
         */
        List<ManifestSession> openSessions = manifestSessionDao.findOpenSessions();

        assertThat(openSessions, hasItem(uploadedSession));
        assertThat(manifestSessionDao.findSessionsEligibleForTubeTransfer(), not(hasItem(uploadedSession)));

        for(String collabSample:firstUploadedScannedSamples) {

            uploadedSession.addRecord(new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, collabSample),
                    new Metadata(Metadata.Key.PATIENT_ID, "PT_" + collabSample),
                    new Metadata(Metadata.Key.BROAD_SAMPLE_ID,
                            sourceSampleToMercurySample.get(collabSample).getSampleKey()),
                    new Metadata(Metadata.Key.TUMOR_NORMAL, "T")));
        }

        assertThat(uploadedSession.getStatus(), is(equalTo(ManifestSession.SessionStatus.ACCESSIONING)));
        manifestSessionDao.persist(uploadedSession);

        for (ManifestRecord manifestRecord : uploadedSession.getRecords()) {
            assertThat(manifestRecord.getManifestRecordId(), is(notNullValue()));
        }

        /*
         *  Test that the session is listed in all open sessions and not listed in the closed sessions
         */
        openSessions = manifestSessionDao.findOpenSessions();

        assertThat(openSessions, hasItem(uploadedSession));
        assertThat(manifestSessionDao.findSessionsEligibleForTubeTransfer(), not(hasItem(uploadedSession)));

        String UPLOADED_COLLABORATOR_SESSION_1 = "03101067213";
        String UPLOADED_PATIENT_ID_SESSION_1 = "PT_" + UPLOADED_COLLABORATOR_SESSION_1;
        assertThat(uploadedSession.findRecordByKey(UPLOADED_COLLABORATOR_SESSION_1, Metadata.Key.SAMPLE_ID)
                .getValueByKey(Metadata.Key.PATIENT_ID), is(UPLOADED_PATIENT_ID_SESSION_1));

        manifestSessionDao.flush();
        manifestSessionDao.clear();

        ManifestSession acceptedSession = manifestSessionDao.find(uploadedSession.getManifestSessionId());

        assertThat(acceptedSession, is(notNullValue()));
        assertThat(acceptedSession.getManifestSessionId(), is(notNullValue()));
        assertThat(acceptedSession.hasErrors(), is(false));

        ManifestSession sessionOfScan = manifestSessionDao.find(uploadedSession.getManifestSessionId());

        /*
         *  Mimic the scan of all samples in the manifest.
         */
        for (String sourceSampleToTest : firstUploadedScannedSamples) {

            manifestSessionEjb.accessionScan(sessionOfScan.getManifestSessionId(),
                    sourceSampleToMercurySample.get(sourceSampleToTest).getSampleKey(),
                    sourceSampleToTargetVessel.get(sourceSampleToTest).getLabel());
            manifestSessionDao.flush();
            manifestSessionDao.clear();

            ManifestSession reFetchedSessionOfScan = manifestSessionDao.find(sessionOfScan.getManifestSessionId());

            ManifestRecord sourceRecordToTest = reFetchedSessionOfScan.findRecordByKey(sourceSampleToMercurySample.get(sourceSampleToTest).getSampleKey(),
                    Metadata.Key.BROAD_SAMPLE_ID);

            assertThat(sourceRecordToTest.getStatus(), is(ManifestRecord.Status.SCANNED));
            assertThat(reFetchedSessionOfScan.hasErrors(), is(false));
        }

        /*
         * Mimic the user getting a "Pre-close" validation
         */
        ManifestStatus sessionStatus = manifestSessionEjb.getSessionStatus(sessionOfScan.getManifestSessionId());

        assertThat(sessionStatus, is(notNullValue()));
        assertThat(sessionStatus.getSamplesInManifest(), is(firstUploadedScannedSamples.size()));
        // Deliberately missed one scan.
        assertThat(sessionStatus.getSamplesSuccessfullyScanned(), is(firstUploadedScannedSamples.size()));
        // All records have been scanned
        assertThat(sessionStatus.getSamplesEligibleForAccessioningInManifest(), is(0));
        assertThat(sessionStatus.getErrorMessages(), is(empty()));

        /*
         * Mimic a user closing the session, and the ramifications of that.
         */
        JiraIssue receiptTicket = createReceiptTicket();
        manifestSessionEjb.updateReceiptInfo(sessionOfScan.getManifestSessionId(), receiptTicket.getKey());
        manifestSessionEjb.closeSession(sessionOfScan.getManifestSessionId());

        // Clear to force a reload.
        manifestSessionDao.flush();
        manifestSessionDao.clear();
        ManifestSession closedSession = manifestSessionDao.find(sessionOfScan.getManifestSessionId());

        assertThat(closedSession.getUpdateData().getModifiedDate(), is(not(equalTo(updateData.getModifiedDate()))));

        assertThat(closedSession, is(notNullValue()));

        assertThat(closedSession.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));
        assertThat(closedSession.getManifestEvents(), hasSize(0));

        for (ManifestRecord manifestRecord : closedSession.getRecords()) {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE));
            LabVessel transferedVessel = labVesselDao.findByIdentifier(sourceSampleToTargetVessel.get(manifestRecord.getValueByKey(
                    Metadata.Key.SAMPLE_ID)).getLabel());
            assertThat(transferedVessel.canBeUsedForAccessioning(), is(false));
        }

        /*
         * Validate that the session is in the list of closed sessions.
         */
        List<ManifestSession> sessionsEligibleForTubeTransfer = manifestSessionDao.findSessionsEligibleForTubeTransfer();
        assertThat(sessionsEligibleForTubeTransfer, not(hasItem(closedSession)));
    }

        /**
         * Creates useful test data for manual UI testing.
         * Keep it disabled in source control.
         *
         * @throws Exception
         */
    @Test(enabled = false)
    public void setupTestDataForTestCases() throws Exception {
        // Persist everything.
        manifestSessionDao.persist(manifestSessionI);
        manifestSessionDao.flush();

        assertThat(manifestSessionI, is(notNullValue()));

        logger.info(String.format("The session ID is %d with a name of %s", manifestSessionI.getManifestSessionId(),
                manifestSessionI.getSessionName()));
        for (ManifestRecord manifestRecord : manifestSessionI.getRecords()) {
            logger.info(String.format(
                    "For session, Record %d has a sample ID of %s with available Mercury Sample of %s and " +
                    "available lab vessel of %s", manifestRecord.getManifestRecordId(),
                    manifestRecord.getSampleId(),
                    sourceSampleToMercurySample.get(manifestRecord.getSampleId()).getSampleKey(),
                    sourceSampleToTargetVessel.get(manifestRecord.getSampleId()).getLabel()));
        }

        manifestSessionEjb.acceptManifestUpload(manifestSessionI.getManifestSessionId());
        for (ManifestRecord manifestRecord : manifestSessionI.getRecords()) {
            manifestSessionEjb.accessionScan(manifestSessionI.getManifestSessionId(), manifestRecord.getSampleId(),
                    manifestRecord.getSampleId());
        }

        manifestSessionEjb.closeSession(manifestSessionI.getManifestSessionId());

        for (LabVessel vessel : sourceSampleToTargetVessel.values()) {
            labVesselDao.persist(vessel);
            labVesselDao.flush();
        }
    }

    public void testCreateManifestSampleAlreadyExists() throws Exception {
        String sampleId = String.valueOf(System.currentTimeMillis());
        MercurySample mercurySample = new MercurySample(sampleId, MercurySample.MetadataSource.MERCURY);
        LabVessel labVessel = new BarcodedTube("A" + sampleId, BarcodedTube.BarcodedTubeType.MatrixTube);
        LabEvent labEvent = new LabEvent(LabEventType.COLLABORATOR_TRANSFER, new Date(), "inTheLab", 0l,
                0l, "mercury");
        labVessel.addInPlaceEvent(labEvent);
        mercurySample.addLabVessel(labVessel);
        mercurySampleDao.persist(mercurySample);

        try {
            manifestSessionEjb.createManifestSession("RP-12", "new session", true, Collections.singleton(
                    ClinicalSampleTestFactory.createSample(ImmutableMap.of(Metadata.Key.BROAD_SAMPLE_ID, sampleId))));
        } catch (InformaticsServiceException e) {
            assertThat(e.getMessage(),
                    containsString(ManifestRecord.ErrorStatus.INVALID_TARGET.getBaseMessage()));
            assertThat(e.getMessage(), containsString("The target vessel has already been used for a tube transfer."));
        }

    }

    public void testSampleReceipt() throws IOException {
        JiraIssue receiptIssue = createReceiptTicket();
        researchProjectDao.persist(researchProject);
        researchProjectDao.flush();
        researchProjectDao.clear();
        String excelFilePath = "manifest-upload/duplicates/good-manifest-1.xlsx";
        InputStream testStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(excelFilePath);

        uploadedSession =
                manifestSessionEjb.uploadManifest(researchProject.getBusinessKey(), testStream, excelFilePath,
                        ManifestSessionEjb.AccessioningProcessType.CRSP, false);
        manifestSessionEjb.acceptManifestUpload(uploadedSession.getManifestSessionId());

        manifestSessionDao.flush();
        manifestSessionDao.clear();

        ManifestSession acceptedSession = manifestSessionDao.find(uploadedSession.getManifestSessionId());
        String sourceSampleToTest = firstUploadedScannedSamples.iterator().next();
        manifestSessionEjb.accessionScan(acceptedSession.getManifestSessionId(), sourceSampleToTest,
                sourceSampleToTest);
        manifestSessionDao.flush();
        manifestSessionDao.clear();

        ManifestSession reFetchedSessionOfScan = manifestSessionDao.find(acceptedSession.getManifestSessionId());

        ManifestRecord sourceRecordToTest = reFetchedSessionOfScan.findRecordByKey(sourceSampleToTest,
                Metadata.Key.SAMPLE_ID);

        assertThat(sourceRecordToTest.getStatus(), is(ManifestRecord.Status.SCANNED));
        assertThat(reFetchedSessionOfScan.hasErrors(), is(false));


        manifestSessionEjb.updateReceiptInfo(acceptedSession.getManifestSessionId(), receiptIssue.getKey());
        manifestSessionEjb.closeSession(acceptedSession.getManifestSessionId());
        manifestSessionDao.flush();
        manifestSessionDao.clear();

        List<String> comments = extractComment(receiptIssue.getKey());
        assertThat(comments.get(0), Matchers.containsString(sourceSampleToTest));
    }

    public void testReuseOfRctTicket() throws IOException {
        researchProjectEjb.submitToJira(researchProject);
        researchProjectDao.persist(researchProject);

        String researchProjectKey = researchProject.getBusinessKey();
        JiraIssue receiptIssue = createReceiptTicket();
        String sampleId1 = String.format("%d.1", System.currentTimeMillis());
        String sampleId2 = String.format("%d.2", System.currentTimeMillis());

        // Upload Sample Vessels
        ParentVesselBean bean1 = new ParentVesselBean(sampleId1, sampleId1, "Cryo vial [2.0 (1.8)mL]", null);
        ParentVesselBean bean2 = new ParentVesselBean(sampleId2, sampleId2, "Cryo vial [2.0 (1.8)mL]", null);
        List<LabVessel> labVessels =
                labVesselFactory.buildLabVessels(Arrays.asList(bean1, bean2), "QADudeLM", new Date(), null,
                        MercurySample.MetadataSource.MERCURY).getLeft();
        labVesselDao.persistAll(labVessels);
        manifestSessionDao.flush(); manifestSessionDao.clear();

        // Create accessioning session 1
        ManifestSession manifestSession1 = createManifestSession(sampleId1, researchProjectKey);
        manifestSessionDao.flush(); manifestSessionDao.clear();

        // Create accessioning session 2
        ManifestSession manifestSession2 = createManifestSession(sampleId2, researchProjectKey);
        manifestSessionDao.flush(); manifestSessionDao.clear();

        // Associate RCT with session 1
        ManifestSession acceptedSession1 = manifestSessionDao.find(manifestSession1.getManifestSessionId());
        manifestSessionEjb.updateReceiptInfo(acceptedSession1.getManifestSessionId(), receiptIssue.getKey());
        manifestSessionDao.flush(); manifestSessionDao.clear();

        // Associate RCT with session 2
        ManifestSession acceptedSession2 = manifestSessionDao.find(manifestSession2.getManifestSessionId());
        manifestSessionEjb.updateReceiptInfo(acceptedSession2.getManifestSessionId(), receiptIssue.getKey());
        manifestSessionDao.flush(); manifestSessionDao.clear();

        // Accession and complete session 1
        manifestSessionEjb.accessionScan(acceptedSession1.getManifestSessionId(), sampleId1, sampleId1
        );
        manifestSessionDao.flush(); manifestSessionDao.clear();

        manifestSessionEjb.closeSession(acceptedSession1.getManifestSessionId());
        manifestSessionDao.flush(); manifestSessionDao.clear();

        // Accession and complete session 2
        manifestSessionEjb.accessionScan(acceptedSession2.getManifestSessionId(), sampleId2, sampleId2
        );
        manifestSessionDao.flush(); manifestSessionDao.clear();

        manifestSessionEjb.closeSession(acceptedSession2.getManifestSessionId());
        manifestSessionDao.flush(); manifestSessionDao.clear();
    }

    private ManifestSession createManifestSession(String sampleId, String researchProjectKey) {

        // Re-fetch researchProject because the current value may be a detached/un-managed entity.
        researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);

        ManifestRecord manifestRecord = new ManifestRecord(
                new Metadata(Metadata.Key.BROAD_SAMPLE_ID, sampleId),
                new Metadata(Metadata.Key.SAMPLE_ID, "test"),
                new Metadata(Metadata.Key.PATIENT_ID, "test"));
        manifestRecord.setManifestRecordIndex(1);
        ManifestSession manifestSession1 = manifestSessionEjb
                .createManifestSession("ManifestSessionContainerTest", Collections.singletonList(manifestRecord),
                        researchProject, true, ManifestSessionEjb.AccessioningProcessType.CRSP);
        manifestSessionEjb.acceptManifestUpload(manifestSession1.getManifestSessionId());
        return manifestSession1;
    }

    private List<String> extractComment(String issueKey) throws IOException {
        JiraIssue jiraIssue = jiraService.getIssueInfo(issueKey, "comment");
        List<String> allComments=new ArrayList<>();
        Map<String, List<Map<String, String>>> map= (HashMap) jiraIssue.getFieldValue("comment");
        if (map.containsKey("comments")) {
            for (Map<String, String> jiraComment : map.get("comments")) {
                if (jiraComment.containsKey("body")){
                    allComments.add(jiraComment.get("body"));
                }
            }
        }
        return allComments;
    }

    /** Tests writing a csv file to the Google bucket. */
    @Test
    public void testCsvToGoogleBucket() {
        String filename = "test-" + System.currentTimeMillis() + ".csv";
        // Writes a manifest that has 3 rows, 3 columns.
        byte[] upload = "header 1,header 2,header 3\n1-1,1-2,1-3\n2-1,2-2,2-3\n".getBytes();
        manifestSessionEjb.copyToGoogleBucket(filename, upload);
        // Reads the file back from the Google Bucket. There should be no errors and identical file content,
        // after removing the csv field quotes added by OpenCsv
        MessageCollection messageCollection = new MessageCollection();
        byte[] download = googleBucketDao.download(filename, messageCollection);
        String downloadErrors = StringUtils.join(messageCollection.getErrors(), "; ");
        // Removes the test file from the bucket.
        googleBucketDao.delete(filename, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        Assert.assertEquals(downloadErrors, "");
        Assert.assertEquals(new String(download).replaceAll("\"", ""), new String(upload).replaceAll("\"", ""));
    }

    /** Tests writing a xlsx file to the Google bucket (as a csv file). */
    @Test
    public void testXlsxToGoogleBucket() throws IOException {
        String filename = "test_1584995285.xlsx";
        byte[] upload = IOUtils.toByteArray(VarioskanParserTest.getTestResource(filename));
        manifestSessionEjb.copyToGoogleBucket(filename, upload);
        // Reads the file back from the Google Bucket. There should be no errors and identical file content,
        // after removing the csv field quotes added by OpenCsv
        MessageCollection messageCollection = new MessageCollection();
        byte[] download = googleBucketDao.download(filename, messageCollection);
        String downloadErrors = StringUtils.join(messageCollection.getErrors(), "; ");
        // Removes the test file from the bucket.
        googleBucketDao.delete(filename, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));

        Assert.assertEquals(downloadErrors, "");
        Assert.assertEquals(new String(download).replaceAll("\"", ""),
                "header 1,header 2,header 3\n1-1,1-2,1-3\n2-1,2-2,2-3\n");
    }
}
