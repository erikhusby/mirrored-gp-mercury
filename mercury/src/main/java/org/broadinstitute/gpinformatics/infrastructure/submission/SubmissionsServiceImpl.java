package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.StringUtils;
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
    private static final long serialVersionUID = -1724342423871535677L;

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

        Map<String, List<String>> submissionParameters = new HashMap<>();

        submissionParameters.put("uuid", Arrays.asList(submissionIdentifiers));

        ClientResponse response =
                JerseyUtils.getWebResource(submissionsConfig.getWSUrl(SubmissionConfig.SUBMISSIONS_STATUS_URI), MediaType.APPLICATION_JSON_TYPE,
                        submissionParameters).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        SubmissionStatusResultBean submissionStatusResultBean = response.getEntity(SubmissionStatusResultBean.class);
        return submissionStatusResultBean.getSubmissionStatuses();
    }

    /**
     * Defines the Rest call to retrieve a list of all available BioProjects
     * @return
     */
    @Override
    public Collection<BioProject> getAllBioProjects() {
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
    public Collection<SubmissionStatusDetailBean> postSubmissions(SubmissionRequestBean submissions) {

        ClientResponse response =
                JerseyUtils.getWebResource(submissionsConfig.getWSUrl(SubmissionConfig.SUBMIT_ACTION),
                        MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).entity(submissions)
                           .post(ClientResponse.class);
        validateResponseStatus("posting submissions", response);

        return response.getEntity(SubmissionStatusResultBean.class).getSubmissionStatuses();
    }

    @Override
    public Collection<String> getSubmissionSamples(BioProject bioProject) {
        Map<String, List<String>> parameterMap = new HashMap<>();
        parameterMap.put(ACCESSION_PARAMETER, Arrays.asList(bioProject.getAccession()));
        ClientResponse response =
                JerseyUtils.getWebResource(submissionsConfig.getWSUrl(SubmissionConfig.SUBMISSION_SAMPLES_ACTION),
                        MediaType.APPLICATION_JSON_TYPE, parameterMap).get(ClientResponse.class);

        validateResponseStatus("receiving submission samples list", response);

        return response.getEntity(SubmissionSampleResultBean.class).getSubmittedSampleIds();
    }

    @Override
    public List<SubmissionRepository> getSubmissionRepositories() {
        ClientResponse response =
                JerseyUtils.getWebResource(submissionsConfig.getWSUrl(SubmissionConfig.ALL_SUBMISSION_SITES), MediaType.APPLICATION_JSON_TYPE)
                        .get(ClientResponse.class);
        validateResponseStatus("receiving Submission Repositories", response);

        return response.getEntity(SubmissionRepositories.class).getSubmissionRepositories();
    }

    @Override
    public List<SubmissionLibraryDescriptor> getSubmissionLibraryDescriptors() {
        ClientResponse response =
                       JerseyUtils.getWebResource(submissionsConfig.getWSUrl(SubmissionConfig.SUBMISSION_TYPES),
                               MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        validateResponseStatus("receiving Submission Library Descriptors", response);
        return response.getEntity(SubmissionLibraryDescriptors.class).getSubmissionLibraryDescriptors();
    }

    protected void validateResponseStatus(String activityName, ClientResponse response) {
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            String error = response.getEntity(String.class);
            String errorMessage = String.format("Error received while %s: %s", activityName, error);
            log.error(errorMessage);
            throw new InformaticsServiceException(errorMessage);
        }
    }

    @Override
    public SubmissionRepository findRepositoryByKey(String key) {
        for (SubmissionRepository submissionRepository : getSubmissionRepositories()) {
            if (submissionRepository.getName().equals(key)) {
                return submissionRepository;
            }
        }
        return null;
    }

    @Override
    public SubmissionRepository repositorySearch(String searchString) {
        if (StringUtils.isNotBlank(searchString)) {
            for (SubmissionRepository submissionRepository : getSubmissionRepositories()) {
                if (submissionRepository.getDescription().toLowerCase().contains(searchString.toLowerCase()) ||
                    submissionRepository.getName().toLowerCase().contains(searchString.toLowerCase())) {
                    return submissionRepository;
                }
            }
        }
        return null;
    }

    @Override
    public SubmissionLibraryDescriptor findLibraryDescriptorTypeByKey(String selectedSubmissionDescriptor) {
        if (StringUtils.isNotBlank(selectedSubmissionDescriptor)) {
            for (SubmissionLibraryDescriptor submissionLibraryDescriptor : getSubmissionLibraryDescriptors()) {
                if (submissionLibraryDescriptor.getName().equals(selectedSubmissionDescriptor)) {
                    return submissionLibraryDescriptor;
                }
            }
        }
        return null;
    }
}
