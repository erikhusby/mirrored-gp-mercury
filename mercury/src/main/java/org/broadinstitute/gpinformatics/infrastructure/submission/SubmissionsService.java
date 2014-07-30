package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;

import java.util.List;

/**
 * TODO scottmat fill in javadoc!!!
 */
public interface SubmissionsService {

    public SubmissionStatusResultBean getSubmissionStatus(String... uuids);

    public List<BioProject> getAllBioProjects();
}
