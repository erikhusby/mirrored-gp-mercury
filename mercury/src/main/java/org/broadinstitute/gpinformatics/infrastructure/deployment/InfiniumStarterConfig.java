package org.broadinstitute.gpinformatics.infrastructure.deployment;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Configuration for the infinium run starter processing.
 */
@SuppressWarnings("UnusedDeclaration")
@ConfigKey("infiniumStarter")
@ApplicationScoped
public class InfiniumStarterConfig extends AbstractConfig implements LoginAndPassword, Serializable {
    private Logger log = Logger.getLogger(InfiniumStarterConfig.class);
    private String dataPath;
    private long minimumIdatFileLength;
    private String decodeDataPath;
    private String archivePath;
    private int numChipsPerArchivePeriod;
    private String jmsHost;
    private int jmsPort;
    private String jmsQueue;
    private String login;
    private String password;
    private String passwordFileName;

    public InfiniumStarterConfig(){}

    @Inject
    public InfiniumStarterConfig(@Nonnull Deployment deploymentConfig) {
        super(deploymentConfig);
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public long getMinimumIdatFileLength() {
        return minimumIdatFileLength;
    }

    public void setMinimumIdatFileLength(long minimumIdatFileLength) {
        this.minimumIdatFileLength = minimumIdatFileLength;
    }

    public String getDecodeDataPath() {
        return decodeDataPath;
    }

    public void setDecodeDataPath(String decodeDataPath) {
        this.decodeDataPath = decodeDataPath;
    }

    public String getArchivePath() {
        return archivePath;
    }

    public void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }

    public int getNumChipsPerArchivePeriod() {
        return numChipsPerArchivePeriod;
    }

    public void setNumChipsPerArchivePeriod(int numChipsPerArchivePeriod) {
        this.numChipsPerArchivePeriod = numChipsPerArchivePeriod;
    }

    public String getJmsHost() {
        return jmsHost;
    }

    public void setJmsHost(String jmsHost) {
        this.jmsHost = jmsHost;
    }

    public int getJmsPort() {
        return jmsPort;
    }

    public void setJmsPort(int jmsPort) {
        this.jmsPort = jmsPort;
    }

    public String getJmsQueue() {
        return jmsQueue;
    }

    public void setJmsQueue(String jmsQueue) {
        this.jmsQueue = jmsQueue;
    }

    @Override
    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    @Override
    public String getPassword() {
        if (password == null && passwordFileName != null) {
            File homeDir = new File(System.getProperty("user.home"));
            File passwordsFile = new File(homeDir, passwordFileName);
            try {
                if (passwordsFile.exists()) {
                    password = FileUtils.readFileToString(passwordsFile).trim();
                } else {
                    String errMsg = "Pipeline password file not found: " + passwordsFile.getPath();
                    log.error(errMsg);
                    throw new RuntimeException(errMsg);
                }
            } catch (IOException e) {
                log.error("Failed to read password file: " + passwordsFile.getPath(), e);
                throw new RuntimeException(e.getMessage());
            }
        }
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordFileName() {
        return passwordFileName;
    }

    public void setPasswordFileName(String passwordFileName) {
        this.passwordFileName = passwordFileName;
    }
}
