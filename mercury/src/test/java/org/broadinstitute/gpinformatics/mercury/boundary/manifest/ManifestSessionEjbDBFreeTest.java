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
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
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

    private static final String PATHS_TO_PREFIXES_PROVIDER = "pathsToPrefixesProvider";

    private static final String BAD_MANIFEST_UPLOAD_PROVIDER = "badManifestUploadProvider";

    private static final BSPUserList.QADudeUser TEST_USER = new BSPUserList.QADudeUser("BUICK USER", 42);

    private static final String TEST_RESEARCH_PROJECT_KEY = "RP-1";

    private static final int NUM_RECORDS_IN_GOOD_MANIFEST = 23;

    private static final String MANIFEST_FILE_DUPLICATES_SAME_SESSION = "manifest-upload/manifest-with-duplicates.xlsx";

    private static final int NUM_DUPLICATES_IN_MANIFEST_WITH_DUPLICATES_IN_SAME_SESSION = 7;

    private static final String MANIFEST_FILE_MISMATCHED_GENDERS_THIS_MANIFEST =
            "manifest-upload/gender-mismatches-within-session/good-manifest.xlsx";

    private static final ImmutableSet<String> PATIENT_IDS_FOR_SAME_MANIFEST_GENDER_MISMATCHES =
            ImmutableSet.of("004-002", "003-009", "005-012");

    public void researchProjectNotFound() {
        ManifestSessionDao manifestSessionDao = Mockito.mock(ManifestSessionDao.class);
        ResearchProjectDao researchProjectDao = Mockito.mock(ResearchProjectDao.class);
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);
        try {
            ejb.uploadManifest(null, null, null, TEST_USER);
            Assert.fail();
        } catch (InformaticsServiceException ignored) {
        }
    }

    @Test(dataProvider = PATHS_TO_PREFIXES_PROVIDER)
    public void extractPrefixFromFilename(String path, String expectedPrefix) {
        ManifestSessionDao manifestSessionDao = Mockito.mock(ManifestSessionDao.class);
        ResearchProjectDao researchProjectDao = Mockito.mock(ResearchProjectDao.class);
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
        ManifestSessionDao manifestSessionDao = Mockito.mock(ManifestSessionDao.class);
        ResearchProjectDao researchProjectDao = Mockito.mock(ResearchProjectDao.class);
        Mockito.when(researchProjectDao.findByBusinessKey(Mockito.anyString())).thenReturn(researchProject);

        return new ManifestSessionEjb(manifestSessionDao, researchProjectDao);
    }

    public void uploadGoodManifest() throws FileNotFoundException {
        ManifestSession manifestSession = uploadManifest("manifest-upload/good-manifest.xlsx");
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
        ManifestSession manifestSession = uploadManifest(MANIFEST_FILE_MISMATCHED_GENDERS_THIS_MANIFEST);
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
        final long TEST_MANIFEST_SESSION_ID = 3L;
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
        final long TEST_MANIFEST_SESSION_ID = 3L;
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);
        ManifestSession manifestSession = ejb.loadManifestSession(TEST_MANIFEST_SESSION_ID);
        assertThat(manifestSession, is(nullValue()));
    }

    public void acceptUploadSessionNotFound() {
        ManifestSessionDao manifestSessionDao = Mockito.mock(ManifestSessionDao.class);
        ResearchProjectDao researchProjectDao = Mockito.mock(ResearchProjectDao.class);
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);
        long MANIFEST_SESSION_ID = 3L;
        try {
            ejb.acceptManifestUpload(MANIFEST_SESSION_ID);
            Assert.fail();
        } catch (InformaticsServiceException ignored) {
            assertThat(ignored.getMessage(), containsString("Unrecognized Manifest Session ID"));
        }
    }

    public void acceptUploadSuccessful() {
        ManifestSessionDao manifestSessionDao = Mockito.mock(ManifestSessionDao.class);
        ResearchProjectDao researchProjectDao = Mockito.mock(ResearchProjectDao.class);
        ManifestSession manifestSession = ManifestTestFactory.buildManifestSession(
                "RP-1000", "ManifestSessionPrefix", TEST_USER, 10);

        Mockito.when(manifestSessionDao.find(Mockito.anyLong())).thenReturn(manifestSession);
        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);
        long MANIFEST_SESSION_ID = 3L;
        ejb.acceptManifestUpload(MANIFEST_SESSION_ID);
        for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
            assertThat(manifestRecord.getStatus(), is(ManifestRecord.Status.UPLOAD_ACCEPTED));
        }
    }

    private ManifestSessionEjb buildEjbForSessionAcceptance(final ManifestSession[] manifestSessionHolder,
                                                            ResearchProject researchProject) {

        ManifestSessionDao manifestSessionDao = Mockito.mock(ManifestSessionDao.class);
        Mockito.when(manifestSessionDao.find(Mockito.anyLong())).thenAnswer(new Answer<ManifestSession>() {
            @Override
            public ManifestSession answer(InvocationOnMock invocation) throws Throwable {
                return manifestSessionHolder[0];
            }
        });
        ResearchProjectDao researchProjectDao = Mockito.mock(ResearchProjectDao.class);
        Mockito.when(researchProjectDao.findByBusinessKey(Mockito.anyString())).thenReturn(researchProject);

        return new ManifestSessionEjb(manifestSessionDao, researchProjectDao);
    }

    public void acceptSessionWithDuplicatesInThisSession() throws FileNotFoundException {
        // This will hold a reference to a ManifestSession that hasn't been created at the time the ManifestSessionDAO
        // mock is being programmed.
        ManifestSession[] manifestSessionHolder = new ManifestSession[1];
        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject(
                TEST_RESEARCH_PROJECT_KEY);

        ManifestSessionEjb ejb = buildEjbForSessionAcceptance(manifestSessionHolder, researchProject);
        ManifestSession manifestSession = uploadManifest(ejb, MANIFEST_FILE_DUPLICATES_SAME_SESSION, researchProject);

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

        // Now that the manifest session has been created, put it in the holder so the DAO will be able to retrieve it.
        manifestSessionHolder[0] = manifestSession;
        // Arbitrary ID, the DAO is programmed to return the same manifest session for any requested ID.
        long MANIFEST_SESSION_ID = 3L;
        ejb.acceptManifestUpload(MANIFEST_SESSION_ID);

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

    public void acceptSessionWithMismatchedGendersThisSession() throws FileNotFoundException {
        // This will hold a reference to a ManifestSession that hasn't been created at the time the ManifestSessionDAO
        // mock is being programmed.
        ManifestSession[] manifestSessionHolder = new ManifestSession[1];
        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject(
                TEST_RESEARCH_PROJECT_KEY);

        ManifestSessionEjb ejb = buildEjbForSessionAcceptance(manifestSessionHolder, researchProject);
        ManifestSession manifestSession = uploadManifest(ejb, MANIFEST_FILE_MISMATCHED_GENDERS_THIS_MANIFEST, researchProject);

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

        // Now that the manifest session has been created, put it in the holder so the DAO will be able to retrieve it.
        manifestSessionHolder[0] = manifestSession;
        // Arbitrary ID, the DAO is programmed to return the same manifest session for any requested ID.
        long MANIFEST_SESSION_ID = 3L;
        ejb.acceptManifestUpload(MANIFEST_SESSION_ID);

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
}