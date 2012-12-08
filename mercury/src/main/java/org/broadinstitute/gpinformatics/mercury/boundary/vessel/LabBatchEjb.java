package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

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
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.vessel.AbstractBatchJiraFieldBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Scott Matthews
 *         Date: 12/6/12
 *         Time: 2:01 PM
 */
@Stateless
public class LabBatchEjb {

    private final static Log logger = LogFactory.getLog(LabBatchEjb.class);

    @Inject
    LabBatchDAO labBatchDao;

    @Inject
    AthenaClientService athenaClientService;

    @Inject
    JiraService jiraService;

    @Inject
    JiraTicketDao jiraTicketDao;

    public LabBatch createLabBatch(@Nonnull Collection<LabVessel> batchContents, @Nonnull String reporter,
                                   String jiraTicket) {

        Collection<String> pdoList = LabVessel.extractPdoList(batchContents);

        LabBatch newBatch =
                new LabBatch(LabBatch.generateBatchName(CreateFields.IssueType.EXOME_EXPRESS.getJiraName(), pdoList),
                             new HashSet<LabVessel>(batchContents));
        labBatchDao.persist(newBatch);

        batchToJira(reporter, jiraTicket, newBatch);

        return newBatch;
    }

    public void batchToJira(String reporter, String jiraTicket, LabBatch newBatch) {
        JiraTicket ticket = jiraTicketDao.fetchByName(jiraTicket);
        try {
            if (ticket == null) {

                // TODO SGM Determine Project and Issue type better.  Use Workflow Configuration
                ticket = createJiraTicket(newBatch, reporter, CreateFields.IssueType.EXOME_EXPRESS,
                                          CreateFields.ProjectType.LCSET_PROJECT);
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
//                jiraService.addComment(pdo, "New Batch Created: " +
//                        newBatch.getJiraTicket().getTicketName() +
//                        " " + newBatch.getBatchName());
            } catch (IOException ioe) {
                logger.error("Error attempting to link Batch " + ticket.getTicketName() + " to Product order " + pdo,
                             ioe);
            }
        }
    }

    public JiraTicket createJiraTicket(@Nonnull LabBatch batch, @Nonnull String reporter,
                                       @Nonnull CreateFields.IssueType batchSubType,
                                       @Nonnull CreateFields.ProjectType project) throws IOException {

        AbstractBatchJiraFieldBuilder fieldBuilder =
                AbstractBatchJiraFieldBuilder.getInstance(project, batch, athenaClientService, jiraService);

        Map<String, CustomFieldDefinition> submissionFields = jiraService.getCustomFields();

        JiraIssue jiraIssue = jiraService
                .createIssue(project.getKeyPrefix(), reporter, batchSubType, batch.getBatchName(),
                             fieldBuilder.generateDescription(), fieldBuilder.getCustomFields(submissionFields));

        return new JiraTicket(jiraService, jiraIssue.getKey());
    }

}
