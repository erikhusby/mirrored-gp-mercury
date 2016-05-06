package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProjects;
import org.broadinstitute.gpinformatics.infrastructure.common.QueryStringSplitter;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class encapsulates all the Rest calls to the submissions service
 */
@Impl
public class SubmissionsServiceImpl implements SubmissionsService {

    private static final Log log = LogFactory.getLog(SubmissionsServiceImpl.class);
    public static final int EPSILON_9_MAX_URL_LENGTH = 2048;

    private final SubmissionConfig submissionsConfig;
    public static final String ACCESSION_PARAMETER = "accession";

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
    public Collection<SubmissionStatusDetailBean> getSubmissionStatus(@Nonnull String... submissionIdentifiers) {
        Collection<SubmissionStatusDetailBean> allResults = new ArrayList<>();

        String baseUrl = submissionsConfig.getWSUrl(SubmissionConfig.SUBMISSIONS_STATUS_URI);
        QueryStringSplitter splitter = new QueryStringSplitter(baseUrl.length(), EPSILON_9_MAX_URL_LENGTH);
        for (Map<String, List<String>> parameters : splitter.split("uuid", Arrays.asList(submissionIdentifiers))) {
            ClientResponse response = JerseyUtils.getWebResource(baseUrl, MediaType.APPLICATION_JSON_TYPE, parameters)
                    .accept(MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_PLAIN_TYPE).get(ClientResponse.class);
            handleErrorResponse(response, "Error while querying submission status");
            SubmissionStatusResultBean result = response.getEntity(SubmissionStatusResultBean.class);
            allResults.addAll(result.getSubmissionStatuses());
        }

        return allResults;
    }

    /**
     * Defines the Rest call to retrieve a list of all available BioProjects
     * @return
     */
    @Override
    public Collection<BioProject> getAllBioProjects() {
        ClientResponse response = JerseyUtils
                .getWebResource(submissionsConfig.getWSUrl(SubmissionConfig.LIST_BIOPROJECTS_ACTION),
                        MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
                .get(ClientResponse.class);

        handleErrorResponse(response, "Error while querying submission status");

        BioProjects bioProjects = response.getEntity(BioProjects.class);
        return bioProjects.getBioprojects();
    }

    /**
     * Defines the Rest call to post submissions for sequenced sample results
     * @param submissions   JAXB representation of all selected sample submission information
     * @return
     */
    @Override
    public Collection<SubmissionStatusDetailBean> postSubmissions(SubmissionRequestBean submissions) {

        ClientResponse response = JerseyUtils.getWebResource(submissionsConfig.getWSUrl(SubmissionConfig.SUBMIT_ACTION),
                MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
                .entity(submissions).post(ClientResponse.class);

        handleErrorResponse(response, "Error received while posting submissions");

        return response.getEntity(SubmissionStatusResultBean.class).getSubmissionStatuses();
    }

    @Override
    public Collection<String> getSubmissionSamples(BioProject bioProject) {
        Map<String, List<String>> parameterMap = new HashMap<>();
        parameterMap.put(ACCESSION_PARAMETER, Arrays.asList(bioProject.getAccession()));
        ClientResponse response = JerseyUtils
                .getWebResource(submissionsConfig.getWSUrl(SubmissionConfig.SUBMISSION_SAMPLES_ACTION),
                        MediaType.APPLICATION_JSON_TYPE, parameterMap)
                .accept(MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_PLAIN_TYPE).get(ClientResponse.class);

        handleErrorResponse(response, "Error received while getting bio project samples");

        return response.getEntity(SubmissionSampleResultBean.class).getSubmittedSampleIds();
    }

    private void handleErrorResponse(ClientResponse response, String contextMessage) {
        if(response.getStatus() != Response.Status.OK.getStatusCode()) {
            String message = contextMessage + ": " + response.getEntity(String.class);
            log.error(message);
            throw new InformaticsServiceException(message);
        }
    }
}
