package org.broadinstitute.gpinformatics.infrastructure.bioproject;


import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.infrastructure.ExternalServiceRuntimeException;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionRepository;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionRequestBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionStatusDetailBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsService;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class BioProjectServiceDownTest {

    @Test
    public void testRefreshCacheErrorHandling() {
        BioProjectList bioProjectList = new BioProjectList(new SubmissionsServiceThatRefusesConnections());
        bioProjectList.refreshCache();
        Assert.assertTrue(bioProjectList.getBioProjects().isEmpty());
    }

    @Alternative
    @Dependent
    public static final class SubmissionsServiceThatRefusesConnections implements SubmissionsService {

        public SubmissionsServiceThatRefusesConnections(){}

        private static final ExternalServiceRuntimeException
            CONNECTION_REFUSED_EXCEPTION =  new ExternalServiceRuntimeException("Connection refused, dude.");

        @Override
        public Collection<SubmissionStatusDetailBean> getSubmissionStatus(String... uuids) {
            throw CONNECTION_REFUSED_EXCEPTION;
        }

        @Override
        public Collection<BioProject> getAllBioProjects() {
            throw CONNECTION_REFUSED_EXCEPTION;
        }

        @Override
        public Collection<SubmissionStatusDetailBean> postSubmissions(SubmissionRequestBean submissions) {
            throw CONNECTION_REFUSED_EXCEPTION;
        }

        @Override
        public Collection<String> getSubmissionSamples(BioProject bioProject) {
            throw CONNECTION_REFUSED_EXCEPTION;
        }

        @Override
        public List<SubmissionRepository> getSubmissionRepositories() {
            return null;
        }

        @Override
        public List<SubmissionLibraryDescriptor> getSubmissionLibraryDescriptors() {
            return null;
        }

        @Override
        public SubmissionRepository findRepositoryByKey(String key) {
            return null;
        }

        @Override
        public SubmissionLibraryDescriptor findLibraryDescriptorTypeByKey(String selectedSubmissionDescriptor) {
            return null;
        }

        @Override
        public List<SubmissionTracker> findOrphans(Map<String, SubmissionTracker> submissionTrackerMap) {
            return Collections.emptyList();
        }
    }

}
