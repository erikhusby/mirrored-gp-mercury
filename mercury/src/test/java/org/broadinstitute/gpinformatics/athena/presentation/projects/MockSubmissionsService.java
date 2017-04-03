/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.presentation.projects;

import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionConfig;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsServiceImpl;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Cookie;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import javax.annotation.Nonnull;

public class MockSubmissionsService extends SubmissionsServiceImpl {
    private MockSubmissionsService(SubmissionConfig submissionsConfig) {
        super(submissionsConfig);
    }

    public static MockSubmissionsService serviceWithResponse(ClientAndServer mockServer, HttpResponse httpResponse) {
        MockSubmissionConfig submissionConfig = new MockSubmissionConfig(mockServer);
        MockSubmissionsService mockSubmissionsService = new MockSubmissionsService(submissionConfig);
        Cookie sessionId = new Cookie("sessionId", String.format("%d", mockSubmissionsService.hashCode()));

        mockServer.when(HttpRequest.request().withCookies(sessionId)).respond(httpResponse);
        return mockSubmissionsService;
    }
}

@ConfigKey("submission")
class MockSubmissionConfig extends SubmissionConfig {
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
