package org.broadinstitute.gpinformatics.infrastructure.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftFileAccessor;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterTest;

/**
 * Super class for all TestGroups.STUBBY test classes <br />
 * Renamed to make clear that the @Deployment method builds using Deployment.STUBBY
 */
public class StubbyContainerTest extends Arquillian {
    private static final Log log = LogFactory.getLog(StubbyContainerTest.class);

    @Deployment
    /**
     * Builds STUBBY deployment with stub CDI alternatives
     */
    public static WebArchive buildMercuryWar() {
        WebArchive stubbyArchive = DeploymentBuilder.buildMercuryWarWithAlternatives(
                "org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceStub",
                "org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcherStub",
                "org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortSearchServiceStub",
                "org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleReceiptServiceStub",
                "org.broadinstitute.gpinformatics.infrastructure.bsp.StubbyBSPSampleSearchService",
                "org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestServiceStub",
                "org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceStub",
                "org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceStub",
                "org.broadinstitute.gpinformatics.infrastructure.ws.WsMessageStoreStub",
                "org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsServiceStub",
                "org.broadinstitute.gpinformatics.infrastructure.thrift.StubbyThriftService"
        ).addAsResource(ThriftFileAccessor.RUN_FILE);
        return stubbyArchive;
    }

    @AfterTest
    public void tearDown() throws Exception {
        log.debug("Trying to force Clover to flush");
        ///CLOVER:FLUSH
    }
}
