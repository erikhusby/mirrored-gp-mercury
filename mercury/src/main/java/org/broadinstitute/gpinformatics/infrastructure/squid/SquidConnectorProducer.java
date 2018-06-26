package org.broadinstitute.gpinformatics.infrastructure.squid;

/**
 * <strong>Not a CDI producer!</strong><br/>
 * Provides non-CDI managed test instances only
 */
public class SquidConnectorProducer {

    public static SquidConnector stubInstance() {
        return new SquidConnectorStub();
    }

    public static SquidConnector failureStubInstance() {
        return new SquidConnectorFailureStub();
    }


}
