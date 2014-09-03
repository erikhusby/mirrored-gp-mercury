package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.manifest.ManifestSessionDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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

    public void test() {
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
        ManifestSession manifestSession = ejb.uploadManifest("RP-1", null, "/path/to/spreadsheet.xls", TEST_USER);
        assertThat(manifestSession, is(notNullValue()));
    }

    @DataProvider(name = PATHS_TO_PREFIXES_PROVIDER)
    private Iterator<Object []> pathsToPrefixesProvider() {
        final String[] paths = {
                // no path
                "",
                // Unix path
                "/some/path/to/",
                // Windows path
                "c:\\ugh\\windows\\"
        };
        // Old and new Excel formats
        final String[] suffixes = {
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