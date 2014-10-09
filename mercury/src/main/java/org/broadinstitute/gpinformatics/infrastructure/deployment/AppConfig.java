package org.broadinstitute.gpinformatics.infrastructure.deployment;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.security.ApplicationInstance;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * The class contains the config settings for the app itself. Use this to generate external links that refer back to
 * the application server, or for writing tests that need to communicate directly with the server.
 */
@SuppressWarnings("UnusedDeclaration")
@ConfigKey("app")
public class AppConfig extends AbstractConfig implements Serializable {

    @Inject
    public AppConfig(@Nonnull Deployment mercuryDeployment) {
        super(mercuryDeployment);
    }

    private String host;

    // Use empty string since port can be missing.
    private String port;

    private int jmsPort;

    private String workflowValidationEmail;

    public String getUrl() {
        if (!StringUtils.isBlank(port)) {
            return "https://" + host + ":" + port + "/Mercury/";
        }
        return "https://" + host + ":8443" + "/Mercury/";
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setJmsPort(int jmsPort) {
        this.jmsPort = jmsPort;
    }

    public int getJmsPort() {
        return jmsPort;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    /**
     * Should we send emails in this deployment?
     * @return
     */
    public boolean shouldSendEmail() {
        return !StringUtils.isBlank(workflowValidationEmail);
    }

    public String getWorkflowValidationEmail() {
        return workflowValidationEmail;
    }

    public void setWorkflowValidationEmail(String workflowValidationEmail) {
        this.workflowValidationEmail = workflowValidationEmail;
    }

    public static AppConfig produce(Deployment deployment) {
        return produce(AppConfig.class, deployment);
    }


}
