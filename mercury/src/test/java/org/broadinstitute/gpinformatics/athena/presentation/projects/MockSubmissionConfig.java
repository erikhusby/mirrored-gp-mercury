package org.broadinstitute.gpinformatics.athena.presentation.projects;

import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionConfig;
import org.mockserver.integration.ClientAndServer;

import javax.annotation.Nonnull;

@ConfigKey("submission")
public class MockSubmissionConfig extends SubmissionConfig {
    public MockSubmissionConfig(@Nonnull Deployment deployment) {
        super(deployment);
    }

    public MockSubmissionConfig(ClientAndServer mockServer) {
        this(Deployment.DEV);
        if (!mockServer.isRunning()) {
            throw new RuntimeException("Mock server not started. Start it.");
        }
        super.setHost("127.0.0.1");
        super.setPort(mockServer.getPort());
    }
}
