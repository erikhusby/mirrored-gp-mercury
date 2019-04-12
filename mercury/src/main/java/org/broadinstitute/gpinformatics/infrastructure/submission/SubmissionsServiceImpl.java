package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProjects;
import org.broadinstitute.gpinformatics.infrastructure.common.QueryStringSplitter;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.JaxRsUtils;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
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
            Response response = clientResponseGet(SubmissionConfig.SUBMISSIONS_STATUS_URI, parameters);
            validateResponseStatus("querying submission status", response);
            SubmissionStatusResultBean result = response.readEntity(SubmissionStatusResultBean.class);
            response.close();
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
        Response response =
                clientResponseGet(SubmissionConfig.LIST_BIOPROJECTS_ACTION, NO_PARAMETERS);
        BioProjects bioProjects = response.readEntity(BioProjects.class);
        validateResponseStatus("querying submission status", response);
        response.close();
        return bioProjects.getBioprojects();
    }

    /**
     * Defines the Rest call to post submissions for sequenced sample results
     * @param submissions   JAXB representation of all selected sample submission information
     * @return
     */
    @Override
    public Collection<SubmissionStatusDetailBean> postSubmissions(SubmissionRequestBean submissions) {
        Response response =
                JaxRsUtils.getWebResource(submissionsConfig.getWSUrl(SubmissionConfig.SUBMIT_ACTION),
                        MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON)
                           .post(Entity.json(submissions));
        validateResponseStatus("posting submissions", response);
        List<SubmissionStatusDetailBean> submissionStatuses =
            response.readEntity(SubmissionStatusResultBean.class).getSubmissionStatuses();
        response.close();
        return submissionStatuses;
    }

    @Override
    public Collection<String> getSubmissionSamples(BioProject bioProject) {
        Map<String, List<String>> parameterMap = new HashMap<>();
        parameterMap.put(ACCESSION_PARAMETER, Arrays.asList(bioProject.getAccession()));

        Response response =
                clientResponseGet(SubmissionConfig.SUBMISSION_SAMPLES_ACTION, parameterMap);
        validateResponseStatus("receiving submission samples list", response);
        List<String> submittedSampleIds = response.readEntity(SubmissionSampleResultBean.class).getSubmittedSampleIds();
        response.close();
        return submittedSampleIds;
    }

    @Override
    public List<SubmissionRepository> getSubmissionRepositories() {
        Response response = clientResponseGet(SubmissionConfig.ALL_SUBMISSION_SITES, NO_PARAMETERS);
        validateResponseStatus("receiving Submission Repositories", response);
        List<SubmissionRepository> submissionRepositories =
                response.readEntity(SubmissionRepositories.class).getSubmissionRepositories();
        response.close();
        return submissionRepositories;
    }

    @Override
    public List<SubmissionLibraryDescriptor> getSubmissionLibraryDescriptors() {
        Response response = clientResponseGet(SubmissionConfig.SUBMISSION_TYPES, NO_PARAMETERS);
        validateResponseStatus("receiving Submission Library Descriptors",response);
        List<SubmissionLibraryDescriptor> submissionLibraryDescriptors =
                response.readEntity(SubmissionLibraryDescriptors.class).getSubmissionLibraryDescriptors();
        response.close();
        return submissionLibraryDescriptors;
    }

    private Response clientResponseGet(String servicePath, Map<String, List<String>> parameters) {
        try {
            return JaxRsUtils.getWebResource(submissionsConfig.getWSUrl(servicePath),
                    MediaType.APPLICATION_JSON_TYPE, parameters).get();
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

    protected void validateResponseStatus(String activityName, Response response) {
        if(response.getStatus() != Response.Status.OK.getStatusCode()) {
            String error = response.readEntity(String.class);
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
    public List<SubmissionTracker> findOrphans(Map<String, SubmissionTracker> submissionTrackerMap) {
        List<SubmissionTracker> orphans = new ArrayList<>();

        // Since the SubmissionService does not return the submitted sample name in it's response it is necessary to
        // look up all the submission statuses to find if the submission exists on the Epsilon 9.
        if (CollectionUtils.isNotEmpty(submissionTrackerMap.keySet())) {
            Collection<SubmissionStatusDetailBean> submissionStatus =
                getSubmissionStatus(submissionTrackerMap.keySet().toArray(new String[0]));
            for (SubmissionStatusDetailBean status : submissionStatus) {
                if (!status.submissionServiceHasRequest()) {
                    orphans.add(submissionTrackerMap.get(status.uuid));
                }
            }
        }
        return orphans;
    }

}
