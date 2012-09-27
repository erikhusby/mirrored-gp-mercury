package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;

import javax.inject.Inject;
import java.io.Serializable;

/**
 * @author breilly
 */
public class IlluminaRunServiceImpl implements IlluminaRunService, Serializable {

    @Inject
    private IlluminaRunResource illuminaRunResource;

    @Override
    public ZimsIlluminaRun getRun(String runName) {
        return illuminaRunResource.getRun(runName);
    }
}
