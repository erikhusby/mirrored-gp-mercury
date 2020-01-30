package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.mercury.control.vessel.FluidigmRunFactoryTest.FLUIDIGM_OUTPUT_CSV;

@Test(groups = TestGroups.STANDARD)
public class FluidigmRunFactoryContainerTest extends Arquillian {

    @Inject
    private LabEventDao labEventDao;

    @Inject
    private FluidigmRunFactory fluidigmRunFactory;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testBasic() throws IOException {
        String platerackBarcodearcode = "FPTestTubeRack" + System.currentTimeMillis();
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();

        String failSampleId = null;
        String passFemaleSmId = null;
        String posControlSmId = null;
        for (VesselPosition vesselPosition : RackOfTubes.RackType.Matrix96.getVesselGeometry().getVesselPositions()) {
            BarcodedTube barcodedTube = new BarcodedTube(platerackBarcodearcode + vesselPosition.toString());
            barcodedTube.setVolume(new BigDecimal("75"));
            String sm = "SM-" + vesselPosition.name() + platerackBarcodearcode;
            MercurySample mercurySample = new MercurySample(sm, MercurySample.MetadataSource.MERCURY);
            barcodedTube.addSample(mercurySample);
            mapPositionToTube.put(vesselPosition, barcodedTube);

            if (vesselPosition == VesselPosition.A01) {
                passFemaleSmId = sm;
            } else if (vesselPosition == VesselPosition.A05) {
                // The positive control is in A05
                BspSampleData sampleData = new BspSampleData(new HashMap<BSPSampleSearchColumn, String>() {{
                    put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, "NA12878");
                }});
                mercurySample.setSampleData(sampleData);
                posControlSmId = sm;
            } else if (vesselPosition == VesselPosition.H09) {
                // Fails in the test upload due to bad call rate
                failSampleId = sm;
            }
        }

        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);

        String plateBarcode = "FPTestIFCChip" + System.currentTimeMillis();
        StaticPlate staticPlate1 = new StaticPlate(plateBarcode, StaticPlate.PlateType.Fluidigm96_96AccessArrayIFC);
        mapBarcodeToPlate.put(staticPlate1.getLabel(), staticPlate1);

        LabEvent labEvent = new LabEvent(LabEventType.FINGERPRINTING_IFC_TRANSFER, new Date(), "BATMAN", 1L, 101L,
                "Bravo");
        labEvent.getSectionTransfers().add(new SectionTransfer(tubeFormation.getContainerRole(), SBSSection.ALL96,
                null, staticPlate1.getContainerRole(), SBSSection.P384COLS7_12BYROW, null, labEvent));


        labEventDao.persist(labEvent);

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        String date = sdf.format(new Date());

        InputStream testSpreadSheet = VarioskanParserTest.getSpreadsheet(FLUIDIGM_OUTPUT_CSV);
        String csv = IOUtils.toString(testSpreadSheet);
        csv = csv.replace("1382363061", plateBarcode);
        csv = csv.replace("1/3/2018 2:41:43 PM", date);
        testSpreadSheet.close();

//        FileUtils.writeStringToFile(new File("/Users/jowalsh/Documents/TestData/Fingerprints/Fingerprints.txt"), csv);

        InputStream uploadStream = new ByteArrayInputStream(csv.getBytes());
        MessageCollection messageCollection = new MessageCollection();
        Pair<StaticPlate, LabMetricRun> fluidigmChipRun = fluidigmRunFactory
                .createFluidigmChipRun(uploadStream, BSPManagerFactoryStub.QA_DUDE_USER_ID, messageCollection);

        Assert.assertEquals(messageCollection.getErrors().size(), 0);
        Assert.assertNotNull(fluidigmChipRun);

        MercurySample femaleMercurySample = mercurySampleDao.findBySampleKey(passFemaleSmId);
        Assert.assertEquals(femaleMercurySample.getFingerprints().size(), 1);

        Fingerprint femaleFp = femaleMercurySample.getFingerprints().iterator().next();
        Assert.assertEquals(femaleFp.getDisposition(), Fingerprint.Disposition.PASS);
        Assert.assertEquals(femaleFp.getGender(), Fingerprint.Gender.FEMALE);

        MercurySample failMercurySample = mercurySampleDao.findBySampleKey(failSampleId);
        Assert.assertEquals(failMercurySample.getFingerprints().size(), 1);

        Fingerprint failFp = failMercurySample.getFingerprints().iterator().next();
        Assert.assertEquals(failFp.getDisposition(), Fingerprint.Disposition.FAIL);
        Assert.assertEquals(failFp.getGender(), Fingerprint.Gender.MALE);

        PlateWell plateWell = fluidigmChipRun.getKey().getContainerRole().getVesselAtPosition(VesselPosition.A05);
        boolean found = false;
        for (LabMetric metric : plateWell.getMetrics()) {
            if (metric.getName() == LabMetric.MetricType.HAPMAP_CONCORDANCE_LOD) {
                found = true;
                Assert.assertEquals(metric.getValue().compareTo(new BigDecimal("30")), 1);
            }
        }
        Assert.assertTrue(found);
    }
}
