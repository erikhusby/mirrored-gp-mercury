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
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
    private static Log logger = LogFactory.getLog(GoogleBucketDao.class);

    public void setConfigGoogleStorageConfig(GoogleStorageConfig googleStorageConfig) {
        this.googleStorageConfig = googleStorageConfig;
    }

    /**
     * Returns a list of the files' blobIds in the bucket.
     */
    public List<String> list(MessageCollection messageCollection) {
        if (makeCredential(false, messageCollection)) {
            try {
                Storage storage = StorageOptions.newBuilder().setCredentials(credentials)
                        .setProjectId(credentials.getProjectId()).build().getService();
                Set<String> names = new HashSet<>();
                storage.list(googleStorageConfig.getBucketName()).iterateAll()
                        .forEach(blob -> names.add(blob.getName()));
                return names.stream().sorted().collect(Collectors.toList());
            } catch (Exception e) {
                String msg = "Error getting file list from Google bucket. ";
                messageCollection.addError(msg + e.toString());
                logger.error(msg, e);
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns true if file exists.
     */
    public boolean exists(String filename, MessageCollection messageCollection) {
        if (makeCredential(false, messageCollection)) {
            try {
                Storage storage = StorageOptions.newBuilder().setCredentials(credentials)
                        .setProjectId(credentials.getProjectId()).build().getService();
                Blob blob = storage.get(BlobId.of(googleStorageConfig.getBucketName(), filename));
                return blob != null && blob.exists();
            } catch (Exception e) {
                String msg = "Error looking for " + filename + ": " + e.getMessage();
                messageCollection.addError(msg);
                logger.error(msg, e);
            }
        }
        return false;
    }

    /**
     * Reads and returns content of a file as bytes, or null if no such file.
     */
    public byte[] download(String filename, MessageCollection messageCollection) {
        if (makeCredential(false, messageCollection)) {
            try {
                Storage storage = StorageOptions.newBuilder().setCredentials(credentials)
                        .setProjectId(credentials.getProjectId()).build().getService();
                Blob blob = storage.get(BlobId.of(googleStorageConfig.getBucketName(), filename));
                return (blob != null) ? blob.getContent() : null;
            } catch (Exception e) {
                String msg = "Error reading " + filename + " from Google bucket. ";
                messageCollection.addError(msg + e.toString());
                logger.error(msg, e);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Writes a file to the bucket for Arquillian testing.
     */
    public void upload(String filename, byte[] content, MessageCollection messageCollection) {
        if (makeCredential(true, messageCollection)) {
            try {
                Storage storage = StorageOptions.newBuilder().setCredentials(writerCredentials)
                        .setProjectId(writerCredentials.getProjectId()).build().getService();
                BlobInfo blobInfo = BlobInfo.newBuilder(googleStorageConfig.getBucketName(), filename).
                        setContentType("text/plain").build();
                Blob blob = storage.get(blobInfo.getBlobId());
                // This will create a new file or overwrite an existing one.
                storage.create(blobInfo, content);
            } catch (Exception e) {
                String msg = "Error writing " + filename + " from Google bucket. ";
                messageCollection.addError(msg + e.toString());
                logger.error(msg, e);
            }
        }
    }

    /**
     * Authorizes Google Storage access for Mercury as a service account.
     * @param forWriting should only be true when writing files to the test bucket for unit testing.
     * @return false if there was a credential error, true if ok.
     */
    private boolean makeCredential(boolean forWriting, MessageCollection messages) {
        String errorMessage = null;
        Exception exception = null;
        if ((forWriting ? writerCredentials : credentials) == null) {
            File homeDir = new File(System.getProperty("user.home"));
            File file = new File(homeDir, forWriting ? googleStorageConfig.getWriterCredentialFilename() :
                    googleStorageConfig.getCredentialFilename());
            if (file.exists()) {
                try {
                    ServiceAccountCredentials serviceAccountCredentials =
                            ServiceAccountCredentials.fromStream(new FileInputStream(file));
                    // Tests that the credential works. It's valid if no exception is thrown.
                    if (serviceAccountCredentials != null && StringUtils.isNotBlank(StorageOptions.newBuilder().
                            setCredentials(serviceAccountCredentials).
                            setProjectId(serviceAccountCredentials.getProjectId()).
                            build().getService().
                            getServiceAccount(serviceAccountCredentials.getProjectId()).toString())) {
                        if (forWriting) {
                            writerCredentials = serviceAccountCredentials;
                        } else {
                            credentials = serviceAccountCredentials;
                        }
                    } else {
                        errorMessage = "Failed to make a valid Google service account credential. ";
                    }
                } catch (FileNotFoundException e) {
                    exception = e;
                    errorMessage = "Credential file is missing: " + file.getAbsolutePath();
                } catch (IOException e) {
                    exception = e;
                    errorMessage = "Exception while reading the credential file: " + e.toString();
                } catch (Exception e) {
                    exception = e;
                    errorMessage = "Exception while making a Google Service Account credential: " + e.toString();
                }
            } else {
                errorMessage = "Credential file is missing: " + file.getAbsolutePath();
            }
        }
        if ((forWriting ? writerCredentials : credentials) != null) {
            return true;
        }
        messages.addError(errorMessage);
        if (exception == null) {
            logger.error(errorMessage);
        } else {
            logger.error(errorMessage, exception);
        }
        return false;
    }

    /**
     * Writes a new credential file.
     *
     * @param writerCredential indicates whether it's the writer or reader credential.
     * @param content the json file content.
     * @param messageCollection for returning any error or info messages.
     */
    public void uploadCredential(boolean writerCredential, String content, MessageCollection messageCollection) {
        FileWriter fileWriter = null;
        try {
            File homeDir = new File(System.getProperty("user.home"));
            String filename = writerCredential ?
                    googleStorageConfig.getWriterCredentialFilename() : googleStorageConfig.getCredentialFilename();
            File file = new File(homeDir, filename);
            if (file.exists() && !file.canWrite()) {
                messageCollection.addError("Existing file is not writable: " + file.getAbsolutePath());
            } else {
                fileWriter = new FileWriter(file);
                fileWriter.write(content);
                messageCollection.addInfo("Wrote new credential content to " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("While writing credential: ", e);
            messageCollection.addError("Cannot write new credential: " + e.toString());
        } finally {
            IOUtils.closeQuietly(fileWriter);
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
            // Reading a non-existent file should return null. If it throws, there's a problem.
            storage.get(BlobId.of(googleStorageConfig.getBucketName(), "ProbablyDoesNotExist"));
            // Tries to get a file listing.
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
        } catch (Exception e) {
            messageCollection.addError("Exception when listing or reading bucket. " + e.toString());
            return Collections.emptyList();
        }
    }

    /**
     * Generates a new credential (the json "key") for Mercury to access the Google storage bucket
     * and writes the new credential to the configured filename, overwriting the existing old credential.
     * If there's a problem a message is output explaining how to manually create the credential file.
     */
    public void rotateServiceAccountKey(MessageCollection messages) {
        if (makeCredential(false, messages)) {
            File homeDir = new File(System.getProperty("user.home"));
            File credentialFile = new File(homeDir, googleStorageConfig.getCredentialFilename());
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
                // The credential is tested. It's valid if no exception is thrown.
                if (StringUtils.isNotBlank(StorageOptions.newBuilder().
                        setCredentials(credentials).setProjectId(credentials.getProjectId()).
                        build().getService().
                        getServiceAccount(credentials.getProjectId()).toString())) {

                    // Writes the json to the credential file.
                    FileUtils.writeStringToFile(credentialFile, newKeyJson, "US-ASCII", false);
                    messages.addInfo("Wrote new service account key to " + credentialFile.getAbsolutePath());
                }
            } catch (Exception e) {
                String msg = "Failed to create new key for the Google service account. " +
                        "It may need to be manually regenerated. ";
                messages.addError(msg + e.toString());
                logger.error(msg, e);
            }
        }
    }
}