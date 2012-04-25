package org.broadinstitute.sequel.control.workflow;

import org.broadinstitute.sequel.entity.workflow.WorkflowState;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Test parsing of BPMN
 */
public class WorkflowParserTest {

    @Test
    public void testParse() {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("HybridSelection.bpmn");
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
}
