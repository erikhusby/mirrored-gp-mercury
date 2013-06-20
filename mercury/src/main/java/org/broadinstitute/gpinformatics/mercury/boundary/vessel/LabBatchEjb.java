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
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.vessel.AbstractBatchJiraFieldFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
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
 * of a new batch entity and saving that to Jira
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

    private BucketBean bucketBean;

    /**
     * Alternate create lab batch method to allow a user to define the vessels for use by their barcode
     *
     * @param reporter       The User that is attempting to create the batch
     * @param labVesselNames The plastic ware that the newly created lab batch will represent
     * @param jiraTicket     Optional parameter that represents an existing Jira Ticket that refers to this batch
     *
     * @return
     */
    public LabBatch createLabBatch(@Nonnull String reporter, @Nonnull Set<String> labVesselNames,
                                   String jiraTicket, LabBatch.LabBatchType labBatchType) {

        Set<LabVessel> vesselsForBatch = new HashSet<LabVessel>(labVesselNames.size());

        for (String currVesselLabel : labVesselNames) {
            vesselsForBatch.add(tubeDAO.findByIdentifier(currVesselLabel));
        }

        return createLabBatch(vesselsForBatch, reporter, jiraTicket, labBatchType);
    }

    /**
     * Allows a user to define the vessels for use
     *
     * @param labVessels The plastic ware that the newly created lab batch will represent
     * @param reporter   The User that is attempting to create the batch
     * @param jiraTicket Optional parameter that represents an existing Jira Ticket that refers to this batch
     */
    public LabBatch createLabBatch(@Nonnull Set<LabVessel> labVessels, @Nonnull String reporter,
                                   @Nonnull String jiraTicket, LabBatch.LabBatchType labBatchType) {

        LabBatch batchObject = new LabBatch(jiraTicket, labVessels, labBatchType);

        labBatchDao.persist(batchObject);

        setJiraInformaiton(batchObject, jiraTicket);

        batchToJira(reporter, jiraTicket, batchObject);

        jiraBatchNotification(batchObject);


        return batchObject;
    }

    /**
     * createLabBatch will, given a group of lab plastic ware, create a batch entity and a new Jira Ticket for that
     * entity
     *
     * @param batchObject   A constructed, but not persisted, batch object containing all initial information necessary
     *                      to persist a new batch
     * @param reworkVessels Vessels to add as rework.
     * @param reporter      The User that is attempting to create the batch
     */
    public LabBatch createLabBatch(LabBatch batchObject, Set<LabVessel> reworkVessels, String reporter) {
        Collection<String> pdoList = LabVessel.extractPdoKeyList(batchObject.getStartingLabVessels());

        if (StringUtils.isBlank(batchObject.getBatchName())) {
            throw new InformaticsServiceException("The name for the batch object cannot be null");
        }

        labBatchDao.persist(batchObject);

        batchToJira(reporter, null, batchObject);

        jiraBatchNotification(batchObject);

        return batchObject;
    }

    /**
     * createLabBatch will, given a group of lab plastic ware, create a batch entity and a new Jira Ticket for that
     * entity
     *
     * @param batchObject A constructed, but not persisted, batch object containing all initial information necessary
     *                    to persist a new batch
     * @param reporter    The User that is attempting to create the batch
     */
    public LabBatch createLabBatch(@Nonnull LabBatch batchObject, @Nonnull String reporter) {

        Collection<String> pdoList = LabVessel.extractPdoKeyList(batchObject.getStartingLabVessels());

        if (StringUtils.isBlank(batchObject.getBatchName())) {
            throw new InformaticsServiceException("The Name for the batch Object cannot be null");
        }

        labBatchDao.persist(batchObject);

        batchToJira(reporter, null, batchObject);

        jiraBatchNotification(batchObject);

        return batchObject;
    }

    /**
     * Creates a new lab batch, using an existing JIRA ticket for tracking, and adds the vessels to the named bucket.
     *
     * @param vesselLabels the vessel labels to add to the batch and the bucket
     * @param operator     the person creating the batch
     * @param jiraTicket   the existing JIRA ticket to use
     * @param bucketName   the name of the bucket to add the vessels to
     * @param location     the machine location of the operation for the bucket event
     *
     * @return the new lab batch
     */
    public LabBatch createLabBatchAndRemoveFromBucket(@Nonnull List<String> vesselLabels,
                                                      @Nonnull String operator, @Nonnull String jiraTicket,
                                                      @Nonnull String bucketName, @Nonnull String location,
                                                      @Nonnull LabBatch.LabBatchType labBatchType) {
        Set<LabVessel> vessels =
                new HashSet<LabVessel>(tubeDAO.findByListIdentifiers(vesselLabels));
        Bucket bucket = bucketDao.findByName(bucketName);
        LabBatch batch = createLabBatch(vessels, operator, jiraTicket, labBatchType);
        bucketBean.start(operator, vessels, bucket, location);
        return batch;
    }

    /**
     * Creates a new lab batch and adds the vessels to the named bucket.
     *
     * @param batch      a constructed, but not persisted, batch object containing all initial information necessary
     *                   to persist a new batch
     * @param operator   the person creating the batch
     * @param bucketName the name of the bucket to add the vessels to
     * @param location   the machine location of the operation for the bucket event
     *
     * @return the persisted lab batch
     */
    public LabBatch createLabBatchAndRemoveFromBucket(@Nonnull LabBatch batch, @Nonnull String operator,
                                                      @Nonnull String bucketName, @Nonnull String location) {
        Bucket bucket = bucketDao.findByName(bucketName);
        batch = createLabBatch(batch, operator);
        //todo jac Hard coded labEventType for the one bucket.  This will need to change when we have multiple.
        bucketBean.add(batch.getReworks(), bucket, operator, location, LabEventType.PICO_PLATING_BUCKET);
        bucketBean.start(operator, batch.getStartingLabVessels(), bucket, location);
        return batch;
    }

    /**
     * @param batchObject
     * @param jiraTicket
     */
    private void setJiraInformaiton(LabBatch batchObject, String jiraTicket) {

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
     * batchToJira Extracts all necessary information from the Given Batch Object and creates (if necessary) and
     * associates a jira ticket that will represent this batch
     *
     * @param reporter   The User that is attempting to create the batch
     * @param jiraTicket Optional parameter that represents an existing Jira Ticket that refers to this batch
     * @param newBatch   The source of the Batch information that will assist in populating the Jira Ticket
     */
    public void batchToJira(String reporter, String jiraTicket, LabBatch newBatch) {

        JiraTicket ticket = null;
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

                // TODO SGM Determine Project and Issue type better.  Use Workflow Configuration
                JiraIssue jiraIssue = jiraService
                        .createIssue(fieldBuilder.getProjectType().getKeyPrefix(), reporter,
                                CreateFields.IssueType.EXOME_EXPRESS, fieldBuilder.getSummary(),
                                newBatch.getBatchDescription(), fieldBuilder.getCustomFields(submissionFields));

                ticket = new JiraTicket(jiraService, jiraIssue.getKey());

                newBatch.setJiraTicket(ticket);
            }
        } catch (IOException ioe) {
            logger.error("Error attempting to create Lab Batch in Jira", ioe);
            throw new InformaticsServiceException("Error attempting to create Lab Batch in Jira", ioe);
        }
    }

    /**
     * Isolates the logic for updating the Jira ticket associated with a batch with information on what has changed
     * for this new batch
     *
     * @param newBatch
     */
    public void jiraBatchNotification(LabBatch newBatch) {
        for (String pdo : LabVessel.extractPdoKeyList(newBatch.getStartingLabVessels())) {
            try {
                if (newBatch.getJiraTicket() != null) {
                    jiraService.addLink(AddIssueLinkRequest.LinkType.Parentage, pdo, newBatch.getJiraTicket()
                            .getTicketName());
                }
            } catch (Exception ioe) {
                logger.error("Error attempting to link Batch " + newBatch.getJiraTicket().getTicketName()
                             + " to Product order " + pdo,
                        ioe);
            }
        }
    }

    /**
     * Primarily utilized by the deck messaging, this method will validate that all given Vessels have previously been
     * defined in a batch known to the Mercury System.
     *
     * @param batchVessels
     *
     * @return
     */
    public boolean validatePriorBatch(Collection<LabVessel> batchVessels) {

        boolean result = false;

        Iterator<LabVessel> vesselIterator = batchVessels.iterator();

        LabVessel firstVessel = vesselIterator.next();

        for (LabBatch testBatch : firstVessel.getNearestLabBatches()) {
            if (testBatch.getStartingLabVessels().containsAll(batchVessels)) {
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
    public void setBucketBean(BucketBean bucketBean) {
        this.bucketBean = bucketBean;
    }
}
