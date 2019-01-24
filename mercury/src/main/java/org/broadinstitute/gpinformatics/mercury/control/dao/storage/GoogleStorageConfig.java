package org.broadinstitute.gpinformatics.mercury.control.dao.storage;

/**
 * The configuration parameters used by Google Storage.
 * The yaml file section is determined by the subclass.
 */
public interface GoogleStorageConfig {
    /** The absolute pathname of the local server directory where Google Oauth2 credentials are held. */
    public String getCredentialDirectory();

    /** The Google Oauth2 credentials file found in the credential directory. */
    public String getCredentialFilename();

    /** The Google project of Mercury accessing Google Storage. */
    public String getProject();

    /** The Google Storage bucket name to access. */
    public String getBucketName();
}
