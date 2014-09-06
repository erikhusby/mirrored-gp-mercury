package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;

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
    private ManifestSessionDao manifestSessionDao;
    private ResearchProjectDao researchProjectDao;

    public void researchProjectNotFound() {
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);
        try {
            ejb.uploadManifest(null, null, null, TEST_USER);
            Assert.fail();
        } catch (InformaticsServiceException ignored) {
        }
    }

    @BeforeMethod
    public void setUp() throws Exception {
        manifestSessionDao = Mockito.mock(ManifestSessionDao.class);
        researchProjectDao = Mockito.mock(ResearchProjectDao.class);
    }

    @Test(dataProvider = PATHS_TO_PREFIXES_PROVIDER)
    public void extractPrefixFromFilename(String path, String expectedPrefix) {
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);
        try {
            String actualPrefix = ejb.extractPrefixFromFilename(path);
            assertThat(actualPrefix, is(equalTo(expectedPrefix)));
        } catch (InformaticsServiceException ignored) {
        }
    }

    /**
     * Worker method for manifest upload testing.
     */
    private ManifestSession uploadManifest(String pathToManifestFile) throws FileNotFoundException {
        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject(
                TEST_RESEARCH_PROJECT_KEY);
        return uploadManifest(pathToManifestFile, researchProject);
    }

    private ManifestSession uploadManifest(ManifestSessionEjb manifestSessionEjb, String pathToManifestFile, ResearchProject researchProject)
            throws FileNotFoundException {
        String PATH_TO_SPREADSHEET = TestUtils.getTestData(pathToManifestFile);
        InputStream inputStream = new FileInputStream(PATH_TO_SPREADSHEET);
        return manifestSessionEjb.uploadManifest(researchProject.getBusinessKey(), inputStream, PATH_TO_SPREADSHEET,
                TEST_USER);
    }

    /**
     * Worker method for manifest upload testing.
     */
    private ManifestSession uploadManifest(String pathToManifestFile, ResearchProject researchProject)
            throws FileNotFoundException {
        ManifestSessionEjb ejb = buildEjbForUpload(researchProject);
        return uploadManifest(ejb, pathToManifestFile, researchProject);
    }

    private ManifestSessionEjb buildEjbForUpload(ResearchProject researchProject) {
        Mockito.when(researchProjectDao.findByBusinessKey(Mockito.anyString())).thenReturn(researchProject);

        return new ManifestSessionEjb(manifestSessionDao, researchProjectDao);
    }

    public void uploadGoodManifest() throws FileNotFoundException {
        ManifestSession manifestSession = uploadManifest(MANIFEST_FILE_GOOD);
        assertThat(manifestSession, is(notNullValue()));
        assertThat(manifestSession.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));
        assertThat(manifestSession.hasErrors(), is(false));
        assertThat(manifestSession.getManifestEvents(), is(empty()));
    }

    @DataProvider(name = BAD_MANIFEST_UPLOAD_PROVIDER)
    public Object [][] badManifestUploadProvider() {
        return new Object[][]{
                {"Not an Excel file", "manifest-upload/not-an-excel-file.txt"},
                {"Missing required field", "manifest-import/test-manifest-missing-required.xlsx"},
                {"Missing column", "manifest-upload/manifest-with-missing-column.xlsx"},
                {"Empty manifest", "manifest-upload/empty-manifest.xlsx"}
        };
    }

    @Test(dataProvider = BAD_MANIFEST_UPLOAD_PROVIDER)
    public void uploadBadManifest(String description, String pathToManifestFile) throws FileNotFoundException {
        try {
            uploadManifest(pathToManifestFile);
            Assert.fail(description);
        } catch (InformaticsServiceException ignored) {
        }
    }

    public void uploadManifestThatDuplicatesSampleIdInSameManifest() throws FileNotFoundException {
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

    public void uploadManifestThatDuplicatesSampleIdInAnotherManifest() throws FileNotFoundException {
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

    public void uploadManifestThatMismatchesGenderInSameManifest() throws FileNotFoundException {
        ManifestSession manifestSession = uploadManifest(MANIFEST_FILE_MISMATCHED_GENDERS_SAME_SESSION);
        assertThat(manifestSession, is(notNullValue()));
        assertThat(manifestSession.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));

        // The assert below is for size * 2 since all manifest records in question are in this manifest session.
        assertThat(manifestSession.getManifestEvents(), hasSize(PATIENT_IDS_FOR_SAME_MANIFEST_GENDER_MISMATCHES.size() * 2));
        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            assertThat(manifestRecord.isQuarantined(), is(false));
            Metadata patientIdMetadata = manifestRecord.getMetadataByKey(Metadata.Key.PATIENT_ID);
            if (PATIENT_IDS_FOR_SAME_MANIFEST_GENDER_MISMATCHES.contains(patientIdMetadata.getValue())) {
                assertThat(manifestRecord.getManifestEvents(), hasSize(1));
                assertThat(manifestRecord.getManifestEvents().get(0).getSeverity(), is(ManifestEvent.Severity.ERROR));
            }
        }
    }

    public void uploadManifestThatMismatchesGenderInAnotherManifest() throws FileNotFoundException {
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

    public void loadManifestSessionSuccess() {
        ManifestSessionDao manifestSessionDao = Mockito.mock(ManifestSessionDao.class);
        ResearchProjectDao researchProjectDao = Mockito.mock(ResearchProjectDao.class);
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
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);
        ManifestSession manifestSession = ejb.loadManifestSession(TEST_MANIFEST_SESSION_ID);
        assertThat(manifestSession, is(notNullValue()));
        assertThat(manifestSession.getManifestSessionId(), is(TEST_MANIFEST_SESSION_ID));
    }

    public void loadManifestSessionFailure() {
        ManifestSessionDao manifestSessionDao = Mockito.mock(ManifestSessionDao.class);
        ResearchProjectDao researchProjectDao = Mockito.mock(ResearchProjectDao.class);
        final long TEST_MANIFEST_SESSION_ID = ARBITRARY_MANIFEST_SESSION_ID;
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);
        ManifestSession manifestSession = ejb.loadManifestSession(TEST_MANIFEST_SESSION_ID);
        assertThat(manifestSession, is(nullValue()));
    }

    public void acceptUploadSessionNotFound() {
        ManifestSessionDao manifestSessionDao = Mockito.mock(ManifestSessionDao.class);
        ResearchProjectDao researchProjectDao = Mockito.mock(ResearchProjectDao.class);
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);
        long MANIFEST_SESSION_ID = ARBITRARY_MANIFEST_SESSION_ID;
        try {
            ejb.acceptManifestUpload(MANIFEST_SESSION_ID);
            Assert.fail();
        } catch (InformaticsServiceException ignored) {
            assertThat(ignored.getMessage(), containsString("Unrecognized Manifest Session ID"));
        }
    }

    /**
     * Utility class to encapsulate both objects returned by the #buildForAcceptSession method, as well as affording
     * the level of indirection required for programming a ManifestSessionDao mock to return a particular
     * ManifestSession before that ManifestSession has actually been created.
     *
     */
    private class ManifestSessionAndEjbHolder {
        ManifestSession manifestSession;
        ManifestSessionEjb ejb;
    }

    private ManifestSessionAndEjbHolder buildHolderForAcceptSession(String fileName) throws FileNotFoundException {
        final ManifestSessionAndEjbHolder holder = new ManifestSessionAndEjbHolder();

        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject(
                TEST_RESEARCH_PROJECT_KEY);

        Mockito.when(manifestSessionDao.find(Mockito.anyLong())).thenAnswer(new Answer<ManifestSession>() {
            @Override
            public ManifestSession answer(InvocationOnMock invocation) throws Throwable {
                return holder.manifestSession;
            }
        });
        Mockito.when(researchProjectDao.findByBusinessKey(Mockito.anyString())).thenReturn(researchProject);

        holder.ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);
        holder.manifestSession = uploadManifest(holder.ejb, fileName, researchProject);
        return holder;

    }

    public void acceptUploadSuccessful() throws FileNotFoundException {
        ManifestSessionAndEjbHolder holder = buildHolderForAcceptSession(MANIFEST_FILE_GOOD);
        holder.ejb.acceptManifestUpload(ARBITRARY_MANIFEST_SESSION_ID);
        for (ManifestRecord manifestRecord : holder.manifestSession.getRecords()) {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
        }
    }

    public void acceptSessionWithDuplicatesInThisSession() throws FileNotFoundException {
        ManifestSessionAndEjbHolder holder = buildHolderForAcceptSession(MANIFEST_FILE_DUPLICATES_SAME_SESSION);

        List<ManifestRecord> manifestRecordsMarkedAsDuplicates = new ArrayList<>();

        for (ManifestRecord manifestRecord : holder.manifestSession.getRecords()) {
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

        for (ManifestRecord manifestRecord : holder.manifestSession.getRecords()) {
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

    public void acceptSessionWithMismatchedGendersThisSession() throws FileNotFoundException {
        ManifestSessionAndEjbHolder holder = buildHolderForAcceptSession(MANIFEST_FILE_MISMATCHED_GENDERS_SAME_SESSION);

        List<ManifestRecord> manifestRecordsWithMismatchedGenders = new ArrayList<>();

        for (ManifestRecord manifestRecord : holder.manifestSession.getRecords()) {
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

        // Arbitrary ID, the DAO is programmed to return the same manifest session for any requested ID.
        holder.ejb.acceptManifestUpload(ARBITRARY_MANIFEST_SESSION_ID);

        for (ManifestRecord manifestRecord : holder.manifestSession.getRecords()) {
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
    private Iterator<Object []> pathsToPrefixesProvider() {
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
    public Object [][] goodManifestAccessionScanProvider() {
        return new Object[][]{
                // Good tube barcode should succeed.
                {GOOD_TUBE_BARCODE, true},
                // Bad tube barcode should fail.
                {BAD_TUBE_BARCODE, false}};
    }

    @Test(dataProvider = GOOD_MANIFEST_ACCESSION_SCAN_PROVIDER)
    public void accessionScanGoodManifest(String tubeBarcode, boolean successExpected) throws FileNotFoundException {
        ManifestSessionAndEjbHolder holder = buildHolderForAcceptSession(MANIFEST_FILE_GOOD);
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

    public void accessionScanManifestWithDuplicates() throws FileNotFoundException {
        ManifestSessionAndEjbHolder holder = buildHolderForAcceptSession(MANIFEST_FILE_DUPLICATES_SAME_SESSION);
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

    public void accessionScanGenderMismatches() throws FileNotFoundException {
        ManifestSessionAndEjbHolder holder = buildHolderForAcceptSession(MANIFEST_FILE_MISMATCHED_GENDERS_SAME_SESSION);
        ManifestSessionEjb ejb = holder.ejb;
        ejb.acceptManifestUpload(ARBITRARY_MANIFEST_SESSION_ID);
        // The correct state after manifest upload is checked in other tests.

        ManifestSession manifestSession = holder.manifestSession;
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

        ejb.accessionScan(ARBITRARY_MANIFEST_SESSION_ID, manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue());

        assertThat(manifestSession.getManifestEvents(), hasSize(EXPECTED_NUMBER_OF_EVENTS_ON_SESSION));
        assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.SCANNED));
    }

    public void accessionScanDoubleScan() throws FileNotFoundException {
        ManifestSessionAndEjbHolder holder = buildHolderForAcceptSession(MANIFEST_FILE_GOOD);
        ManifestSessionEjb ejb = holder.ejb;
        ejb.acceptManifestUpload(ARBITRARY_MANIFEST_SESSION_ID);
        // The correct state after manifest upload is checked in other tests.

        ejb.accessionScan(ARBITRARY_MANIFEST_SESSION_ID, GOOD_TUBE_BARCODE);
        // The results of this are checked in accessionScanGoodManifest

        try {
            // Should fail on a second scan.
            ejb.accessionScan(ARBITRARY_MANIFEST_SESSION_ID, GOOD_TUBE_BARCODE);
            Assert.fail();
        } catch (InformaticsServiceException e) {
            assertThat(e.getMessage(), containsString(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_SCAN.getBaseMessage()));
        }
    }

    public void closeGoodManifest() throws Exception {

        ManifestSession session = ManifestTestFactory.buildManifestSession(TEST_RESEARCH_PROJECT_KEY, "BuickCloseGood",
                new BSPUserList.QADudeUser("LU", 342L),20, ManifestRecord.Status.SCANNED);

        Mockito.when(manifestSessionDao.find(Mockito.anyLong())).thenReturn(session);
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);

        ejb.closeSession(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(session.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));
        assertThat(session.getManifestEvents(), is(empty()));
        for (ManifestRecord manifestRecord : session.getRecords()) {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED ));
        }
    }

    public void closeManifestWithDuplicate() throws Exception {

        ManifestSession session = ManifestTestFactory.buildManifestSession(TEST_RESEARCH_PROJECT_KEY, "BuickCloseGood",
                new BSPUserList.QADudeUser("LU", 342L), 19, ManifestRecord.Status.SCANNED);
        String dupeSampleId = GOOD_TUBE_BARCODE;
        addExtraRecord(session, dupeSampleId, ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID,
                ManifestRecord.Status.UPLOADED);

        Mockito.when(manifestSessionDao.find(Mockito.anyLong())).thenReturn(session);
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);

        ejb.closeSession(ARBITRARY_MANIFEST_SESSION_ID);
        assertThat(session.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));

        assertThat(session.getManifestEvents().size(), is(1));

        for (ManifestRecord manifestRecord : session.getRecords()) {
            if (manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue().equals(dupeSampleId)) {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOADED));
            } else {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
            }
        }
    }

    public void closeManifestWithUnScannedRecord() throws Exception {
        ManifestSession session = ManifestTestFactory.buildManifestSession(TEST_RESEARCH_PROJECT_KEY, "BuickCloseGood",
                new BSPUserList.QADudeUser("LU", 342L), 19, ManifestRecord.Status.SCANNED);
        String unscannedBarcode = GOOD_TUBE_BARCODE;
        addExtraRecord(session, unscannedBarcode, ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID,
                ManifestRecord.Status.UPLOAD_ACCEPTED);

        Mockito.when(manifestSessionDao.find(Mockito.anyLong())).thenReturn(session);
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);

        ejb.closeSession(ARBITRARY_MANIFEST_SESSION_ID);

        assertThat(session.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));

        assertThat(session.getManifestEvents().size(), is(1));

        for (ManifestRecord manifestRecord : session.getRecords()) {
            if(manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue().equals(unscannedBarcode)) {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
            } else {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
            }
        }
    }

    public void closeManifestWithDuplicateAndUnScannedRecords() throws Exception {

        ManifestSession session = ManifestTestFactory.buildManifestSession(TEST_RESEARCH_PROJECT_KEY, "BuickCloseGood",
                new BSPUserList.QADudeUser("LU", 342L), 19, ManifestRecord.Status.SCANNED);

        String unscannedBarcode = GOOD_TUBE_BARCODE;
        addExtraRecord(session, unscannedBarcode, ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID,
                ManifestRecord.Status.UPLOAD_ACCEPTED);


        String dupeSampleId = GOOD_TUBE_BARCODE+"dupe";
        addExtraRecord(session, dupeSampleId, ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID,
                ManifestRecord.Status.UPLOADED);


        Mockito.when(manifestSessionDao.find(Mockito.anyLong())).thenReturn(session);
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);

        ejb.closeSession(ARBITRARY_MANIFEST_SESSION_ID);


        assertThat(session.getStatus(), is(ManifestSession.SessionStatus.COMPLETED));

        assertThat(session.getManifestEvents().size(), is(2));
        for (ManifestRecord manifestRecord : session.getRecords()) {
            if(manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue().equals(unscannedBarcode)) {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
            } else if(manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue().equals(dupeSampleId)) {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOADED));
            } else {
                assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.ACCESSIONED));
            }
        }

    }

    private void addExtraRecord(ManifestSession session, String sampleId, ManifestRecord.ErrorStatus targetStatus,
                                ManifestRecord.Status status) {
        ManifestRecord dupeRecord = ManifestTestFactory.buildManifestRecord(20, sampleId);
        dupeRecord.setStatus(status);
        session.addRecord(dupeRecord);
        Optional<ManifestRecord.ErrorStatus> possibleStatus = Optional.of(targetStatus);
        if(possibleStatus.isPresent()) {
            session.addManifestEvent(new ManifestEvent(getSeverity(possibleStatus.get()),
                    possibleStatus.get().formatMessage(Metadata.Key.SAMPLE_ID.name(), sampleId), dupeRecord));
        }
    }

    private ManifestEvent.Severity getSeverity(ManifestRecord.ErrorStatus status) {
        EnumSet<ManifestRecord.ErrorStatus> quarantinedSet = EnumSet.of(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID,
                ManifestRecord.ErrorStatus.NOT_READY_FOR_TUBE_TRANSFER);
        return (quarantinedSet.contains(status))?ManifestEvent.Severity.QUARANTINED: ManifestEvent.Severity.ERROR;
    }
}