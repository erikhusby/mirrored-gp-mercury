package org.broadinstitute.gpinformatics.infrastructure.sap;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;

/**
 * TODO scottmat fill in javadoc!!!
 */
public interface SapIntegrationClient extends Serializable {
    String testConnection(String age) throws IOException;
}
