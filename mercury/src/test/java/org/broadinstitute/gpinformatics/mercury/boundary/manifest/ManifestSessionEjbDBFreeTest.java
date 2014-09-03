package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestSessionEjbDBFreeTest {

    private static final String PATHS_TO_PREFIXES_PROVIDER = "pathsToPrefixesProvider";

    private static final BSPUserList.QADudeUser TEST_USER = new BSPUserList.QADudeUser("BUICK USER", 42);

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

    public void uploadManifest() throws FileNotFoundException {
        ManifestSessionDao manifestSessionDao = Mockito.mock(ManifestSessionDao.class);
        ResearchProjectDao researchProjectDao = Mockito.mock(ResearchProjectDao.class);
        Mockito.when(researchProjectDao.findByBusinessKey(Mockito.anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String key = (String) invocation.getArguments()[0];
                return ResearchProjectTestFactory.createTestResearchProject(key);
            }
        });

        ManifestSessionEjb ejb = new ManifestSessionEjb(manifestSessionDao, researchProjectDao);
        String PATH_TO_SPREADSHEET = TestUtils.getTestData("manifest-import/test-manifest.xlsx");
        InputStream inputStream = new FileInputStream(PATH_TO_SPREADSHEET);
        ManifestSession manifestSession = ejb.uploadManifest("RP-1", inputStream, PATH_TO_SPREADSHEET, TEST_USER);
        assertThat(manifestSession, is(notNullValue()));
        assertThat(manifestSession.getRecords(), hasSize(23));
        assertThat(manifestSession.hasErrors(), is(false));
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

    public void acceptUpload() {
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