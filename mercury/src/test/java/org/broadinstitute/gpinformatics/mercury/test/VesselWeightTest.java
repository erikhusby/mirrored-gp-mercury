package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Tests InitialTare and SampleReceipt messages
 */
@Test(groups = TestGroups.STANDARD)
public class VesselWeightTest extends Arquillian {

    @Inject
    private BSPConfig bspConfig;

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testEndToEnd() {
        // todo jmt add weight to data warehouse?
        // todo jmt service to provide machine name for previous message, so can check that initial tare and receipt weight are done on same machine?

        // Send an InitialTare message with receptacleWeight
        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(false);
        // Can't find a BSP web service to create tubes, so have to use existing
        List<String> tubeBarcodes = new ArrayList<>();
        tubeBarcodes.add("1075671760");
        tubeBarcodes.add("1075671761");
        PlateEventType initialTareEvent = bettaLimsMessageTestFactory.buildRackEvent(
                LabEventType.INITIAL_TARE.getName(), "TARETEST", tubeBarcodes);
        Random random = new SecureRandom();
        BigDecimal tube1Tare = new BigDecimal(random.nextInt(10000));
        BigDecimal tube2Tare = new BigDecimal(random.nextInt(10000));
        initialTareEvent.getPositionMap().getReceptacle().get(0).setReceptacleWeight(tube1Tare);
        initialTareEvent.getPositionMap().getReceptacle().get(1).setReceptacleWeight(tube2Tare);
        BettaLIMSMessage initialTareMessage = new BettaLIMSMessage();
        initialTareMessage.getPlateEvent().add(initialTareEvent);
        bettaLimsMessageResource.processMessage(initialTareMessage);

        BarcodedTube tube1 = barcodedTubeDao.findByBarcode(tubeBarcodes.get(0));
        Assert.assertEquals(tube1.getReceptacleWeight(), tube1Tare);

        // Verify that BSP Tare Weight annotation is set
        // http://bsp/ws/bsp/sample/gettareweight?manufacturer_barcodes=1082117278
        String getTareWeightUrl = bspConfig.getWSUrl("sample/gettareweight");
        ClientBuilder clientBuilder = JerseyUtils.getClientBuilderAcceptCertificate();

        Client client = clientBuilder.newClient();
        client.register(new BasicAuthentication(bspConfig.getLogin(), bspConfig.getPassword()));
        String response = client.target(getTareWeightUrl)
                .queryParam("manufacturer_barcodes", tubeBarcodes.get(0))
                .request()
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
        bettaLimsMessageTestFactory.advanceTime();
        PlateEventType sampleReceiptEvent = bettaLimsMessageTestFactory
                .buildRackEvent(LabEventType.SAMPLE_RECEIPT.getName(), "SRECEIPTTEST", tubeBarcodes);
        BigDecimal tube1Volume = new BigDecimal(Math.abs(random.nextInt(10000)));
        BigDecimal tube2Volume = new BigDecimal(Math.abs(random.nextInt(10000)));
        sampleReceiptEvent.getPositionMap().getReceptacle().get(0).setVolume(tube1Volume);
        sampleReceiptEvent.getPositionMap().getReceptacle().get(1).setVolume(tube2Volume);
        BettaLIMSMessage sampleReceiptMessage = new BettaLIMSMessage();
        sampleReceiptMessage.getPlateEvent().add(sampleReceiptEvent);
        bettaLimsMessageResource.processMessage(sampleReceiptMessage);

        // Verify that BSP volume is set
        Map<String, GetSampleDetails.SampleInfo> mapBarcodeToSampleInfo =
                sampleDataFetcher.fetchSampleDetailsByBarcode(tubeBarcodes);
        Assert.assertEquals(mapBarcodeToSampleInfo.get(tubeBarcodes.get(0)).getVolume(), tube1Volume.floatValue());
    }
}
