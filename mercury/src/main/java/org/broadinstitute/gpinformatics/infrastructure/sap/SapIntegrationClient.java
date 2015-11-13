package org.broadinstitute.gpinformatics.infrastructure.sap;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * TODO scottmat fill in javadoc!!!
 */
public interface SapIntegrationClient {
    String testConnection(String age) throws IOException;
}
