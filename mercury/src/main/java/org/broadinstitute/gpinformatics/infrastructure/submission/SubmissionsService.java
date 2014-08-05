package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;

import java.util.Collection;

/**
 * TODO scottmat fill in javadoc!!!
 */
public interface SubmissionsService {

    public Collection<SubmissionStatusDetailBean> getSubmissionStatus(String... uuids);

    public Collection<BioProject> getAllBioProjects();

    public Collection<SubmissionStatusDetailBean> postSubmissions(SubmissionRequestBean submissions);
}
