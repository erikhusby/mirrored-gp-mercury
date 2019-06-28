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

package org.broadinstitute.gpinformatics.infrastructure;

import org.broadinstitute.gpinformatics.athena.presentation.projects.MockSubmissionsService;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsService;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public class MockServerTest {
    private ClientAndServer mockServer;

    @BeforeClass(alwaysRun = true)
    public void startMockServer() {
        mockServer = ClientAndServer.startClientAndServer(0);
    }

    @AfterClass(alwaysRun = true)
    public void stopMockServer() {
        mockServer.stop();
    }

    protected SubmissionsService serviceWithResponse(HttpResponse httpResponse) {
        return MockSubmissionsService.serviceWithResponse(mockServer, httpResponse);
    }
}
