package org.broadinstitute.gpinformatics.mercury.control.dao.storage;

/**
 * The configuration parameters used by Google Storage.
 * The yaml file section is determined by the subclass.
 */
public interface GoogleStorageConfig {
    public String getCredentialFile();
    public String getProject();
    public String getBucketName();
}
