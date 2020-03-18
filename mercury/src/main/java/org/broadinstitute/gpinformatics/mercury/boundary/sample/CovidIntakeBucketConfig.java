package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleStorageConfig;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Configuration for Mercury to access Covid intake manifest files in Google Storage.
 */
@SuppressWarnings("UnusedDeclaration")
@ConfigKey("covidIntakeStorage")
@ApplicationScoped
public class CovidIntakeBucketConfig extends AbstractConfig implements Serializable, GoogleStorageConfig {
    private String credentialFilename;
    private String writerCredentialFilename;
    private String bucketName;

    public CovidIntakeBucketConfig() {
    }

    @Inject
    public CovidIntakeBucketConfig(@Nonnull Deployment deployment) {
        super(deployment);
    }

    @Override
    public String getCredentialFilename() {
        return credentialFilename;
    }

    @Override
    public String getWriterCredentialFilename() {
        return writerCredentialFilename;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    public void setCredentialFilename(String credentialFilename) {
        this.credentialFilename = credentialFilename;
    }

    public void setWriterCredentialFilename(String writerCredentialFilename) {
        this.writerCredentialFilename = writerCredentialFilename;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Override
    public String getDailyCredentialRenewal() {
        return null;
    }

    public void setDailyCredentialRenewal(String dailyCredentialRenewal) {
        // ignored
    }
}
