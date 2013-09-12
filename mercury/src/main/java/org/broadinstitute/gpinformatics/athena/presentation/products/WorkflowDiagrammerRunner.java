package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.Startup;
import javax.ejb.Singleton;
import java.io.File;

@Startup
@Singleton
public class WorkflowDiagrammerRunner {
    private static Log logger = LogFactory.getLog(WorkflowDiagrammerRunner.class);

    public WorkflowDiagrammerRunner() {
        clearDiagramDirectory();
    }

    private void clearDiagramDirectory() {
        try {
            File directory = WorkflowDiagrammer.makeDiagramFileDir();
            for (File file : directory.listFiles()) {
                file.delete();
            }
        } catch (Exception e) {
            logger.error("Cannot clear diagram directory " + WorkflowDiagrammer.DIAGRAM_DIRECTORY, e);
        }
    }
}
