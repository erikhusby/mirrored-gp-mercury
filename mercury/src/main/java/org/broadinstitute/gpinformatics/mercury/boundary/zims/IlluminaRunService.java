package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;

/**
 * @author breilly
 */
public interface IlluminaRunService {

    public ZimsIlluminaRun getRun(String runName);
}
