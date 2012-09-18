package org.broadinstitute.gpinformatics.mercury.control.pass;

import org.broadinstitute.gpinformatics.mercury.boundary.Sample;
import org.broadinstitute.gpinformatics.mercury.boundary.AbstractPass;
import org.broadinstitute.gpinformatics.mercury.entity.project.PassBackedProjectPlan;
import org.broadinstitute.gpinformatics.mercury.entity.project.Starter;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.mercury.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.mercury.infrastructure.jira.issue.CreateIssueResponse;

import java.io.IOException;
import java.util.*;

/**
 * Utilities for putting {@link Sample}s from {@link AbstractPass passes} into
 * {@link org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket}s.
 */
public class PassBatchUtil {

    /**
     * Create a {@link LabBatch} for every block of numSamplesPerBatch in
     * the pass.
     * @param passPlan
     * @param numSamplesPerBatch
     * @param batchPrefix
     * @return
     */
    public static Set<LabBatch> createBatches(PassBackedProjectPlan passPlan,
                                              int numSamplesPerBatch,
                                              String batchPrefix) {
        int numSamplesInCurrentBatch = 0;
        int batchNumber = 1;
        final Set<LabBatch> labBatches =  new HashSet<LabBatch>();
        final Set<Starter> samplesInBatch = new HashSet<Starter>(numSamplesPerBatch);
        for (Starter stockSample : passPlan.getStarters()) {
            samplesInBatch.add(stockSample);
            numSamplesInCurrentBatch++;
            if (numSamplesInCurrentBatch == numSamplesPerBatch) {
                LabBatch labBatch = new LabBatch(passPlan,batchPrefix + "-" + batchNumber,samplesInBatch);
                numSamplesInCurrentBatch = 0;
                samplesInBatch.clear();
                batchNumber++;
                labBatches.add(labBatch);
            }
        }
        return labBatches;
    }

    /**
     * Take the given batch and create a jira ticket for it.
     * @param batch
     * @param jiraService
     * @param projectPrefix
     * @param issuetype
     * @param summary
     * @param description
     */
    public static void createJiraForBatch(LabBatch batch,
                                   JiraService jiraService,
                                   String projectPrefix,
                                   CreateIssueRequest.Fields.Issuetype issuetype,
                                   String summary,
                                   String description) {
        CreateIssueResponse createResponse = null;
        try {
            createResponse = jiraService.createIssue(projectPrefix,issuetype,summary,description, null);
        }
        catch(IOException e) {
            throw new RuntimeException("Failed to create jira ticket",e);
        }
        if (createResponse == null) {
            throw new RuntimeException("Could not create jira ticket.");
        }
        if (createResponse.getId() == null) {
            throw new RuntimeException("Could not create jira ticket.");
        }
        final String ticketName = batch.getJiraTicket().getTicketName();
        // todo arz set work request field to something bogus, add bsp stock samples to list of samples.
        final StringBuilder sampleNames = new StringBuilder();
        for (Starter starter : batch.getStarters()) {
            sampleNames.append(starter).append("\n");
        }
        //jiraService.updateField(ticketName,"WorkRequestId","SequeL");
        //jiraService.updateField(ticketName,"Samples",sampleNames.toString());
    }
}
