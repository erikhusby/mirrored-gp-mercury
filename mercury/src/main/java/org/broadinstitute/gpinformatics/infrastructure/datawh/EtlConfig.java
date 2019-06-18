package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Configuration for the data warehouse ETL service.
 */
@ConfigKey("datawhEtl")
@ApplicationScoped
public class EtlConfig extends AbstractConfig implements Serializable {
    private String datawhEtlDirRoot;

    public EtlConfig(){}

    @Inject
    public EtlConfig(@Nonnull Deployment deployment) {
        super(deployment);
    }

    public String getDatawhEtlDirRoot() {
        return datawhEtlDirRoot;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setDatawhEtlDirRoot(String datawhEtlDirRoot) {
        this.datawhEtlDirRoot = datawhEtlDirRoot;
    }
}
