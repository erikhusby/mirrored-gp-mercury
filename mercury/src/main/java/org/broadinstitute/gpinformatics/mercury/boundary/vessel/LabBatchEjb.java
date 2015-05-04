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
import org.broadinstitute.gpinformatics.mercury.control.vessel.LCSetJiraFieldFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

    /**
     * Alternate create lab batch method to allow a user to define the vessels for use by their barcode.
     *
     * @param reporter       The username of the person that is attempting to create the batch
     * @param labVesselNames The plastic ware that the newly created lab batch will represent
     * @param batchName      The name for the batch being created. This can be an existing JIRA ticket id
     * @param labBatchType   The type of batch to create
     * @param issueType      The type of issue to create in JIRA for this lab batch
     *
     * @return The lab batch that was created.
     */
    public LabBatch createLabBatch(@Nonnull String reporter, @Nonnull Set<String> labVesselNames,
                                   @Nonnull String batchName, LabBatch.LabBatchType labBatchType,
                                   @Nonnull CreateFields.IssueType issueType) {
        Set<LabVessel> vesselsForBatch = new HashSet<>(labVesselNames.size());

        for (String currVesselLabel : labVesselNames) {
            vesselsForBatch.add(tubeDao.findByIdentifier(currVesselLabel));
        }

        return createLabBatch(vesselsForBatch, reporter, batchName, labBatchType, issueType);
    }

    /**
     * This method will, given a group of lab plastic ware, create a batch entity and a new JIRA Ticket for that entity
     *
     * @param labVessels   The plastic ware that the newly created lab batch will represent
     * @param reporter     The username of the person that is attempting to create the batch
     * @param batchName    The name for the batch being created -- this can be an existing JIRA ticket id
     * @param labBatchType The type of batch to create
     * @param issueType    The type of issue to create in JIRA for this lab batch
     *
     * @return The lab batch that was created.
     */
    public LabBatch createLabBatch(@Nonnull Set<LabVessel> labVessels, @Nonnull String reporter,
                                   @Nonnull String batchName, @Nonnull LabBatch.LabBatchType labBatchType,
                                   @Nonnull CreateFields.IssueType issueType) {
        LabBatch batchObject = new LabBatch(batchName, labVessels, labBatchType);

        labBatchDao.persist(batchObject);
        setJiraInformation(batchObject, batchName);
        batchToJira(reporter, batchName, batchObject, issueType);

        return batchObject;
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
     * @see #createLabBatch(org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch.LabBatchType, String, String, String, java.util.Date, String, String, java.util.Set, java.util.Set)
     */
    public LabBatch createLabBatch(@Nonnull LabBatch batchObject, String reporter,
                                   @Nonnull CreateFields.IssueType issueType) {
        if (StringUtils.isBlank(batchObject.getBatchName())) {
            throw new InformaticsServiceException("The name for the batch object cannot be null");
        }

        batchToJira(reporter, null, batchObject, issueType);
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
     * Creates a new lab batch and adds the vessels to the named bucket.
     *
     * @param batch      A constructed, but not persisted, batch object containing all initial information necessary
     *                   to persist a new batch
     * @param operator   The person creating the batch
     * @param bucketName The name of the bucket to add the vessels to
     * @param location   The machine location of the operation for the bucket event
     * @param issueType  The type of issue to create in JIRA for this lab batch
     *
     * @return The lab batch that was created.
     */
    public LabBatch createLabBatchAndRemoveFromBucket(@Nonnull LabBatch batch, @Nonnull String operator,
                                                      @Nonnull String bucketName, @Nonnull String location,
                                                      @Nonnull CreateFields.IssueType issueType) {
        batch = createLabBatch(batch, operator, issueType);
        Bucket bucket = bucketDao.findByName(bucketName);
        bucketEjb.createEntriesAndBatchThem(batch.getStartingBatchLabVessels(), bucket);
        return batch;
    }

    /**
     * Creates a new lab batch, using an existing JIRA ticket for tracking, and adds the vessels to the named bucket.
     *
     * @param vesselLabels The vessel labels to add to the batch and the bucket
     * @param operator     The username of the person creating the batch
     * @param batchName    The name of the batch being created. This can be an existing JIRA ticket id
     * @param bucketName   The name of the bucket to add the vessels to
     * @param labBatchType The type of batch to create
     * @param issueType    The type of issue to create in JIRA for this lab batch
     *
     * @return The lab batch that was created.
     */
    public LabBatch createLabBatchAndRemoveFromBucket(@Nonnull List<String> vesselLabels, @Nonnull String operator,
                                                      @Nonnull String batchName, @Nonnull String bucketName,
                                                      @Nonnull LabBatch.LabBatchType labBatchType,
                                                      @Nonnull CreateFields.IssueType issueType) {
        Set<LabVessel> vessels = new HashSet<>(tubeDao.findByListIdentifiers(vesselLabels));
        Bucket bucket = bucketDao.findByName(bucketName);
        LabBatch batch = createLabBatch(vessels, operator, batchName, labBatchType, issueType);
        bucketEjb.createEntriesAndBatchThem(vessels, bucket);
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
     * @return The lab batch that was created.
     */
    public LabBatch createLabBatchAndRemoveFromBucket(@Nonnull LabBatch.LabBatchType labBatchType,
                                                      @Nonnull String workflowName, @Nonnull List<Long> bucketEntryIds,
                                                      @Nonnull List<Long> reworkBucketEntryIds,
                                                      @Nonnull String batchName, @Nonnull String description,
                                                      @Nonnull Date dueDate, @Nonnull String important,
                                                      @Nonnull String username) throws ValidationException {
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

        CreateFields.IssueType issueType = CreateFields.IssueType.MAP_WORKFLOW_TO_ISSUE_TYPE.get(workflowName);

        batchToJira(username, null, batch, issueType);

        //link the JIRA tickets for the batch created to the pdo batches.
        for (String pdoKey : pdoKeys) {
            linkJiraBatchToTicket(pdoKey, batch);
        }

        return batch;
    }

    /**
     * This method fetches the JIRA ticket, or creates a new one, sets the batch description and JIRA ticket.
     *
     * @param batchObject The batch object to set the description and JIRA ticket of
     * @param jiraTicket  The JIRA ticket key to fetch and set to the batch
     */
    private void setJiraInformation(LabBatch batchObject, String jiraTicket) {
        JiraTicket ticket;
        try {
            JiraIssue jiraIssue = jiraService.getIssue(jiraTicket);

            ticket = jiraTicketDao.fetchByName(jiraTicket);
            if (ticket == null) {
                ticket = new JiraTicket(jiraService, jiraIssue.getKey());
            }

            batchObject.setBatchDescription(jiraIssue.getDescription());

            batchObject.setJiraTicket(ticket);
        } catch (IOException ioe) {
            logger.error("Error attempting to create Lab Batch in Jira", ioe);
            throw new InformaticsServiceException("Error attempting to create Lab Batch in Jira", ioe);
        }
    }

    /**
     * This method extracts all necessary information from the given batch object and creates (if necessary) and
     * associates a JIRA ticket that will represent this batch.
     *
     * @param reporter   The username of the person that is attempting to create the batch
     * @param jiraTicket Optional parameter that represents an existing Jira Ticket that refers to this batch
     * @param newBatch   The source of the Batch information that will assist in populating the Jira Ticket
     * @param issueType  The type of issue to create in JIRA for this lab batch
     */
    public void batchToJira(String reporter, @Nullable String jiraTicket, LabBatch newBatch,
                            @Nonnull CreateFields.IssueType issueType) {
        try {
            AbstractBatchJiraFieldFactory fieldBuilder = AbstractBatchJiraFieldFactory
                    .getInstance(newBatch, productOrderDao);

            if (StringUtils.isBlank(newBatch.getBatchDescription())) {
                newBatch.setBatchDescription(fieldBuilder.generateDescription());
            } else {
                newBatch.setBatchDescription(
                        fieldBuilder.generateDescription() + "\n\n" + newBatch.getBatchDescription());
            }

            if (jiraTicket == null) {
                Map<String, CustomFieldDefinition> submissionFields = jiraService.getCustomFields();
                if (issueType == null) {
                    throw new InformaticsServiceException("JIRA issue type must be specified");
                }
                JiraIssue jiraIssue = jiraService
                        .createIssue(fieldBuilder.getProjectType(), reporter, issueType, fieldBuilder.getSummary(),
                                     fieldBuilder.getCustomFields(submissionFields));

                JiraTicket ticket = new JiraTicket(jiraService, jiraIssue.getKey());

                newBatch.setJiraTicket(ticket);
            }
        } catch (IOException ioe) {
            logger.error("Error attempting to create a lab batch in JIRA", ioe);
            throw new InformaticsServiceException("Error attempting to create a lab batch in JIRA", ioe);
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
     * Primarily utilized by the deck messaging, this method will validate that all given Vessels have previously been
     * defined in a batch known to the Mercury System.
     *
     * @param batchVessels The vessels to check the batches of
     *
     * @return A boolean that determines if the vessels passed in are all in a batch known to Mercury
     */
    public boolean validatePriorBatch(Collection<LabVessel> batchVessels) {
        boolean result = false;

        Iterator<LabVessel> vesselIterator = batchVessels.iterator();
        LabVessel firstVessel = vesselIterator.next();

        for (LabBatch testBatch : firstVessel.getNearestLabBatches()) {
            if (testBatch.getStartingBatchLabVessels().containsAll(batchVessels)) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * This method adds bucket entries, from PDO creation or rework, to an existing lab batch.
     *
     * @param businessKey    the business key for the lab batch we are adding samples to
     * @param bucketEntryIds the bucket entries whose vessel are being added to the batch
     * @param reworkEntries  the rework bucket entries whose vessels are being added to the batch
     *
     * @throws IOException This exception is thrown when the JIRA service can not be contacted.
     */
    public void addToLabBatch(String businessKey, List<Long> bucketEntryIds, List<Long> reworkEntries)
            throws IOException {
        LabBatch batch = labBatchDao.findByBusinessKey(businessKey);
        Set<String> pdoKeys = new HashSet<>();

        List<BucketEntry> bucketEntries = bucketEntryDao.findByIds(bucketEntryIds);
        Set<LabVessel> labVessels = new HashSet<>();
        for (BucketEntry bucketEntry : bucketEntries) {
            labVessels.add(bucketEntry.getLabVessel());
            pdoKeys.add(bucketEntry.getProductOrder().getBusinessKey());
            bucketEntry.getBucket().removeEntry(bucketEntry);
        }

        batch.addLabVessels(labVessels);
        bucketEjb.moveFromBucketToBatch(bucketEntries, batch);

        List<BucketEntry> reworkBucketEntries = bucketEntryDao.findByIds(reworkEntries);
        Set<LabVessel> reworkVessels = new HashSet<>();
        Bucket reworkFromBucket = null;

        for (BucketEntry entry : reworkBucketEntries) {
            reworkVessels.add(entry.getLabVessel());
            pdoKeys.add(entry.getProductOrder().getBusinessKey());
            entry.getBucket().removeEntry(entry);
            reworkFromBucket = entry.getBucket();
        }

        batch.addReworks(reworkVessels);
        bucketEjb.moveFromBucketToBatch(reworkBucketEntries, batch);

        Set<CustomField> customFields = new HashSet<>();
        Map<String, CustomFieldDefinition> submissionFields = jiraService.getCustomFields();
        customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.GSSR_IDS,
                                         LCSetJiraFieldFactory.buildSamplesListString(batch, reworkFromBucket)));

        int sampleCount = batch.getStartingBatchLabVessels().size();
        customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.NUMBER_OF_SAMPLES,
                                         sampleCount));

        AbstractBatchJiraFieldFactory fieldBuilder = AbstractBatchJiraFieldFactory
                .getInstance(batch, productOrderDao);

        if (StringUtils.isBlank(batch.getBatchDescription())) {
            batch.setBatchDescription(fieldBuilder.generateDescription());
        } else {
            batch.setBatchDescription(
                    fieldBuilder.generateDescription() + "\n\n" + batch.getBatchDescription());
        }

        customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.DESCRIPTION,
                                         batch.getBatchDescription()));

        jiraService.updateIssue(batch.getJiraTicket().getTicketName(), customFields);

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
                    for (LabBatchStartingVessel labBatchStartingVessel : sampleInstance.getAllBatchVessels(
                            LabBatch.LabBatchType.WORKFLOW)) {
                        if (labBatchStartingVessel.getLabBatch().getBatchName().equals(lcsetName)) {
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
}
