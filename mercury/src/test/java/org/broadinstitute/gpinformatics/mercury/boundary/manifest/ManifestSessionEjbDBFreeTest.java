package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.collect.ImmutableSet;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestStatus;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsEmptyCollection.emptyCollectionOf;

@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestSessionEjbDBFreeTest {

    private static final String MANIFEST_FILE_GOOD = "manifest-upload/good-manifest.xlsx";

    private static final String PATHS_TO_PREFIXES_PROVIDER = "pathsToPrefixesProvider";

    private static final String BAD_MANIFEST_UPLOAD_PROVIDER = "badManifestUploadProvider";

    private static final BSPUserList.QADudeUser TEST_USER = new BSPUserList.QADudeUser("BUICK USER", 42);

    private static final String TEST_RESEARCH_PROJECT_KEY = "RP-1";

    private static final int NUM_RECORDS_IN_GOOD_MANIFEST = 23;

    private static final String MANIFEST_FILE_DUPLICATES_SAME_SESSION = "manifest-upload/manifest-with-duplicates.xlsx";

    private static final int NUM_DUPLICATES_IN_MANIFEST_WITH_DUPLICATES_IN_SAME_SESSION = 7;

    private static final String MANIFEST_FILE_MISMATCHED_GENDERS_SAME_SESSION =
            "manifest-upload/gender-mismatches-within-session/good-manifest.xlsx";

    private static final Set<String> PATIENT_IDS_FOR_SAME_MANIFEST_GENDER_MISMATCHES =
            ImmutableSet.of("004-002", "003-009", "005-012");

    private static final Set<String> DUPLICATED_SAMPLE_IDS =
            ImmutableSet.of("03101231193", "03101411324", "03101254356");

    private static final long ARBITRARY_MANIFEST_SESSION_ID = 3L;

    private static final String GOOD_MANIFEST_ACCESSION_SCAN_PROVIDER = "goodManifestAccessionScanProvider";

    private static final String GOOD_TUBE_BARCODE = "03101231193";

    private static final String BAD_TUBE_BARCODE = "bad tube barcode";
    private static final String FEMALE = "Female";
    private static final String MALE = "Male";
    private ManifestSessionDao manifestSessionDao;
    private ResearchProjectDao researchProjectDao;
    private String sourceForTransfer = "9294923";

    private static final String TEST_SAMPLE_KEY = "SM-BUICK1";
    private static final String BSP_TEST_SAMPLE_KEY = TEST_SAMPLE_KEY + "BSP";
    private static final String TEST_SAMPLE_ALREADY_TRANSFERRED = TEST_SAMPLE_KEY + "ACCESSIONED";
    private static final String TEST_SAMPLE_KEY_UNASSOCIATED = "SM-BUICK2";

    private MercurySample testSampleForAccessioning;
    private MercurySample testUnassociatedSampleForAccessioning;
    private MercurySample testSampleForBsp;
    private MercurySample testSampleAlreadyTransferred;

    private static final String TEST_VESSEL_LABEL_ALREADY_TRANSFERRED = "A0993929292Trans";
    private static final String TEST_VESSEL_LABEL = "A0993929292";
    private LabVessel testVessel;
    private LabVessel testVesselAlreadyTransferred;

    private MercurySampleDao mercurySampleDao;
    private LabVesselDao labVesselDao;
    private final BSPUserList.QADudeUser testLabUser = new BSPUserList.QADudeUser("LU", 342L);

    @BeforeMethod
    public void setUp() throws Exception {
        manifestSessionDao = Mockito.mock(ManifestSessionDao.class);
        researchProjectDao = Mockito.mock(ResearchProjectDao.class);
        mercurySampleDao = Mockito.mock(MercurySampleDao.class);
        labVesselDao = Mockito.mock(LabVesselDao.class);

        testSampleForAccessioning =
                new MercurySample(TEST_SAMPLE_KEY, MercurySample.MetadataSource.MERCURY);
        testUnassociatedSampleForAccessioning =
                new MercurySample(TEST_SAMPLE_KEY_UNASSOCIATED, MercurySample.MetadataSource.MERCURY);
        testSampleForBsp =
                new MercurySample(BSP_TEST_SAMPLE_KEY, MercurySample.MetadataSource.BSP);
        testSampleAlreadyTransferred = new MercurySample(TEST_SAMPLE_ALREADY_TRANSFERRED,
                MercurySample.MetadataSource.MERCURY);
        testVessel =
                new BarcodedTube(TEST_VESSEL_LABEL, BarcodedTube.BarcodedTubeType.MatrixTube2mL);
        testVesselAlreadyTransferred = new BarcodedTube(TEST_VESSEL_LABEL_ALREADY_TRANSFERRED,
                BarcodedTube.BarcodedTubeType.MatrixTube2mL);
    }

    /**
     * Worker method for manifest upload testing.
     */
    private ManifestSession uploadManifest(String pathToManifestFile) throws Exception {
        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject(
                TEST_RESEARCH_PROJECT_KEY);
        return uploadManifest(pathToManifestFile, researchProject);
    }

    private ManifestSession uploadManifest(ManifestSessionEjb manifestSessionEjb, String pathToManifestFile,
                                           ResearchProject researchProject)
            throws Exception {
        String PATH_TO_SPREADSHEET = TestUtils.getTestData(pathToManifestFile);
        InputStream inputStream = new FileInputStream(PATH_TO_SPREADSHEET);
        return manifestSessionEjb.uploadManifest(researchProject.getBusinessKey(), inputStream, PATH_TO_SPREADSHEET,
                TEST_USER);
    }

    /**
     * Worker method for manifest upload testing.
     */
    private ManifestSession uploadManifest(String pathToManifestFile, ResearchProject researchProject)
            throws Exception {
        ManifestSessionEjb ejb = buildEjbForUpload(researchProject);
        return uploadManifest(ejb, pathToManifestFile, researchProject);
    }

    private ManifestSessionEjb buildEjbForUpload(ResearchProject researchProject) {
        Mockito.when(researchProjectDao.findByBusinessKey(Mockito.anyString())).thenReturn(researchProject);

        return new ManifestSessionEjb(manifestSessionDao, researchProjectDao, mercurySampleDao, labVesselDao);
    }

    /**
     * Utility class to encapsulate both objects returned by the #buildForAcceptSession method.
     */
    private class ManifestSessionAndEjbHolder {
        ManifestSession manifestSession;
        ManifestSessionEjb ejb;
    }

    /**
     * helper method to initialize the EJ and session used for testing based on parameters.  This version is called if
     * the user wants a new Research Project Created
     *
     * @param initialStatus      Initial status for all the manifest records created
     * @param numberOfRecords    the number of initial manifest records to create
     *
     * @return A new instance of EJB and Holder class (which holds a manifest session ejb and
     * session
     *
     * @throws Exception
     */
    private ManifestSessionAndEjbHolder buildHolderForSession(ManifestRecord.Status initialStatus, int numberOfRecords)
            throws Exception {

        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject(
                TEST_RESEARCH_PROJECT_KEY);
        return buildHolderForSession(initialStatus, numberOfRecords, researchProject);
    }

    /**
     * @param initialStatus   Initial status for all the manifest records created
     * @param numberOfRecords the number of initial manifest records to create
     * @param researchProject An instance of a research project with which to associate the new session
     *
     * @return A new instance of the ManifestSessionAndEjbHolder class (which holds a manifest session ejb and
     * session)
     */
    private ManifestSessionAndEjbHolder buildHolderForSession(ManifestRecord.Status initialStatus,
                                                              int numberOfRecords,
                                                              ResearchProject researchProject)
            throws Exception {

        final ManifestSessionAndEjbHolder holder = new ManifestSessionAndEjbHolder();
        holder.manifestSession = ManifestTestFactory.buildManifestSession(researchProject.getBusinessKey(),
                "BuickCloseGood", testLabUser, numberOfRecords, initialStatus);

        Mockito.when(manifestSessionDao.find(Mockito.anyLong())).thenAnswer(new Answer<ManifestSession>() {
            @Override
            public ManifestSession answer(InvocationOnMock invocation) throws Throwable {
                return holder.manifestSession;
            }
        });
        Mockito.when(researchProjectDao.findByBusinessKey(Mockito.anyString())).thenReturn(researchProject);

        Mockito.when(mercurySampleDao.findBySampleKey(Mockito.eq(TEST_SAMPLE_KEY))).thenReturn(
                Collections.singletonList(testSampleForAccessioning));

        Mockito.when(mercurySampleDao.findBySampleKey(Mockito.eq(BSP_TEST_SAMPLE_KEY))).thenReturn(
                Collections.singletonList(testSampleForBsp));

        Mockito.when(mercurySampleDao.findBySampleKey(Mockito.eq(TEST_SAMPLE_ALREADY_TRANSFERRED))).thenReturn(
                Collections.singletonList(testSampleAlreadyTransferred));

        Mockito.when(mercurySampleDao.findBySampleKey(Mockito.eq(TEST_SAMPLE_KEY_UNASSOCIATED))).thenReturn(
                Collections.singletonList(testUnassociatedSampleForAccessioning)
        );

        testVessel.addSample(testSampleForAccessioning);

        Mockito.when(labVesselDao.findByIdentifier(Mockito.eq(TEST_VESSEL_LABEL))).thenReturn(testVessel);


        testVesselAlreadyTransferred.addSample(testSampleAlreadyTransferred);
        LabEvent collaboratorTransferEvent =
                new LabEvent(LabEventType.COLLABORATOR_TRANSFER, new Date(), " ", 1L, testLabUser.getUserId(), "");
        testVesselAlreadyTransferred.addInPlaceEvent(collaboratorTransferEvent);

        Mockito.when(labVesselDao.findByIdentifier(Mockito.eq(TEST_VESSEL_LABEL_ALREADY_TRANSFERRED)))
                .thenReturn(testVesselAlreadyTransferred);

        holder.ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao, mercurySampleDao, labVesselDao);

        return holder;
    }

    /* ================================================================= */
    /* =====  Test Cases =============================================== */
    /* ================================================================= */

    public void researchProjectNotFound() {
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao, mercurySampleDao,
                labVesselDao);
        try {
            ejb.uploadManifest(null, null, null, TEST_USER);
            Assert.fail();
        } catch (InformaticsServiceException ignored) {
        }
    }

    @Test(dataProvider = PATHS_TO_PREFIXES_PROVIDER)
    public void extractPrefixFromFilename(String path, String expectedPrefix) {
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao, mercurySampleDao,
                labVesselDao);
        try {
            String actualPrefix = ejb.extractPrefixFromFilename(path);
            assertThat(actualPrefix, is(equalTo(expectedPrefix)));
        } catch (InformaticsServiceException ignored) {
        }
    }


    /********************************************************************/
    /**  =======  upload manifest tests ============================== **/
    /********************************************************************/

    public void uploadGoodManifest() throws Exception {
        ManifestSession manifestSession = uploadManifest(MANIFEST_FILE_GOOD);
        assertThat(manifestSession, is(notNullValue()));
        assertThat(manifestSession.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));
        assertThat(manifestSession.hasErrors(), is(false));
        assertThat(manifestSession.getManifestEvents(), is(empty()));
    }

    @DataProvider(name = BAD_MANIFEST_UPLOAD_PROVIDER)
    public Object[][] badManifestUploadProvider() {
        return new Object[][]{
                {"Not an Excel file", "manifest-upload/not-an-excel-file.txt"},
                {"Missing required field", "manifest-import/test-manifest-missing-required.xlsx"},
                {"Missing column", "manifest-upload/manifest-with-missing-column.xlsx"},
                {"Empty manifest", "manifest-upload/empty-manifest.xlsx"}
        };
    }

    @Test(dataProvider = BAD_MANIFEST_UPLOAD_PROVIDER)
    public void uploadBadManifest(String description, String pathToManifestFile) throws Exception {
        try {
            uploadManifest(pathToManifestFile);
            Assert.fail(description);
        } catch (InformaticsServiceException ignored) {
        }
    }

    public void uploadManifestThatDuplicatesSampleIdInSameManifest() throws Exception {
        ManifestSession manifestSession = uploadManifest(MANIFEST_FILE_DUPLICATES_SAME_SESSION);
        assertThat(manifestSession, is(notNullValue()));
        assertThat(manifestSession.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));
        assertThat(manifestSession.hasErrors(), is(true));
        List<ManifestRecord> quarantinedRecords = new ArrayList<>();

        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            if (manifestRecord.isQuarantined()) {
                quarantinedRecords.add(manifestRecord);
            }
        }
        // This file contains two instances of duplication and one triplication, so 2 x 2 + 3 = 7.
        assertThat(quarantinedRecords, hasSize(NUM_DUPLICATES_IN_MANIFEST_WITH_DUPLICATES_IN_SAME_SESSION));
    }

    public void uploadManifestThatDuplicatesSampleIdInAnotherManifest() throws Exception {
        ResearchProject researchProject =
                ResearchProjectTestFactory.createTestResearchProject(TEST_RESEARCH_PROJECT_KEY);

        ManifestSession manifestSession1 =
                uploadManifest("manifest-upload/duplicates/good-manifest-1.xlsx", researchProject);
        assertThat(manifestSession1, is(notNullValue()));
        assertThat(manifestSession1.getManifestEvents(), is(empty()));
        assertThat(manifestSession1.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));

        ManifestSession manifestSession2 =
                uploadManifest("manifest-upload/duplicates/good-manifest-2.xlsx", researchProject);
        assertThat(manifestSession2, is(notNullValue()));
        assertThat(manifestSession2.getManifestEvents(), hasSize(2));
        assertThat(manifestSession2.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));
    }

    public void uploadManifestThatMismatchesGenderInSameManifest() throws Exception {
        ManifestSession manifestSession = uploadManifest(MANIFEST_FILE_MISMATCHED_GENDERS_SAME_SESSION);
        assertThat(manifestSession, is(notNullValue()));
        assertThat(manifestSession.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));

        // The assert below is for size * 2 since all manifest records in question are in this manifest session.
        assertThat(manifestSession.getManifestEvents(),
                hasSize(PATIENT_IDS_FOR_SAME_MANIFEST_GENDER_MISMATCHES.size() * 2));
        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            assertThat(manifestRecord.isQuarantined(), is(false));
            Metadata patientIdMetadata = manifestRecord.getMetadataByKey(Metadata.Key.PATIENT_ID);
            if (PATIENT_IDS_FOR_SAME_MANIFEST_GENDER_MISMATCHES.contains(patientIdMetadata.getValue())) {
                assertThat(manifestRecord.getManifestEvents(), hasSize(1));
                assertThat(manifestRecord.getManifestEvents().get(0).getSeverity(), is(ManifestEvent.Severity.ERROR));
            }
        }
    }

    public void uploadManifestThatMismatchesGenderInAnotherManifest() throws Exception {
        ResearchProject researchProject =
                ResearchProjectTestFactory.createTestResearchProject(TEST_RESEARCH_PROJECT_KEY);

        ManifestSession manifestSession1 =
                uploadManifest("manifest-upload/gender-mismatches-across-sessions/good-manifest-1.xlsx",
                        researchProject);
        assertThat(manifestSession1, is(notNullValue()));
        assertThat(manifestSession1.getManifestEvents(), is(empty()));
        assertThat(manifestSession1.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));

        ManifestSession manifestSession2 =
                uploadManifest("manifest-upload/gender-mismatches-across-sessions/good-manifest-2.xlsx",
                        researchProject);
        assertThat(manifestSession2, is(notNullValue()));
        Set<String> expectedPatientIds = ImmutableSet.of("001-001", "005-005", "009-001");
        assertThat(manifestSession2.getManifestEvents(), hasSize(expectedPatientIds.size()));
        assertThat(manifestSession2.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));
        for (ManifestRecord manifestRecord : manifestSession2.getRecords()) {
            assertThat(manifestRecord.isQuarantined(), is(false));
            Metadata patientIdMetadata = manifestRecord.getMetadataByKey(Metadata.Key.PATIENT_ID);
            if (expectedPatientIds.contains(patientIdMetadata.getValue())) {
                assertThat(manifestRecord.getManifestEvents(), hasSize(1));
                assertThat(manifestRecord.getManifestEvents().get(0).getSeverity(), is(ManifestEvent.Severity.ERROR));
            }
        }
    }

    /********************************************************************/
    /**  =======  Load session tests  ================================ **/
    /********************************************************************/

    public void loadManifestSessionSuccess() {
        final long TEST_MANIFEST_SESSION_ID = ARBITRARY_MANIFEST_SESSION_ID;
        Mockito.when(manifestSessionDao.find(Mockito.anyLong())).then(new Answer<ManifestSession>() {
            @Override
            public ManifestSession answer(final InvocationOnMock invocation) throws Throwable {
                return new ManifestSession() {
                    @Override
                    public Long getManifestSessionId() {
                        return TEST_MANIFEST_SESSION_ID;
                    }
                };
            }
        });
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao, mercurySampleDao,
                labVesselDao);
        ManifestSession manifestSession = ejb.loadManifestSession(TEST_MANIFEST_SESSION_ID);
        assertThat(manifestSession, is(notNullValue()));
        assertThat(manifestSession.getManifestSessionId(), is(TEST_MANIFEST_SESSION_ID));
    }

    public void loadManifestSessionFailure() {
        final long TEST_MANIFEST_SESSION_ID = ARBITRARY_MANIFEST_SESSION_ID;
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao, mercurySampleDao,
                labVesselDao);
        ManifestSession manifestSession = ejb.loadManifestSession(TEST_MANIFEST_SESSION_ID);
        assertThat(manifestSession, is(nullValue()));
    }

    /********************************************************************/
    /**  =======  Accept upload tests ================================ **/
    /********************************************************************/

    public void acceptUploadSessionNotFound() {
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao, mercurySampleDao,
                labVesselDao);
        long MANIFEST_SESSION_ID = ARBITRARY_MANIFEST_SESSION_ID;
        try {
            ejb.acceptManifestUpload(MANIFEST_SESSION_ID);
            Assert.fail();
        } catch (InformaticsServiceException ignored) {
            assertThat(ignored.getMessage(), containsString("Unrecognized Manifest Session ID"));
        }
    }

    public void acceptUploadSuccessful() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.UPLOADED, 20);
        holder.ejb.acceptManifestUpload(ARBITRARY_MANIFEST_SESSION_ID);
        for (ManifestRecord manifestRecord : holder.manifestSession.getRecords()) {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
        }
    }

    public void acceptSessionWithDuplicatesInThisSession() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.UPLOADED, 20);

        ManifestSession manifestSession = holder.manifestSession;

        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101231193");
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101231193");
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101254356");
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101254356");
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101254356");
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101411324");
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101411324");

        List<ManifestRecord> manifestRecordsMarkedAsDuplicates = new ArrayList<>();

        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOADED));
            if (manifestRecord.isQuarantined()) {
                manifestRecordsMarkedAsDuplicates.add(manifestRecord);
            } else {
                assertThat(manifestRecord.getManifestEvents(), is(empty()));
            }
        }
        assertThat(manifestRecordsMarkedAsDuplicates,
                hasSize(NUM_DUPLICATES_IN_MANIFEST_WITH_DUPLICATES_IN_SAME_SESSION));

        // Arbitrary ID, the DAO is programmed to return the same manifest session for any requested ID.
        holder.ejb.acceptManifestUpload(ARBITRARY_MANIFEST_SESSION_ID);

        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            boolean shouldBeMarkedAsDuplicate = manifestRecordsMarkedAsDuplicates.contains(manifestRecord);
            assertThat(manifestRecord.isQuarantined(), is(shouldBeMarkedAsDuplicate));
            if (!shouldBeMarkedAsDuplicate) {
                assertThat(manifestRecord.getManifestEvents(), is(empty()));
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
            } else {
                // No new events should have been added.
                assertThat(manifestRecord.getManifestEvents(), hasSize(1));
            }
        }
    }

    public void acceptSessionWithMismatchedGendersThisSession() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.UPLOADED, 20);

        ManifestSession manifestSession = holder.manifestSession;

        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101947686", Metadata.Key.PATIENT_ID, "003-009", Metadata.Key.GENDER, FEMALE);
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101989209", Metadata.Key.PATIENT_ID, "003-009", Metadata.Key.GENDER, MALE);
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101231193", Metadata.Key.PATIENT_ID, "004-002", Metadata.Key.GENDER, MALE);
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101067213", Metadata.Key.PATIENT_ID, "004-002", Metadata.Key.GENDER, FEMALE);
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101752021", Metadata.Key.PATIENT_ID, "005-012", Metadata.Key.GENDER, FEMALE);
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101752020", Metadata.Key.PATIENT_ID, "005-012", Metadata.Key.GENDER, MALE);

        List<ManifestRecord> manifestRecordsWithMismatchedGenders = new ArrayList<>();

        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOADED));
            assertThat(manifestRecord.isQuarantined(), is(false));

            List<ManifestEvent> manifestEvents = manifestRecord.getManifestEvents();
            boolean hasManifestEvents = !manifestEvents.isEmpty();
            if (hasManifestEvents) {
                assertThat(manifestEvents, hasSize(1));
                assertThat(manifestEvents.get(0).getSeverity(), is(ManifestEvent.Severity.ERROR));
                manifestRecordsWithMismatchedGenders.add(manifestRecord);
            }
            String patientId = manifestRecord.getMetadataByKey(Metadata.Key.PATIENT_ID).getValue();
            assertThat(PATIENT_IDS_FOR_SAME_MANIFEST_GENDER_MISMATCHES.contains(patientId),
                    is(equalTo(hasManifestEvents)));
        }

        assertThat(manifestRecordsWithMismatchedGenders, is(not(empty())));
        assertThat(manifestSession.getManifestEvents().size(), is(6));

        // Arbitrary ID, the DAO is programmed to return the same manifest session for any requested ID.
        holder.ejb.acceptManifestUpload(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(manifestSession.getManifestEvents().size(), is(6));
        assertThat(manifestSession.getManifestEvents(), is(not(empty())));

        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            boolean shouldBeMarkedAsDuplicate = manifestRecordsWithMismatchedGenders.contains(manifestRecord);
            assertThat(manifestRecord.getManifestEvents().isEmpty(), is(!shouldBeMarkedAsDuplicate));
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));

            if (shouldBeMarkedAsDuplicate) {
                // No new events should have been added.
                assertThat(manifestRecord.getManifestEvents(), hasSize(1));
            }
        }
    }

    @DataProvider(name = PATHS_TO_PREFIXES_PROVIDER)
    private Iterator<Object[]> pathsToPrefixesProvider() {
        String[] paths = {
                // no path
                "",
                // Unix path
                "/some/path/to/",
                // Windows path
                "c:\\ugh\\windows\\"
        };
        // Old and new Excel formats
        String[] suffixes = {
                ".xls",
                ".xlsx"
        };

        List<Object[]> pathsAndBaseFileNames = new ArrayList<>();
        for (String path : paths) {
            for (String suffix : suffixes) {
                String BASE_FILENAME = "spreadsheet";
                pathsAndBaseFileNames.add(new Object[]{path + BASE_FILENAME + suffix, BASE_FILENAME});
            }
        }
        return pathsAndBaseFileNames.iterator();
    }

    @DataProvider(name = GOOD_MANIFEST_ACCESSION_SCAN_PROVIDER)
    public Object[][] goodManifestAccessionScanProvider() {
        return new Object[][]{
                // Good tube barcode should succeed.
                {GOOD_TUBE_BARCODE, true},
                // Bad tube barcode should fail.
                {BAD_TUBE_BARCODE, false}};
    }


    /********************************************************************/
    /**  =======  accession scan tests =============================== **/
    /********************************************************************/

    @Test(dataProvider = GOOD_MANIFEST_ACCESSION_SCAN_PROVIDER)
    public void accessionScanGoodManifest(String tubeBarcode, boolean successExpected) throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.UPLOADED, 20);
        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                null, ManifestRecord.Status.UPLOAD_ACCEPTED, Metadata.Key.SAMPLE_ID, "03101231193");

        ManifestSessionEjb ejb = holder.ejb;
        ejb.acceptManifestUpload(ARBITRARY_MANIFEST_SESSION_ID);
        // The correct state after manifest upload is checked in other tests.

        try {
            ejb.accessionScan(ARBITRARY_MANIFEST_SESSION_ID, tubeBarcode);
            if (!successExpected) {
                Assert.fail();
            }
        } catch (InformaticsServiceException e) {
            if (successExpected) {
                Assert.fail();
            }
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.NOT_IN_MANIFEST.getBaseMessage()));
        }

        ManifestSession manifestSession = holder.manifestSession;
        assertThat(manifestSession.getManifestEvents(), is(empty()));

        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            String collaboratorSampleId = manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue();
            if (collaboratorSampleId.equals(tubeBarcode)) {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SCANNED));
            } else {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
            }
            assertThat(manifestRecord.getManifestEvents(), is(empty()));
        }
    }

    public void accessionScanManifestWithDuplicates() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.UPLOADED, 20);

        ManifestSession manifestSession = holder.manifestSession;

        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101231193");
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101231193");
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101254356");
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101254356");
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101254356");
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101411324");
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, "03101411324");

        ManifestSessionEjb ejb = holder.ejb;
        ejb.acceptManifestUpload(ARBITRARY_MANIFEST_SESSION_ID);
        // The correct state after manifest upload is checked in other tests.

        try {
            // Take an arbitrary one of the duplicated sample IDs.
            String duplicatedSampleId = DUPLICATED_SAMPLE_IDS.iterator().next();

            ejb.accessionScan(ARBITRARY_MANIFEST_SESSION_ID, duplicatedSampleId);
            Assert.fail();
        } catch (InformaticsServiceException e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.getBaseMessage()));
        }
    }

    public void accessionScanGenderMismatches() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.UPLOAD_ACCEPTED, 20);

        ManifestSession manifestSession = holder.manifestSession;

        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.UPLOAD_ACCEPTED,
                Metadata.Key.SAMPLE_ID, "03101947686", Metadata.Key.PATIENT_ID, "003-009", Metadata.Key.GENDER, FEMALE);
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.UPLOAD_ACCEPTED,
                Metadata.Key.SAMPLE_ID, "03101989209", Metadata.Key.PATIENT_ID, "003-009", Metadata.Key.GENDER, MALE);
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.UPLOAD_ACCEPTED,
                Metadata.Key.SAMPLE_ID, "03101231193", Metadata.Key.PATIENT_ID, "004-002", Metadata.Key.GENDER, MALE);
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.UPLOAD_ACCEPTED,
                Metadata.Key.SAMPLE_ID, "03101067213", Metadata.Key.PATIENT_ID, "004-002", Metadata.Key.GENDER, FEMALE);
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.UPLOAD_ACCEPTED,
                Metadata.Key.SAMPLE_ID, "03101752021", Metadata.Key.PATIENT_ID, "005-012", Metadata.Key.GENDER, FEMALE);
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.UPLOAD_ACCEPTED,
                Metadata.Key.SAMPLE_ID, "03101752020", Metadata.Key.PATIENT_ID, "005-012", Metadata.Key.GENDER, MALE);

        // There should be twice as many manifest events as there are patient IDs with mismatched genders as all the
        // mismatches are in the same manifest.
        int EXPECTED_NUMBER_OF_EVENTS_ON_SESSION = PATIENT_IDS_FOR_SAME_MANIFEST_GENDER_MISMATCHES.size() * 2;
        assertThat(manifestSession.getManifestEvents(), hasSize(EXPECTED_NUMBER_OF_EVENTS_ON_SESSION));

        // Pick an arbitrary one of the mismatched gender patient IDs.
        String mismatchedGenderPatientId = PATIENT_IDS_FOR_SAME_MANIFEST_GENDER_MISMATCHES.iterator().next();
        // Find one of the records for this patient.
        ManifestRecord manifestRecord =
                manifestSession.getRecordWithMatchingValueForKey(Metadata.Key.PATIENT_ID, mismatchedGenderPatientId);

        assertThat(manifestRecord.getManifestEvents(), hasSize(1));
        assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));

        ManifestEvent manifestEvent = manifestRecord.getManifestEvents().iterator().next();
        assertThat(manifestEvent.getMessage(), containsString(
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER.getBaseMessage()));
        assertThat(manifestEvent.getSeverity(), is(ManifestEvent.Severity.ERROR));

        holder.ejb.accessionScan(ARBITRARY_MANIFEST_SESSION_ID,
                manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue());

        assertThat(manifestSession.getManifestEvents(), hasSize(EXPECTED_NUMBER_OF_EVENTS_ON_SESSION));
        assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SCANNED));
    }

    public void accessionScanDoubleScan() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.UPLOADED, 20);
        ManifestSessionEjb ejb = holder.ejb;
        ejb.acceptManifestUpload(ARBITRARY_MANIFEST_SESSION_ID);
        // The correct state after manifest upload is checked in other tests.

        String goodTubeBarcode = "SAMPLE_ID_11";
        ejb.accessionScan(ARBITRARY_MANIFEST_SESSION_ID, goodTubeBarcode);
        // The results of this are checked in accessionScanGoodManifest

        try {
            // Should fail on a second scan.
            ejb.accessionScan(ARBITRARY_MANIFEST_SESSION_ID, goodTubeBarcode);
            Assert.fail();
        } catch (InformaticsServiceException e) {
            assertThat(e.getMessage(),
                    containsString(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_SCAN.getBaseMessage()));
        }
    }

    /********************************************************************/
    /**  =======  Prepare to close    ================================ **/
    /********************************************************************/

    public void prepareForSessionCloseGoodSession() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20);

        ManifestStatus sessionStatus = holder.ejb.getSessionStatus(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(sessionStatus.getErrorMessages(), is(empty()));
        assertThat(sessionStatus.getSamplesEligibleInManifest(), is(20));
        assertThat(sessionStatus.getSamplesSuccessfullyScanned(), is(20));
        assertThat(sessionStatus.getSamplesInManifest(), is(20));
    }

    public void prepareForSessionCloseWithDuplicate() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20);

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE);

        ManifestStatus sessionStatus = holder.ejb.getSessionStatus(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(sessionStatus.getErrorMessages().size(), is(1));
        assertThat(sessionStatus.getErrorMessages(), hasItem(containsString(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.getBaseMessage())));
        assertThat(sessionStatus.getSamplesEligibleInManifest(), is(20));
        assertThat(sessionStatus.getSamplesSuccessfullyScanned(), is(20));
        assertThat(sessionStatus.getSamplesInManifest(), is(21));
    }

    public void prepareForSessionCloseWithUnScanned() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20);

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                null, ManifestRecord.Status.UPLOAD_ACCEPTED, Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE
        );

        ManifestStatus sessionStatus = holder.ejb.getSessionStatus(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(sessionStatus.getErrorMessages().size(), is(1));
        assertThat(sessionStatus.getErrorMessages(), hasItem(containsString(ManifestRecord.ErrorStatus.MISSING_SAMPLE.getBaseMessage())));
        assertThat(sessionStatus.getSamplesEligibleInManifest(), is(21));
        assertThat(sessionStatus.getSamplesSuccessfullyScanned(), is(20));
        assertThat(sessionStatus.getSamplesInManifest(), is(21));
    }

    public void prepareForSessionCloseWithUnScannedAndDuplicate() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20);

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                null, ManifestRecord.Status.UPLOAD_ACCEPTED, Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE
        );

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE);

        ManifestStatus sessionStatus = holder.ejb.getSessionStatus(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(sessionStatus.getErrorMessages().size(), is(2));
        assertThat(sessionStatus.getErrorMessages(), hasItem(containsString(ManifestRecord.ErrorStatus.MISSING_SAMPLE.getBaseMessage())));
        assertThat(sessionStatus.getErrorMessages(), hasItem(containsString(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.getBaseMessage())));
        assertThat(sessionStatus.getSamplesEligibleInManifest(), is(21));
        assertThat(sessionStatus.getSamplesSuccessfullyScanned(), is(20));
        assertThat(sessionStatus.getSamplesInManifest(), is(22));
    }

    public void prepareForSessionCloseWithMismatchedGenders() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20);

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.SCANNED,
                Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE);

        ManifestStatus sessionStatus = holder.ejb.getSessionStatus(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(sessionStatus.getErrorMessages().size(), is(1));
        assertThat(sessionStatus.getErrorMessages(), hasItem(containsString(ManifestRecord.ErrorStatus.MISMATCHED_GENDER.getBaseMessage())));
        assertThat(sessionStatus.getSamplesEligibleInManifest(), is(21));
        assertThat(sessionStatus.getSamplesSuccessfullyScanned(), is(21));
        assertThat(sessionStatus.getSamplesInManifest(), is(21));
    }

    /********************************************************************/
    /**  =======  close    ================================ **/
    /********************************************************************/

    public void closeGoodManifest() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20);

        holder.ejb.closeSession(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(holder.manifestSession.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));
        assertThat(holder.manifestSession.getManifestEvents(), is(empty()));
        for (ManifestRecord manifestRecord : holder.manifestSession.getRecords()) {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
        }
    }

    public void closeManifestWithDuplicate() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20);
        String duplicateSampleId = GOOD_TUBE_BARCODE;
        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, duplicateSampleId);

        holder.ejb.closeSession(ARBITRARY_MANIFEST_SESSION_ID);
        assertThat(holder.manifestSession.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));

        assertThat(holder.manifestSession.getManifestEvents().size(), is(1));

        for (ManifestRecord manifestRecord : holder.manifestSession.getRecords()) {
            if (manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue().equals(duplicateSampleId)) {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOADED));
            } else {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
            }
        }
    }

    public void closeManifestWithUnScannedRecord() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20);
        String unScannedBarcode = GOOD_TUBE_BARCODE;
        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                null, ManifestRecord.Status.UPLOAD_ACCEPTED, Metadata.Key.SAMPLE_ID, unScannedBarcode);

        holder.ejb.closeSession(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(holder.manifestSession.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));

        assertThat(holder.manifestSession.getManifestEvents().size(), is(1));

        for (ManifestRecord manifestRecord : holder.manifestSession.getRecords()) {
            if (manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue().equals(unScannedBarcode)) {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
                assertThat(manifestRecord.isQuarantined(), is(true));
            } else {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
            }
        }
    }

    public void closeManifestWithDuplicateAndUnScannedRecords() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20);

        String unscannedBarcode = GOOD_TUBE_BARCODE;
        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                null, ManifestRecord.Status.UPLOAD_ACCEPTED, Metadata.Key.SAMPLE_ID, unscannedBarcode);

        String dupeSampleId = GOOD_TUBE_BARCODE + "dupe";
        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, dupeSampleId);

        holder.ejb.closeSession(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(holder.manifestSession.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));

        assertThat(holder.manifestSession.getManifestEvents().size(), is(2));
        for (ManifestRecord manifestRecord : holder.manifestSession.getRecords()) {
            if (manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue().equals(unscannedBarcode)) {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
                assertThat(manifestRecord.isQuarantined(), is(true));
            } else if (manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue().equals(dupeSampleId)) {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOADED));
                assertThat(manifestRecord.isQuarantined(), is(true));
            } else {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
            }
        }
    }

    public void closeManifestWithMisMatchedGenderRecords() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.SCANNED, 20);

        String misMatch1Barcode = GOOD_TUBE_BARCODE + "BadGender1";
        ManifestSession manifestSession = holder.manifestSession;
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.SCANNED,
                Metadata.Key.SAMPLE_ID, misMatch1Barcode);

        String misMatch2Barcode = GOOD_TUBE_BARCODE + "BadGender2";
        ManifestTestFactory.addExtraRecord(manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.SCANNED,
                Metadata.Key.SAMPLE_ID, misMatch2Barcode);

        holder.ejb.closeSession(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(manifestSession.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));

        assertThat(manifestSession.getManifestEvents().size(), is(2));

        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
        }
    }

    /********************************************************************/
    /**  =======  Validate source test (Supports Ajax Call) ========== **/
    /********************************************************************/

    public void validateSourceOnCleanSession() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20);

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                null, ManifestRecord.Status.ACCESSIONED, Metadata.Key.SAMPLE_ID, sourceForTransfer);

        ManifestRecord foundRecord =
                holder.ejb.validateSourceTubeForTransfer(ARBITRARY_MANIFEST_SESSION_ID, sourceForTransfer);

        assertThat(foundRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue(), is(equalTo(sourceForTransfer)));
        assertThat(foundRecord.getManifestEvents(), is(emptyCollectionOf(ManifestEvent.class)));
    }

    public void validateSourceOnDuplicateRecord() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20);

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, sourceForTransfer);

        try {
            holder.ejb.validateSourceTubeForTransfer(ARBITRARY_MANIFEST_SESSION_ID, sourceForTransfer);
            Assert.fail();
        } catch (Exception ignored) {
            assertThat(ignored.getMessage(), is(equalTo(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE
                    .formatMessage(ManifestSession.SAMPLE_ID_KEY, sourceForTransfer))));
        }
    }

    public void validateSourceNotFoundInManifest() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20);

        try {
            holder.ejb.validateSourceTubeForTransfer(ARBITRARY_MANIFEST_SESSION_ID, sourceForTransfer);
            Assert.fail();
        } catch (Exception ignored) {
            assertThat(ignored.getMessage(),
                    is(equalTo(ManifestRecord.ErrorStatus.NOT_IN_MANIFEST.formatMessage(ManifestSession.SAMPLE_ID_KEY,
                            sourceForTransfer))));
        }
    }

    public void validateSourceOnMismatchedGenderRecord() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20);

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.ACCESSIONED,
                Metadata.Key.SAMPLE_ID, sourceForTransfer);

        ManifestRecord foundRecord =
                holder.ejb.validateSourceTubeForTransfer(ARBITRARY_MANIFEST_SESSION_ID, sourceForTransfer);

        assertThat(foundRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue(), is(equalTo(sourceForTransfer)));
        assertThat(foundRecord.getManifestEvents().size(), is(equalTo(1)));
    }

    public void validateSourceOnUnScannedRecord() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20);

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                ManifestRecord.ErrorStatus.MISSING_SAMPLE, ManifestRecord.Status.UPLOAD_ACCEPTED,
                Metadata.Key.SAMPLE_ID, sourceForTransfer);

        try {
            holder.ejb.validateSourceTubeForTransfer(ARBITRARY_MANIFEST_SESSION_ID, sourceForTransfer);
            Assert.fail();
        } catch (Exception ignored) {
            assertThat(ignored.getMessage(), is(equalTo(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE
                    .formatMessage(ManifestSession.SAMPLE_ID_KEY, sourceForTransfer))));
        }
    }

    /********************************************************************/
    /**  =======  Validate target sample tests ======================= **/
    /********************************************************************/

    public void validateValidTargetSample() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 1);

        MercurySample foundSample = holder.ejb.validateTargetSample(TEST_SAMPLE_KEY);

        assertThat(foundSample.getSampleKey(), is(equalTo(TEST_SAMPLE_KEY)));

    }

    public void validateTargetForBSPSample() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 1);

        try {
            holder.ejb.validateTargetSample(BSP_TEST_SAMPLE_KEY);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSession.SAMPLE_ID_KEY, BSP_TEST_SAMPLE_KEY)));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.SAMPLE_NOT_ELIGIBLE_FOR_CLINICAL_MESSAGE));
        }
    }

    public void validateTargetForNotFoundSample() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 1);

        try {
            holder.ejb.validateTargetSample(TEST_SAMPLE_KEY + "BAD");
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSession.SAMPLE_ID_KEY, TEST_SAMPLE_KEY + "BAD")));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.SAMPLE_NOT_FOUND_MESSAGE));
        }
    }

    /********************************************************************/
    /**  =======  Validate tube and Sample tests ===================== **/
    /********************************************************************/

    public void validateTargetTubeAndSampleOnValidRecord() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 1);

        LabVessel foundVessel = holder.ejb.validateTargetSampleAndVessel(TEST_SAMPLE_KEY, TEST_VESSEL_LABEL);

        assertThat(foundVessel.getLabel(), is(equalTo(TEST_VESSEL_LABEL)));

        assertThat(foundVessel.getSampleNames(), hasItem(TEST_SAMPLE_KEY));
    }

    public void validateTargetTubeAndSampleNotAssociated() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 1);

        try {
            holder.ejb.validateTargetSampleAndVessel(TEST_SAMPLE_KEY_UNASSOCIATED, TEST_VESSEL_LABEL);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSession.VESSEL_LABEL, TEST_VESSEL_LABEL)));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.UNASSOCIATED_TUBE_SAMPLE_MESSAGE));
        }
    }

    public void validateTargetTubeWithNonRegisteredSample() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 1);

        try {
            holder.ejb.validateTargetSampleAndVessel(TEST_SAMPLE_KEY + "BAD", TEST_VESSEL_LABEL);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSession.SAMPLE_ID_KEY, TEST_SAMPLE_KEY + "BAD")));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.SAMPLE_NOT_FOUND_MESSAGE));
        }
    }

    public void validateTargetTubeNotRegisteredWithRegisteredSample() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 1);

        try {
            holder.ejb.validateTargetSampleAndVessel(TEST_SAMPLE_KEY, TEST_VESSEL_LABEL + "BAD");
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSession.VESSEL_LABEL, TEST_VESSEL_LABEL + "BAD")));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.VESSEL_NOT_FOUND_MESSAGE));
        }
    }

    public void validateTargetTubeAlreadyAccessioned() throws Exception {

        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 1);

        try {
            holder.ejb.validateTargetSampleAndVessel(TEST_SAMPLE_ALREADY_TRANSFERRED,
                    TEST_VESSEL_LABEL_ALREADY_TRANSFERRED);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET.getBaseMessage()));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.VESSEL_USED_FOR_PREVIOUS_TRANSFER));
        }
    }

    /********************************************************************/
    /**  =======  Record Transfer Tests ============================== **/
    /********************************************************************/

    public void transferSourceValidRecord() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20);

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                null, ManifestRecord.Status.ACCESSIONED, Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE);

        ManifestRecord usedRecord = holder.manifestSession.findRecordByCollaboratorId(GOOD_TUBE_BARCODE);
        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.ACCESSIONED)));

        List<MercurySample> testSample = mercurySampleDao.findBySampleKey(TEST_SAMPLE_KEY);

        for (MercurySample mercurySample : testSample) {
            assertThat(mercurySample.getMetadata(), is(empty()));
        }

        holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID,GOOD_TUBE_BARCODE, TEST_SAMPLE_KEY, TEST_VESSEL_LABEL,
                testLabUser);

        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE)));

        for (MercurySample mercurySample : testSample) {
            assertThat(mercurySample.getMetadata(), is(not(empty())));
        }
    }

    public void transferSourceRecordNotFound() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20);

        List<MercurySample> testSample = mercurySampleDao.findBySampleKey(TEST_SAMPLE_KEY);

        for (MercurySample mercurySample : testSample) {
            assertThat(mercurySample.getMetadata(), is(empty()));
        }

        try {
            holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID,GOOD_TUBE_BARCODE, TEST_SAMPLE_KEY,
                    TEST_VESSEL_LABEL, testLabUser);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    containsString(ManifestRecord.ErrorStatus.NOT_IN_MANIFEST
                            .formatMessage(ManifestSession.SAMPLE_ID_KEY, GOOD_TUBE_BARCODE)));
            for (MercurySample mercurySample : testSample) {
                assertThat(mercurySample.getMetadata(), is(empty()));
            }
        }
    }

    public void transferSourceToSampleNotFound() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20);

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                null, ManifestRecord.Status.ACCESSIONED, Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE);

        ManifestRecord usedRecord = holder.manifestSession.findRecordByCollaboratorId(GOOD_TUBE_BARCODE);
        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.ACCESSIONED)));

        try {
            holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID, GOOD_TUBE_BARCODE, TEST_SAMPLE_KEY+"BAD",
                    TEST_VESSEL_LABEL, testLabUser);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSession.SAMPLE_ID_KEY, TEST_SAMPLE_KEY + "BAD")));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.SAMPLE_NOT_FOUND_MESSAGE));
        }
    }

    public void transferSourceToVesselNotFound() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20);

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                null, ManifestRecord.Status.ACCESSIONED, Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE);

        ManifestRecord usedRecord = holder.manifestSession.findRecordByCollaboratorId(GOOD_TUBE_BARCODE);
        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.ACCESSIONED)));

        List<MercurySample> testSample = mercurySampleDao.findBySampleKey(TEST_SAMPLE_KEY);

        for (MercurySample mercurySample : testSample) {
            assertThat(mercurySample.getMetadata(), is(empty()));
        }

        try {
            holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID, GOOD_TUBE_BARCODE, TEST_SAMPLE_KEY,
                    TEST_VESSEL_LABEL+"BAD", testLabUser);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSession.VESSEL_LABEL, TEST_VESSEL_LABEL + "BAD")));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.VESSEL_NOT_FOUND_MESSAGE));
        }
    }
    public void transferSourceToMisMatchedSampleAndVesselNotFound() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20);

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                null, ManifestRecord.Status.ACCESSIONED, Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE);

        ManifestRecord usedRecord = holder.manifestSession.findRecordByCollaboratorId(GOOD_TUBE_BARCODE);
        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.ACCESSIONED)));

        List<MercurySample> testSample = mercurySampleDao.findBySampleKey(TEST_SAMPLE_KEY_UNASSOCIATED);

        for (MercurySample mercurySample : testSample) {
            assertThat(mercurySample.getMetadata(), is(empty()));
        }

        try {
            holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID, GOOD_TUBE_BARCODE,
                    TEST_SAMPLE_KEY_UNASSOCIATED, TEST_VESSEL_LABEL, testLabUser);
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET
                    .formatMessage(ManifestSession.VESSEL_LABEL, TEST_VESSEL_LABEL)));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.UNASSOCIATED_TUBE_SAMPLE_MESSAGE));
        }
    }

    public void transferSourceQuarantinedRecord() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20);

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE);

        ManifestRecord usedRecord = holder.manifestSession.findRecordByCollaboratorId(GOOD_TUBE_BARCODE);
        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.UPLOADED)));

        List<MercurySample> testSample = mercurySampleDao.findBySampleKey(TEST_SAMPLE_KEY);

        for (MercurySample mercurySample : testSample) {
            assertThat(mercurySample.getMetadata(), is(empty()));
        }

        try {
            holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID, GOOD_TUBE_BARCODE, TEST_SAMPLE_KEY,
                    TEST_VESSEL_LABEL, testLabUser);
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    containsString(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE
                            .formatMessage(ManifestSession.SAMPLE_ID_KEY, GOOD_TUBE_BARCODE)));
            for (MercurySample mercurySample : testSample) {
                assertThat(mercurySample.getMetadata(), is(empty()));
            }
        }
    }

    public void transferSourceMismatchedGenderRecord() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20);

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.ACCESSIONED,
                Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE);

        ManifestRecord usedRecord = holder.manifestSession.findRecordByCollaboratorId(GOOD_TUBE_BARCODE);
        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.ACCESSIONED)));

        List<MercurySample> testSample = mercurySampleDao.findBySampleKey(TEST_SAMPLE_KEY);

        for (MercurySample mercurySample : testSample) {
            assertThat(mercurySample.getMetadata(), is(empty()));
        }

        holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID,GOOD_TUBE_BARCODE, TEST_SAMPLE_KEY, TEST_VESSEL_LABEL,
                testLabUser);

        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE)));

        for (MercurySample mercurySample : testSample) {
            assertThat(mercurySample.getMetadata(), is(not(empty())));
        }
    }

    public void transferSourceGoodRecordUsedTarget() throws Exception {
        ManifestSessionAndEjbHolder holder = buildHolderForSession(ManifestRecord.Status.ACCESSIONED, 20);

        ManifestTestFactory.addExtraRecord(holder.manifestSession,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, GOOD_TUBE_BARCODE);

        ManifestRecord usedRecord = holder.manifestSession.findRecordByCollaboratorId(GOOD_TUBE_BARCODE);
        assertThat(usedRecord.getStatus(), is(equalTo(ManifestRecord.Status.UPLOADED)));

        List<MercurySample> testSample = mercurySampleDao.findBySampleKey(TEST_SAMPLE_ALREADY_TRANSFERRED);

        for (MercurySample mercurySample : testSample) {
            assertThat(mercurySample.getMetadata(), is(empty()));
        }

        try {
            holder.ejb.transferSample(ARBITRARY_MANIFEST_SESSION_ID, GOOD_TUBE_BARCODE, TEST_SAMPLE_ALREADY_TRANSFERRED,
                    TEST_VESSEL_LABEL_ALREADY_TRANSFERRED, testLabUser);
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.INVALID_TARGET.getBaseMessage()));
            assertThat(e.getMessage(), containsString(ManifestSessionEjb.VESSEL_USED_FOR_PREVIOUS_TRANSFER));
            for (MercurySample mercurySample : testSample) {
                assertThat(mercurySample.getMetadata(), is(empty()));
            }
        }
    }
}