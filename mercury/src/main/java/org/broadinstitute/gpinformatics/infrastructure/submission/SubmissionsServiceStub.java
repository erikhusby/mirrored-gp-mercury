package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProjects;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;
import java.util.Random;

@Stub
@Alternative
public class SubmissionsServiceStub implements SubmissionsService {
    public static final String TEST_PROJECT_NAME = "Primary submission";

    private static final int HTTP_STATUS_CODE_OK = 200;

    private String githubBaseUri;


    @Override
    public SubmissionStatusResultBean getSubmissionStatus(String... uuids) {
        SubmissionStatusDetailBean detail1 =
                new SubmissionStatusDetailBean("d835cc7-cd63-4cc6-9621-868155618745", "Submitted");
        SubmissionStatusDetailBean detail2 =
                new SubmissionStatusDetailBean("d835cc7-cd63-4cc6-9621-868155618745", "Failure",
                        "And error was returned from NCBI");

        SubmissionStatusResultBean results = new SubmissionStatusResultBean();
        results.setSubmissionStatuses(detail1, detail2);

        return results;
    }

    @Override
    public BioProjects getAllBioProjects() {
        return new BioProjects(
                new BioProject(generateTestName("PRJ"), generateTestName("phs"), TEST_PROJECT_NAME),
                new BioProject(generateTestName("PRJ"), generateTestName("phs"), TEST_PROJECT_NAME),
                new BioProject(generateTestName("PRJ"), generateTestName("phs"), TEST_PROJECT_NAME),
                new BioProject(generateTestName("PRJ"), generateTestName("phs"), TEST_PROJECT_NAME),
                new BioProject(generateTestName("PRJ"), generateTestName("phs"), TEST_PROJECT_NAME),
                new BioProject(generateTestName("PRJ"), generateTestName("phs"), TEST_PROJECT_NAME)
        );
    }

    @Override
    public SubmissionStatusResultBean postSubmissions(SubmissionRequestBean submissions) {
        SubmissionStatusDetailBean detail1 =
                new SubmissionStatusDetailBean("d835cc7-cd63-4cc6-9621-868155618745", "Submitted");
        SubmissionStatusDetailBean detail2 =
                new SubmissionStatusDetailBean("d835cc7-cd63-4cc6-9621-868155618745", "Failure",
                        "And error was returned from NCBI");

        SubmissionStatusResultBean results = new SubmissionStatusResultBean();
        results.setSubmissionStatuses(detail1, detail2);

        return results;
    }

    private static String generateTestName(String prefix) {
        return String.format("%s%d", prefix, new Random().nextInt(9999));
    }


}
