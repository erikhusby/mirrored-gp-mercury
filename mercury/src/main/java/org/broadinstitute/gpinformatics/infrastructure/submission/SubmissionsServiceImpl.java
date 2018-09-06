package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProjects;
import org.broadinstitute.gpinformatics.infrastructure.common.QueryStringSplitter;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class encapsulates all the Rest calls to the submissions service
 */
@Dependent
@Default
public class SubmissionsServiceImpl implements SubmissionsService {

    private static final Log log = LogFactory.getLog(SubmissionsServiceImpl.class);
    private static final long serialVersionUID = -1724342423871535677L;
    public static final int EPSILON_9_MAX_URL_LENGTH = 2048;
    private static final Map<String, List<String>> NO_PARAMETERS = Collections.<String, List<String>>emptyMap();

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
            ClientResponse response = clientResponseGet(SubmissionConfig.SUBMISSIONS_STATUS_URI, parameters);
            validateResponseStatus("querying submission status",response);
            SubmissionStatusResultBean result = response.getEntity(SubmissionStatusResultBean.class);
            allResults.addAll(result.getSubmissionStatuses());
        }

        Map<String, String> libraryDescriptionMap=new HashMap<>();
        for (SubmissionLibraryDescriptor library : getSubmissionLibraryDescriptors()) {
            libraryDescriptionMap.put(library.getName(), library.getDescription());
        }
        Map<String, String> siteDescriptionMap = new HashMap<>();
        for (SubmissionRepository submissionRepository : getSubmissionRepositories()) {
            siteDescriptionMap.put(submissionRepository.getName(), submissionRepository.getDescription());
        }

        Map<String, BioProject> bioProjectMap = new HashMap<>();
        for (BioProject bioProject : getAllBioProjects()) {
            bioProjectMap.put(bioProject.getAccession(), bioProject);
        }
        for (SubmissionStatusDetailBean result : allResults) {
            if (result.getBioproject() != null) {
                String accession = result.getBioproject().getAccession();
                BioProject fullBioProject = bioProjectMap.get(accession);
                if (fullBioProject != null) {
                    result.setBioproject(fullBioProject);
                }
                String site = result.getSite();
                if (StringUtils.isNotBlank(site)) {
                    String siteDescription = siteDescriptionMap.get(site);
                    if (StringUtils.isNotBlank(siteDescription)) {
                        result.setSite(siteDescription);
                    }
                }
                String library = result.getSubmissionDatatype();
                if (StringUtils.isNotBlank(library)) {
                    String libraryDescription = libraryDescriptionMap.get(library);
                    if (StringUtils.isNotBlank(libraryDescription)) {
                        result.setSubmissionDatatype(libraryDescription);
                    }
                }
            }
        }
        return allResults;
    }

    /**
     * Defines the Rest call to retrieve a list of all available BioProjects
     * @return
     */
    @Override
    public Collection<BioProject> getAllBioProjects() {
        ClientResponse response =
                clientResponseGet(SubmissionConfig.LIST_BIOPROJECTS_ACTION, NO_PARAMETERS);
        BioProjects bioProjects = response.getEntity(BioProjects.class);
        validateResponseStatus("querying submission status", response);
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
        List<SubmissionStatusDetailBean> submissionStatuses =
            response.getEntity(SubmissionStatusResultBean.class).getSubmissionStatuses();
        return submissionStatuses;
    }

    @Override
    public Collection<String> getSubmissionSamples(BioProject bioProject) {
        Map<String, List<String>> parameterMap = new HashMap<>();
        parameterMap.put(ACCESSION_PARAMETER, Arrays.asList(bioProject.getAccession()));

        ClientResponse response =
                clientResponseGet(SubmissionConfig.SUBMISSION_SAMPLES_ACTION, parameterMap);
        validateResponseStatus("receiving submission samples list", response);
        return response.getEntity(SubmissionSampleResultBean.class).getSubmittedSampleIds();
    }

    @Override
    public List<SubmissionRepository> getSubmissionRepositories() {
        ClientResponse response = clientResponseGet(SubmissionConfig.ALL_SUBMISSION_SITES, NO_PARAMETERS);
        validateResponseStatus("receiving Submission Repositories", response);
        return response.getEntity(SubmissionRepositories.class).getSubmissionRepositories();
    }

    @Override
    public List<SubmissionLibraryDescriptor> getSubmissionLibraryDescriptors() {
        ClientResponse response = clientResponseGet(SubmissionConfig.SUBMISSION_TYPES, NO_PARAMETERS);
        validateResponseStatus("receiving Submission Library Descriptors",response);
        return response.getEntity(SubmissionLibraryDescriptors.class).getSubmissionLibraryDescriptors();
    }

    private ClientResponse clientResponseGet(String servicePath, Map<String, List<String>> parameters) {
        try {
            return JerseyUtils.getWebResource(submissionsConfig.getWSUrl(servicePath),
                    MediaType.APPLICATION_JSON_TYPE, parameters).get(ClientResponse.class);
        } catch (Exception e) {
            throw new InformaticsServiceException(
                "Error communicating with Submissions server. " + CoreActionBean.ERROR_CONTACT_SUPPORT, e);
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
    public SubmissionLibraryDescriptor findLibraryDescriptorTypeByKey(String name) {
        if (StringUtils.isNotBlank(name)) {
            for (SubmissionLibraryDescriptor submissionLibraryDescriptor : getSubmissionLibraryDescriptors()) {
                if (submissionLibraryDescriptor.getName().equals(name)) {
                    return submissionLibraryDescriptor;
                }
            }
        }
        return null;
    }

    protected void validateResponseStatus(String activityName, ClientResponse response) {
        if(response.getStatus() != Response.Status.OK.getStatusCode()) {
            String error = response.getEntity(String.class);
            String errorMessage =
                    String.format("Error received while %s: %s (%d)", activityName, error, response.getStatus());
            log.error(errorMessage);
            throw new InformaticsServiceException(errorMessage);
        }
    }

    /**
     * Queries the SubmissionService to determine if a UUID has been submitted.
     */
    @Override
    public List<SubmissionTracker> findOrphans(Collection<SubmissionTracker> submissionTrackers) {
        List<SubmissionTracker> orphans = new ArrayList<>();

        // Since the SubmissionService does not return the submitted sample name in it's response it is necessary to
        // look up the submission statuses individually to find if it exists on the Epsilon 9.
        for (SubmissionTracker submissionTracker : submissionTrackers) {
            Collection<SubmissionStatusDetailBean> submissionStatus =
                getSubmissionStatus(submissionTracker.createSubmissionIdentifier());
            for (SubmissionStatusDetailBean status : submissionStatus) {
                if (!status.submissionServiceHasRequest()) {
                    orphans.add(submissionTracker);
                }
            }
        }
        return orphans;
    }

}
