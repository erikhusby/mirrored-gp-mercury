package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.map.LazyMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.AbstractBatchJiraFieldFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates the business logic related to {@link LabBatch}s.  This includes the creation
 * of a new batch entity and saving that to JIRA.
 */
@Stateful
@RequestScoped
public class LabBatchEjb {

    private final static Log logger = LogFactory.getLog(LabBatchEjb.class);

    private LabBatchDao labBatchDao;

    private JiraService jiraService;

    private JiraTicketDao jiraTicketDao;

    private LabVesselDao tubeDao;

    private BucketDao bucketDao;

    private BucketEntryDao bucketEntryDao;

    private BucketEjb bucketEjb;

    private ProductOrderDao productOrderDao;

    private SampleDataFetcher sampleDataFetcher;

    private ControlDao controlDao;

    private WorkflowLoader workflowLoader;

    /**
     * This method will create a batch entity and a new JIRA Ticket for that entity.
     * <p/>
     * TODO: consider making this private; seems strange to take a batch object, persist it, then return the same object
     *
     * @param batchObject A constructed, but not persisted, batch object containing all initial information necessary
     *                    to persist a new batch.
     * @param reporter    The username of the person that is attempting to create the batch.
     * @param issueType   The type of issue to create in JIRA for this lab batch.
     *
     * @return The lab batch that was created.
     *
     * @see #createLabBatch(LabBatch, String, CreateFields.IssueType, MessageReporter)
     */
    public LabBatch createLabBatch(@Nonnull LabBatch batchObject, String reporter,
                                   @Nonnull CreateFields.IssueType issueType, MessageReporter messageReporter) {
        return createLabBatch(batchObject, reporter, issueType,null, messageReporter);
    }


    /**
     * This method will create a batch entity and a new JIRA Ticket for that entity.
     * <p/>
     * TODO: consider making this private; seems strange to take a batch object, persist it, then return the same object
     *
     * @param batchObject A constructed, but not persisted, batch object containing all initial information necessary
     *                    to persist a new batch.
     * @param reporter    The username of the person that is attempting to create the batch.
     * @param issueType   The type of issue to create in JIRA for this lab batch.
     *
     * @return The lab batch that was created.
     *
     * @see #createLabBatch(LabBatch, String, CreateFields.IssueType, MessageReporter)
     */
    public LabBatch createLabBatch(@Nonnull LabBatch batchObject, String reporter,
                                   @Nonnull CreateFields.IssueType issueType) {
        return createLabBatch(batchObject, reporter, issueType, MessageReporter.UNUSED);
    }

    public LabBatch createLabBatch(LabBatch batchObject, String reporter, CreateFields.IssueType issueType,
                                    CreateFields.ProjectType projectType) {
        return createLabBatch(batchObject, reporter, issueType, projectType, MessageReporter.UNUSED);
    }

    public LabBatch createLabBatch(LabBatch batchObject, String reporter, CreateFields.IssueType issueType,
                                    CreateFields.ProjectType projectType, MessageReporter messageReporter) {
        if (StringUtils.isBlank(batchObject.getBatchName())) {
            throw new InformaticsServiceException("The name for the batch object cannot be null");
        }

        batchToJira(reporter, null, batchObject, issueType, projectType, messageReporter);
        labBatchDao.persist(batchObject);
        return batchObject;
    }

    /**
     * Creates a lab batch for a set of lab vessels (and reworks).
     *
     * @param labBatchType  the type of lab batch to create
     * @param workflowName  the workflow that the batch is to run through
     * @param batchName     the name for the batch (also the summary for JIRA)
     * @param description   the description for the batch (for JIRA)
     * @param dueDate       the due date for the batch (for JIRA)
     * @param important     the important notes for the batch (for JIRA)
     * @param username      the user creating the batch (for JIRA)
     * @param vessels       the vessels to include in the batch
     * @param reworkVessels the vessels being reworked to include in the batch
     *
     * @return a new lab batch
     */
    public LabBatch createLabBatch(@Nonnull LabBatch.LabBatchType labBatchType, @Nonnull String workflowName,
                                   @Nonnull String batchName, @Nonnull String description, @Nonnull Date dueDate,
                                   @Nonnull String important, @Nonnull String username,
                                   @Nonnull Set<LabVessel> vessels, @Nonnull Set<LabVessel> reworkVessels) {
        LabBatch batch =
                new LabBatch(batchName, vessels, reworkVessels, labBatchType, workflowName, description, dueDate,
                             important);
        labBatchDao.persist(batch);

        return batch;
    }

    /**
     * Creates a new lab batch and archives the bucket entries used to create the batch.
     *
     * @param labBatchType         the type of lab batch to create
     * @param workflowName         the workflow that the batch is to run through
     * @param bucketEntryIds       the IDs of the bucket entries to include in the batch
     * @param reworkBucketEntryIds the vessels being reworked to include in the batch
     * @param batchName            the name for the batch (also the summary for JIRA)
     * @param description          the description for the batch (for JIRA)
     * @param dueDate              the due date for the batch (for JIRA)
     * @param important            the important notes for the batch (for JIRA)
     * @param username             the user creating the batch (for JIRA)
     *
     * @param bucketName
     * @return The lab batch that was created.
     */
    public LabBatch createLabBatchAndRemoveFromBucket(@Nonnull LabBatch.LabBatchType labBatchType,
                                                      @Nonnull String workflowName, @Nonnull List<Long> bucketEntryIds,
                                                      @Nonnull List<Long> reworkBucketEntryIds,
                                                      @Nonnull String batchName, @Nonnull String description,
                                                      @Nonnull Date dueDate, @Nonnull String important,
                                                      @Nonnull String username, String bucketName) throws ValidationException {
        return createLabBatchAndRemoveFromBucket(labBatchType, workflowName, bucketEntryIds, reworkBucketEntryIds,
                batchName, description, dueDate, important, username, bucketName, MessageReporter.UNUSED,
                Collections.<String>emptyList());
    }
    /**
     * Creates a new lab batch and archives the bucket entries used to create the batch.
     *
     * @param labBatchType         the type of lab batch to create
     * @param workflowName         the workflow that the batch is to run through
     * @param bucketEntryIds       the IDs of the bucket entries to include in the batch
     * @param reworkBucketEntryIds the vessels being reworked to include in the batch
     * @param batchName            the name for the batch (also the summary for JIRA)
     * @param description          the description for the batch (for JIRA)
     * @param dueDate              the due date for the batch (for JIRA)
     * @param important            the important notes for the batch (for JIRA)
     * @param username             the user creating the batch (for JIRA)
     *
     * @param bucketName
     * @return The lab batch that was created.
     */
    public LabBatch createLabBatchAndRemoveFromBucket(@Nonnull LabBatch.LabBatchType labBatchType,
                                                      @Nonnull String workflowName, @Nonnull List<Long> bucketEntryIds,
                                                      @Nonnull List<Long> reworkBucketEntryIds,
                                                      @Nonnull String batchName, @Nonnull String description,
                                                      @Nonnull Date dueDate, @Nonnull String important,
                                                      @Nonnull String username, String bucketName,
                                                      @Nonnull MessageReporter reporter, List<String> watchers)
            throws ValidationException {
        List<BucketEntry> bucketEntries = bucketEntryDao.findByIds(bucketEntryIds);
        List<BucketEntry> reworkBucketEntries = bucketEntryDao.findByIds(reworkBucketEntryIds);
        Set<String> pdoKeys = new HashSet<>();

        Map<String, Integer> tubeBarcodeCounts =
                LazyMap.lazyMap(new HashMap<String, Integer>(), new Factory<Integer>() {
                    @Override
                    public Integer create() {
                        return 0;
                    }
                });
        Set<LabVessel> vessels = new HashSet<>();

        List<String> bucketDefNames = new ArrayList<>();

        for (BucketEntry bucketEntry : bucketEntries) {
            vessels.add(bucketEntry.getLabVessel());
            String tubeBarcode = bucketEntry.getLabVessel().getLabel();
            tubeBarcodeCounts.put(tubeBarcode, tubeBarcodeCounts.get(tubeBarcode) + 1);
            pdoKeys.add(bucketEntry.getProductOrder().getBusinessKey());
            bucketDefNames.add(bucketEntry.getBucket().getBucketDefinitionName());
        }

        Set<LabVessel> reworkVessels = new HashSet<>();
        for (BucketEntry bucketEntry : reworkBucketEntries) {
            reworkVessels.add(bucketEntry.getLabVessel());
            String tubeBarcode = bucketEntry.getLabVessel().getLabel();
            tubeBarcodeCounts.put(tubeBarcode, tubeBarcodeCounts.get(tubeBarcode) + 1);
            pdoKeys.add(bucketEntry.getProductOrder().getBusinessKey());
        }

        // Validate that no tubes appear in the proposed batch more than once
        List<String> repeatedTubeBarcodes = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : tubeBarcodeCounts.entrySet()) {
            if (entry.getValue() > 1) {
                repeatedTubeBarcodes.add(entry.getKey());
            }
        }
        if (!repeatedTubeBarcodes.isEmpty()) {
            throw new ValidationException(
                    "The following tubes were selected more than once for the batch, which is not allowed: "
                    + repeatedTubeBarcodes);
        }

        LabBatch batch =
                createLabBatch(labBatchType, workflowName, batchName, description, dueDate, important, username,
                               vessels, reworkVessels);

        Set<BucketEntry> allBucketEntries = new HashSet<>(bucketEntries);
        allBucketEntries.addAll(reworkBucketEntries);
        bucketEjb.moveFromBucketToBatch(allBucketEntries, batch);

        WorkflowBucketDef bucketDef = getWorkflowBucketDef(bucketName);

        CreateFields.IssueType issueType = CreateFields.IssueType.valueOf(bucketDef.getBatchJiraIssueType());

        batchToJira(username, null, batch, issueType,
                CreateFields.ProjectType.fromKeyPrefix(bucketDef.getBatchJiraProjectType()), reporter, watchers);

        //link the JIRA tickets for the batch created to the pdo batches.
        for (String pdoKey : pdoKeys) {
            linkJiraBatchToTicket(pdoKey, batch);
        }

        return batch;
    }

    private WorkflowBucketDef getWorkflowBucketDef(String bucketName) {
        WorkflowConfig workflowConfig = workflowLoader.load();
        WorkflowBucketDef bucketDef = null;

        for (Workflow workflow : Workflow.SUPPORTED_WORKFLOWS) {
            ProductWorkflowDef workflowDef = workflowConfig.getWorkflowByName(workflow.getWorkflowName());
            ProductWorkflowDefVersion workflowVersion = workflowDef.getEffectiveVersion();
            for (WorkflowBucketDef bucket : workflowVersion.getCreationBuckets()) {
                if (bucketName.equals(bucket.getName())) {
                    bucketDef = bucket;
                }
            }
        }
        return bucketDef;
    }

    /**
     * This method extracts all necessary information from the given batch object and creates (if necessary) and
     * associates a JIRA ticket that will represent this batch.
     *
     * @param reporter    The username of the person that is attempting to create the batch
     * @param jiraTicket  Optional parameter that represents an existing Jira Ticket that refers to this batch
     * @param newBatch    The source of the Batch information that will assist in populating the Jira Ticket
     * @param issueType   The type of issue to create in JIRA for this lab batch
     * @param projectType
     */
    public void batchToJira(String reporter, @Nullable String jiraTicket, LabBatch newBatch,
                            @Nonnull CreateFields.IssueType issueType, CreateFields.ProjectType projectType) {
        batchToJira(reporter, jiraTicket, newBatch, issueType, projectType, MessageReporter.UNUSED);
    }

    private void batchToJira(String reporter, String jiraTicket, LabBatch newBatch, CreateFields.IssueType issueType,
                             CreateFields.ProjectType projectType, MessageReporter messageReporter) {
        batchToJira(reporter, jiraTicket, newBatch, issueType, projectType, messageReporter,
                Collections.<String>emptyList());
    }

    /**
     * This method extracts all necessary information from the given batch object and creates (if necessary) and
     * associates a JIRA ticket that will represent this batch.
     *
     * @param reporter    The username of the person that is attempting to create the batch
     * @param jiraTicket  Optional parameter that represents an existing Jira Ticket that refers to this batch
     * @param newBatch    The source of the Batch information that will assist in populating the Jira Ticket
     * @param issueType   The type of issue to create in JIRA for this lab batch
     * @param projectType
     */
    public void batchToJira(String reporter, @Nullable String jiraTicket, LabBatch newBatch,
                            @Nonnull CreateFields.IssueType issueType, CreateFields.ProjectType projectType,
                            @Nonnull MessageReporter messageReporter, List<String> watchers) {
        try {
            if (issueType == null) {
                throw new InformaticsServiceException("JIRA issue type must be specified");
            }

            AbstractBatchJiraFieldFactory fieldBuilder = AbstractBatchJiraFieldFactory
                    .getInstance(projectType, newBatch, productOrderDao);
            if (projectType == null) {
                projectType = fieldBuilder.getProjectType();
            }

            if (StringUtils.isBlank(newBatch.getBatchDescription())) {
                newBatch.setBatchDescription(fieldBuilder.generateDescription());
            } else {
                newBatch.setBatchDescription(
                        fieldBuilder.generateDescription() + "\n\n" + newBatch.getBatchDescription());
            }

            if (jiraTicket == null) {
                Map<String, CustomFieldDefinition> customFieldDefinitions = jiraService.getRequiredFields(
                        new CreateFields.Project(projectType), issueType);

                List<CustomField> batchJiraTicketFields =
                        new ArrayList<>(fieldBuilder.getCustomFields(customFieldDefinitions));
                verifyAllowedValues(batchJiraTicketFields, messageReporter);

                JiraIssue jiraIssue = jiraService
                        .createIssue(fieldBuilder.getProjectType(), reporter, issueType, fieldBuilder.getSummary(),
                                batchJiraTicketFields);
                jiraIssue.addWatchers(watchers);
                JiraTicket ticket = new JiraTicket(jiraService, jiraIssue.getKey());

                newBatch.setJiraTicket(ticket);
            }
        } catch (IOException ioe) {
            logger.error("Error attempting to create a lab batch in JIRA", ioe);
            throw new InformaticsServiceException("Error attempting to create a lab batch in JIRA", ioe);
        }
    }

    private void verifyAllowedValues(List<CustomField> batchJiraTicketFields, @Nonnull MessageReporter messageReporter) {
        ListIterator<CustomField> jiraBatchFieldsIterator = batchJiraTicketFields.listIterator();

        Set<String> messages = new HashSet<>();
        while (jiraBatchFieldsIterator.hasNext()) {
            CustomField batchJiraTicketField = jiraBatchFieldsIterator.next();
            if (StringUtils.isBlank(batchJiraTicketField.getValue().toString())) {
                jiraBatchFieldsIterator.remove();
                break;
            }
            CustomFieldDefinition batchJiraTicketFieldFieldDefinition = batchJiraTicketField.getFieldDefinition();
            Collection<CustomField.ValueContainer> allowedValues = batchJiraTicketFieldFieldDefinition.getAllowedValues();

            List<CustomField.ValueContainer> valueContainerValues = new ArrayList<>();
            if (batchJiraTicketField.getValue() instanceof Object[]) {
                for (Object objectValue : ((Object[]) batchJiraTicketField.getValue())) {
                    if (objectValue instanceof CustomField.ValueContainer){
                        valueContainerValues.add((CustomField.ValueContainer) objectValue);
                    }
                }
            }
            if (batchJiraTicketField.getValue() instanceof CustomField.ValueContainer) {
                valueContainerValues.add((CustomField.ValueContainer) batchJiraTicketField.getValue());
            }
            for (CustomField.ValueContainer valueContainer : valueContainerValues) {
                if (!allowedValues.isEmpty() && StringUtils.isNotBlank(valueContainer.getValue()) && !allowedValues
                        .contains(valueContainer)) {
                    String fieldName = batchJiraTicketFieldFieldDefinition.getName();
                    messages.add(String.format(
                            "Unknown value '%s' for field '%s'. This will not prevent the batch being created but you will need to update it manually.",
                            valueContainer.getValue(), fieldName));
                    jiraBatchFieldsIterator.remove();
                }
            }
        }
        for (String message : messages) {
            messageReporter.addMessage(message);
        }
    }

    /**
     * This method links two batch JIRA tickets together in a parent to child relationship.
     *
     * @param parentBatch The parent batch to link
     * @param childBatch  The child batch to link
     */
    public void linkJiraBatches(LabBatch parentBatch, LabBatch childBatch) {
        try {
            if (childBatch.getJiraTicket() != null) {
                jiraService.addLink(AddIssueLinkRequest.LinkType.Parentage, parentBatch.getJiraTicket().getTicketName(),
                        childBatch.getJiraTicket().getTicketName());
            }
        } catch (Exception ioe) {
            logger.error("Error attempting to link batch " + childBatch.getJiraTicket().getTicketName()
                         + " to product order " + parentBatch,
                         ioe);
        }
    }

    /**
     * This method links two batch JIRA tickets together in a parent to child relationship.
     *
     * @param parentTicket The parent ticket key to link
     * @param childBatch   The child batch to link
     */
    public void linkJiraBatchToTicket(String parentTicket, LabBatch childBatch) {
        try {
            if (childBatch.getJiraTicket() != null) {
                jiraService.addLink(AddIssueLinkRequest.LinkType.Parentage, parentTicket,
                                    childBatch.getJiraTicket()
                                              .getTicketName());
            }
        } catch (Exception ioe) {
            logger.error("Error attempting to link batch " + childBatch.getJiraTicket().getTicketName()
                         + " to product order " + parentTicket,
                         ioe);
        }
    }

    /**
     * This method adds bucket entries, from PDO creation or rework, to an existing lab batch.
     *
     * @param businessKey    the business key for the lab batch we are adding samples to
     * @param bucketEntryIds the bucket entries whose vessel are being added to the batch
     * @param reworkEntries  the rework bucket entries whose vessels are being added to the batch
     *
     * @param bucketName
     * @param messageReporter
     * @throws IOException This exception is thrown when the JIRA service can not be contacted.
     */
    public void addToLabBatch(String businessKey, List<Long> bucketEntryIds, List<Long> reworkEntries,
                              String bucketName, MessageReporter messageReporter, List<String> watchers)
            throws IOException {
        LabBatch batch = labBatchDao.findByBusinessKey(businessKey);
        Set<String> pdoKeys = new HashSet<>();
        StringBuilder commentString = new StringBuilder();
        Set<String> bucketDefNames = new HashSet<>();
        List<BucketEntry> bucketEntries = bucketEntryDao.findByIds(bucketEntryIds);
        Set<LabVessel> labVessels = new HashSet<>();

        for (BucketEntry bucketEntry : bucketEntries) {
            bucketDefNames.add(bucketEntry.getBucket().getBucketDefinitionName());
            labVessels.add(bucketEntry.getLabVessel());
            pdoKeys.add(bucketEntry.getProductOrder().getBusinessKey());
            bucketEntry.getBucket().removeEntry(bucketEntry);
            commentString.append(String.format("Added vessel *%s* with material type *%s* to *%s*.\n", bucketEntry.getLabVessel().getLabel(),
                    bucketEntry.getLabVessel().getLatestMaterialType().getDisplayName(), bucketName));
        }

        batch.addLabVessels(labVessels);
        bucketEjb.moveFromBucketToBatch(bucketEntries, batch);

        List<BucketEntry> reworkBucketEntries = bucketEntryDao.findByIds(reworkEntries);
        Set<LabVessel> reworkVessels = new HashSet<>();
        Bucket reworkFromBucket = null;

        if (!reworkBucketEntries.isEmpty()) {
            commentString.append("\n\n");
        }
        for (BucketEntry entry : reworkBucketEntries) {
            reworkVessels.add(entry.getLabVessel());
            pdoKeys.add(entry.getProductOrder().getBusinessKey());
            entry.getBucket().removeEntry(entry);
            reworkFromBucket = entry.getBucket();
            commentString.append(String.format("Added rework for vessel %s with material type %s to %s.\n",
                    entry.getLabVessel().getLabel(), entry.getLabVessel().getLatestMaterialType().getDisplayName(),
                    bucketName));

        }

        batch.addReworks(reworkVessels);
        bucketEjb.moveFromBucketToBatch(reworkBucketEntries, batch);

        CreateFields.ProjectType projectType = null;
        CreateFields.IssueType issueType=null;

        if(batch.getLabBatchType() == LabBatch.LabBatchType.WORKFLOW) {
            WorkflowBucketDef bucketDef = getWorkflowBucketDef(bucketName);
            projectType = CreateFields.ProjectType.fromKeyPrefix(bucketDef.getBatchJiraProjectType());
            issueType= CreateFields.IssueType.valueOf(bucketDef.getBatchJiraIssueType());
        }

        AbstractBatchJiraFieldFactory fieldBuilder = AbstractBatchJiraFieldFactory
                .getInstance(projectType, batch, productOrderDao);
        if (projectType == null) {
            projectType = fieldBuilder.getProjectType();
        }

        if (StringUtils.isBlank(batch.getBatchDescription())) {
            batch.setBatchDescription(fieldBuilder.generateDescription());
        }

        Map<String, CustomFieldDefinition> requiredFields =
                jiraService.getRequiredFields(new CreateFields.Project(projectType), issueType);
        Map<String, CustomFieldDefinition> submissionFields = jiraService.getCustomFields();
        submissionFields.putAll(requiredFields);

        List<CustomField> batchJiraTicketFields = new ArrayList<>(fieldBuilder.getCustomFields(submissionFields));

        verifyAllowedValues(batchJiraTicketFields, messageReporter);
        JiraIssue jiraIssue = jiraService.getIssue(batch.getJiraTicket().getTicketName());
        jiraIssue.addWatchers(watchers);
        jiraIssue.addComment(commentString.toString());
        jiraIssue.updateIssue(batchJiraTicketFields);

        //link the JIRA tickets for the batch created to the pdo batches.
        for (String pdoKey : pdoKeys) {
            linkJiraBatchToTicket(pdoKey, batch);
        }

    }

    /**
     * Finds in the controls in a scan of a new LCSET rack.
     * @param lcsetName LCSET-1234
     * @param rackScan map from rack position to barcode
     * @param messageCollection errors returned to ActionBean
     * @return list of control barcodes
     */
    public List<String> findControlsInRackScan(String lcsetName, Map<String, String> rackScan,
            MessageCollection messageCollection) {
        List<String> controlBarcodes = new ArrayList<>();

        Map<String, LabVessel> mapBarcodeToTube = tubeDao.findByBarcodes(new ArrayList<>(rackScan.values()));
        List<Control> controls = controlDao.findAllActive();
        List<String> controlAliases = new ArrayList<>();
        for (Control control : controls) {
            controlAliases.add(control.getCollaboratorParticipantId());
        }

        List<String> sampleNames = new ArrayList<>();
        for (Map.Entry<String, String> positionBarcodeEntry : rackScan.entrySet()) {
            LabVessel barcodedTube = mapBarcodeToTube.get(positionBarcodeEntry.getValue());
            if (barcodedTube == null) {
                messageCollection.addError("Failed to find tube " + positionBarcodeEntry.getValue());
            } else {
                Set<SampleInstanceV2> sampleInstances = barcodedTube.getSampleInstancesV2();
                if (sampleInstances.size() == 1) {
                    SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
                    sampleNames.add(sampleInstance.getEarliestMercurySampleName());
                } else {
                    messageCollection.addError("Multiple samples in " + barcodedTube.getLabel());
                }
            }
        }
        Map<String, SampleData> mapSampleNameToData = sampleDataFetcher.fetchSampleData(sampleNames);

        for (Map.Entry<String, String> positionBarcodeEntry : rackScan.entrySet()) {
            LabVessel barcodedTube = mapBarcodeToTube.get(positionBarcodeEntry.getValue());
            if (barcodedTube != null) {
                Set<SampleInstanceV2> sampleInstances = barcodedTube.getSampleInstancesV2();
                if (sampleInstances.size() == 1) {
                    SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
                    SampleData sampleData = mapSampleNameToData.get(sampleInstance.getEarliestMercurySampleName());
                    boolean found = false;
                    for (LabBatch labBatch : sampleInstance.getAllWorkflowBatches()) {
                        if (labBatch.getBatchName().equals(lcsetName)) {
                            found = true;
                        }
                    }
                    if (controlAliases.contains(sampleData.getCollaboratorParticipantId())) {
                        if (found) {
                            messageCollection.addWarning(barcodedTube.getLabel() +  " is already in this LCSET");
                        } else {
                            controlBarcodes.add(barcodedTube.getLabel());
                        }
                    } else {
                        if (!found) {
                            messageCollection.addError(barcodedTube.getLabel() + " is not in this LCSET");
                        }
                    }
                }
            }
        }
        return controlBarcodes;
    }

    /**
     * Adds controls to an LCSET, without bucket entries.
     * @param lcsetName LCSET-1234
     * @param controlBarcodes list of barcodes that was confirmed by the user
     */
    public void addControlsToLcset(String lcsetName, List<String> controlBarcodes) {
        LabBatch lcset = labBatchDao.findByName(lcsetName);
        Map<String, LabVessel> mapBarcodeToTube = tubeDao.findByBarcodes(controlBarcodes);
        for (Map.Entry<String, LabVessel> stringBarcodedTubeEntry : mapBarcodeToTube.entrySet()) {
            LabVessel barcodedTube = stringBarcodedTubeEntry.getValue();
            lcset.addLabVessel(barcodedTube);
        }
    }

    /*
       To Support DBFree Tests
    */
    @Inject
    public void setLabBatchDao(LabBatchDao labBatchDao) {
        this.labBatchDao = labBatchDao;
    }

    @Inject
    public void setJiraService(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    @Inject
    public void setJiraTicketDao(JiraTicketDao jiraTicketDao) {
        this.jiraTicketDao = jiraTicketDao;
    }

    @Inject
    public void setTubeDao(LabVesselDao tubeDao) {
        this.tubeDao = tubeDao;
    }

    @Inject
    public void setBucketDao(BucketDao bucketDao) {
        this.bucketDao = bucketDao;
    }

    @Inject
    public void setBucketEjb(BucketEjb bucketEjb) {
        this.bucketEjb = bucketEjb;
    }

    @Inject
    public void setBucketEntryDao(BucketEntryDao bucketEntryDao) {
        this.bucketEntryDao = bucketEntryDao;
    }

    @Inject
    public void setProductOrderDao(ProductOrderDao productOrderDao) {
        this.productOrderDao = productOrderDao;
    }

    @Inject
    public void setSampleDataFetcher(SampleDataFetcher sampleDataFetcher) {
        this.sampleDataFetcher = sampleDataFetcher;
    }

    @Inject
    public void setControlDao(ControlDao controlDao) {
        this.controlDao = controlDao;
    }

    @Inject
    public void setWorkflowLoader(WorkflowLoader workflowLoader) {
        this.workflowLoader = workflowLoader;
    }

}
