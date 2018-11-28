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

import org.broadinstitute.gpinformatics.infrastructure.MockServerTest;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.model.JsonBody;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
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

        String responseString="{\"submissionStatuses\":[{\"uuid\":\"MERCURY_TEST_SUB_1499713455783_0001\",\"status\":\"Failure\",\"errors\":[\"Invalid v2\",\"Unable to access bam path for PRJNA75723 4304714212_K RP-418 v2 located GCP.\"]},{\"uuid\":\"MERCURY_TEST_SUB_1499713455783_0002\",\"status\":\"Failure\",\"errors\":[\"Invalid v2\",\"Unable to access bam path for PRJNA75723 4377315018_E RP-418 v2 located OnPrem.\"]}]}";

        HttpResponse httpResponse = new HttpResponse()
            .withStatusCode(HttpStatusCode.OK_200.code())
            .withHeader("Content-Type", "application/json")
            .withBody(new JsonBody(responseString));

        submissionsService = serviceWithResponse(httpResponse);

        submissionRepository = new SubmissionRepository(SubmissionRepository.DEFAULT_REPOSITORY_NAME,
            SubmissionRepository.DEFAULT_REPOSITORY_DESCRIPTOR);
        submissionLibraryDescriptor = SubmissionLibraryDescriptor.WHOLE_GENOME;
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
        assertThat(statusDetailBean.getStatus(), is(SubmissionStatusDetailBean.Status.FAILURE));
        assertThat(statusDetailBean.getErrors(), hasItem(startsWith("Unable to access")));
    }

    private static synchronized String getTestUUID() {
        return String.format("MERCURY_TEST_SUB_%d_%04d", System.currentTimeMillis(), sequenceNumber++);
    }
}
