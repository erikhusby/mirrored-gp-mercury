package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;

@Stub
@Alternative
public class SubmissionsServiceStub implements SubmissionsService {
    @Override
    public SubmissionStatusResultBean getSubmissionStatus(String... uuids) {
        SubmissionStatusDetailBean detail1 = new SubmissionStatusDetailBean("d835cc7-cd63-4cc6-9621-868155618745","Submitted");
        SubmissionStatusDetailBean detail2 = new SubmissionStatusDetailBean("d835cc7-cd63-4cc6-9621-868155618745","Failure", "And error was returned from NCBI");

        SubmissionStatusResultBean results = new SubmissionStatusResultBean();
        results.setSubmissionStatuses(detail1, detail2);

        return results;
    }
}
