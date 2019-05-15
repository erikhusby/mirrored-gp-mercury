package org.broadinstitute.gpinformatics.mercury.control.dao.storage;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.IamScopes;
import com.google.api.services.iam.v1.model.CreateServiceAccountKeyRequest;
import com.google.api.services.iam.v1.model.ServiceAccountKey;
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
import org.apache.tools.ant.filters.StringInputStream;
import org.broadinstitute.bsp.client.util.MessageCollection;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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
    private ServiceAccountCredentials credentials = null;
    private ServiceAccountCredentials writerCredentials = null;
    private File credentialFile = null;
    private static Log logger = LogFactory.getLog(GoogleBucketDao.class);

    public void setConfigGoogleStorageConfig(GoogleStorageConfig googleStorageConfig) {
        this.googleStorageConfig = googleStorageConfig;
    }

    /**
     * Returns a list of the files' blobIds in the bucket.
     */
    public List<String> list(MessageCollection messageCollection) {
        makeCredential(messageCollection);
        if (credentials == null) {
            return Collections.emptyList();
        } else {
            try {
                Storage storage = StorageOptions.newBuilder().setCredentials(credentials)
                        .setProjectId(credentials.getProjectId()).build().getService();
                Set<String> names = new HashSet<>();
                storage.list(googleStorageConfig.getBucketName()).iterateAll()
                        .forEach(blob -> names.add(blob.getName()));
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
                        .setProjectId(credentials.getProjectId()).build().getService();
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
                        .setProjectId(writerCredentials.getProjectId()).build().getService();
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
            File homeDir = new File(System.getProperty("user.home"));
            credentialFile = new File(homeDir, googleStorageConfig.getCredentialFilename());
            if (credentialFile.exists()) {
                try {
                    credentials = ServiceAccountCredentials.fromStream(new FileInputStream(credentialFile));
                } catch (FileNotFoundException e) {
                    messages.addError("Credential file is missing: %s", credentialFile.getAbsolutePath());
                } catch (IOException e) {
                    messages.addError("Credential file stream gives: %s", e.toString());
                } catch (BaseServiceException e) {
                    String msg = "Error getting Google Service Account credentials. ";
                    messages.addError(msg + e.toString());
                    logger.error(msg, e);
                }
            } else {
                messages.addError("Credential file is missing: %s", credentialFile.getAbsolutePath());
            }
        } else {
            try {
                credentials.refreshIfExpired();
            } catch (IOException e) {
                credentials = null;
                makeCredential(messages);
            }
        }
    }

    /**
     * Authorizes Google Storage access for Mercury as a service account.
     */
    private void makeWriterCredential(MessageCollection messages) {
        if (writerCredentials == null) {
            File homeDir = new File(System.getProperty("user.home"));
            File writerCredentialFile = new File(homeDir, googleStorageConfig.getWriterCredentialFilename());
            if (writerCredentialFile.exists()) {
                try {
                    writerCredentials = ServiceAccountCredentials.fromStream(new FileInputStream(writerCredentialFile));
                } catch (FileNotFoundException e) {
                    messages.addError("Writer credential file is missing: %s", writerCredentialFile.getAbsolutePath());
                } catch (IOException e) {
                    messages.addError("Writer credential file stream gives: %s", e.toString());
                } catch (BaseServiceException e) {
                    String msg = "Error getting Google Service Account credentials. ";
                    messages.addError(msg + e.toString());
                    logger.error(msg, e);
                }
            } else {
                messages.addError("Writer credential file is missing: %s", writerCredentialFile.getAbsolutePath());
            }
        } else {
            try {
                writerCredentials.refreshIfExpired();
            } catch (IOException e) {
                writerCredentials = null;
                makeWriterCredential(messages);
            }
        }
    }

    /**
     * Generates status messages while reading a bucket file and returns a list of all filenames.
     */
    public List<String> test(MessageCollection messageCollection) {
        File homeDir = new File(System.getProperty("user.home"));
        if (StringUtils.isBlank(googleStorageConfig.getCredentialFilename()) ||
                StringUtils.isBlank(googleStorageConfig.getBucketName())) {
            messageCollection.addError("mercury-config.yaml is missing MayoManifestGoogleStorage configuration.");
            return Collections.emptyList();
        }
        File credentialFile = new File(homeDir, googleStorageConfig.getCredentialFilename());
        try {
            if (!credentialFile.exists() || !credentialFile.canRead() ||
                    StringUtils.isBlank(FileUtils.readFileToString(credentialFile))) {
                messageCollection.addError("Credential file " + credentialFile.getAbsolutePath() +
                        " is missing, unreadable, or empty.");
                return Collections.emptyList();
            } else if (!credentialFile.canWrite()) {
                messageCollection.addError("Credential file " + credentialFile.getAbsolutePath() +
                        " must be writable by Mercury.");
            }
        } catch (Exception e) {
            messageCollection.addError("Exception when reading credentialFile " + credentialFile.getAbsolutePath() +
                    " : " + e.toString());
            return Collections.emptyList();
        }
        try {
            credentials = ServiceAccountCredentials.fromStream(new FileInputStream(credentialFile));
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
            messageCollection.addInfo("Credential file has AuthenticationType: " + credentials.getAuthenticationType());
            messageCollection.addInfo("Token server: " + credentials.getTokenServerUri().toString());
            messageCollection.addInfo("Account: " + credentials.getAccount());
            messageCollection.addInfo("Project: " + credentials.getProjectId());
        } catch (Exception e) {
            messageCollection.addError("Exception when extracting data from GoogleCredential: " + e.toString());
            return Collections.emptyList();
        }
        try {
            Storage storage = StorageOptions.newBuilder().setCredentials(credentials).
                    setProjectId(credentials.getProjectId()).build().getService();
            messageCollection.addInfo("Storage object service account: " +
                    storage.getServiceAccount(credentials.getProjectId()).toString());
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

    public void rotateServiceAccountKey(MessageCollection messages) {
        makeCredential(messages);
        // If Mercury can successfully login, Mercury can generate a new credential (the json "key")
        // and then write it to the configured filename.
        // If Mercury cannot login, a message is output explaining how to manually create a credential file.
        if (credentials != null) {
            try {
                JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
                HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
                GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(credentialFile));
                if (credential.createScopedRequired()) {
                    credential = credential.createScoped(IamScopes.all());
                }
                Iam.Projects.ServiceAccounts serviceAccounts =
                        new Iam.Builder(transport, jsonFactory, credential).build().projects().serviceAccounts();
                String uniqueId = String.format("projects/%s/serviceAccounts/%s", credentials.getProjectId(),
                        credentials.getAccount());
                // Creates a new key and writes it to the credential file, overwriting the old key.
                ServiceAccountKey newKey = serviceAccounts.keys().
                        create(uniqueId, new CreateServiceAccountKeyRequest()).execute();
                String newKeyJson = new String(Base64.getDecoder().decode(newKey.getPrivateKeyData()));
                credentials = ServiceAccountCredentials.fromStream(new StringInputStream(newKeyJson));
                // If credentials was created ok (i.e. no exceptions), writes the json to the credential file.
                FileUtils.writeStringToFile(credentialFile, newKeyJson, "US-ASCII", false);
            } catch (Exception e) {
                String msg = "Failed to create new key for the service account: ";
                messages.addError(msg + e.toString());
            }
        } else {
            boolean isProd = googleStorageConfig.getCredentialFilename().contains("/prod_");
            boolean isDev = googleStorageConfig.getCredentialFilename().contains("/dev_");
            boolean isRc = googleStorageConfig.getCredentialFilename().contains("/rc_");
            assert(isProd || isDev || isRc);
            String loginAs = isProd ? "pmi-ops.org" : isRc ? "rc-reader" : "dev-reader";
            String serviceAccountName = isProd ? "awardee-broad@all-of-us-rdr-stable.iam.gserviceaccount.com" :
                    isRc ? "rc-reader@mercury-mayobucket-test-23591" : "dev-reader@mercury-mayobucket-test-23591";
            String msg = "If no other Google credential errors exist, the Google service account credential " +
                    "needs to be manually regenerated. Use these steps: " +
                    "Login with your \"" + loginAs + "\" role account to " +
                    "the Google Cloud Console at https://console.cloud.google.com. " +
                    "Click the command prompt icon at the top right (\"Activate Cloud Shell\") " +
                    "to get a shell within the browser window. At the prompt run: " +
                    "\"gcloud iam service-accounts keys create newKey.json --iam-account " + serviceAccountName +
                    "\" to generate a new credential (a json file). " +
                    "Then run: \"cat newKey.json\" to output the json file to the shell window. " +
                    "Copy-paste the json from the shell window into an editor on your computer. " +
                    "Remove any line breaks found in \"private_key\" element so that it is one long line. " +
                    "Write the edited json to " + credentialFile.getAbsolutePath() +
                    " on the server where Mercury is running. Change the file permissions to allow Mercury to " +
                    "write the file in the future. No need to restart Mercury. " +
                    "To check if it was successful click Mercury -> Admin -> Mayo Manifest Admin -> Test Bucket Access";
            messages.addError(msg);
        }
    }
}