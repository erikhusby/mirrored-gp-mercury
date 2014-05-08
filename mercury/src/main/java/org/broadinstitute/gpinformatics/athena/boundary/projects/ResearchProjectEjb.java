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

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.orders.UpdateField;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ProjectPerson;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.IssueFieldsResponse;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.Transition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Stateful
@RequestScoped
/**
 * This class is responsible for interactions between Jira and Research Projects
 */
public class ResearchProjectEjb {
    private final JiraService jiraService;
    private final UserBean userBean;
    private final BSPUserList userList;
    private final BSPCohortList cohortList;
    private final AppConfig appConfig;
    private final ResearchProjectDao researchProjectDao;

    // EJBs require a no arg constructor.
    @SuppressWarnings("unused")
    public ResearchProjectEjb() {
        this(null, null, null, null, null, null);
    }

    @Inject
    public ResearchProjectEjb(JiraService jiraService, UserBean userBean, BSPUserList userList,
                              BSPCohortList cohortList, AppConfig appConfig, ResearchProjectDao researchProjectDao) {
        this.jiraService = jiraService;
        this.userBean = userBean;
        this.userList = userList;
        this.cohortList = cohortList;
        this.appConfig = appConfig;
        this.researchProjectDao = researchProjectDao;
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

        List<String> fundingSources = new ArrayList<>();
        for (ResearchProjectFunding fundingSrc : researchProject.getProjectFunding()) {
            fundingSources.add(fundingSrc.getFundingId());
        }
        researchProjectUpdateFields
                .add(new ResearchProjectUpdateField(RequiredSubmissionFields.FUNDING_SOURCE,
                        StringUtils.join(fundingSources, ",")));

        String piNames = buildProjectPiJiraString(researchProject);
        if (!StringUtils.isBlank(piNames)) {
            researchProjectUpdateFields.add(new ResearchProjectUpdateField(RequiredSubmissionFields.BROAD_PIS, piNames));
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
                         + (updateComment.isEmpty() ? "No JIRA Product Order fields were updated\n\n" : updateComment);

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
     * RequiredSubmissionFields is an enum intended to assist in the creation of a Jira ticket
     * for Research Projects
     */
    public enum RequiredSubmissionFields implements CustomField.SubmissionField {
        //        Sponsoring_Scientist("Sponsoring Scientist"),
        COHORTS("Cohort(s)"),
        FUNDING_SOURCE("Funding Source"),
        MERCURY_URL("Mercury URL"),
        DESCRIPTION("Description"),
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
