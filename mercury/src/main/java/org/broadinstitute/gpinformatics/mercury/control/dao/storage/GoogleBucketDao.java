package org.broadinstitute.gpinformatics.mercury.control.dao.storage;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.BaseServiceException;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class accesses Google Storage buckets using Service Account authentication.
 * Requires a pre-configured stored credential, in the form of json string in a file.
 *
 */
@RequestScoped
@Stateful
public class GoogleBucketDao {
    private GoogleStorageConfig googleStorageConfig;
    private Credentials credentials = null;
    private Credentials writerCredentials = null;
    private static Log logger = LogFactory.getLog(GoogleBucketDao.class);

    public void setConfigGoogleStorageConfig(GoogleStorageConfig googleStorageConfig) {
        this.googleStorageConfig = googleStorageConfig;
    }

    /** Returns a list of the files' blobIds in the bucket. */
    public List<String> list(MessageCollection messageCollection) {
        makeCredential(messageCollection);
        if (credentials == null) {
            return Collections.emptyList();
        } else {
            try {
                Storage storage = StorageOptions.newBuilder().setCredentials(credentials)
                        .setProjectId(googleStorageConfig.getProject()).build().getService();
                Set<String> names = new HashSet<>();
                storage.list(googleStorageConfig.getBucketName()).iterateAll().forEach(blob -> names.add(blob.getName()));
                return names.stream().sorted().collect(Collectors.toList());
            } catch (BaseServiceException e) {
                String msg = "Error getting file list from Google bucket. ";
                messageCollection.addError(msg + e.toString());
                logger.error(msg, e);
                return Collections.emptyList();
            }
        }
    }

    /**
     * Reads and returns content of a file as bytes, or null if no such file.
     */
    public byte[] download(String filename, MessageCollection messageCollection) {
        makeCredential(messageCollection);
        if (credentials == null) {
            return null;
        } else {
            try {
                Storage storage = StorageOptions.newBuilder().setCredentials(credentials)
                        .setProjectId(googleStorageConfig.getProject()).build().getService();
                Blob blob = storage.get(BlobId.of(googleStorageConfig.getBucketName(), filename));
                return (blob != null) ? blob.getContent() : null;
            } catch (BaseServiceException e) {
                String msg = "Error reading " + filename + " from Google bucket. ";
                messageCollection.addError(msg + e.toString());
                logger.error(msg, e);
                return null;
            }
        }
    }

    /**
     * Writes a file to the bucket for Arquillian testing.
     */
    public void upload(String filename, byte[] content, MessageCollection messageCollection) {
        makeWriterCredential(messageCollection);
        if (writerCredentials != null) {
            try {
                Storage storage = StorageOptions.newBuilder().setCredentials(writerCredentials)
                        .setProjectId(googleStorageConfig.getProject()).build().getService();
                BlobInfo blobInfo = BlobInfo.newBuilder(googleStorageConfig.getBucketName(), filename).
                        setContentType("text/plain").build();
                Blob blob = storage.get(blobInfo.getBlobId());
                // This will create a new file or overwrite an existing one.
                storage.create(blobInfo, content);
            } catch (BaseServiceException e) {
                String msg = "Error writing " + filename + " from Google bucket. ";
                messageCollection.addError(msg + e.toString());
                logger.error(msg, e);
            }
        }
    }

    /**
     * Authorizes Google Storage access for Mercury as a service account.
     */
    private void makeCredential(MessageCollection messages) {
        if (credentials == null) {
            File credentialFile = new File(googleStorageConfig.getCredentialFile());
            if (credentialFile.exists()) {
                // The credential file is expected to contain the json service account key
                // generated by page https://console.cloud.google.com/apis/credentials
                try {
                    credentials = GoogleCredentials.fromStream(new FileInputStream(credentialFile));
                } catch (FileNotFoundException e) {
                    messages.addError("Credential file is missing: %s", googleStorageConfig.getCredentialFile());
                } catch (IOException e) {
                    messages.addError("Credential file stream gives: %s", e.toString());
                } catch (BaseServiceException e) {
                    String msg = "Error getting Google Service Account credentials. ";
                    messages.addError(msg + e.toString());
                    logger.error(msg, e);
                }
            } else {
                messages.addError("Credential file is missing: %s", googleStorageConfig.getCredentialFile());
            }
        }
    }

    /**
     * Authorizes Google Storage access for Mercury as a service account.
     */
    private void makeWriterCredential(MessageCollection messages) {
        if (writerCredentials == null) {
            File writerCredentialFile = new File(googleStorageConfig.getWriterCredentialFile());
            if (writerCredentialFile.exists()) {
                try {
                    writerCredentials = GoogleCredentials.fromStream(new FileInputStream(writerCredentialFile));
                } catch (FileNotFoundException e) {
                    messages.addError("Writer credential file is missing: %s",
                            googleStorageConfig.getWriterCredentialFile());
                } catch (IOException e) {
                    messages.addError("Writer credential file stream gives: %s", e.toString());
                } catch (BaseServiceException e) {
                    String msg = "Error getting Google Service Account credentials. ";
                    messages.addError(msg + e.toString());
                    logger.error(msg, e);
                }
            } else {
                messages.addError("Writer credential file is missing: %s",
                        googleStorageConfig.getWriterCredentialFile());
            }
        }
    }

    /** Generates status messages while reading a bucket file and returns a list of all filenames. */
    public List<String> test(MessageCollection messageCollection) {
        if (StringUtils.isBlank(googleStorageConfig.getCredentialFile()) ||
                StringUtils.isBlank(googleStorageConfig.getProject()) ||
                StringUtils.isBlank(googleStorageConfig.getBucketName())) {
            messageCollection.addError("mercury-config.yaml needs GoogleStorage credentialFile, project, bucketName.");
            return Collections.emptyList();
        }
        File credentialFile = new File(googleStorageConfig.getCredentialFile());
        try {
            if (!credentialFile.exists() || !credentialFile.canRead() ||
                    StringUtils.isBlank(FileUtils.readFileToString(credentialFile))) {
                messageCollection.addError("Credential file " + credentialFile.getAbsolutePath() +
                        " is missing, unreadable, or empty.");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            messageCollection.addError("Exception when reading credentialFile " + credentialFile.getAbsolutePath() +
                    " : " + e.toString());
            return Collections.emptyList();
        }
        try {
            credentials = GoogleCredentials.fromStream(new FileInputStream(credentialFile));
        } catch (Exception e) {
            messageCollection.addError("Exception when parsing credentialFile " + credentialFile.getAbsolutePath() +
                    " : " + e.toString());
            return Collections.emptyList();
        }
        if (credentials == null) {
            messageCollection.addError("Credential file " + credentialFile.getAbsolutePath() +
                    " cannot be parsed into a GoogleCredential object.");
            return Collections.emptyList();
        }
        try {
            ServiceAccountCredentials serviceAccountCredentials = (ServiceAccountCredentials) credentials;
            messageCollection.addInfo("Credential file has AuthenticationType: " + credentials.getAuthenticationType());
            messageCollection.addInfo("Token server: " + serviceAccountCredentials.getTokenServerUri().toString());
            messageCollection.addInfo("Account: " + serviceAccountCredentials.getAccount());
        } catch (Exception e) {
            messageCollection.addError("Exception when extracting data from GoogleCredential: " + e.toString());
            return Collections.emptyList();
        }
        try {
            Storage storage = StorageOptions.newBuilder().setCredentials(credentials).
                    setProjectId(googleStorageConfig.getProject()).build().getService();
            messageCollection.addInfo("Storage object service account: " +
                    storage.getServiceAccount(googleStorageConfig.getProject()).toString());
            messageCollection.addInfo("Storage object host: " + storage.getOptions().getHost().toString());
            List<String> filenames = new ArrayList<>();
            storage.list(googleStorageConfig.getBucketName()).iterateAll()
                    .forEach(blob -> filenames.add(blob.getName()));
            messageCollection.addInfo("List of bucket " + googleStorageConfig.getBucketName() +
                    " : " + filenames.size() + " filenames.");
            if (!filenames.isEmpty()) {
                int randomIdx = new Random().nextInt(filenames.size());
                Blob blob = storage.get(BlobId.of(googleStorageConfig.getBucketName(), filenames.get(randomIdx)));
                if (blob == null) {
                    messageCollection.addError("Failed to read " + filenames.get(randomIdx) + " : null blob.");
                } else {
                    messageCollection
                            .addInfo("Can read file: " + blob.getBlobId() + ", size: " + blob.getContent().length);
                }
            }
            return filenames;
        } catch (BaseServiceException e) {
            String msg = "Exception when listing or reading bucket. ";
            messageCollection.addError(msg + e.toString());
            logger.error(msg, e);
            return Collections.emptyList();
        }
    }
}