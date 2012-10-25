package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.SequencingLibraryAnnotation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowAnnotation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowState;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
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
                    CreateIssueRequest.Fields.Issuetype.WHOLE_EXOME_HYBSEL );
            workflowDescription.initFromFile(bpmnDoc);

            Collection<WorkflowAnnotation> workflowAnnotations = workflowDescription.getAnnotations("PreflightNormalization");
            Assert.assertFalse(workflowAnnotations.isEmpty());
            boolean hasSeqLibAnnotation = false;
            for (WorkflowAnnotation workflowAnnotation : workflowAnnotations) {
                if (workflowAnnotation instanceof SequencingLibraryAnnotation) {
                    hasSeqLibAnnotation = true;
                }
            }
            Assert.assertTrue(hasSeqLibAnnotation);
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
