package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nullable;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Configuration for the data warehouse ETL service.
 */
@ConfigKey("datawhEtl")
public class EtlConfig extends AbstractConfig implements Serializable {
    private String datawhEtlDirRoot;

    @Inject
    public EtlConfig(@Nullable Deployment deployment) {
        super(deployment);
    }

    public String getDatawhEtlDirRoot() {
        return datawhEtlDirRoot;
    }

    public void setDatawhEtlDirRoot(String datawhEtlDirRoot) {
        this.datawhEtlDirRoot = datawhEtlDirRoot;
    }

}
