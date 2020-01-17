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

import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.common.MercuryStringUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.emptyCollectionOf;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION, singleThreaded = true)
public class SubmissionsServiceImplTest {

    private static int sequenceNumber = 1;

    public static final String BIO_PROJECT_ACCESSION_ID = "PRJNA325068";
    public static final String SAMPLE1_BIO_PROJECT_ACCESSION_ID = "PRJNA41443";
    public static final String SAMPLE1_ID = "TCGA-GU-A762-10A-01D-A339-08";
    private static final String SAMPLE2_ID = "ALCH-ABNA-TTP1-A-1-0-D-A488-36";
    private static final String SAMPLE3_ID = "ALCH-ABBH-NB1-A-1-0-D-A485-36";
    public static final String broadProject= "RP-1145";
    public static final String sample1broadProject= "RP-1657";
    public static final String bamVersion ="4";
    public static final String sample1bamVersion ="1";

    private SubmissionsService submissionsService;

    private BioProject bioProject;
    private BioProject sample1BioProject;
    private SubmissionContactBean contactBean;
    private SubmissionRepository submissionRepository;
    private SubmissionLibraryDescriptor submissionLibraryDescriptor;

    @BeforeMethod
    public void setUp() throws Exception {
        bioProject = new BioProject(BIO_PROJECT_ACCESSION_ID);
        sample1BioProject = new BioProject(SAMPLE1_BIO_PROJECT_ACCESSION_ID);

        contactBean =
                new SubmissionContactBean("Jeff", "A", "Gentry", "jgentry@broadinstitute.org", "617-555-9292", "homer");
        submissionsService = new SubmissionsServiceImpl(SubmissionConfig.produce(Deployment.DEV));
        submissionRepository = submissionsService.findRepositoryByKey("GDC_PROTECTED");
        submissionLibraryDescriptor = SubmissionLibraryDescriptor.WHOLE_EXOME;
    }

    public void testServerResponseBadRequest() {
        Response clientResponse = Mockito.mock(Response.class);
        Mockito.when(clientResponse.getStatus()).thenReturn(Response.Status.BAD_REQUEST.getStatusCode());
        String activityName = "just testing y'all";
        String exceptonMessage = "There was an error";
        String errorMessage = String.format("Error received while %s: %s (%d)", activityName, exceptonMessage,
                Response.Status.BAD_REQUEST.getStatusCode());
        Mockito.when(clientResponse.readEntity(String.class)).thenReturn(exceptonMessage);
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
        Response clientResponse = Mockito.mock(Response.class);
        Mockito.when(clientResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
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
        String[] expectedSampleNames = {SAMPLE1_ID, SAMPLE2_ID, SAMPLE3_ID};

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

    @Test
    public void testSubmit() throws IOException {
        SubmissionBean submissionBean1 = new SubmissionBean(getTestUUID(), "jgentry",
                sample1BioProject, new SubmissionBioSampleBean(SAMPLE1_ID, SubmissionBioSampleBean.GCP, contactBean),
                submissionRepository, submissionLibraryDescriptor, sample1broadProject, sample1bamVersion);

        SubmissionBean submissionBean2 = new SubmissionBean(getTestUUID(), "jgentry",
                bioProject, new SubmissionBioSampleBean(SAMPLE2_ID, SubmissionBioSampleBean.ON_PREM, contactBean),
                submissionRepository, submissionLibraryDescriptor, broadProject, bamVersion);
        SubmissionRequestBean submissionRequestBean =
                new SubmissionRequestBean(Collections.singletonList(submissionBean1));
        System.out.println(MercuryStringUtils.serializeJsonBean(submissionRequestBean));
        Collection<SubmissionStatusDetailBean>
                submissionResult = submissionsService.postSubmissions(submissionRequestBean);
        assertThat(submissionResult.size(), is(2));
        for (SubmissionStatusDetailBean submissionStatusDetailBean : submissionResult) {
            assertThat(submissionStatusDetailBean.getErrors(), emptyCollectionOf(String.class));
            assertThat(submissionStatusDetailBean.getStatus(),
                    is(SubmissionStatusDetailBean.Status.READY_FOR_SUBMISSION));
            assertThat(DateUtils.convertDateTimeToString(submissionStatusDetailBean.getLastStatusUpdate()),
                    notNullValue());
        }
    }

    public void testGetSubmissionStatusAlwaysReturnsSomething() {
        String[] testUUIDs = {"you wont find me", "yeah, and me neither"};
        Collection<SubmissionStatusDetailBean> submissionStatus = submissionsService.getSubmissionStatus(testUUIDs);
        assertThat(submissionStatus.size(), is(testUUIDs.length));
        for (SubmissionStatusDetailBean submissionStatusDetailBean : submissionStatus) {
            assertThat(submissionStatusDetailBean.getErrors(), emptyCollectionOf(String.class));
            assertThat(testUUIDs, hasItemInArray(submissionStatusDetailBean.getUuid()));
            assertThat(submissionStatusDetailBean.getStatusString(), nullValue());
            assertThat(submissionStatusDetailBean.getLastStatusUpdate(), nullValue());
            assertThat(submissionStatusDetailBean.submissionServiceHasRequest(), is(false));
        }
    }

    @Test
    public void testGetSubmissionStatusReadyForSubmission() {
        String testUUID = getTestUUID();

        SubmissionBean submissionBean = new SubmissionBean(testUUID, "jgentry", bioProject,
                new SubmissionBioSampleBean(SAMPLE1_ID, SubmissionBioSampleBean.ON_PREM, contactBean), submissionRepository,
                submissionLibraryDescriptor, broadProject, bamVersion);
        SubmissionRequestBean submissionRequestBean = new SubmissionRequestBean(Collections.singletonList(submissionBean));
        Collection<SubmissionStatusDetailBean> submissionResult =
                submissionsService.postSubmissions(submissionRequestBean);
        assertThat(submissionResult.size(), is(1));
        SubmissionStatusDetailBean submissionStatusDetailBean = submissionResult.iterator().next();
        assertThat(submissionStatusDetailBean.getErrors(), emptyCollectionOf(String.class));
        assertThat(submissionStatusDetailBean.getStatus(),
                is(SubmissionStatusDetailBean.Status.READY_FOR_SUBMISSION));
        assertThat(DateUtils.convertDateTimeToString(submissionStatusDetailBean.getLastStatusUpdate()),
                notNullValue());
        assertThat(submissionStatusDetailBean.submissionServiceHasRequest(), is(true));

        submissionResult = submissionsService.getSubmissionStatus(testUUID);
        assertThat(submissionResult.size(), is(1));
        submissionStatusDetailBean = submissionResult.iterator().next();
        assertThat(submissionStatusDetailBean.getErrors(), emptyCollectionOf(String.class));
        assertThat(submissionStatusDetailBean.getStatus(),
                is(SubmissionStatusDetailBean.Status.READY_FOR_SUBMISSION));
        assertThat(DateUtils.convertDateTimeToString(submissionStatusDetailBean.getLastStatusUpdate()),
                notNullValue());
        assertThat(submissionStatusDetailBean.submissionServiceHasRequest(), is(true));
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

    public void testFindLibraryDescriptorTypeByKey() throws Exception {
        SubmissionLibraryDescriptor libraryDescriptorTypeByKey =
                submissionsService.findLibraryDescriptorTypeByKey(SubmissionLibraryDescriptor.WHOLE_GENOME.getName());

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
