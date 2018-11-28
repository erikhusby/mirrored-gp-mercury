package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * TODO scottmat fill in javadoc!!!
 */
public interface SubmissionsService extends Serializable {

    Collection<SubmissionStatusDetailBean> getSubmissionStatus(String... uuids);

    Collection<BioProject> getAllBioProjects();

    Collection<SubmissionStatusDetailBean> postSubmissions(SubmissionRequestBean submissions);

    Collection<String> getSubmissionSamples(BioProject bioProject);

    List<SubmissionRepository> getSubmissionRepositories();

    List<SubmissionLibraryDescriptor> getSubmissionLibraryDescriptors();

    SubmissionRepository findRepositoryByKey(String key);

    SubmissionLibraryDescriptor findLibraryDescriptorTypeByKey(String selectedSubmissionDescriptor);

    List<SubmissionTracker> findOrphans(Map<String, SubmissionTracker> submissionTrackerMap);
}
