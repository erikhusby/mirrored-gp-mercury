package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleStorageConfig;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Configuration for Mercury to write Covid manifest files in Google Storage.
 */
@SuppressWarnings("UnusedDeclaration")
@ConfigKey("covidManifestGoogleStorage")
@ApplicationScoped
public class CovidManifestBucketConfig extends AbstractConfig implements Serializable, GoogleStorageConfig {
    private String credentialFilename;
    private String bucketName;

    public CovidManifestBucketConfig() {
    }

    @Inject
    public CovidManifestBucketConfig(@Nonnull Deployment deployment) {
        super(deployment);
    }

    @Override
    public String getCredentialFilename() {
        return credentialFilename;
    }

    @Override
    public String getWriterCredentialFilename() {
        return credentialFilename;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    public void setCredentialFilename(String credentialFilename) {
        this.credentialFilename = credentialFilename;
    }

    public void setWriterCredentialFilename(String writerCredentialFilename) {
        this.credentialFilename = writerCredentialFilename;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Override
    public String getDailyCredentialRenewal() {
        return null;
    }

    public void setDailyCredentialRenewal(String dailyCredentialRenewal) {
        // not used
    }
}
