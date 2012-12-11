package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import clover.org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.Visibility;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.link.AddIssueLinkRequest;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.vessel.AbstractBatchJiraFieldFactory;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
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
@Stateless
public class LabBatchEjb {

    private final static Log logger = LogFactory.getLog(LabBatchEjb.class);

    LabBatchDAO labBatchDao;

    AthenaClientService athenaClientService;

    JiraService jiraService;

    JiraTicketDao jiraTicketDao;

    /**
     * Alternate create lab batch method to allow a user to define the vessels for use by their barcode
     *
     * @param reporter    The User that is attempting to create the batch
     * @param labVessels The plastic ware that the newly created lab batch will represent
     * @param jiraTicket  Optional parameter that represents an existing Jira Ticket that refers to this batch
     */
    public LabBatch createLabBatch(@Nonnull Collection<LabVessel> labVessels, @Nonnull String reporter, String jiraTicket) {

        Collection<String> pdoList = LabVessel.extractPdoList(labVessels);

        LabBatch batchObject =
                new LabBatch(LabBatch.generateBatchName(CreateFields.IssueType.EXOME_EXPRESS.getJiraName(), pdoList),
                             new HashSet<LabVessel>(labVessels));

        labBatchDao.persist(batchObject);

        batchToJira(reporter, jiraTicket, batchObject);

        return batchObject;
    }

    /**
     * createLabBatch will, given a group of lab plastic ware, create a batch entity and a new Jira Ticket for that
     * entity
     *
     * @param batchObject A constructed, but not persisted, batch object containing all initial information necessary
     *                    to persist a new batch
     * @param reporter    The User that is attempting to create the batch
     * @param jiraTicket  Optional parameter that represents an existing Jira Ticket that refers to this batch
     */
    public LabBatch createLabBatch(@Nonnull LabBatch batchObject, @Nonnull String reporter, String jiraTicket) {

        Collection<String> pdoList = LabVessel.extractPdoList(batchObject.getStartingLabVessels());

        if (StringUtils.isBlank(batchObject.getBatchName())) {
            batchObject.setBatchName(
                    LabBatch.generateBatchName(CreateFields.IssueType.EXOME_EXPRESS.getJiraName(), pdoList));
        }

        labBatchDao.persist(batchObject);

        batchToJira(reporter, jiraTicket, batchObject);

        return batchObject;
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
        JiraTicket ticket;

        try {
            AbstractBatchJiraFieldFactory fieldBuilder = AbstractBatchJiraFieldFactory
                    .getInstance(CreateFields.ProjectType.LCSET_PROJECT, newBatch, athenaClientService);

            if (StringUtils.isBlank(newBatch.getBatchDescription())) {
                newBatch.setBatchDescription(fieldBuilder.generateDescription());
            }

            if (jiraTicket == null) {

                Map<String, CustomFieldDefinition> submissionFields = jiraService.getCustomFields();

                // TODO SGM Determine Project and Issue type better.  Use Workflow Configuration
                JiraIssue jiraIssue = jiraService
                        .createIssue(CreateFields.ProjectType.LCSET_PROJECT.getKeyPrefix(), reporter,
                                     CreateFields.IssueType.EXOME_EXPRESS, newBatch.getBatchName(),
                                     newBatch.getBatchDescription(), fieldBuilder.getCustomFields(submissionFields));

                ticket = new JiraTicket(jiraService, jiraIssue.getKey());

            } else {
                ticket = jiraTicketDao.fetchByName(jiraTicket);
            }
            newBatch.setJiraTicket(ticket);
        } catch (IOException ioe) {
            logger.error("Error attempting to create Lab Batch in Jira", ioe);
            throw new InformaticsServiceException("Error attempting to create Lab Batch in Jira", ioe);
        }

        for (String pdo : LabVessel.extractPdoList(newBatch.getStartingLabVessels())) {
            try {
                jiraService.addLink(AddIssueLinkRequest.LinkType.Related, pdo, newBatch.getJiraTicket().getTicketName(),
                                    "New Batch Created: " +
                                            newBatch.getJiraTicket().getTicketName() +
                                            " " + newBatch.getBatchName(), Visibility.Type.role,
                                    Visibility.Value.QA_Jira_Users);
            } catch (IOException ioe) {
                logger.error("Error attempting to link Batch " + ticket.getTicketName() + " to Product order " + pdo,
                             ioe);
            }
        }
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

}
