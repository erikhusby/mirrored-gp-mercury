package org.broadinstitute.gpinformatics.mercury.control.dao.storage;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nonnull;

/**
 * The minimal configuration parameters used by Google Storage.
 * The yaml file section is determined by the subclass, which gives the ConfigKey.
 */
public abstract class GoogleStorageConfig extends AbstractConfig {
    public GoogleStorageConfig() {
        super();
    }

    protected GoogleStorageConfig(@Nonnull Deployment deploymentConfig) {
        super(deploymentConfig);
    }

    public abstract String getCredentialFilename();
    public abstract String getBucketName();

    public String getWriterCredentialFilename() {
        return "";
    }
}
