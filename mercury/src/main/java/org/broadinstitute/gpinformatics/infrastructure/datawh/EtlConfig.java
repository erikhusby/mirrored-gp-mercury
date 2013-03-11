package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import java.io.Serializable;

/**
 * Configuration for the data warehouse ETL service.
 */
@ConfigKey("datawhEtl")
public class EtlConfig extends AbstractConfig implements Serializable {
    private String datawhEtlDirRoot;

    public String getDatawhEtlDirRoot() {
        return datawhEtlDirRoot;
    }

    public void setDatawhEtlDirRoot(String datawhEtlDirRoot) {
        this.datawhEtlDirRoot = datawhEtlDirRoot;
    }

}
