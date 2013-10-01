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

package org.broadinstitute.gpinformatics.athena.boundary.kits;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Stateful
@RequestScoped
public class SampleKitEjb {
    @Inject
    JiraService jiraService;

    @Inject
    private Deployment deployment;

    private Log log = LogFactory.getLog(SampleKitEjb.class);

    @Inject
    private BSPConfig bspConfig;

    private Map<String, CustomFieldDefinition> sampleKitJiraFields;

    public SampleKitEjb() {
        init();
    }

    public SampleKitEjb(@Nonnull JiraService jiraService, @Nonnull Deployment deployment) {
        this.jiraService = jiraService;
        this.deployment = deployment;
        init();
    }

    public enum JiraField implements CustomField.SubmissionField {
        TASK_ID("Task ID"),// (text)
        SHIPPING_ADDRESS("Shipping Address"),
        SITE_NAME("Site Name"),
        DELIVERY_METHOD("Kit Delivery Method"),
        COLLABORATOR_INFORMATION("Collaborator Information"),
        WORK_REQUEST_IDS("Work Request ID(s)"),
        PROJECT("Project"),
        PLASTICWARE("Plasticware"),
        SUMMARY("Summary"),
        PROJECT_MANAGERS("PMs"),
        NUMBER_OF_SAMPLES("Number of Samples"),
        SHIPPED_BY("Shipped By"),
        DESCRIPTION("Description");

        private final String fieldName;

        private JiraField(String fieldName) {
            this.fieldName = fieldName;
        }

        @Nonnull
        @Override
        public String getName() {
            return fieldName;
        }
    }

    /**
     * Get a list of Strings representing the different types of delivery methods a SampleKit can contain.
     *
     * @return a list of the available delivey methods (Fedex, Local Pickup Etc).
     */
    public Collection<String> getDeliveryMethods() {
        return getAllowedValues(SampleKitEjb.JiraField.DELIVERY_METHOD);
    }

    /**
     * Get a list of Strings representing the different types of plasticware a SampleKit can contain.
     *
     * @return a list of the available plasticware.
     */
    public Collection<String> getPlasticware() {
        return getAllowedValues(JiraField.PLASTICWARE);
    }

    /**
     * Return a list of allowed values for a select list field.
     *
     * @param jiraField the field you would like pre-defined values from.
     *
     * @return a collection of values allowed for this jira field.
     */
    public Collection<String> getAllowedValues(JiraField jiraField) {
        Collection<String> allowedValues = new ArrayList<>();
        if (sampleKitJiraFields.containsKey(jiraField.getName())) {
            CustomFieldDefinition customFieldDefinition = sampleKitJiraFields.get(jiraField.getName());
            allowedValues = customFieldDefinition.getAllowedValues();
        }
        return allowedValues;
    }

    /**
     * Initialize the fields for a sampleKit.
     */
    public void init() {
        MercuryConfiguration mercuryConfiguration = MercuryConfiguration.getInstance();
        bspConfig = (BSPConfig) mercuryConfiguration.getConfig(BSPConfig.class, deployment);
        try {
            CreateFields.Project kitRequestProject =
                    new CreateFields.Project(CreateFields.ProjectType.SAMPLE_KIT_INITIATION.getKeyPrefix());
            sampleKitJiraFields =
                    jiraService.getRequiredFields(kitRequestProject, CreateFields.IssueType.SAMPLE_KIT);

        } catch (IOException e) {
            log.error("Could not communicate with Jira.", e);
        }
    }

    /**
     * Create a KIT Request in Jira.
     *
     * @param kitRequestDto Object containing required and optional fields for jira issue. One Jira issue
     *                      will be created per rack.
     *
     * @return Returns a List of Jira issue ID's.
     *
     * @throws Exception An exception is thrown if there was a problem creating an issue in jira.
     */
    public List<String> createKitRequest(@Nonnull SampleKitRequestDto kitRequestDto) {
        List<String> createdJiraIds = new ArrayList<>(kitRequestDto.getNumberOfRacks());
        // Create one jira ticket per rack.
        while (createdJiraIds.size() < kitRequestDto.getNumberOfRacks()) {
            Collection<CustomField> customFieldList = new LinkedList<>();

            // Plasticware field
            if (!sampleKitJiraFields.containsKey(JiraField.PLASTICWARE.fieldName)) {
                throw new RuntimeException(
                        kitRequestDto.getPlasticware() + " is not a valid tube type for sample kits.");
            }
            customFieldList.add(new CustomField(sampleKitJiraFields.get(JiraField.PLASTICWARE.fieldName),
                    new CustomField.SelectOption(kitRequestDto.getPlasticware())));

            // Destination field
            customFieldList.add(new CustomField(sampleKitJiraFields.get(JiraField.SITE_NAME.fieldName),
                    kitRequestDto.getDestination()));

            // Number of tubes
            customFieldList
                    .add(new CustomField(sampleKitJiraFields.get(JiraField.NUMBER_OF_SAMPLES.fieldName),
                            kitRequestDto.getNumberOfTubesPerRack()));
            // BSP Work Request Ids
            customFieldList
                    .add(new CustomField(sampleKitJiraFields.get(JiraField.WORK_REQUEST_IDS.fieldName),
                            kitRequestDto.getBspKitRequest()));

            // Delivery Method
            if (!sampleKitJiraFields.containsKey(JiraField.DELIVERY_METHOD.fieldName)) {
                throw new RuntimeException(
                        kitRequestDto.getPlasticware() + " is not a valid tube type for sample kits.");
            }
            customFieldList
                    .add(new CustomField(sampleKitJiraFields.get(JiraField.DELIVERY_METHOD.fieldName),
                            new CustomField.SelectOption(kitRequestDto.getDeliveryMethod())));

            // Add Project Managers
            List<CustomField.NameContainer> projectManagers =
                    new ArrayList<>(kitRequestDto.getProjectManagers().size());
            for (String projectManager : kitRequestDto.getProjectManagers()) {
                projectManagers.add(new CustomField.NameContainer(projectManager));
            }

            customFieldList.add(new CustomField(sampleKitJiraFields.get(JiraField.PROJECT_MANAGERS.fieldName),
                    projectManagers));

            // summary field.
            String summary =
                    String.format("Kit Request for %s [%s]",
                            kitRequestDto.getBspKitRequest(), kitRequestDto.getLinkedProductOrder());

            // description field.
            String description = String.format("%s Samples requested for %s [%s]",
                    kitRequestDto.getNumberOfTubesPerRack(),
                    createWorkRequestJiraLink(kitRequestDto.getBspKitRequest()),
                    kitRequestDto.getLinkedProductOrder());

            customFieldList
                    .add(new CustomField(sampleKitJiraFields.get(JiraField.DESCRIPTION.fieldName), description));
            log.info(description);

            try {
                JiraIssue jiraIssue =
                        jiraService.createIssue(CreateFields.ProjectType.SAMPLE_KIT_INITIATION.getKeyPrefix(),
                                kitRequestDto.getRequestedBy(), CreateFields.IssueType.SAMPLE_KIT,
                                summary, customFieldList);
                String jiraIssueKey = jiraIssue.getKey();

                jiraService.addLink(AddIssueLinkRequest.LinkType.Parentage,
                        kitRequestDto.getLinkedProductOrder(), jiraIssueKey);
                createdJiraIds.add(jiraIssueKey);

            } catch (IOException e) {
                throw new InformaticsServiceException("Error attempting to Sample Kit Request in JIRA.", e);
            }
        }
        return createdJiraIds;
    }

    /**
     * construct a Jira external link to a Bsp Work Request.
     *
     * @param workRequestId work request to link to.
     *
     * @return a String which can be used directly in a jira field to link to a Work Request in BSP.
     */
    private String createWorkRequestJiraLink(@Nonnull String workRequestId) {
        String workRequestNumber = workRequestId.replace("WR-", "");
        String searchLink = String.format("workrequest/ManageWorkRequests.action?ShowWorkRequest=&workRequestId=%s",
                workRequestNumber);
        return String.format("[%s|%s]", workRequestId, bspConfig.getUrl(searchLink));
    }
}

