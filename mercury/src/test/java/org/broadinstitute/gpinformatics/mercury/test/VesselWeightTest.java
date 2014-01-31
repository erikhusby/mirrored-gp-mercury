package org.broadinstitute.gpinformatics.mercury.test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Tests InitialTare and SampleReceipt messages
 */
public class VesselWeightTest extends Arquillian {

    @Inject
    private BSPConfig bspConfig;

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testEndToEnd() {
        // The following is a plan for GPLIM-2079
        // todox add receptacleWeight (as opposed to sampleWeight) to bettalims.xsd receptacleType
        // todox add weight attribute to LabVessel
        // todo update LabVessel weight somewhere in LabEventFactory (location TBD)
        // todox add INITIAL_TARE to LabEventType
        // todo add weight to data warehouse?

        // todox add weight handling to LabEventFactory.updateVolumeConcentration
        // todox BSP add receptacleWeight to SetVolumeAndConcentration, use it to set "Tare Weight" annotation
        // todo service to provide machine name for previous message, so can check that initial tare and receipt weight are done on same machine?

        // Send an InitialTare message with receptacleWeight
        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(false);
        // Can't find a BSP web service to create tubes, so have to use existing
        List<String> tubeBarcodes = new ArrayList<>();
        tubeBarcodes.add("1073785008");
        tubeBarcodes.add("1069803776");
        PlateEventType initialTareEvent = bettaLimsMessageTestFactory.buildRackEvent(
                LabEventType.INITIAL_TARE.getName(), "TARETEST", tubeBarcodes);
        Random random = new SecureRandom();
        BigDecimal tube1Tare = new BigDecimal(Math.abs(random.nextInt(10000)));
        BigDecimal tube2Tare = new BigDecimal(Math.abs(random.nextInt(10000)));
        initialTareEvent.getPositionMap().getReceptacle().get(0).setReceptacleWeight(tube1Tare);
        initialTareEvent.getPositionMap().getReceptacle().get(1).setReceptacleWeight(tube2Tare);
        BettaLIMSMessage initialTareMessage = new BettaLIMSMessage();
        initialTareMessage.getPlateEvent().add(initialTareEvent);
        bettaLimsMessageResource.processMessage(initialTareMessage);

        // Verify that BSP Tare Weight annotation is set
        // http://bsp/ws/bsp/sample/gettareweight?manufacturer_barcodes=1082117278
        String getTareWeightUrl = bspConfig.getWSUrl("sample/gettareweight");
        Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter(bspConfig.getLogin(), bspConfig.getPassword()));
        String response = client.resource(getTareWeightUrl)
                .queryParam("manufacturer_barcodes", tubeBarcodes.get(0))
                .get(String.class);
        BufferedReader bufferedReader = new BufferedReader(new StringReader(response));
        try {
            bufferedReader.readLine();
            String[] fields = bufferedReader.readLine().split("\t");
            Assert.assertEquals(fields[2], tube1Tare.toPlainString() + ".0");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException ignored) {
            }
        }

        // Send a SampleReceipt message with volume (and weight?)
        // Verify that BSP volume is set
    }
}
