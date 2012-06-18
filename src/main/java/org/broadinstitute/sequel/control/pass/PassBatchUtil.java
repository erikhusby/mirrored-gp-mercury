package org.broadinstitute.sequel.control.pass;

import org.broadinstitute.sequel.boundary.Sample;
import org.broadinstitute.sequel.boundary.AbstractPass;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.project.PassBackedProjectPlan;
import org.broadinstitute.sequel.entity.project.Starter;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.workflow.LabBatch;
import org.broadinstitute.sequel.infrastructure.jira.JiraService;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueResponse;

import java.io.IOException;
import java.util.*;

/**
 * Utilities for putting {@link Sample}s from {@link AbstractPass passes} into
 * {@link org.broadinstitute.sequel.entity.project.JiraTicket}s.
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
                LabBatch labBatch = new LabBatch(batchPrefix + "-" + batchNumber,samplesInBatch);
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
            createResponse = jiraService.createIssue(projectPrefix,issuetype,summary,description);
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
        for (Starter starter : batch.getStarters()) {
            // todo arz add starters (bsp samples) to lc set ticket
        }
    }
}
