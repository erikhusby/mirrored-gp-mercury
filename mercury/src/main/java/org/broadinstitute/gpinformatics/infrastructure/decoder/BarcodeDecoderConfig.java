package org.broadinstitute.gpinformatics.infrastructure.decoder;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Connection information for the Barcode Decoder web application.
 */
@SuppressWarnings("UnusedDeclaration")
@ConfigKey("decoder")
@ApplicationScoped
public class BarcodeDecoderConfig extends AbstractConfig implements Serializable {

    private String host;

    private int port;

    public BarcodeDecoderConfig() {
    }

    @Inject
    public BarcodeDecoderConfig(@Nonnull Deployment deploymentConfig) {
        super(deploymentConfig);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUrl(String suffix) {
        return String.format("%s%s:%d/%s", getHttpScheme(),getHost(), getPort(), suffix);
    }
}
