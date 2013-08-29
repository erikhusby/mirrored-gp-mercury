package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;

public class WorkflowDiagramerTest extends ContainerTest {

    @Inject
    private WorkflowDiagramer diagramer;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
    }

    /** Enable and run this test to get workflow diagrams generated. */
    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = false)
    public void testMakeDiagrams() throws Exception {
        // Diagram files are put in destDir (java.io.tmpDir)
        String destDir = diagramer.makeAllDiagramFiles();
    }
}
