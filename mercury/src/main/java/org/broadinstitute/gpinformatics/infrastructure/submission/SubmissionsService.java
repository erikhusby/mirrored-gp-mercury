package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;

import java.io.Serializable;
import java.util.Collection;

/**
 * TODO scottmat fill in javadoc!!!
 */
public interface SubmissionsService extends Serializable {

    public Collection<SubmissionStatusDetailBean> getSubmissionStatus(String... uuids);

    public Collection<BioProject> getAllBioProjects();

    public Collection<SubmissionStatusDetailBean> postSubmissions(SubmissionRequestBean submissions);

    public Collection<String> getSubmissionSamples(BioProject bioProject);
}
