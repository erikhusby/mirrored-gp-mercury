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
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import java.util.ArrayList;
import java.util.List;

public class MockServerTest {
    private ClientAndServer mockServer;

    @BeforeTest(alwaysRun = true)
    protected void startMockServer() {
        mockServer = ClientAndServer.startClientAndServer(getPortList());
    }

    private Integer[] getPortList() {
        List<Integer> portList = new ArrayList<>();
        while (portList.size() < 20) {
            portList.add(6000 + portList.size());
        }
        return portList.toArray(new Integer[portList.size()]);
    }

    @AfterTest(alwaysRun = true)
    protected void stopMockServer() {
        mockServer.stop();
    }

    protected SubmissionsService serviceWithResponse(org.mockserver.model.HttpResponse httpResponse) {
        return MockSubmissionsService.serviceWithResponse(mockServer, httpResponse);
    }
}
