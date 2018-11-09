package org.broadinstitute.gpinformatics.infrastructure.deployment;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Set;

/**
 * The class contains the config settings for the app itself. Use this to generate external links that refer back to
 * the application server, or for writing tests that need to communicate directly with the server.
 */
@SuppressWarnings("UnusedDeclaration")
@ConfigKey("app")
@ApplicationScoped
public class AppConfig extends AbstractConfig implements Serializable {

    // Force the JVM into headless mode. This avoids creating a visible icon when running the server locally,
    // since there are some references to awt classes in our code.
    static {
        System.setProperty("java.awt.headless", "true");
    }

    public AppConfig(){}

    @Inject
    public AppConfig(@Nonnull Deployment deploymentConfig) {
        super(deploymentConfig);
    }

    private String host;

    // Use empty string since port can be missing.
    private String port;

    private int jmsPort;

    private String workflowValidationEmail;

    private Set<String> gpBillingManagers;

    public String getUrl() {
        return "https://" + host + ":" + port + "/Mercury/";
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
     *
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

    public void setGpBillingManagers(Set<String> gpBillingManagers) {
        this.gpBillingManagers = gpBillingManagers;
    }

    public Set<String> getGpBillingManagers() {
        return gpBillingManagers;
    }

    public static AppConfig produce(Deployment deployment) {
        return produce(AppConfig.class, deployment);
    }

}
