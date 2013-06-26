package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.vessel.AbstractBatchJiraFieldFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates the business logic related to {@link LabBatch}s.  This includes the creation
 * of a new batch entity and saving that to JIRA
 *
 * @author Scott Matthews
 *         Date: 12/6/12
 *         Time: 2:01 PM
 */
@Stateful
@RequestScoped
public class LabBatchEjb {

    private final static Log logger = LogFactory.getLog(LabBatchEjb.class);

    private LabBatchDAO labBatchDao;

    private AthenaClientService athenaClientService;

    private JiraService jiraService;

    private JiraTicketDao jiraTicketDao;

    private LabVesselDao tubeDAO;

    private BucketDao bucketDao;

    private BucketEjb bucketEjb;

    /**
     * Alternate create lab batch method to allow a user to define the vessels for use by their barcode.
     *
     * @param reporter       The user that is attempting to create the batch.
     * @param labVesselNames The plastic ware that the newly created lab batch will represent.
     * @param batchName      The name for the batch being created. This can be an existing JIRA ticket id.
     * @param labBatchType   The type of batch to create.
     * @param issueType      The type of issue to create in JIRA for this lab batch.
     *
     * @return The lab batch that was created.
     */
    public LabBatch createLabBatch(@Nonnull String reporter, @Nonnull Set<String> labVesselNames,
                                   @Nonnull String batchName, LabBatch.LabBatchType labBatchType,
                                   @Nonnull CreateFields.IssueType issueType) {

        Set<LabVessel> vesselsForBatch = new HashSet<>(labVesselNames.size());

        for (String currVesselLabel : labVesselNames) {
            vesselsForBatch.add(tubeDAO.findByIdentifier(currVesselLabel));
        }

        return createLabBatch(vesselsForBatch, reporter, batchName, labBatchType, issueType);
    }

    /**
     * This method will, given a group of lab plastic ware, create a batch entity and a new JIRA Ticket for that entity.
     *
     * @param labVessels   The plastic ware that the newly created lab batch will represent.
     * @param reporter     The user that is attempting to create the batch.
     * @param batchName    The name for the batch being created. This can be an existing JIRA ticket id.
     * @param labBatchType The type of batch to create.
     * @param issueType    The type of issue to create in JIRA for this lab batch.
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
     *
     * @param batchObject A constructed, but not persisted, batch object containing all initial information necessary
     *                    to persist a new batch.
     * @param reporter    The user that is attempting to create the batch.
     * @param issueType   The type of issue to create in JIRA for this lab batch.
     *
     * @return The lab batch that was created.
     */
    public LabBatch createLabBatch(@Nonnull LabBatch batchObject, String reporter,
                                   @Nonnull CreateFields.IssueType issueType) {
        if (StringUtils.isBlank(batchObject.getBatchName())) {
            throw new InformaticsServiceException("The Name for the batch Object cannot be null");
        }

        labBatchDao.persist(batchObject);

        batchToJira(reporter, null, batchObject, issueType);

        return batchObject;
    }

    /**
     * Creates a new lab batch, using an existing JIRA ticket for tracking, and adds the vessels to the named bucket.
     *
     * @param vesselLabels The vessel labels to add to the batch and the bucket.
     * @param operator     The person creating the batch.
     * @param batchName    The name of the batch being created. This can be an existing JIRA ticket id.
     * @param bucketName   The name of the bucket to add the vessels to.
     * @param location     The machine location of the operation for the bucket event.
     * @param labBatchType The type of batch to create.
     * @param issueType    The type of issue to create in JIRA for this lab batch.
     *
     * @return The lab batch that was created.
     */
    public LabBatch createLabBatchAndRemoveFromBucket(@Nonnull List<String> vesselLabels,
                                                      @Nonnull String operator, @Nonnull String batchName,
                                                      @Nonnull String bucketName, @Nonnull String location,
                                                      @Nonnull LabBatch.LabBatchType labBatchType,
                                                      @Nonnull CreateFields.IssueType issueType) {
        Set<LabVessel> vessels =
                new HashSet<>(tubeDAO.findByListIdentifiers(vesselLabels));
        Bucket bucket = bucketDao.findByName(bucketName);
        LabBatch batch = createLabBatch(vessels, operator, batchName, labBatchType, issueType);
        bucketEjb.start(operator, vessels, bucket, location);
        return batch;
    }

    /**
     * Creates a new lab batch and adds the vessels to the named bucket.
     *
     * @param batch      A constructed, but not persisted, batch object containing all initial information necessary
     *                   to persist a new batch.
     * @param operator   The person creating the batch.
     * @param bucketName The name of the bucket to add the vessels to.
     * @param location   The machine location of the operation for the bucket event.
     * @param issueType  The type of issue to create in JIRA for this lab batch.
     *
     * @return The lab batch that was created.
     */
    public LabBatch createLabBatchAndRemoveFromBucket(@Nonnull LabBatch batch, @Nonnull String operator,
                                                      @Nonnull String bucketName, @Nonnull String location,
                                                      @Nonnull CreateFields.IssueType issueType) {
        Bucket bucket = bucketDao.findByName(bucketName);
        batch = createLabBatch(batch, operator, issueType);
        bucketEjb.start(operator, batch.getStartingBatchLabVessels(), bucket, location);
        return batch;
    }

    /**
     * This method fetches the JIRA ticket, or creates a new one, sets the batch description and JIRA ticket.
     *
     * @param batchObject The batch object to set the description and JIRA ticket of.
     * @param jiraTicket  The JIRA ticket key to fetch and set to the batch.
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
     * @param reporter   The User that is attempting to create the batch.
     * @param jiraTicket Optional parameter that represents an existing Jira Ticket that refers to this batch .
     * @param newBatch   The source of the Batch information that will assist in populating the Jira Ticket.
     * @param issueType  The type of issue to create in JIRA for this lab batch.
     */
    public void batchToJira(String reporter, @Nullable String jiraTicket, LabBatch newBatch,
                            @Nonnull CreateFields.IssueType issueType) {

        JiraTicket ticket;
        try {
            AbstractBatchJiraFieldFactory fieldBuilder = AbstractBatchJiraFieldFactory
                    .getInstance(newBatch, athenaClientService);

            if (StringUtils.isBlank(newBatch.getBatchDescription())) {
                newBatch.setBatchDescription(fieldBuilder.generateDescription());
            } else {
                newBatch.setBatchDescription(
                        fieldBuilder.generateDescription() + "\n\n" + newBatch.getBatchDescription());
            }

            if (jiraTicket == null) {
                Map<String, CustomFieldDefinition> submissionFields = jiraService.getCustomFields();

                JiraIssue jiraIssue = jiraService
                        .createIssue(fieldBuilder.getProjectType().getKeyPrefix(), reporter,
                                issueType, fieldBuilder.getSummary(), fieldBuilder.getCustomFields(submissionFields));

                ticket = new JiraTicket(jiraService, jiraIssue.getKey());

                newBatch.setJiraTicket(ticket);
            }
        } catch (IOException ioe) {
            logger.error("Error attempting to create Lab Batch in JIRA", ioe);
            throw new InformaticsServiceException("Error attempting to create Lab Batch in JIRA", ioe);
        }
    }

    /**
     * This method links two batch JIRA tickets together in a parent to child relationship.
     *
     * @param parentBatch The parent batch to link.
     * @param childBatch  The child batch to link.
     */
    public void linkJiraBatches(LabBatch parentBatch, LabBatch childBatch) {
        try {
            if (childBatch.getJiraTicket() != null) {
                jiraService.addLink(AddIssueLinkRequest.LinkType.Parentage, parentBatch.getJiraTicket().getTicketName(),
                        childBatch.getJiraTicket()
                                .getTicketName());
            }
        } catch (Exception ioe) {
            logger.error("Error attempting to link Batch " + childBatch.getJiraTicket().getTicketName()
                         + " to Product order " + parentBatch,
                    ioe);
        }
    }

    /**
     * This method links two batch JIRA tickets together in a parent to child relationship.
     *
     * @param parentTicket The parent ticket key to link.
     * @param childBatch   The child batch to link.
     */
    public void linkJiraBatchToTicket(String parentTicket, LabBatch childBatch) {
        try {
            if (childBatch.getJiraTicket() != null) {
                jiraService.addLink(AddIssueLinkRequest.LinkType.Parentage, parentTicket,
                        childBatch.getJiraTicket()
                                .getTicketName());
            }
        } catch (Exception ioe) {
            logger.error("Error attempting to link Batch " + childBatch.getJiraTicket().getTicketName()
                         + " to Product order " + parentTicket,
                    ioe);
        }
    }

    /**
     * Primarily utilized by the deck messaging, this method will validate that all given Vessels have previously been
     * defined in a batch known to the Mercury System.
     *
     * @param batchVessels The vessels to check the batches of.
     *
     * @return A boolean that determines if the vessels passed in are all in a batch known to Mercury.
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

    /*
       To Support DBFree Tests
    */
    @Inject
    public void setLabBatchDao(LabBatchDAO labBatchDao) {
        this.labBatchDao = labBatchDao;
    }

    @Inject
    public void setAthenaClientService(AthenaClientService athenaClientService) {
        this.athenaClientService = athenaClientService;
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
    public void setTubeDAO(LabVesselDao tubeDAO) {
        this.tubeDAO = tubeDAO;
    }

    @Inject
    public void setBucketDao(BucketDao bucketDao) {
        this.bucketDao = bucketDao;
    }

    @Inject
    public void setBucketEjb(BucketEjb bucketEjb) {
        this.bucketEjb = bucketEjb;
    }
}
