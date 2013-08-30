package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import javax.inject.Inject;

public class WorkflowDiagramerTest extends ContainerTest {

    @Inject
    private WorkflowDiagramer diagramer;

    /** Enable and run this test to get workflow diagrams generated. */
    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = false)
    public void testMakeDiagrams() throws Exception {
        // Diagram files are put in directory destDir
        String destDir = diagramer.makeAllDiagramFiles();
    }
}
