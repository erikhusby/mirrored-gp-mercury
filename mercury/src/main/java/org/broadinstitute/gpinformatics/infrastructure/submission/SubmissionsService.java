package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProjects;

/**
 * TODO scottmat fill in javadoc!!!
 */
public interface SubmissionsService {

    public SubmissionStatusResultBean getSubmissionStatus(String... uuids);

    public BioProjects getAllBioProjects();

    public SubmissionStatusResultBean postSubmissions(SubmissionRequestBean submissions);
}
