/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2017 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.MockServerTest;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.model.JsonBody;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionsServiceMockTest extends MockServerTest {

    private static int sequenceNumber = 1;

    private static final String BIO_PROJECT_ACCESSION_ID = "PRJNA75723";
    private static final String SAMPLE1_ID = "4304714212_K";
    private static final String SAMPLE2_ID = "4377315018_E";
    private static final String broadProject= "RP-418";
    private static final String bamVersion ="v2";

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

//        String responseString="{\"submissionStatuses\":[{\"uuid\":\"MERCURY_TEST_SUB_1499713455783_0001\",\"status\":\"Failure\",\"errors\":[\"Invalid v2\",\"Unable to access bam path for PRJNA75723 4304714212_K RP-418 v2 located GCP.\"]},{\"uuid\":\"MERCURY_TEST_SUB_1499713455783_0002\",\"status\":\"Failure\",\"errors\":[\"Invalid v2\",\"Unable to access bam path for PRJNA75723 4377315018_E RP-418 v2 located OnPrem.\"]}]}";
//
//        HttpResponse httpResponse = new HttpResponse()
//            .withStatusCode(HttpStatusCode.BAD_REQUEST_400.code())
//            .withHeader("Content-Type", "application/json")
//            .withBody(new JsonBody(responseString));
//
//        submissionsService = serviceWithResponse(httpResponse);

        submissionRepository = new SubmissionRepository(SubmissionRepository.DEFAULT_REPOSITORY_NAME,
            SubmissionRepository.DEFAULT_REPOSITORY_DESCRIPTOR);
        submissionLibraryDescriptor = new SubmissionLibraryDescriptor(SubmissionLibraryDescriptor.WHOLE_GENOME_NAME,
            SubmissionLibraryDescriptor.WHOLE_GENOME_DESCRIPTION);
    }

    @Test
    public void testSubmissionCanHandleErrorsIn200Response() {
        bioProject = new BioProject(BIO_PROJECT_ACCESSION_ID);
        contactBean =
                new SubmissionContactBean("Jeff", "A", "Gentry", "jgentry@broadinstitute.org", "617-555-9292", "homer");

        String responseString="{\"submissionStatuses\":[{\"uuid\":\"MERCURY_TEST_SUB_1499713455783_0001\",\"status\":\"Failure\",\"errors\":[\"Invalid v2\",\"Unable to access bam path for PRJNA75723 4304714212_K RP-418 v2 located GCP.\"]},{\"uuid\":\"MERCURY_TEST_SUB_1499713455783_0002\",\"status\":\"Failure\",\"errors\":[\"Invalid v2\",\"Unable to access bam path for PRJNA75723 4377315018_E RP-418 v2 located OnPrem.\"]}]}";

        HttpResponse httpResponse = new HttpResponse()
            .withStatusCode(HttpStatusCode.OK_200.code())
            .withHeader("Content-Type", "application/json")
            .withBody(new JsonBody(responseString));

        submissionsService = serviceWithResponse(httpResponse);

        SubmissionBean submissionBean1 = new SubmissionBean(getTestUUID(), "jgentry",
                bioProject, new SubmissionBioSampleBean(SAMPLE1_ID, SubmissionBioSampleBean.GCP, contactBean),
                submissionRepository, submissionLibraryDescriptor, broadProject, bamVersion);
        SubmissionBean submissionBean2 = new SubmissionBean(getTestUUID(), "jgentry",
                bioProject, new SubmissionBioSampleBean(SAMPLE2_ID, SubmissionBioSampleBean.ON_PREM, contactBean),
                submissionRepository, submissionLibraryDescriptor, broadProject, bamVersion);
        SubmissionRequestBean submissionRequestBean =
                new SubmissionRequestBean(Arrays.asList(submissionBean1, submissionBean2));
        Collection<SubmissionStatusDetailBean>
                submissionResult = submissionsService.postSubmissions(submissionRequestBean);
        assertThat(submissionResult.size(), is(2));

        SubmissionStatusDetailBean statusDetailBean = submissionResult.iterator().next();
        assertThat(statusDetailBean.getStatus(), is(SubmissionStatusDetailBean.Status.FAILURE.getLabel()));
        assertThat(statusDetailBean.getErrors(), hasItem(startsWith("Unable to access")));
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

    @Test
    public void testGetSubmissionStatusReadyForSubmission() {
        String testUUID = getTestUUID();

        SubmissionBean submissionBean = new SubmissionBean(testUUID, "jgentry", bioProject,
                new SubmissionBioSampleBean(SAMPLE1_ID, SubmissionBioSampleBean.ON_PREM, contactBean), submissionRepository,
                submissionLibraryDescriptor, broadProject, bamVersion);
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
