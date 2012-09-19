package org.broadinstitute.gpinformatics.athena.infrastructure.bsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.enterprise.inject.Default;

@Default // used in fast unit tests, non-integration.
public class MockBSPConnector implements BSPConnector {

    private static Log gLog = LogFactory.getLog(MockBSPConnector.class);

}
