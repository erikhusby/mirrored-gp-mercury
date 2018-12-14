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

import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionConfig;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsServiceImpl;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

public class MockSubmissionsService extends SubmissionsServiceImpl {
    private MockSubmissionsService(SubmissionConfig submissionsConfig) {
        super(submissionsConfig);
    }

    public static MockSubmissionsService serviceWithResponse(ClientAndServer mockServer, HttpResponse httpResponse) {
        MockSubmissionConfig submissionConfig = new MockSubmissionConfig(mockServer);
        MockSubmissionsService mockSubmissionsService = new MockSubmissionsService(submissionConfig);

        mockServer.when(HttpRequest.request()).respond(httpResponse);
        return mockSubmissionsService;
    }
}

