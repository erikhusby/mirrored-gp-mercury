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

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Alternative
@Dependent
public class SubmissionsWillAlwaysWorkSubmissionsService extends SubmissionsServiceStub {

    public SubmissionsWillAlwaysWorkSubmissionsService(){}

    @Override
    public Collection<SubmissionStatusDetailBean> postSubmissions(SubmissionRequestBean submission) {
        List<SubmissionStatusDetailBean> results = new ArrayList<>();
        for (SubmissionBean submissionBean : submission.getSubmissions()) {
            SubmissionStatusDetailBean bean = new SubmissionStatusDetailBean(submissionBean.getUuid(),
                    SubmissionStatusDetailBean.Status.SUBMITTED,
                    SubmissionRepository.DEFAULT_REPOSITORY_DESCRIPTOR,
                    SubmissionLibraryDescriptor.WHOLE_GENOME.getDescription(),
                    new Date());
            results.add(bean);
        }
        return results;
    }

    @Override
    public Collection<String> getSubmissionSamples(BioProject bioProject) {
        String[] samples = {"HG01583", "HG00096", "HG01500", "HG00419",
                "HG01051", "HG01879", "HG01112", "HG00759", "HG01595", "HG01565", "HG00268"};
        return Arrays.asList(samples);
    }

    @Override
    public Collection<SubmissionStatusDetailBean> getSubmissionStatus(String... uuids) {
        List<SubmissionStatusDetailBean> results = new ArrayList<>();
        for (String uuid : uuids) {
            SubmissionStatusDetailBean statusDetailBean = new SubmissionStatusDetailBean(uuid,
                    SubmissionStatusDetailBean.Status.FAILURE,
                    SubmissionRepository.DEFAULT_REPOSITORY_DESCRIPTOR,
                    SubmissionLibraryDescriptor.WHOLE_GENOME.getDescription(), new Date(), "error1", "error2");
            results.add(statusDetailBean);
        }
        return results;
    }
}
