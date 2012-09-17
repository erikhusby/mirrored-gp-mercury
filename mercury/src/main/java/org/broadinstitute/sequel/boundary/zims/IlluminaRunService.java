package org.broadinstitute.sequel.boundary.zims;

import org.broadinstitute.sequel.entity.zims.ZimsIlluminaRun;

import javax.inject.Inject;

/**
 * @author breilly
 */
public interface IlluminaRunService {

    public ZimsIlluminaRun getRun(String runName);
}
