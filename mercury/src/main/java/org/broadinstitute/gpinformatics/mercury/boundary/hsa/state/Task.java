package org.broadinstitute.gpinformatics.mercury.boundary.hsa.state;

import org.broadinstitute.gpinformatics.mercury.boundary.hsa.dragen.DragenAppContext;

public interface Task {
    TaskResult fireEvent(DragenAppContext context);
}
