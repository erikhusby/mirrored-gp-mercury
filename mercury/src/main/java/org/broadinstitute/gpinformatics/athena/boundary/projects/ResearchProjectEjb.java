/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the 
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support 
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its 
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.orders.UpdateField;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.SubmissionTrackerDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ProjectPerson;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTuple;
import org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.MercuryStringUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.IssueFieldsResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionBioSampleBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionContactBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionRepository;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionRequestBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionStatusDetailBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsService;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateful
@RequestScoped
/**
 * This class is responsible for interactions between Jira and Research Projects
 */
public class ResearchProjectEjb {

    private static final Log log = LogFactory.getLog(ResearchProjectEjb.class);

    private final JiraService jiraService;
    private final UserBean userBean;
    private final BSPUserList userList;
    private final BSPCohortList cohortList;
    private final AppConfig appConfig;
    private final ResearchProjectDao researchProjectDao;
    private final SubmissionsService submissionsService;
    private SubmissionTrackerDao submissionTrackerDao;

    // EJBs require a no arg constructor.
    @SuppressWarnings("unused")
    public ResearchProjectEjb() {
        this(null, null, null, null, null, null, null, null);
    }

    @Inject
    public ResearchProjectEjb(JiraService jiraService, UserBean userBean, BSPUserList userList,
                              BSPCohortList cohortList, AppConfig appConfig, ResearchProjectDao researchProjectDao,
                              SubmissionsService submissionsService, SubmissionTrackerDao submissionTrackerDao) {
        this.jiraService = jiraService;
        this.userBean = userBean;
        this.userList = userList;
        this.cohortList = cohortList;
        this.appConfig = appConfig;
        this.researchProjectDao = researchProjectDao;
        this.submissionsService = submissionsService;
        this.submissionTrackerDao = submissionTrackerDao;
    }

    /**
     * Add a list of users to a research project under the specified role. If a user is already assigned that role
     * in the research project, nothing is changed.
     */
    public void addPeople(String researchProjectKey, RoleType roleType, Collection<BspUser> people) {
        ResearchProject researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);
        if (researchProject != null) {
            researchProject.addPeople(roleType, people);
        }
    }

    public void submitToJira(@Nonnull ResearchProject researchProject) throws IOException {
        if (researchProject.isSavedInJira()) {
            updateJiraIssue(researchProject);
        } else {
            Map<String, CustomFieldDefinition> submissionFields = jiraService.getCustomFields();

            List<CustomField> listOfFields = new ArrayList<>();

            listOfFields.add(new CustomField(submissionFields, RequiredSubmissionFields.COHORTS,
                    cohortList.getCohortListString(researchProject.getCohortIds())));

            List<String> fundingSources = new ArrayList<>();
            for (ResearchProjectFunding fundingSrc : researchProject.getProjectFunding()) {
                fundingSources.add(fundingSrc.getFundingId());
            }

            listOfFields.add(new CustomField(submissionFields, RequiredSubmissionFields.FUNDING_SOURCE,
                    StringUtils.join(fundingSources, ',')));

            listOfFields.add(new CustomField(submissionFields, RequiredSubmissionFields.MERCURY_URL, ""));


            String piNames = buildProjectPiJiraString(researchProject);
            if (!StringUtils.isBlank(piNames)) {
                listOfFields
                        .add(new CustomField(submissionFields, RequiredSubmissionFields.BROAD_PIS, piNames));
            }
            if (researchProject.getSynopsis() != null) {
                listOfFields.add(new CustomField(submissionFields, RequiredSubmissionFields.DESCRIPTION,
                        researchProject.getSynopsis()));
            }

            String username = userList.getById(researchProject.getCreatedBy()).getUsername();

            // Create the jira ticket and then assign the key right away because whatever else happens, this jira ticket
            // IS created. If callers want to respond to errors, they can check for the key and decide what to do.
            JiraIssue issue = jiraService.createIssue(CreateFields.ProjectType.RESEARCH_PROJECTS, username,
                    CreateFields.IssueType.RESEARCH_PROJECT, researchProject.getTitle(), listOfFields);
            researchProject.setJiraTicketKey(issue.getKey());

            // Update ticket with link back into Mercury
            CustomField mercuryUrlField = new CustomField(
                    submissionFields, RequiredSubmissionFields.MERCURY_URL,
                    appConfig.getUrl() + ResearchProjectActionBean.ACTIONBEAN_URL_BINDING + "?" +
                    ResearchProjectActionBean.VIEW_ACTION + "&" +
                    ResearchProjectActionBean.RESEARCH_PROJECT_PARAMETER + "=" + issue.getKey());

            issue.updateIssue(Collections.singleton(mercuryUrlField));
        }
    }

    public void updateJiraIssue(@Nonnull ResearchProject researchProject) throws IOException {
        Transition transition = jiraService.findAvailableTransitionByName(researchProject.getJiraTicketKey(),
                JiraTransition.DEVELOPER_EDIT.getStateName());

        List<ResearchProjectUpdateField> researchProjectUpdateFields = new ArrayList<>();
        researchProjectUpdateFields
                .add(new ResearchProjectUpdateField(RequiredSubmissionFields.DESCRIPTION,
                        researchProject.getSynopsis()));
        researchProjectUpdateFields.add(new ResearchProjectUpdateField(RequiredSubmissionFields.SUMMARY,
                researchProject.getTitle()));


        List<String> fundingSources = new ArrayList<>();
        for (ResearchProjectFunding fundingSrc : researchProject.getProjectFunding()) {
            fundingSources.add(fundingSrc.getFundingId());
        }
        researchProjectUpdateFields
                .add(new ResearchProjectUpdateField(RequiredSubmissionFields.FUNDING_SOURCE,
                        StringUtils.join(fundingSources, ",")));

        String piNames = buildProjectPiJiraString(researchProject);
        if (!StringUtils.isBlank(piNames)) {
            researchProjectUpdateFields.add(new ResearchProjectUpdateField(RequiredSubmissionFields.BROAD_PIS,
                    piNames));
        }

        String[] customFieldNames = new String[researchProjectUpdateFields.size()];

        int i = 0;
        for (UpdateField updateField : researchProjectUpdateFields) {
            customFieldNames[i++] = updateField.getDisplayName();
        }

        Map<String, CustomFieldDefinition> customFieldDefinitions = jiraService.getCustomFields(customFieldNames);

        IssueFieldsResponse issueFieldsResponse =
                jiraService.getIssueFields(researchProject.getJiraTicketKey(), customFieldDefinitions.values());


        List<CustomField> customFields = new ArrayList<>();

        StringBuilder updateCommentBuilder = new StringBuilder();

        for (ResearchProjectUpdateField field : researchProjectUpdateFields) {
            String message = field.getUpdateMessage(researchProject, customFieldDefinitions, issueFieldsResponse);
            if (!message.isEmpty()) {
                customFields.add(field.createCustomField(customFieldDefinitions));
                updateCommentBuilder.append(message);
            }
        }
        String updateComment = updateCommentBuilder.toString();

        // If we detect from the comment that nothing has changed, make a note of that.  The user may have changed
        // something in the ResearchProject that is not reflected in JIRA.
        String comment = "\n" + researchProject.getJiraTicketKey() + " was edited by "
                         + userBean.getLoginUserName() + "\n\n"
                         + (updateComment.isEmpty() ? "No JIRA Research Project fields were updated\n\n" : updateComment);

        jiraService.postNewTransition(researchProject.getJiraTicketKey(), transition, customFields, comment);
    }

    /**
     * @return a String of PI's in project to send to Jira.
     */
    private String buildProjectPiJiraString(ResearchProject researchProject) {
        List<String> piNameList = new ArrayList<>();
        for (ProjectPerson currPI : researchProject.findPeopleByType(RoleType.BROAD_PI)) {
            BspUser bspUser = userList.getById(currPI.getPersonId());
            if (bspUser != null) {
                piNameList.add(bspUser.getFullName());
            }
        }
        return StringUtils.join(piNameList, '\n');
    }

    /**
     * When called, this method will post to the submission service the samples and their related information
     * that have been selected to be submitted.
     *
     * @param researchProjectBusinessKey Unique key of the Research Project under which the
     * @param selectedBioProject         BioProject to be associated with all submissions
     * @param submissionDtos             Collection of submissionDTOs selected to be submitted
     * @param repository                 Repository where submission will be sent.
     * @param submissionLibraryDescriptor             The name of the library descriptor to be sent in the submission.
     *
     * @return the results from the post to the submission service
     */
    public Collection<SubmissionStatusDetailBean> processSubmissions(@Nonnull String researchProjectBusinessKey,
                                                                     @Nonnull BioProject selectedBioProject,
                                                                     @Nonnull List<SubmissionDto> submissionDtos,
                                                                     @Nonnull SubmissionRepository repository,
                                                                     @Nonnull SubmissionLibraryDescriptor
                                                                     submissionLibraryDescriptor) throws ValidationException {
        validateSubmissionDto(submissionDtos);
        validateSubmissionSamples(selectedBioProject, submissionDtos);

        ResearchProject submissionProject = researchProjectDao.findByBusinessKey(researchProjectBusinessKey);

        Map<SubmissionTracker, SubmissionDto> submissionDtoMap = new HashMap<>();

        for (SubmissionDto submissionDto : submissionDtos) {
            SubmissionTracker tracker = submissionDto.buildSubmissionTracker();
            submissionProject.addSubmissionTracker(tracker);
            submissionDtoMap.put(tracker, submissionDto);
        }

        researchProjectDao.persist(submissionProject);

        List<SubmissionBean> submissionBeans = new ArrayList<>();

        for (Map.Entry<SubmissionTracker, SubmissionDto> dtoByTracker : submissionDtoMap.entrySet()) {

            BioProject submitBioProject = new BioProject();
            submitBioProject.setAccession(selectedBioProject.getAccession());

            SubmissionBioSampleBean bioSampleBean =
                new SubmissionBioSampleBean(dtoByTracker.getValue().getSampleName(),
                    dtoByTracker.getValue().getProcessingLocation(),
                    new SubmissionContactBean(userBean.getBspUser().getFirstName(),
                        userBean.getBspUser().getLastName(), userBean.getBspUser().getEmail()
                    ));

            SubmissionBean submissionBean =
                    new SubmissionBean(dtoByTracker.getKey().createSubmissionIdentifier(),
                            userBean.getBspUser().getUsername(), submitBioProject, bioSampleBean, repository,
                            submissionLibraryDescriptor, dtoByTracker.getValue().getAggregationProject(),
                            String.valueOf(dtoByTracker.getValue().getVersion()));
            submissionBeans.add(submissionBean);
        }

        SubmissionRequestBean requestBean = new SubmissionRequestBean(submissionBeans);
        try {
            log.debug(MercuryStringUtils.serializeJsonBean(requestBean));
        } catch (IOException e) {
            log.error("Error serializing " + SubmissionRequestBean.class.getName(), e);
        }

        Collection<SubmissionStatusDetailBean> submissionResults;
        try {
            submissionResults = submissionsService.postSubmissions(requestBean);
        } catch (Exception e) {
            log.error("Error Posting to Submission Service " + e.getMessage(), e);
            throw new InformaticsServiceException(e.getMessage(), e);
        }

        Map<String, SubmissionTracker> submissionIdentifierToTracker =
            SubmissionTracker.uuidMap(submissionProject.getSubmissionTrackers());

        List<String> errorMessages = new ArrayList<>();

        updateAndPersistSubmissionDtoStatusFromResults(submissionProject, submissionDtoMap, submissionResults,
            submissionIdentifierToTracker, errorMessages);
        if (CollectionUtils.isNotEmpty(errorMessages)) {
            throw new ValidationException("There were some errors during submission.  ", errorMessages);
        }

        return submissionResults;
    }

    public void updateAndPersistSubmissionDtoStatusFromResults(ResearchProject submissionProject,
                                                                  Map<SubmissionTracker, SubmissionDto> submissionDtoMap,
                                                                  Collection<SubmissionStatusDetailBean> submissionResults,
                                                                  Map<String, SubmissionTracker> submissionIdentifierToTracker,
                                                                  List<String> errorMessages) {
        List<SubmissionTracker> trackersToDelete =
            updateSubmissionDtoStatusFromResults(submissionProject, submissionDtoMap, submissionResults,
                submissionIdentifierToTracker, errorMessages);
        for (SubmissionTracker deleteTracker : trackersToDelete) {
            submissionTrackerDao.remove(deleteTracker);
        }
        if (!trackersToDelete.isEmpty()) {
            submissionTrackerDao.persist(submissionProject);
        }
    }

    /**
     * Validates a list of submissionDTOs being submitted in a research project. SubmissionDTOs need to:
     * <ul>
     *     <li>Not be empty.</li>
     *     <li>Have bassDTOs with distinct tuples </li>
     *     <li>Have distinct tuples compared to previous Submissions</li>
     * </ul>
     * @param submissionDtos
     * @throws ValidationException
     */
    public void validateSubmissionDto(@Nonnull List<SubmissionDto> submissionDtos)
            throws ValidationException {

        if (submissionDtos.isEmpty()) {
            throw new InformaticsServiceException("At least one selection is needed to post submissions");
        }

        Set<String> errors = new HashSet<>();
        Set<SubmissionTuple> tuples = new HashSet<>(submissionDtos.size());
        for (SubmissionDto submissionDto : submissionDtos) {
            SubmissionTuple submissionTuple = submissionDto.getSubmissionTuple();
            if (submissionTuple != null) {
                if (!tuples.add(submissionTuple)) {
                    errors.add(submissionTuple.toString());
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(String.format("Attempt to submit duplicate samples: %s", errors));
        }

        if (tuples.isEmpty()) {
            throw new ValidationException("No data was found in submission request.");
        }

        List<SubmissionTracker> submissionTrackers = submissionTrackerDao.findSubmissionTrackers(submissionDtos);

        for (SubmissionTracker submissionTracker : submissionTrackers) {
            errors.add(submissionTracker.getSubmissionTuple().getSampleName());
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(String.format("Some samples have already been submitted: %s", errors));
        }
    }

    /**
     * Update SubmissionDtoStatus after submission request is made to Epsilon 9. Epsilon 9 will return any errors
     * generated when attempting to create submission requests. If there were any errors returned the corresponding
     * SubmissionTrackers need to be removed. In order to keep this method DAO free, a List of trackers needing
     * removal are returned. It is the callers responsibility to persist the changes.
     *
     * @return a List of are returned containing SubmissionTrackers which need to be removed.
     */
    protected List<SubmissionTracker> updateSubmissionDtoStatusFromResults(
        ResearchProject researchProject, Map<SubmissionTracker, SubmissionDto> submissionDtoMap,
        Collection<SubmissionStatusDetailBean> submissionResults,
        Map<String, SubmissionTracker> submissionIdentifierToTracker, List<String> errorMessages) {
        List<SubmissionStatusDetailBean> unmatchedSubmissionStatusDetailBeans = new ArrayList<>();

        // Since the database persists SubmissionTrackers in order to generate UUIDs to pass to the submissionsService,
        // We need to check for errors and manually remove ones which failed.
        List<SubmissionTracker> removeTrackers = new ArrayList<>();
        for (SubmissionStatusDetailBean status : submissionResults) {

            // remove current tracker from the map, so we can look up any trackers remaining when we exit the loop.
            SubmissionTracker submissionTracker = submissionIdentifierToTracker.remove(status.getUuid());

            if (CollectionUtils.isNotEmpty(status.getErrors())) {
                if (StringUtils.isBlank(status.getUuid())) {
                    unmatchedSubmissionStatusDetailBeans.add(status);
                    errorMessages.addAll(status.getErrors());
                } else {
                    for (String errorMessage : status.getErrors()) {
                        errorMessages
                            .add(String.format("%s: %s", submissionTracker.getSubmittedSampleName(), errorMessage));
                    }
                }
                if (submissionTracker != null) {
                    removeTrackers.add(submissionTracker);
                }
            } else {
                submissionDtoMap.get(submissionTracker).setStatusDetailBean(status);
            }
        }

        List<SubmissionTracker> orphanTrackers = submissionsService.findOrphans(submissionIdentifierToTracker.values());
        removeTrackers.addAll(orphanTrackers);
        researchProject.getSubmissionTrackers().removeAll(removeTrackers);
        if (CollectionUtils.isNotEmpty(unmatchedSubmissionStatusDetailBeans)) {
            String serializedStatus = "";
            try {
                serializedStatus = MercuryStringUtils.serializeJsonBean(unmatchedSubmissionStatusDetailBeans);
            } catch (IOException e) {
                log.error("Error generating error message", e);
            }
            String errorMessage = String.format("Unable to determine if there is a SubmissionTracker for some samples. %s",
                CoreActionBean.ERROR_CONTACT_SUPPORT);
            log.error(String.format("%s: %s", errorMessage, serializedStatus));
            errorMessages.add(errorMessage);
        }
        return removeTrackers;
    }

    public void validateSubmissionSamples(BioProject bioProject, Collection<SubmissionDto> submissionDtos)
            throws ValidationException {
        try {
            Collection<String> submissionSamples = submissionsService.getSubmissionSamples(bioProject);
            Set<String> invalidSamples = new HashSet<>();

            for (SubmissionDto submissionDto : submissionDtos) {
                if (! submissionSamples.contains(submissionDto.getSampleName())) {
                    invalidSamples.add(submissionDto.getSampleName());
                }
            }

            if (!invalidSamples.isEmpty()) {
                throw new ValidationException(
                        String.format(
                                "Some sample(s) have not been pre-accessioned and are not available for submission: %s",
                                invalidSamples));
            }
        } catch (InformaticsServiceException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    /**
     * RequiredSubmissionFields is an enum intended to assist in the creation of a Jira ticket
     * for Research Projects
     */
    public enum RequiredSubmissionFields implements CustomField.SubmissionField {
        //        Sponsoring_Scientist("Sponsoring Scientist"),
        COHORTS("Cohort(s)"),
        FUNDING_SOURCE("Funding Source"),
        MERCURY_URL("Mercury URL"),
        DESCRIPTION("Description"),
        SUMMARY("Summary"),
        BROAD_PIS("Broad PI(s)");

        private final String fieldName;
        private final boolean nullable;

        private RequiredSubmissionFields(String fieldName, boolean nullable) {
            this.fieldName = fieldName;
            this.nullable = nullable;
        }

        RequiredSubmissionFields(String fieldName) {
            this(fieldName, false);
        }

        @Nonnull
        @Override
        public String getName() {
            return fieldName;
        }

        @Override
        public boolean isNullable() {
            return nullable;
        }
    }

    /**
     * JIRA Transition states used by PDOs.
     */
    public enum JiraTransition {
        DEVELOPER_EDIT("Developer Edit"),
        PUT_ON_HOLD("Put On Hold"),
        CANCEL("Cancel"),
        COMPLETE("Complete");

        /**
         * The text that represents this transition state in JIRA.
         */
        private final String stateName;

        private JiraTransition(String stateName) {
            this.stateName = stateName;
        }

        @Nonnull
        public String getStateName() {
            return stateName;
        }
    }

    private static class ResearchProjectUpdateField extends UpdateField<ResearchProject> {
        public ResearchProjectUpdateField(
                @Nonnull CustomField.SubmissionField field, @Nonnull Object newValue) {
            super(field, newValue);
        }
    }

}
