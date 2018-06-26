/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Alternative
@Dependent
public class SubmissionsWillAlwaysFailSubmissionsService extends SubmissionsServiceStub {

    public SubmissionsWillAlwaysFailSubmissionsService(){}

    String[] errors = {"Blame the Sirius Cybernetics Corporation", "I'd make a suggestion, but you wouldn't listen.",
            "Here I am, brain the size of a planet, and they ask me to take you to the bridge. Call that job satisfaction, 'cause I don't."};
    @Override
    public Collection<SubmissionStatusDetailBean> postSubmissions(SubmissionRequestBean submission) {
        List<SubmissionStatusDetailBean> results = new ArrayList<>();
        for (SubmissionBean submissionBean : submission.getSubmissions()) {
            SubmissionStatusDetailBean bean = new SubmissionStatusDetailBean(submissionBean.getUuid(),
                    SubmissionStatusDetailBean.Status.FAILURE, SubmissionRepository.DEFAULT_REPOSITORY_DESCRIPTOR,
                    SubmissionLibraryDescriptor.WHOLE_GENOME.getDescription(), new Date(), errors);
            results.add(bean);
        }
        return results;
    }

    @Override
    public Collection<String> getSubmissionSamples(BioProject bioProject) {
        return Collections.emptyList();
    }

    @Override
    public Collection<SubmissionStatusDetailBean> getSubmissionStatus(String... uuids) {
        List<SubmissionStatusDetailBean> results = new ArrayList<>();
        for (String uuid : uuids) {
            SubmissionStatusDetailBean statusDetailBean = new SubmissionStatusDetailBean(uuid,
                    SubmissionStatusDetailBean.Status.FAILURE, SubmissionRepository.DEFAULT_REPOSITORY_DESCRIPTOR,
                    SubmissionLibraryDescriptor.WHOLE_GENOME.getDescription(), new Date(), errors);
            results.add(statusDetailBean);
        }
        return results;
    }

    @Override
    public List<SubmissionLibraryDescriptor> getSubmissionLibraryDescriptors() {
        throw new InformaticsServiceException("error with getSubmissionLibraryDescriptors");
    }

    @Override
    public List<SubmissionRepository> getSubmissionRepositories() {
        throw new InformaticsServiceException("error with getSubmissionRepositories");
    }
}
