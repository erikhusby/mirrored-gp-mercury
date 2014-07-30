package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.sun.jersey.api.client.ClientResponse;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProjects;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Impl
public class SubmissionsServiceImpl implements SubmissionsService {

    private final SubmissionConfig submissionsConfig;

    @Inject
    public SubmissionsServiceImpl(SubmissionConfig submissionsConfig) {
        this.submissionsConfig = submissionsConfig;
    }


    @Override
    public SubmissionStatusResultBean getSubmissionStatus(@Nonnull String... uuids) {

        Map<String, List<String>> submissionParameters = new HashMap<>();

        submissionParameters.put("uuid", Arrays.asList(uuids));

        ClientResponse response =
                JerseyUtils.getWebResource(submissionsConfig.getWSUrl(SubmissionConfig.SUBMISSIONS_STATUS_URI), MediaType.APPLICATION_JSON_TYPE,
                        submissionParameters).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        return response.getEntity(SubmissionStatusResultBean.class);
    }

    @Override
    public List<BioProject> getAllBioProjects() {
        BioProjects bioProjects;
        ClientResponse response =
                JerseyUtils.getWebResource(submissionsConfig.getWSUrl(SubmissionConfig.LIST_BIOPROJECTS_ACTION), MediaType.APPLICATION_JSON_TYPE)
                        .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        bioProjects = response.getEntity(BioProjects.class);
        return bioProjects.getBioprojects();
    }

    @Override
    public SubmissionStatusResultBean postSubmissions(SubmissionRequestBean submissions) {

        ClientResponse response =
                JerseyUtils.getWebResource(submissionsConfig.getWSUrl(SubmissionConfig.SUBMIT_ACTION),
                        MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).entity(submissions)
                           .post(ClientResponse.class);
        return response.getEntity(SubmissionStatusResultBean.class);
    }
}
