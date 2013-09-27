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
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;

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
    private Log logger;

    private static Map<String, CustomFieldDefinition> sampleKitJiraFields;

    public SampleKitEjb() {
        initJiraFields();
    }

    public SampleKitEjb(@Nonnull JiraService jiraService) {
        this.jiraService = jiraService;
        initJiraFields();
    }

    public enum JiraField implements CustomField.SubmissionField {
        TASK_ID("Task ID"),// (text)
        SHIPPING_ADDRESS("Shipping Address"), // (large text)
        SITE_NAME("Site Name"), // (text)
        DELIVERY_METHOD("Kit Delivery Method"), // (select list with options:
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

        private JiraField(String fieldNameIn) {
            fieldName = fieldNameIn;
        }

        @Nonnull
        @Override
        public String getName() {
            return fieldName;
        }
    }

    /**
     * Return a list of allowed values for a select list field.
     *
     * @param jiraField the field you would like pre-defined values from.
     *
     * @return
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
     *
     * @return
     */
    public Map<String, CustomFieldDefinition> initJiraFields() {
        try {
            if (sampleKitJiraFields == null) {
                CreateFields.Project kitRequestProject =
                        new CreateFields.Project(CreateFields.ProjectType.SAMPLE_KIT_INITIATION.getKeyPrefix());
                sampleKitJiraFields =
                        jiraService.getRequiredFields(kitRequestProject, CreateFields.IssueType.SAMPLE_KIT);
            }
        } catch (IOException e) {
            logger.error("Could not communicate with Jira.", e);
        }
        return sampleKitJiraFields;
    }
    /*
    Can choose from a drop down list of plasticware (currently restricted to 0.75 mL tubes that have linear barcodes etched/printed on the side by the manufacturer
    Can choose how many empty racks to request
    Can choose number of tubes per rack
    Can select a destination for the kit using an address search widget that queries BSP people by name to find addresses
    Creation of kit request in mercury also creates a KIT-type jira ticket (work with Amy to get the details of the ticket)
    Each kit (rack) request creates a single KIT-type jira ticket
    Creation of kit request in mercury triggers the creation of a kit request in BSP
    Jira ticket created for kit is linked to BSP kit request
    Jira kit tickets are linked to the jira PDO tickets so PMs and PDMs can see their kit requests from jira in the context of a PDO

    private int numberOfRacks;
    private int numberOfTubesPerRack;
    private String destination;
    private String bspKitRequest;
    private ProductOrder linkedProductOrder;
     */

    public List<String> createKitRequest(@Nonnull SampleKitRequestDto sampleKitRequestDto) throws Exception {
        List<String> createdJiraIds = new ArrayList<>(sampleKitRequestDto.getNumberOfRacks());
        // Create one jira ticket per rack.
        while (createdJiraIds.size() < sampleKitRequestDto.getNumberOfRacks()) {
            Collection<CustomField> customFieldList = new LinkedList<>();

            // Plasticware field
            if (!sampleKitJiraFields.containsKey(JiraField.PLASTICWARE.fieldName)) {
                throw new RuntimeException(
                        sampleKitRequestDto.getPlasticware() + " is not a valid tube type for sample kits.");
            }
            customFieldList.add(new CustomField(sampleKitJiraFields.get(JiraField.PLASTICWARE.fieldName),
                    new CustomField.SelectOption(sampleKitRequestDto.getPlasticware())));

            // Destination field
            customFieldList.add(new CustomField(sampleKitJiraFields.get(JiraField.SITE_NAME.fieldName),
                    sampleKitRequestDto.getDestination()));

            // Number of tubes
            customFieldList
                    .add(new CustomField(sampleKitJiraFields.get(JiraField.NUMBER_OF_SAMPLES.fieldName),
                            sampleKitRequestDto.getNumberOfTubesPerRack()));
            // BSP Work Request Ids
            customFieldList
                    .add(new CustomField(sampleKitJiraFields.get(JiraField.WORK_REQUEST_IDS.fieldName),
                            sampleKitRequestDto.getBspKitRequest()));

            // Delivery Method
            if (!sampleKitJiraFields.containsKey(JiraField.DELIVERY_METHOD.fieldName)) {
                throw new RuntimeException(
                        sampleKitRequestDto.getPlasticware() + " is not a valid tube type for sample kits.");
            }
            customFieldList
                    .add(new CustomField(sampleKitJiraFields.get(JiraField.DELIVERY_METHOD.fieldName),
                            new CustomField.SelectOption(sampleKitRequestDto.getDeliveryMethod())));

            // Add Project Managers
            List<CustomField.NameContainer> projectManagers=new ArrayList<>(sampleKitRequestDto.getProjectManagers().size());
            for (String projectManager : sampleKitRequestDto.getProjectManagers()) {
                projectManagers.add(new CustomField.NameContainer(projectManager));
            }

            customFieldList.add(new CustomField(sampleKitJiraFields.get(JiraField.PROJECT_MANAGERS.fieldName),
                    projectManagers));

            String summary =
                    String.format("Sample initiation for %s Samples", sampleKitRequestDto.getNumberOfTubesPerRack());

            String description = String.format("%s Samples requested for productOrder %s",
                    sampleKitRequestDto.getNumberOfTubesPerRack(),
                    sampleKitRequestDto.getLinkedProductOrder());
            customFieldList
                    .add(new CustomField(sampleKitJiraFields.get(JiraField.DESCRIPTION.fieldName), description));

            JiraIssue jiraIssue = jiraService.createIssue(CreateFields.ProjectType.SAMPLE_KIT_INITIATION.getKeyPrefix(),
                    null, CreateFields.IssueType.SAMPLE_KIT,
                    summary, customFieldList);
            createdJiraIds.add(jiraIssue.getKey());
        }
        return createdJiraIds;
    }

}
