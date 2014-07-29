package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;

@Stub
@Alternative
public class SubmissionsServiceStub implements SubmissionsService {
    @Override
    public SubmissionStatusResults getSubmissionStatus(String... uuids) {
        SubmissionStatusDetails detail1 = new SubmissionStatusDetails("d835cc7-cd63-4cc6-9621-868155618745","Submitted");
        SubmissionStatusDetails detail2 = new SubmissionStatusDetails("d835cc7-cd63-4cc6-9621-868155618745","Failure", "And error was returned from NCBI");

        SubmissionStatusResults results = new SubmissionStatusResults();
        results.setSubmissionStatuses(detail1, detail2);

        return results;
    }
}
