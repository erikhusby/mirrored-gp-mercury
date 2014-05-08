package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExtractionsBloodJaxbBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Tests ExtractionsCryovial Workflow.
 * HamiltonSampleCarrier32 to deepwell to deepwell to rack
 */
public class ExtractionsCryovialMessagingEndToEndTest  extends Arquillian {


    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    private final SimpleDateFormat testSuffixDateFormat = new SimpleDateFormat("MMddHHmmss");

    @Test
    public void testEndToEnd() {
        String testSuffix = testSuffixDateFormat.format(new Date());

        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(false);

        // Can't find a BSP web service to create tubes, so have to use existing
        List<String> tubeBarcodes = new ArrayList<>();
        tubeBarcodes.add("1073785008");
        tubeBarcodes.add("1069803776");

        ExtractionsBloodJaxbBuilder extractionsBloodJaxbBuilder = new ExtractionsBloodJaxbBuilder(
                bettaLimsMessageTestFactory, testSuffix, tubeBarcodes, "BLOODCRYOVIALTEST"
        ).invoke();

        for(BettaLIMSMessage bettaLIMSMessage: extractionsBloodJaxbBuilder.getMessageList()){
            bettaLimsMessageResource.processMessage(bettaLIMSMessage);
        }
    }
}
