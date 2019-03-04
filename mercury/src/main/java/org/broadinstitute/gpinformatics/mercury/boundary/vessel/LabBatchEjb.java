package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.map.LazyMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.BSPExportsService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.IsExported;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.mercury.BSPRestClient;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.AbstractBatchJiraFieldFactory;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationDto;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationUtils;
import org.broadinstitute.gpinformatics.mercury.presentation.run.FctDto;
import org.broadinstitute.gpinformatics.mercury.presentation.workflow.CreateFctDto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.BSPRestSender.BSP_CONTAINER_UPDATE_LAYOUT;
import static org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationActionBean.CONTROLS;

/**
 * Encapsulates the business logic related to {@link LabBatch}s.  This includes the creation
 * of a new batch entity and saving that to JIRA.
 */
@Stateful
@RequestScoped
public class LabBatchEjb {
    public static final String PARTIAL_FCT_MESSAGE = "{0} makes partially filled FCT with {1} empty lanes.";
    public static final String SPLIT_DESIGNATION_MESSAGE =
            "{0} designation for {1} got partially allocated. A newly created designation" +
            " now represents the unallocated lanes.";
    public static final String DESIGNATION_ERROR_MSG = "Designated tube {0} has invalid ";

    private static final Log logger = LogFactory.getLog(LabBatchEjb.class);

    private LabBatchDao labBatchDao;

    private JiraService jiraService;

    private LabVesselDao tubeDao;

    private BarcodedTubeDao barcodedTubeDao;

    private BucketEntryDao bucketEntryDao;

    private BucketEjb bucketEjb;

    private ProductOrderDao productOrderDao;

    private ProductDao productDao;

    private SampleDataFetcher sampleDataFetcher;

    private ControlDao controlDao;

    private WorkflowConfig workflowConfig;

    private LabVesselDao labVesselDao;

    private FlowcellDesignationEjb flowcellDesignationEjb;

    private BSPRestClient bspRestClient;

    private BSPExportsService bspExportsService;

    private static final VesselPosition[] VESSEL_POSITIONS = {VesselPosition.LANE1, VesselPosition.LANE2,
            VesselPosition.LANE3, VesselPosition.LANE4, VesselPosition.LANE5, VesselPosition.LANE6,
            VesselPosition.LANE7, VesselPosition.LANE8};

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
     * @param bucketName           the bucket from which to remove
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
     * @param bucketName           the bucket from which to remove
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

        for (BucketEntry bucketEntry : bucketEntries) {
            vessels.add(bucketEntry.getLabVessel());
            String tubeBarcode = bucketEntry.getLabVessel().getLabel();
            tubeBarcodeCounts.put(tubeBarcode, tubeBarcodeCounts.get(tubeBarcode) + 1);
            pdoKeys.add(bucketEntry.getProductOrder().getBusinessKey());
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

        createJiraTicket(workflowName, username, bucketName, reporter, watchers, pdoKeys, batch);

        return batch;
    }

    public void createJiraTicket(@Nonnull String workflowName, @Nonnull String username, String bucketName,
            @Nonnull MessageReporter reporter, List<String> watchers, Set<String> pdoKeys, LabBatch batch) {
        Pair<String, String> projectAndIssue = getProjectAndIssueTypes(bucketName,workflowName);

        CreateFields.IssueType issueType = CreateFields.IssueType.valueOf(projectAndIssue.getRight());

        batchToJira(username, null, batch, issueType,
                CreateFields.ProjectType.fromKeyPrefix(projectAndIssue.getLeft()), reporter, watchers);

        //link the JIRA tickets for the batch created to the pdo batches.
        for (String pdoKey : pdoKeys) {
            linkJiraBatchToTicket(pdoKey, batch);
        }
    }

    /**
     * This method returns the Jira Project and Issue Type to be used when a bucket entry goes into
     * a lab batch. Prefers the values from the bucket def if they are defined there; otherwise uses
     * values found on the workflow def that was selected by the user.
     */
    private Pair<String, String> getProjectAndIssueTypes(String bucketName, String workflowName) {
        String workflowIssueType = null;
        String projectType = null;
        ProductWorkflowDefVersion workflowVersion =
                workflowConfig.getWorkflowByName(workflowName).getEffectiveVersion();

        for (WorkflowBucketDef bucket : workflowVersion.getCreationBuckets()) {
            if (bucketName.equals(bucket.getName())) {
                workflowIssueType = StringUtils.isNotBlank(bucket.getBatchJiraIssueType()) ?
                        bucket.getBatchJiraIssueType() : workflowVersion.getProductWorkflowDefBatchJiraIssueType();
                projectType = StringUtils.isNotBlank(bucket.getBatchJiraProjectType()) ?
                        bucket.getBatchJiraProjectType() : workflowVersion.getProductWorkflowDefBatchJiraProjectType();
                break;
            }
        }
        return Pair.of(projectType, workflowIssueType);
    }

     /**
     * This method extracts all necessary information from the given batch object and creates (if necessary) and
     * associates a JIRA ticket that will represent this batch.
     *
     * @param reporter    The username of the person that is attempting to create the batch
     * @param jiraTicket  Optional parameter that represents an existing Jira Ticket that refers to this batch
     * @param newBatch    The source of the Batch information that will assist in populating the Jira Ticket
     * @param issueType   The type of issue to create in JIRA for this lab batch
     * @param projectType JIRA project
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
     * @param projectType JIRA project
     */
    public void batchToJira(String reporter, @Nullable String jiraTicket, LabBatch newBatch,
                            @Nonnull CreateFields.IssueType issueType, CreateFields.ProjectType projectType,
                            @Nonnull MessageReporter messageReporter, List<String> watchers) {
        try {
            if (issueType == null) {
                throw new InformaticsServiceException("JIRA issue type must be specified");
            }

            AbstractBatchJiraFieldFactory fieldBuilder = AbstractBatchJiraFieldFactory
                    .getInstance(projectType, newBatch, productOrderDao, workflowConfig);
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
                         + " to parent batch " + parentTicket,
                         ioe);
        }
    }

    /**
     * This method adds bucket entries, from PDO creation or rework, to an existing lab batch.
     *
     * @param businessKey    the business key for the lab batch we are adding samples to
     * @param bucketEntryIds the bucket entries whose vessel are being added to the batch
     * @param reworkEntries  the rework bucket entries whose vessels are being added to the batch
     * @param removeBucketEntryIds bucket entries to remove from the batch
     * @param bucketName     bucket to add to
     * @param messageReporter reference to action bean
     * @throws IOException This exception is thrown when the JIRA service can not be contacted.
     */
    public void updateLabBatch(String businessKey, List<Long> bucketEntryIds, List<Long> reworkEntries,
            List<Long> removeBucketEntryIds, String bucketName, MessageReporter messageReporter, List<String> watchers)
            throws IOException, ValidationException {
        LabBatch batch = labBatchDao.findByBusinessKey(businessKey);
        if (batch == null) {
            throw new ValidationException(String.format("Batch '%s' does not exist.", businessKey));
        }
        Set<String> pdoKeys = new HashSet<>();
        StringBuilder commentString = new StringBuilder();
        List<BucketEntry> bucketEntries = bucketEntryDao.findByIds(bucketEntryIds);
        Set<LabVessel> labVessels = new HashSet<>();

        for (BucketEntry bucketEntry : bucketEntries) {
            labVessels.add(bucketEntry.getLabVessel());
            pdoKeys.add(bucketEntry.getProductOrder().getBusinessKey());
            bucketEntry.getBucket().removeEntry(bucketEntry);
            String sampleName = getSample(bucketEntry);
            commentString.append(String.format("Added vessel *%s / %s* with material type *%s* from *%s*.\n",
                    bucketEntry.getLabVessel().getLabel(), sampleName,
                    bucketEntry.getLabVessel().getLatestMaterialType().getDisplayName(), bucketName));
        }

        batch.addLabVessels(labVessels);
        bucketEjb.moveFromBucketToBatch(bucketEntries, batch);

        List<BucketEntry> reworkBucketEntries = bucketEntryDao.findByIds(reworkEntries);
        Set<LabVessel> reworkVessels = new HashSet<>();

        if (!reworkBucketEntries.isEmpty()) {
            commentString.append("\n\n");
        }
        for (BucketEntry entry : reworkBucketEntries) {
            reworkVessels.add(entry.getLabVessel());
            pdoKeys.add(entry.getProductOrder().getBusinessKey());
            entry.getBucket().removeEntry(entry);
            String sampleName = getSample(entry);
            commentString.append(String.format("Added rework for vessel %s / %s with material type %s to %s.\n",
                    entry.getLabVessel().getLabel(), sampleName,
                    entry.getLabVessel().getLatestMaterialType().getDisplayName(), bucketName));

        }

        batch.addReworks(reworkVessels);
        bucketEjb.moveFromBucketToBatch(reworkBucketEntries, batch);

        List<BucketEntry> removeBucketEntries = bucketEntryDao.findByIds(removeBucketEntryIds);
        for (BucketEntry removeBucketEntry : removeBucketEntries) {
            removeBucketEntry.getLabVessel().removeFromBatch(removeBucketEntry.getLabBatch());
            batch.removeBucketEntry(removeBucketEntry);
            String sampleName = getSample(removeBucketEntry);
            commentString.append(String.format("Removed vessel *%s / %s* with material type *%s* from *%s*.\n",
                    removeBucketEntry.getLabVessel().getLabel(), sampleName,
                    removeBucketEntry.getLabVessel().getLatestMaterialType().getDisplayName(), bucketName));
        }

        CreateFields.ProjectType projectType = null;
        CreateFields.IssueType issueType=null;

        if(batch.getLabBatchType() == LabBatch.LabBatchType.WORKFLOW) {
            Pair<String, String> projectAndIssue = getProjectAndIssueTypes(bucketName, batch.getWorkflowName());
            projectType = CreateFields.ProjectType.fromKeyPrefix(projectAndIssue.getLeft());
            issueType= CreateFields.IssueType.valueOf(projectAndIssue.getRight());
        }

        AbstractBatchJiraFieldFactory fieldBuilder = AbstractBatchJiraFieldFactory
                .getInstance(projectType, batch, productOrderDao, workflowConfig);
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
        // todo jmt send email with commentString to jiraIssue.getReporter()
        jiraIssue.addWatchers(watchers);
        jiraIssue.addComment(commentString.toString());
        jiraIssue.updateIssue(batchJiraTicketFields);

        //link the JIRA tickets for the batch created to the pdo batches.
        for (String pdoKey : pdoKeys) {
            linkJiraBatchToTicket(pdoKey, batch);
        }

    }

    private String getSample(BucketEntry entry) {
        String sampleName = null;
        for (SampleInstanceV2 sampleInstanceV2 : entry.getLabVessel().getSampleInstancesV2()) {
            sampleName = sampleInstanceV2.getNearestMercurySampleName();
            if (sampleName != null) {
                break;
            }
        }
        return sampleName;
    }

    /**
     * Returned by validateRackScan method.
     */
    public static class ValidateRackScanReturn {
        private List<LabVessel> controlTubes;
        private List<LabVessel> addTubes;
        private List<LabVessel> removeTubes;

        ValidateRackScanReturn(List<LabVessel> controlTubes, List<LabVessel> addTubes,
                List<LabVessel> removeTubes) {
            this.controlTubes = controlTubes;
            this.addTubes = addTubes;
            this.removeTubes = removeTubes;
        }

        public List<LabVessel> getControlTubes() {
            return controlTubes;
        }

        public List<LabVessel> getAddTubes() {
            return addTubes;
        }

        public List<LabVessel> getRemoveTubes() {
            return removeTubes;
        }
    }

    public ValidateRackScanReturn validateTypedControls(String labBatchName, List<String> parsedControls,
                                      MessageCollection messageCollection) {
        LabBatch labBatch = labBatchDao.findByBusinessKey(labBatchName);
        if (labBatch == null) {
            messageCollection.addError("Failed to find " + labBatchName);
            return new ValidateRackScanReturn(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        if (labBatch.getBatchName().startsWith("LCSET")) {
            messageCollection.addError("LCSETs must be rack scanned to add controls.");
        }
        if (!labBatch.getWorkflowName().equals(Workflow.DNA_RNA_EXTRACTION_STOOL)) {
            messageCollection.addError("Only available for DNA and RNA from Stool");
        }
        Map<String, LabVessel> mapBarcodeToTube = tubeDao.findByBarcodes(parsedControls);
        List<Control> controls = controlDao.findAllActive();
        List<String> controlAliases = new ArrayList<>();
        for (Control control : controls) {
            controlAliases.add(control.getCollaboratorParticipantId());
        }

        List<LabVessel> startingVessels = labBatch.getLabBatchStartingVessels().stream()
                .map(LabBatchStartingVessel::getLabVessel)
                .collect(Collectors.toList());
        List<LabVessel> controlsInBatch = mapBarcodeToTube.values().stream()
                .filter(startingVessels::contains)
                .collect(Collectors.toList());
        if (!controlsInBatch.isEmpty()) {
            for (LabVessel labVessel: controlsInBatch) {
                messageCollection.addError(labVessel.getLabel() + " already in lab batch.");
            }
        }

        List<String> sampleNames = new ArrayList<>();
        Map<String, GetSampleDetails.SampleInfo> mapBarcodeToSampleInfo = new HashMap<>();
        for (Map.Entry<String,LabVessel> entry: mapBarcodeToTube.entrySet()) {
            LabVessel barcodedTube = entry.getValue();
            if (barcodedTube == null) {
                messageCollection.addError("Failed to find tube " + entry.getKey());
            } else {
                Set<SampleInstanceV2> sampleInstances = barcodedTube.getSampleInstancesV2();
                buildSampleNamesFromInstance(sampleInstances, barcodedTube, sampleNames, mapBarcodeToSampleInfo,
                        messageCollection);
            }
        }

        Map<String, SampleData> mapSampleNameToData = sampleDataFetcher.fetchSampleData(sampleNames);

        // Check each tube to determine whether it's a control, and whether it needs to be added to the LCSET.
        List<LabVessel> controlTubes = new ArrayList<>();
        List<LabVessel> addTubes = new ArrayList<>();
        Set<BucketEntry> bucketEntries = new HashSet<>();
        boolean addAndRemoveSamples = false;
        for (Map.Entry<String, LabVessel> entry : mapBarcodeToTube.entrySet()) {
            LabVessel barcodedTube = entry.getValue();
            addAndRemoveSamples = buildSamplesToAddAndRemove(labBatchName, controlAliases, barcodedTube, controlTubes,
                    addTubes, bucketEntries, mapBarcodeToSampleInfo, mapSampleNameToData, messageCollection, addAndRemoveSamples);
        }

        if (messageCollection.hasErrors()) {
            return new ValidateRackScanReturn(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }

        return new ValidateRackScanReturn(controlTubes, addTubes, null);
    }

    /**
     * Finds the controls in a scan of a new LCSET rack.  Also find tubes that need to be added to and removed
     * from the LCSET.
     * @param lcsetName LCSET-1234
     * @param rackScan map from rack position to barcode
     * @param messageCollection errors returned to ActionBean
     * @return tubes segregated by action
     */
    public ValidateRackScanReturn validateRackScan(String lcsetName, Map<String, String> rackScan,
            MessageCollection messageCollection) {

        Map<String, LabVessel> mapBarcodeToTube = tubeDao.findByBarcodes(new ArrayList<>(rackScan.values()));
        List<Control> controls = controlDao.findAllActive();
        List<String> controlAliases = new ArrayList<>();
        for (Control control : controls) {
            controlAliases.add(control.getCollaboratorParticipantId());
        }

        // We need the collaborator participant IDs, to know if each tube is a control
        List<String> sampleNames = new ArrayList<>();
        Map<String, GetSampleDetails.SampleInfo> mapBarcodeToSampleInfo = new HashMap<>();
        for (Map.Entry<String, String> positionBarcodeEntry : rackScan.entrySet()) {
            LabVessel barcodedTube = mapBarcodeToTube.get(positionBarcodeEntry.getValue());
            if (barcodedTube == null) {
                messageCollection.addError("Failed to find tube " + positionBarcodeEntry.getValue());
            } else {
                Set<SampleInstanceV2> sampleInstances = barcodedTube.getSampleInstancesV2();
                buildSampleNamesFromInstance(sampleInstances, barcodedTube, sampleNames, mapBarcodeToSampleInfo,
                        messageCollection);
            }
        }
        Map<String, SampleData> mapSampleNameToData = sampleDataFetcher.fetchSampleData(sampleNames);

        // Check each tube to determine whether it's a control, and whether it needs to be added to the LCSET.
        List<LabVessel> controlTubes = new ArrayList<>();
        List<LabVessel> addTubes = new ArrayList<>();
        Set<BucketEntry> bucketEntries = new HashSet<>();
        boolean addAndRemoveSamples = false;
        for (Map.Entry<String, String> positionBarcodeEntry : rackScan.entrySet()) {
            LabVessel barcodedTube = mapBarcodeToTube.get(positionBarcodeEntry.getValue());
            addAndRemoveSamples = buildSamplesToAddAndRemove(lcsetName, controlAliases, barcodedTube, controlTubes, addTubes,
                    bucketEntries, mapBarcodeToSampleInfo, mapSampleNameToData, messageCollection, addAndRemoveSamples);
        }

        // Any tubes that are in the LCSET, but not in the scan, will need to be removed
        List<LabVessel> removeTubes = new ArrayList<>();
        if (addAndRemoveSamples) {
            LabBatch labBatch = labBatchDao.findByBusinessKey(lcsetName);
            if (labBatch == null) {
                messageCollection.addError("Failed to find " + lcsetName);
            } else {
                for (BucketEntry bucketEntry : labBatch.getBucketEntries()) {
                    if (!bucketEntries.contains(bucketEntry)) {
                        removeTubes.add(bucketEntry.getLabVessel());
                    }
                }
            }
        }

        return new ValidateRackScanReturn(controlTubes, addTubes, removeTubes);
    }

    private void buildSampleNamesFromInstance(Set<SampleInstanceV2> sampleInstances, LabVessel barcodedTube, List<String> sampleNames,
                      Map<String, GetSampleDetails.SampleInfo> mapBarcodeToSampleInfo, MessageCollection messageCollection) {
        if (sampleInstances.size() == 1) {
            SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
            if (sampleInstance.getEarliestMercurySampleName() == null) {
                // Assume this is a control that has no history in Mercury, so fetch from BSP by barcode
                // todo jmt accumulate these and fetch in bulk?
                mapBarcodeToSampleInfo.putAll(sampleDataFetcher.fetchSampleDetailsByBarcode(
                        Collections.singletonList(sampleInstance.getInitialLabVessel().getLabel())));
                sampleNames.add(mapBarcodeToSampleInfo.get(sampleInstance.getInitialLabVessel().getLabel()).getSampleId());
            } else {
                sampleNames.add(sampleInstance.getEarliestMercurySampleName());
            }
        } else {
            messageCollection.addError("Multiple samples in " + barcodedTube.getLabel());
        }
    }

    private boolean buildSamplesToAddAndRemove(String lcsetName, List<String> controlAliases, LabVessel barcodedTube,
                                               List<LabVessel> controlTubes, List<LabVessel> addTubes, Set<BucketEntry> bucketEntries,
                                               Map<String, GetSampleDetails.SampleInfo> mapBarcodeToSampleInfo,
                                               Map<String, SampleData> mapSampleNameToData, MessageCollection messageCollection,
                                               boolean addAndRemoveSamples) {
        if (barcodedTube != null) {
            Set<SampleInstanceV2> sampleInstances = barcodedTube.getSampleInstancesV2();
            if (sampleInstances.size() == 1) {
                SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
                String earliestMercurySampleName = sampleInstance.getEarliestMercurySampleName();
                if (earliestMercurySampleName == null) {
                    // Assume this is a control that has no history in Mercury, so lookup by barcode
                    earliestMercurySampleName = mapBarcodeToSampleInfo.get(sampleInstance.getInitialLabVessel().getLabel()).getSampleId();
                }
                SampleData sampleData = mapSampleNameToData.get(earliestMercurySampleName);
                boolean found = false;
                for (BucketEntry bucketEntry : sampleInstance.getAllBucketEntries()) {
                    if (Objects.equals(bucketEntry.getLabBatch().getBatchName(), lcsetName)) {
                        bucketEntries.add(bucketEntry);
                        // Exome Express currently does strange things with multiple LCSETs at shearing, so
                        // limit this logic to WGS.
                        if (Objects.equals(bucketEntry.getProductOrder().getProduct().getAggregationDataType(),
                                Aggregation.DATA_TYPE_WGS)) {
                            addAndRemoveSamples = true;
                        }
                        found = true;
                        break;
                    }
                }
                // Check pending buckets for XTR tickets
                if (!found) {
                    for (BucketEntry bucketEntry : sampleInstance.getPendingBucketEntries()) {
                        bucketEntries.add(bucketEntry);
                        if (bucketEntry.getProductOrder().getProduct().getWorkflowName() != null && bucketEntry
                                .getProductOrder().getProduct().getWorkflowName()
                                .equals(Workflow.DNA_RNA_EXTRACTION_STOOL)) {
                            addAndRemoveSamples = true;
                        }
                        break;
                    }
                }
                if (controlAliases.contains(sampleData.getCollaboratorParticipantId())) {
                    if (found) {
                        messageCollection.addWarning(barcodedTube.getLabel() +  " is already in this Lab Batch");
                    } else {
                        controlTubes.add(barcodedTube);
                    }
                } else {
                    if (!found) {
                        if (addAndRemoveSamples) {
                            addTubes.add(barcodedTube);
                        } else {
                            messageCollection.addError(barcodedTube.getLabel() + " is not in this Lab Batch");
                        }
                    }
                }
            }
        }

        return addAndRemoveSamples;
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

    /**
     * Updates an LCSET after it is scanned on the LCSET Controls page.  Actions are:
     * <ul>
     * <li>add control tubes</li>
     * <li>add sample tubes (e.g. clinical samples that displaced research samples)</li>
     * <li>remove sample tubes (e.g. research samples displaced by clinical samples)</li>
     * <li>update rack layout in BSP (controls are not added through automation)</li>
     * <li>auto-export from BSP to Mercury</li>
     * </ul>
     * @param lcsetName name of batch to update
     * @param controlBarcodes positive and negative control tubes
     * @param messageReporter action bean
     * @param addBarcodes tubes added to the rack since the LCSET was created
     * @param removeBarcodes tubes removed from the rack since the LCSET was created
     */
    public Map<String, BarcodedTube> updateLcsetFromScan(String lcsetName, List<String> controlBarcodes,
            MessageReporter messageReporter, Collection<String> addBarcodes, List<String> removeBarcodes,
            List<String> allBarcodes) {

        // Reflect addition of control tubes by re-array
        addControlsToLcset(lcsetName, controlBarcodes);

        // Add to batch
        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(allBarcodes);
        mapBarcodeToTube.putAll(barcodedTubeDao.findByBarcodes(removeBarcodes));
        List<Long> bucketEntryIds = new ArrayList<>();
        String bucketName = null;
        for (String addBarcode : addBarcodes) {
            BarcodedTube barcodedTube = mapBarcodeToTube.get(addBarcode);
            SampleInstanceV2 sampleInstance = barcodedTube.getSampleInstancesV2().iterator().next();
            for (BucketEntry bucketEntry : sampleInstance.getPendingBucketEntries()) {
                if (bucketEntry.getLabBatch() == null) {
                    bucketEntryIds.add(bucketEntry.getBucketEntryId());
                    bucketName = bucketEntry.getBucket().getBucketDefinitionName();
                }
            }
        }
        if (addBarcodes.size() != bucketEntryIds.size()) {
            throw new RuntimeException("Expected " + addBarcodes.size() + " add bucket entries, " + " found " +
                    bucketEntryIds.size());
        }

        List<Long> removeBucketEntryIds = new ArrayList<>();
        for (String removeBarcode : removeBarcodes) {
            BarcodedTube barcodedTube = mapBarcodeToTube.get(removeBarcode);
            SampleInstanceV2 sampleInstance = barcodedTube.getSampleInstancesV2().iterator().next();
            for (BucketEntry bucketEntry : sampleInstance.getAllBucketEntries()) {
                if (bucketEntry.getLabBatch().getBusinessKey().equals(lcsetName)) {
                    removeBucketEntryIds.add(bucketEntry.getBucketEntryId());
                    bucketName = bucketEntry.getBucket().getBucketDefinitionName();
                }
            }
        }
        if (removeBarcodes.size() != removeBucketEntryIds.size()) {
            throw new RuntimeException("Expected " + removeBarcodes.size() + " remove bucket entries, " + " found " +
                    removeBucketEntryIds.size());
        }

        if (!bucketEntryIds.isEmpty() || !removeBucketEntryIds.isEmpty()) {
            try {
                updateLabBatch(lcsetName, bucketEntryIds, Collections.<Long>emptyList(), removeBucketEntryIds, bucketName,
                        messageReporter, Collections.<String>emptyList());
            } catch (IOException | ValidationException e) {
                throw new RuntimeException(e);
            }
        }
        return mapBarcodeToTube;
    }

    /**
     * Exports an LCSET after it is scanned on the LCSET Controls page.  Actions are:
     * <ul>
     * <li>update rack layout in BSP (controls are not added through automation)</li>
     * <li>auto-export from BSP to Mercury</li>
     * </ul>
     * @param mapBarcodeToTube map from position to barcode
     * @param rackBarcode needed to update layout in BSP
     * @param rackScan map from position to barcode
     * @param userBean logged in user
     * @param messageReporter action bean
     */
    public void exportRack(Map<String, BarcodedTube> mapBarcodeToTube, String rackBarcode,
                                Map<String, String> rackScan, UserBean userBean, MessageReporter messageReporter) {
        List<LabVessel> bspTubes = new ArrayList<>();
        for (String barcode : rackScan.values()) {
            BarcodedTube barcodedTube = mapBarcodeToTube.get(barcode);
            SampleInstanceV2 sampleInstanceV2 = barcodedTube.getSampleInstancesV2().iterator().next();
            MercurySample mercurySample = sampleInstanceV2.getRootOrEarliestMercurySample();
            if (mercurySample == null || mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP) {
                bspTubes.add(barcodedTube);
            }
        }

        int needsExport = 0;
        if (!bspTubes.isEmpty()) {
            IsExported.ExportResults exportResults = bspExportsService.findExportDestinations(bspTubes);
            for (IsExported.ExportResult exportResult : exportResults.getExportResult()) {
                if (exportResult.isError()) {
                    continue;
                }
                Set<IsExported.ExternalSystem> externalSystems = exportResult.getExportDestinations();
                if (CollectionUtils.isEmpty(externalSystems) || !externalSystems.contains(IsExported.ExternalSystem.Mercury)) {
                    needsExport++;
                }
            }
        }

        if (needsExport > 0) {
            if (StringUtils.isEmpty(rackBarcode)) {
                throw new RuntimeException("Rack barcode is required to auto-export");
            }
            // Update rack in BSP, to add control
            WebTarget webTarget = bspRestClient.getWebResource(bspRestClient.getUrl(BSP_CONTAINER_UPDATE_LAYOUT));
            PlateTransferEventType plateTransferEventType = new PlateTransferEventType();
            PositionMapType positionMap = new PositionMapType();
            positionMap.setBarcode(rackBarcode);
            plateTransferEventType.setPositionMap(positionMap);
            // BSP requires an entry for every position, with an empty string barcode if no tube
            for (VesselPosition vesselPosition : RackOfTubes.RackType.Matrix96.getVesselGeometry().getVesselPositions()) {
                String scanBarcode = rackScan.get(vesselPosition.name());
                ReceptacleType receptacleType = new ReceptacleType();
                receptacleType.setPosition(vesselPosition.name());
                String receptacleTypeBarcode = "";
                if (scanBarcode != null) {
                    BarcodedTube barcodedTube = mapBarcodeToTube.get(scanBarcode);
                    SampleInstanceV2 sampleInstanceV2 = barcodedTube.getSampleInstancesV2().iterator().next();
                    MercurySample mercurySample = sampleInstanceV2.getRootOrEarliestMercurySample();
                    if (mercurySample == null || mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP) {
                        receptacleTypeBarcode = scanBarcode;
                    }
                }
                receptacleType.setBarcode(receptacleTypeBarcode);
                positionMap.getReceptacle().add(receptacleType);
            }

            PlateType plateType = new PlateType();
            plateType.setBarcode(rackBarcode);
            plateType.setPhysType(RackOfTubes.RackType.Matrix96.getDisplayName());
            plateTransferEventType.setPlate(plateType);
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.getPlateTransferEvent().add(plateTransferEventType);
            Response response = webTarget.request(MediaType.TEXT_PLAIN).post(Entity.xml(bettaLIMSMessage));
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                bspExportsService.export(rackBarcode, userBean.getLoginUserName());
                response.close();
            } else {
                messageReporter.addMessage(response.readEntity(String.class));
                response.close();
                throw new RuntimeException("Failed to update layout in BSP.");
            }
        }
    }

    /**
     * Make FCT LabBatches and tickets, based on DTOs from the Create FCT Ticket web page.
     * @param createFctDtos information from web page
     * @param selectedFlowcellType holds number of lanes
     * @param userName for audit trail
     * @param messageReporter reference to the action bean
     * @return the list of created flowcell batches.
     */
    public List<LabBatch> makeFcts(final List<CreateFctDto> createFctDtos,
            IlluminaFlowcell.FlowcellType selectedFlowcellType, String userName, MessageReporter messageReporter) {
        // Checks for unallocated lanes.
        int unallocatedLanes = unallocatedLanes(createFctDtos, selectedFlowcellType);
        if (unallocatedLanes > 0) {
            throw new RuntimeException("Flowcells could not be fully filled (" +
                    (selectedFlowcellType.getVesselGeometry().getRowCount() - unallocatedLanes) + " empty lanes)");
        }

        // Map of loading tubes.
        Map<String, LabVessel> loadingVessels = labVesselDao.findByBarcodes(new ArrayList<String>() {{
            for (CreateFctDto dto : createFctDtos) {
                if (dto.getNumberLanes() > 0) {
                    add(dto.getBarcode());
                }
            }
        }});

        if (loadingVessels.isEmpty()) {
            messageReporter.addMessage("No lanes were selected.");
        } else {
            Pair<List<LabBatch>, List<CreateFctDto>> fctReturn = makeFctDaoFree(createFctDtos, loadingVessels,
                    Collections.<String, FlowcellDesignation>emptyMap(), selectedFlowcellType, false);

            List<LabBatch> fctBatches = fctReturn.getLeft();
            if (fctBatches.isEmpty()) {
                messageReporter.addMessage("No FCTs were created.");
            } else {
                StringBuilder createdBatchLinks = new StringBuilder("<ol>");
                for (LabBatch fctBatch : fctBatches) {
                    Set<String> lcsetNames = new HashSet<>(laneToLinkedLcsets(fctBatch).values());
                    if (CollectionUtils.isEmpty(lcsetNames)) {
                        throw new RuntimeException("Found no LCSETs to link to the FCT");
                    }
                    createLabBatch(fctBatch, userName, selectedFlowcellType.getIssueType(), messageReporter);
                    for (String lcset : lcsetNames) {
                        linkJiraBatchToTicket(lcset, fctBatch);
                    }
                    createdBatchLinks.append("<li><a target=\"JIRA\" href=\"");
                    createdBatchLinks.append(fctBatch.getJiraTicket().getBrowserUrl());
                    createdBatchLinks.append("\" class=\"external\" target=\"JIRA\">");
                    createdBatchLinks.append(fctBatch.getBusinessKey());
                    createdBatchLinks.append("</a></li>");
                }
                createdBatchLinks.append("</ol>");
                messageReporter.addMessage("Created {0} FCT tickets: {1}", fctBatches.size(),
                        createdBatchLinks.toString());
            }
            return fctBatches;
        }
        return Collections.emptyList();
    }

    /**
     * For FCT & MISEQ batch entities that were just created and still have the transient VesselToLanesInfo,
     * returns a map of lane to linked lcsets, which are the workflow batch lcsets of the flowcell loading tube.
     */
    public static Multimap<String, String> laneToLinkedLcsets(LabBatch fctBatch) {
        Multimap<String, String> map = HashMultimap.create();
        for (LabBatchStartingVessel batchStartingVessel : fctBatch.getLabBatchStartingVessels()) {
            map.put(batchStartingVessel.getVesselPosition().name(), batchStartingVessel.getLinkedLcset());
        }
        return map;
    }

    /**
     * Creates FCTs from a collection of designation dtos.
     *
     * @param uiDtos DTOs that represent designations, one per loading tube. If a dto is split it
     *               will be added to this list. Dto status is updated for those put in an FCT.
     * @param userName
     * @param messageReporter for action bean message display.
     * @param diversifySamples controls whether samples are bunched together on a flowcell or spread
     *                         across multiple flowcells.
     * @return Pair of FCT batch name and JIRA url.
     */
    public List<MutablePair<String, String>> makeFcts(final List<DesignationDto> uiDtos, String userName,
            MessageReporter messageReporter, boolean diversifySamples) {
        // Only uses the selected dtos.
        final List<DesignationDto> designationDtos = new ArrayList<>();
        for (DesignationDto designationDto : uiDtos) {
            if (designationDto.isSelected()) {
                designationDtos.add(designationDto);
            }
        }
        // Validates the designations. Any invalid one will stop all FCT creation.
        boolean hasError = false;
        for (DesignationDto designationDto : designationDtos) {
            designationDto.setGroupByRegulatoryDesignation(!isMixedFlowcellOk(designationDto));
            if (!isValidDto(designationDto, messageReporter)) {
                hasError = true;
            }
        }
        if (hasError) {
            return Collections.emptyList();
        }

        // Makes map of FlowcellDesignation and map of loading tubes.
        Map<String, FlowcellDesignation> flowcellDesignations = new HashMap<>();
        List<String> barcodes = new ArrayList<>();
        for (DesignationDto dto : designationDtos) {
            barcodes.add(dto.getBarcode());
            flowcellDesignations.put(dto.getBarcode(),
                    labVesselDao.findById(FlowcellDesignation.class, dto.getDesignationId()));
        }
        Map<String, LabVessel> loadingVessels = labVesselDao.findByBarcodes(barcodes);

        // Processes the dtos onto flowcell lanes, combining compatible designations where possible.
        // Each complete flowcell has its FCT pushed to JIRA, with parent-child JIRA links to LCSET(s).
        Pair<List<LabBatch>, List<DesignationDto>> fctReturn = makeFctDaoFree(designationDtos, loadingVessels,
                flowcellDesignations, null, diversifySamples);
        List<MutablePair<String, String>> fctUrls = new ArrayList<>();
        for (LabBatch fctBatch : fctReturn.getLeft()) {
            Set<String> lcsetNames = new HashSet<>(laneToLinkedLcsets(fctBatch).values());
            if (CollectionUtils.isEmpty(lcsetNames)) {
                throw new RuntimeException("Found no LCSETs to link to the FCT");
            }
            createLabBatch(fctBatch, userName, fctBatch.getFlowcellType().getIssueType(), messageReporter);
            for (String lcset : lcsetNames) {
                linkJiraBatchToTicket(lcset, fctBatch);
            }
            fctUrls.add(MutablePair.of(fctBatch.getBatchName(), fctBatch.getJiraTicket().getBrowserUrl()));
        }

        // Puts up a message about the per-group unallocated lane counts.
        Map<String, Integer> groupCount = new HashMap<>();
        Map<String, IlluminaFlowcell.FlowcellType> groupFlowcellType = new HashMap<>();
        for (DesignationDto dto : designationDtos) {
            if (dto.getAllocatedLanes() == dto.getNumberLanes().intValue()) {
                dto.setStatus(FlowcellDesignation.Status.IN_FCT);
            } else {
                String groupDescription = dtoGroupDescription(dto);
                Integer count = groupCount.get(groupDescription);
                groupCount.put(groupDescription, (count == null ? 0 : count.intValue()) + dto.getNumberLanes());
                groupFlowcellType.put(groupDescription, dto.getSequencerModel());
            }
        }
        for (String groupDescription : groupCount.keySet()) {
            int emptyLaneCount = groupFlowcellType.get(groupDescription).getVesselGeometry().getRowCount() -
                    groupCount.get(groupDescription);
            messageReporter.addMessage(MessageFormat.format(PARTIAL_FCT_MESSAGE, groupDescription, emptyLaneCount));
        }

        // Any new split dtos need to be added to the UI's dto list. There should only be one split
        // per designation grouping. Puts up a message about any split dtos.
        for (DesignationDto dtoSplit : fctReturn.getRight()) {
            dtoSplit.setStatus(FlowcellDesignation.Status.QUEUED);
            designationDtos.add(dtoSplit);
            uiDtos.add(dtoSplit);
            String groupDescription = dtoGroupDescription(dtoSplit);
            messageReporter.addMessage(MessageFormat.format(SPLIT_DESIGNATION_MESSAGE, 1, groupDescription));
        }

        // Persists all designations.
        for (DesignationDto designationDto : designationDtos) {
            designationDto.setSelected(true);
        }
        DesignationUtils.updateDesignationsAndDtos(designationDtos,
                EnumSet.allOf(FlowcellDesignation.Status.class), flowcellDesignationEjb);

        return fctUrls;
    }

    private boolean isValidDto(DesignationDto designationDto, MessageReporter messageReporter) {
        boolean isValid = true;
        String errorString = MessageFormat.format(DESIGNATION_ERROR_MSG, designationDto.getBarcode());

        if (designationDto.getStatus() == null || designationDto.getStatus() != FlowcellDesignation.Status.QUEUED) {
            errorString += (isValid ? "" : "and ") + "status (" + designationDto.getStatus() + ") ";
            isValid = false;
        }
        if (designationDto.getLoadingConc() == null || designationDto.getLoadingConc().doubleValue() <= 0) {
            errorString += (isValid ? "" : "and ") + "loading conc (" + designationDto.getLoadingConc() + ") ";
            isValid = false;
        }
        if (designationDto.getNumberLanes() == null || designationDto.getNumberLanes() <= 0) {
            errorString += (isValid ? "" : "and ") + "number of lanes (" + designationDto.getNumberLanes() + ") ";
            isValid = false;
        }
        if (designationDto.getReadLength() == null || designationDto.getReadLength() <= 0) {
            errorString += (isValid ? "" : "and ") + "read length (" + designationDto.getReadLength() + ") ";
            isValid = false;
        }
        if (designationDto.getSequencerModel() == null ||
            designationDto.getSequencerModel().getCreateFct() == IlluminaFlowcell.CreateFct.NO ||
            designationDto.getSequencerModel().getIssueType() == null) {
            errorString += (isValid ? "" : "and ") + "sequencer model (" +
                           (designationDto.getSequencerModel() == null ?
                                   "null" : designationDto.getSequencerModel().getDisplayName()) + ") ";
            isValid = false;
        }
        if (designationDto.getIndexType() == null) {
            errorString += (isValid ? "" : "and ") + "index type (null) ";
            isValid = false;
        }
        if (designationDto.getPairedEndRead() == null) {
            errorString += (isValid ? "" : "and ") + "paired end read (null) ";
            isValid = false;
        }
        if (designationDto.getPoolTest() == null) {
            errorString += (isValid ? "" : "and ") + "pool test (null) ";
            isValid = false;
        }
        if (StringUtils.isBlank(designationDto.getLcset())) {
            errorString += (isValid ? "" : "and ") + "lcset (null) ";
            isValid = false;
        }
        if (designationDto.getGroupByRegulatoryDesignation() &&
                !DesignationUtils.RESEARCH.equals(designationDto.getRegulatoryDesignation()) &&
                !DesignationUtils.CLINICAL.equals(designationDto.getRegulatoryDesignation())) {
            errorString += (isValid ? "" : "and ") +
                    "regulatory designation (" + designationDto.getRegulatoryDesignation() + ") ";
            isValid = false;
        }

        if (!isValid) {
            messageReporter.addMessage(errorString);
        }
        return isValid;
    }

    public boolean isMixedFlowcellOk(FctDto designationDto) {
        // Mixed flowcells are permitted for genomes
        boolean mixedFlowcellOk = false;
        for (String productName : designationDto.getProductNames()) {
            if (!productName.equals(CONTROLS)) {
                Product product = productDao.findByName(productName);
                if (Objects.equals(product.getAggregationDataType(), Aggregation.DATA_TYPE_WGS)) {
                    mixedFlowcellOk = true;
                    break;
                }
            }
        }
        return mixedFlowcellOk;
    }

    /** Returns the number of lanes that would not fit onto an even number of flowcell lanes. */
    public <DTO_TYPE extends FctDto> int unallocatedLanes(Collection<DTO_TYPE> dtos,
            IlluminaFlowcell.FlowcellType flowcellType) {

        int totalDtoLanes = 0;
        for (DTO_TYPE dto : dtos) {
            totalDtoLanes += dto.getNumberLanes();
        }
        int lanesPerFlowcell = flowcellType.getVesselGeometry().getRowCount();
        int unallocatedDtoLanes = totalDtoLanes % lanesPerFlowcell;
        return unallocatedDtoLanes;
    }

    /**
     * Allocates the designations to flowcells.
     *
     * A given lane only contains material from one designation (one loading tube). But a designation may
     * span multiple lanes and multiple flowcells, depending on the designation lane count.
     *
     * @param dtos the selected FctDtos, one per loading tube.
     * @param loadingTubes maps loading tube barcode to loading tube.
     * @param designations maps  loading tube barcode to FlowcellDesignation.
     * @param createFctFlowcellType  the type of flowcells to create. Expect this to be null for
     *                               DesignationDtos which have the flowcell type in the dto.
     * @param diversifySamples When false, allocates a sample's lanes contiguously on flowcell(s). When true,
     *                         attempts to allocate only one lane of a sample per flowcell, but allows a
     *                         second lane if necessary to make a complete flowcell.
     * @return  The list of the fct batches to be persisted and a list of split dtos.
     *   Fct batches will be in order of creation, which should put loading tubes on contiguous flowcell
     *   lanes across sequential fcts, provided the fcts get persisted in the order returned.
     *   Fct batch identity (i.e. equals, hashcode) is unstable (see GPLIM-4011).
     */
    public <DTO_TYPE extends FctDto> Pair<List<LabBatch>, List<DTO_TYPE>> makeFctDaoFree(List<DTO_TYPE> dtos,
            Map<String, LabVessel> loadingTubes, Map<String, FlowcellDesignation> designations,
            @Nullable IlluminaFlowcell.FlowcellType createFctFlowcellType, boolean diversifySamples) {

        // Makes compatible groups of dtos that can go together on the same flowcell. The dtos in each
        // group are ordered by priority, number of lanes, and barcode.
        List<List<DTO_TYPE>> dtoGroups = new ArrayList<>();
        dtos.stream().
                sorted(DesignationDto.BY_ALLOCATION_ORDER).
                forEach(dto -> {
                    // Finds the compatible group.
                    List<DTO_TYPE> group = dtoGroups.stream().filter(testGroup -> dto.isCompatible(testGroup)).
                            findFirst().orElse(null);
                    if (group == null) {
                        group = new ArrayList<>();
                        dtoGroups.add(group);
                    }
                    group.add(dto);
                });

        // For each group, makes an expanded list with each dto repeated times its lane count, in the
        // correct allocation order. The list size is only allowed to be a multiple of the flowcell size.
        // If the last few dto lanes don't fit on a complete flowcell are not put on the list.
        List<List<DTO_TYPE>> dtoLaneGroups = new ArrayList<>();
        for (List<DTO_TYPE> group : dtoGroups) {
            final IlluminaFlowcell.FlowcellType flowcellType =
                    OrmUtil.proxySafeIsInstance(group.get(0), DesignationDto.class) ?
                            OrmUtil.proxySafeCast(group.get(0), DesignationDto.class).getSequencerModel() :
                            createFctFlowcellType;
            final int flowcellLaneSize = flowcellType.getVesselGeometry().getRowCount();

            List<DTO_TYPE> dtoLanes = new ArrayList<>();
            if (!diversifySamples) {
                // Dtos are on contiguous lanes.
                // Calculates where the last last complete flowcell ends.
                int totalLaneCount = group.stream().mapToInt(DTO_TYPE::getNumberLanes).sum();
                totalLaneCount -= (totalLaneCount % flowcellLaneSize);
                for (DTO_TYPE dto : group) {
                    int i;
                    for (i = 0; i < Math.min(dto.getNumberLanes(), totalLaneCount); ++i) {
                        dtoLanes.add(dto);
                    }
                    totalLaneCount -= i;
                }
            } else {
                // Dto lanes are interspersed with the other dtos' lanes.
                final int MAX_DIVERSIFIED_SAMPLE_LANES = 2;
                List<DTO_TYPE> flowcellLanes = new ArrayList<>();
                boolean flowcellComplete;
                do {
                    flowcellComplete = false;
                    // Does two passes on the dto list to fill a flowcell, taking one lane of a dto on each pass.
                    for (int i = 0; !flowcellComplete && (i < MAX_DIVERSIFIED_SAMPLE_LANES); ++i) {
                        for (DTO_TYPE dto : group) {
                            // Skips the dto once it becomes fully allocated.
                            if (dto.getAllocatedLanes() < dto.getNumberLanes()) {
                                dto.setAllocatedLanes(dto.getAllocatedLanes() + 1);
                                flowcellLanes.add(dto);
                                // Only saves the dtos when they make a full flowcells.
                                if (flowcellLanes.size() == flowcellLaneSize) {
                                    dtoLanes.addAll(flowcellLanes);
                                    flowcellLanes.clear();
                                    flowcellComplete = true;
                                    break;
                                }
                            }
                        }
                    }
                } while (flowcellComplete);
            }

            if (!dtoLanes.isEmpty()) {
                dtoLaneGroups.add(dtoLanes);
            }
        }

        // Reverts the dto allocation counts back to zero.
        dtos.stream().forEach(dto -> dto.setAllocatedLanes(0));

        List<LabBatch> createdFcts = new ArrayList<>();
        // Iterates on each dto group and allocates flowcells.
        for (List<DTO_TYPE> group : dtoLaneGroups) {
            final IlluminaFlowcell.FlowcellType flowcellType =
                    OrmUtil.proxySafeIsInstance(group.get(0), DesignationDto.class) ?
                            OrmUtil.proxySafeCast(group.get(0), DesignationDto.class).getSequencerModel() :
                            createFctFlowcellType;
            final int flowcellLaneSize = flowcellType.getVesselGeometry().getRowCount();
            int laneIndex = 0;
            Map<String, LabBatch.VesselToLanesInfo> laneInfos = new HashMap<>();

            for (DTO_TYPE dto : group) {
                LabBatch.VesselToLanesInfo laneInfo = laneInfos.get(dto.getBarcode());
                if (laneInfo == null) {
                    laneInfo = new LabBatch.VesselToLanesInfo(new ArrayList<>(), dto.getLoadingConc(),
                            loadingTubes.get(dto.getBarcode()), dto.getLcset(), dto.getProduct(), new ArrayList<>());
                    laneInfos.put(dto.getBarcode(), laneInfo);
                }
                laneInfo.getLanes().add(VESSEL_POSITIONS[laneIndex++]);
                if (designations.containsKey(dto.getBarcode())) {
                    laneInfo.getDesignations().add(designations.get(dto.getBarcode()));
                }
                dto.setAllocatedLanes(dto.getAllocatedLanes() + 1);

                // When enough lanes exist for a complete flowcell, an FCT batch is made and lane info is reset.
                if (laneIndex == flowcellLaneSize) {
                    // The batch name will be overwritten by the FCT-ID from JIRA, but if it's not made unique at
                    // this point then there is a unique constraint violation.  Perhaps Hibernate does an insert
                    // then an update, it's not clear why.
                    LabBatch fctBatch = new LabBatch(dto.getBarcode() + " " + createdFcts.size(),
                            laneInfos.values().stream().collect(Collectors.toList()),
                            flowcellType.getBatchType(), flowcellType);
                    fctBatch.setBatchDescription(dto.getBarcode() + " FCT ticket ");
                    createdFcts.add(fctBatch);
                    laneInfos.clear();
                    laneIndex = 0;
                }
            }
            if (!laneInfos.isEmpty()) {
                throw new RuntimeException("Unexpected allocations for loading tubes " +
                        StringUtils.join(laneInfos.keySet(), ", "));
            }
        }

        // Splits a dto if it is partially allocated.
        List<DTO_TYPE> splitDtos = new ArrayList<>();
        for (DTO_TYPE dto : dtos) {
            if (dto.getAllocatedLanes() > 0 && dto.getAllocatedLanes() < dto.getNumberLanes()) {
                splitDtos.add((DTO_TYPE)dto.split());
            }
        }
        return Pair.of(createdFcts, splitDtos);
    }

    public static String dtoGroupDescription(DesignationDto dto) {
        return "FctGrouping{" + dto.getSequencerModel() + ", " + dto.calculateCycles() + " cycles, " +
                dto.getReadLength() + " readLength, " + dto.getIndexType() + " index" +
                (dto.getGroupByRegulatoryDesignation() ? ", " + dto.getRegulatoryDesignation() : "") + "}";
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
    public void setTubeDao(LabVesselDao tubeDao) {
        this.tubeDao = tubeDao;
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
    public void setProductDao(ProductDao productDao) {
        this.productDao = productDao;
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
    public void setWorkflowConfig(WorkflowConfig workflowConfig) {
        this.workflowConfig = workflowConfig;
    }

    @Inject
    public void setLabVesselDao(LabVesselDao labVesselDao) {
        this.labVesselDao = labVesselDao;
    }

    @Inject
    public void setFlowcellDesignationEjb(FlowcellDesignationEjb flowcellDesignationEjb) {
        this.flowcellDesignationEjb = flowcellDesignationEjb;
    }

    @Inject
    public void setBarcodedTubeDao(BarcodedTubeDao barcodedTubeDao) {
        this.barcodedTubeDao = barcodedTubeDao;
    }

    @Inject
    public void setBspRestClient(BSPRestClient bspRestClient) {
        this.bspRestClient = bspRestClient;
    }

    @Inject
    public void setBspExportsService(BSPExportsService bspExportsService) {
        this.bspExportsService = bspExportsService;
    }
}
