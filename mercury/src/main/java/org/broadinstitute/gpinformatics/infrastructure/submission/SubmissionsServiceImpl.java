package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProjects;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class encapsulates all the Rest calls to the submissions service
 */
@Impl
public class SubmissionsServiceImpl implements SubmissionsService {

    private static final Log log = LogFactory.getLog(SubmissionsServiceImpl.class);

    private final SubmissionConfig submissionsConfig;

    @Inject
    public SubmissionsServiceImpl(SubmissionConfig submissionsConfig) {
        this.submissionsConfig = submissionsConfig;
    }

    /**
     * Defines the Rest call to get the status of submissions that have previously been placed
     * @param submissionIdentifiers     Identifiers assigned to posted submissions in order to easily retrieve their
     *                                  status
     * @return
     */
    @Override
    public SubmissionStatusResultBean getSubmissionStatus(@Nonnull String... submissionIdentifiers) {

        Map<String, List<String>> submissionParameters = new HashMap<>();

        submissionParameters.put("uuid", Arrays.asList(submissionIdentifiers));

        ClientResponse response =
                JerseyUtils.getWebResource(submissionsConfig.getWSUrl(SubmissionConfig.SUBMISSIONS_STATUS_URI), MediaType.APPLICATION_JSON_TYPE,
                        submissionParameters).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        return response.getEntity(SubmissionStatusResultBean.class);
    }

    /**
     * Defines the Rest call to retrieve a list of all available BioProjects
     * @return
     */
    @Override
    public List<BioProject> getAllBioProjects() {
        BioProjects bioProjects;
        ClientResponse response =
                JerseyUtils.getWebResource(submissionsConfig.getWSUrl(SubmissionConfig.LIST_BIOPROJECTS_ACTION), MediaType.APPLICATION_JSON_TYPE)
                        .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        bioProjects = response.getEntity(BioProjects.class);
        return bioProjects.getBioprojects();
    }

    /**
     * Defines the Rest call to post submissions for sequenced sample results
     * @param submissions   JAXB representation of all selected sample submission information
     * @return
     */
    @Override
    public SubmissionStatusResultBean postSubmissions(SubmissionRequestBean submissions) {

        ClientResponse response =
                JerseyUtils.getWebResource(submissionsConfig.getWSUrl(SubmissionConfig.SUBMIT_ACTION),
                        MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).entity(submissions)
                           .post(ClientResponse.class);

        if(response.getStatus() != Response.Status.OK.getStatusCode()) {
            log.error("Error received while posting submissions: " +response.getEntity(String.class));
            throw new InformaticsServiceException(response.getEntity(String.class));
        }

        return response.getEntity(SubmissionStatusResultBean.class);
    }
}
