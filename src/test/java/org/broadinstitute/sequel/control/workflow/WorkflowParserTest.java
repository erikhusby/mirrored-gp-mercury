package org.broadinstitute.sequel.control.workflow;

import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.workflow.WorkflowAnnotation;
import org.broadinstitute.sequel.entity.workflow.WorkflowState;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * Test parsing of BPMN
 */
public class WorkflowParserTest {

    @Test
    public void testParse() {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("HybridSelectionV2.bpmn");
        try {
            WorkflowParser workflowParser = new WorkflowParser(inputStream);
            WorkflowState startState = workflowParser.getStartState();
            Assert.assertNotNull(startState, "No start state");
            Assert.assertEquals(workflowParser.getMapNameToTransitionList().size(), 34, "Wrong number of transition names");
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    public void test_visual_paradigm() {
        final String bpmnDoc = "SimpleSubProcessVisualParadigm.xml";
        final InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(bpmnDoc);
        try {
            WorkflowParser workflowParser = new WorkflowParser(inputStream);
            Assert.assertEquals(workflowParser.getWorkflowName(),"Hybrid Selection");
            WorkflowState startState = workflowParser.getStartState();
            Assert.assertNotNull(startState, "No start state");
            Assert.assertEquals(workflowParser.getMapNameToTransitionList().size(), 3, "Wrong number of transition names");

            WorkflowDescription workflowDescription = new WorkflowDescription("HS", null,
                    CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
            workflowDescription.initFromFile(bpmnDoc);

            Collection<WorkflowAnnotation> workflowAnnotations = workflowDescription.getAnnotations("PreflightNormalization");
            Assert.assertFalse(workflowAnnotations.isEmpty());
            Assert.assertTrue(workflowAnnotations.contains(WorkflowAnnotation.SINGLE_SAMPLE_LIBRARY));
            Assert.assertEquals(workflowAnnotations.size(),1);

            Assert.assertTrue(workflowDescription.getAnnotations("PreflightPicoSetup").isEmpty());

        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

}
