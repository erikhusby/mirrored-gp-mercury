package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;

/**
 * Configuration for the data warehouse ETL service.
 */
@ConfigKey("datawhEtl")
public class EtlConfig extends AbstractConfig {
    private String datawhEtlDirRoot;

    public String getDatawhEtlDirRoot() {
        return datawhEtlDirRoot;
    }

    public void setDatawhEtlDirRoot(String datawhEtlDirRoot) {
        this.datawhEtlDirRoot = datawhEtlDirRoot;
    }
}
