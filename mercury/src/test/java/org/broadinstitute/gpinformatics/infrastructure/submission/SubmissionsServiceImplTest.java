/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.sun.jersey.api.client.ClientResponse;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class SubmissionsServiceImplTest {

    private static int sequenceNumber = 1;

    public static final String BIO_PROJECT_ACCESSION_ID = "PRJNA75723";
    public static final String SAMPLE1_ID = "4304714212_K";
    public static final String SAMPLE2_ID = "4377315018_E";

    private SubmissionsService submissionsService;

    private BioProject bioProject;
    private SubmissionContactBean contactBean;
    private SubmissionRepository submissionRepository;
    private SubmissionLibraryDescriptor submissionLibraryDescriptor;

    @BeforeMethod
    public void setUp() throws Exception {
        bioProject = new BioProject(BIO_PROJECT_ACCESSION_ID);
        contactBean =
                new SubmissionContactBean("Jeff", "A", "Gentry", "jgentry@broadinstitute.org", "617-555-9292", "homer");
        submissionsService = new SubmissionsServiceImpl(SubmissionConfig.produce(Deployment.DEV));
        submissionRepository = submissionsService.getSubmissionRepositories().iterator().next();
        submissionLibraryDescriptor =submissionsService.getSubmissionLibraryDescriptors().iterator().next();
    }

    public void testServerResponseBadRequest() {
        ClientResponse clientResponse = Mockito.mock(ClientResponse.class);
        Mockito.when(clientResponse.getStatus()).thenReturn(ClientResponse.Status.BAD_REQUEST.getStatusCode());
        String activityName = "just testing y'all";
        String exceptonMessage = "There was an error";
        String errorMessage = String.format("Error received while %s: %s", activityName, exceptonMessage);
        Mockito.when(clientResponse.getEntity(String.class)).thenReturn(exceptonMessage);
        SubmissionsServiceImpl submissionsServiceImpl = ((SubmissionsServiceImpl) submissionsService);
        try {
            submissionsServiceImpl.validateResponseStatus(activityName, clientResponse);
            Assert.fail("Should have thrown an exception but didn't");
        } catch (InformaticsServiceException e) {
            assertThat(e.getLocalizedMessage(), equalTo(errorMessage));
        } catch (Exception e) {
            Assert.fail("Wrong exception thrown: " + e.getClass().getName());

        }
    }

    public void testServerResponseOK() {
        ClientResponse clientResponse = Mockito.mock(ClientResponse.class);
        Mockito.when(clientResponse.getStatus()).thenReturn(ClientResponse.Status.OK.getStatusCode());
        SubmissionsServiceImpl submissionsServiceImpl = ((SubmissionsServiceImpl) submissionsService);
        try {
            submissionsServiceImpl.validateResponseStatus(null, clientResponse);
        } catch (Exception e) {
            Assert.fail("Wrong exception thrown: " + e.getClass().getName());
        }
    }

    public void testGetAllBioProjects() throws Exception {
        Collection<BioProject> allBioProjects = submissionsService.getAllBioProjects();
        assertThat(allBioProjects, is(not(Matchers.emptyCollectionOf(BioProject.class))));
    }

    public void testGetSubmissionSamples() throws Exception {
        String[] expectedSampleNames = {SAMPLE1_ID, SAMPLE2_ID, "4304714040_C"};

        Collection<String> submissionSamples = submissionsService.getSubmissionSamples(bioProject);
        int minimumExpectedSizeOfResult = 300;
        assertThat(submissionSamples.size(), greaterThan(minimumExpectedSizeOfResult));
        assertThat(submissionSamples, hasItems(expectedSampleNames));
    }

    public void testSubmissionRepositoriesHaveActiveStatus(){
        Collection<SubmissionRepository> submissionRepositories = submissionsService.getSubmissionRepositories();
        for (SubmissionRepository repository : submissionRepositories) {
            if (repository.getName().equals(SubmissionRepository.DEFAULT_REPOSITORY_NAME)) {
                assertThat(repository.isActive(), is(true));
            }
        }
    }

    public void testSubmit() {
        SubmissionBean submissionBean1 = new SubmissionBean(getTestUUID(), "jgentry",
                bioProject, new SubmissionBioSampleBean(SAMPLE1_ID, "/some/funky/file.bam", contactBean),
                submissionRepository, submissionLibraryDescriptor);
        SubmissionBean submissionBean2 = new SubmissionBean(getTestUUID(), "jgentry",
                bioProject, new SubmissionBioSampleBean(SAMPLE2_ID, "/some/funky/file2.bam", contactBean),
                submissionRepository, submissionLibraryDescriptor);
        SubmissionRequestBean submissionRequestBean =
                new SubmissionRequestBean(Arrays.asList(submissionBean1, submissionBean2));
        Collection<SubmissionStatusDetailBean>
                submissionResult = submissionsService.postSubmissions(submissionRequestBean);
        assertThat(submissionResult.size(), is(2));
        for (SubmissionStatusDetailBean submissionStatusDetailBean : submissionResult) {
            assertThat(submissionStatusDetailBean.getStatus(),
                    is(SubmissionStatusDetailBean.Status.READY_FOR_SUBMISSION.getLabel()));
            assertThat(submissionStatusDetailBean.getErrors(), emptyCollectionOf(String.class));
            assertThat(DateUtils.convertDateTimeToString(submissionStatusDetailBean.getLastStatusUpdate()),
                    notNullValue());
        }
    }

    public void testGetSubmissionStatusAlwaysReturnsSomething() {
        String[] testUUIDs = {"you wont find me", "yeah, and me neither"};
        Collection<SubmissionStatusDetailBean> submissionStatus = submissionsService.getSubmissionStatus(testUUIDs);
        assertThat(submissionStatus.size(), is(testUUIDs.length));
        for (SubmissionStatusDetailBean submissionStatusDetailBean : submissionStatus) {
            assertThat(testUUIDs, hasItemInArray(submissionStatusDetailBean.getUuid()));
            assertThat(submissionStatusDetailBean.getStatus(), nullValue());
            assertThat(submissionStatusDetailBean.getErrors(), emptyCollectionOf(String.class));
            assertThat(submissionStatusDetailBean.getLastStatusUpdate(), nullValue());
        }
    }

    public void testGetSubmissionStatusReadyForSubmission() {
        String testUUID = getTestUUID();

        SubmissionBean submissionBean = new SubmissionBean(testUUID, "jgentry", bioProject,
                new SubmissionBioSampleBean(SAMPLE1_ID, "/some/funky/file.bam", contactBean), submissionRepository,
                submissionLibraryDescriptor);
        SubmissionRequestBean submissionRequestBean = new SubmissionRequestBean(Arrays.asList(submissionBean));
        Collection<SubmissionStatusDetailBean> submissionResult =
                submissionsService.postSubmissions(submissionRequestBean);
        assertThat(submissionResult.size(), is(1));
        SubmissionStatusDetailBean submissionStatusDetailBean = submissionResult.iterator().next();
        assertThat(submissionStatusDetailBean.getStatus(),
                is(SubmissionStatusDetailBean.Status.READY_FOR_SUBMISSION.getLabel()));
        assertThat(submissionStatusDetailBean.getErrors(), emptyCollectionOf(String.class));
        assertThat(DateUtils.convertDateTimeToString(submissionStatusDetailBean.getLastStatusUpdate()),
                notNullValue());

        submissionResult = submissionsService.getSubmissionStatus(testUUID);
        assertThat(submissionResult.size(), is(1));
        submissionStatusDetailBean = submissionResult.iterator().next();
        assertThat(submissionStatusDetailBean.getStatus(),
                is(SubmissionStatusDetailBean.Status.READY_FOR_SUBMISSION.getLabel()));
        assertThat(submissionStatusDetailBean.getErrors(), emptyCollectionOf(String.class));
        assertThat(DateUtils.convertDateTimeToString(submissionStatusDetailBean.getLastStatusUpdate()),
                notNullValue());
    }

    private static synchronized String getTestUUID() {
        return String.format("MERCURY_TEST_SUB_%d_%04d", System.currentTimeMillis(), sequenceNumber++);
    }

    public void testFindRepositoryByKeyOK() throws Exception {
        SubmissionRepository repositoryByKey =
                submissionsService.findRepositoryByKey(SubmissionRepository.DEFAULT_REPOSITORY_NAME);

        assertThat(repositoryByKey.getName(), equalTo(SubmissionRepository.DEFAULT_REPOSITORY_NAME));
    }

    public void testFindRepositoryByKeyNullInput() throws Exception {
        SubmissionRepository repositoryByKey =
                submissionsService.findRepositoryByKey(null);

        assertThat(repositoryByKey, nullValue());
    }

    public void testFindRepositoryByKeyNoSuchKey() throws Exception {
        SubmissionRepository repositoryByKey =
                submissionsService.findRepositoryByKey("I'm making this up.");
        assertThat(repositoryByKey, nullValue());

    }

    public void testRepositorySearchPartialMatch() throws Exception {
        String repositoryName = SubmissionRepository.DEFAULT_REPOSITORY_NAME;
        String searchString = repositoryName.substring(repositoryName.length() - 3, repositoryName.length());
        assertThat(searchString, not(equalTo(repositoryName)));

        SubmissionRepository submissionRepository = submissionsService.repositorySearch(searchString);
        assertThat(submissionRepository.getName(), equalTo(repositoryName));
    }

    public void testRepositorySearchPartialWrongCase() throws Exception {
        SubmissionRepository submissionRepository =
                submissionsService.repositorySearch(SubmissionRepository.DEFAULT_REPOSITORY_NAME.toLowerCase());
        assertThat(submissionRepository.getName(), equalTo(SubmissionRepository.DEFAULT_REPOSITORY_NAME));
    }

    public void testRepositorySearchNoResultReturnsNull() throws Exception {
        SubmissionRepository submissionRepository = submissionsService.repositorySearch("nunsuch");
        assertThat(submissionRepository, nullValue());
    }

    public void testRepositorySearchNullSearchReturnsNull() throws Exception {
        SubmissionRepository submissionRepository = submissionsService.repositorySearch(null);
        assertThat(submissionRepository, nullValue());
    }

    public void testFindLibraryDescriptorTypeByKey() throws Exception {
        SubmissionLibraryDescriptor libraryDescriptorTypeByKey =
                submissionsService.findLibraryDescriptorTypeByKey(SubmissionLibraryDescriptor.WHOLE_GENOME_NAME);

        assertThat(libraryDescriptorTypeByKey, equalTo(ProductFamily.defaultLibraryDescriptor()));
    }

    public void testFindLibraryDescriptorTypeByKeyNoResultReturnsNull() throws Exception {
        SubmissionLibraryDescriptor libraryDescriptorTypeByKey =
                submissionsService.findLibraryDescriptorTypeByKey("nunsuch");

        assertThat(libraryDescriptorTypeByKey, nullValue());
    }
    public void testFindLibraryDescriptorTypeByKeyNullInputReturnsNull() throws Exception {
        SubmissionLibraryDescriptor libraryDescriptorTypeByKey =
                submissionsService.findLibraryDescriptorTypeByKey(null);

        assertThat(libraryDescriptorTypeByKey, nullValue());
    }

}
