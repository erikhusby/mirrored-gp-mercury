package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestSessionEjbDBFreeTest {

    private static final String PATHS_TO_PREFIXES_PROVIDER = "pathsToPrefixesProvider";

    private static final String BAD_MANIFEST_UPLOAD_PROVIDER = "badManifestUploadProvider";

    private static final BSPUserList.QADudeUser TEST_USER = new BSPUserList.QADudeUser("BUICK USER", 42);

    private static final String TEST_RESEARCH_PROJECT_KEY = "RP-1";
    public static final int NUM_RECORDS_IN_GOOD_MANIFEST = 23;

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
        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject(TEST_RESEARCH_PROJECT_KEY);
        return uploadManifest(pathToManifestFile, researchProject);
    }

    /**
     * Worker method for manifest upload testing.
     */
    private ManifestSession uploadManifest(String pathToManifestFile, ResearchProject researchProject)
            throws FileNotFoundException {
        ManifestSessionDao manifestSessionDao = Mockito.mock(ManifestSessionDao.class);
        ResearchProjectDao researchProjectDao = Mockito.mock(ResearchProjectDao.class);
        Mockito.when(researchProjectDao.findByBusinessKey(Mockito.anyString())).thenReturn(researchProject);

        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);
        String PATH_TO_SPREADSHEET = TestUtils.getTestData(pathToManifestFile);
        InputStream inputStream = new FileInputStream(PATH_TO_SPREADSHEET);
        return ejb.uploadManifest(researchProject.getBusinessKey(), inputStream, PATH_TO_SPREADSHEET, TEST_USER);
    }

    public void uploadGoodManifest() throws FileNotFoundException {
        ManifestSession manifestSession = uploadManifest("manifest-upload/good-manifest.xlsx");
        assertThat(manifestSession, is(notNullValue()));
        assertThat(manifestSession.getRecords(), hasSize(NUM_RECORDS_IN_GOOD_MANIFEST));
        assertThat(manifestSession.hasErrors(), is(false));
        assertThat(manifestSession.getManifestEvents(), is(empty()));
    }

    public void uploadManifestWithDuplicates() throws FileNotFoundException {
        ManifestSession manifestSession = uploadManifest("manifest-upload/manifest-with-duplicates.xlsx");
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
        assertThat(quarantinedRecords, hasSize(7));
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