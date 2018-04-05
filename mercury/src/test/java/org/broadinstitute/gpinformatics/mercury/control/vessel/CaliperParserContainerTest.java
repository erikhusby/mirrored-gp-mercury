package org.broadinstitute.gpinformatics.mercury.control.vessel;

import com.google.common.base.Joiner;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test the Caliper upload with persistence.
 */
@Test(groups = TestGroups.STANDARD, singleThreaded = true)
public class CaliperParserContainerTest extends Arquillian {

    private static final FastDateFormat SIMPLE_DATE_FORMAT = FastDateFormat.getInstance("yyyyMMddHHmmss");

    @Inject
    private VesselEjb vesselEjb;

    @Inject
    private LabVesselDao labVesselDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testPersistence() throws Exception {
        String timestamp = SIMPLE_DATE_FORMAT.format(new Date());
        String plateBarcode = timestamp + "01";
        MessageCollection messageCollection = new MessageCollection();

        final boolean PERSIST_VESSELS = true;
        final boolean ACCEPT_CALIPER_REDO = true;

        Pair<LabMetricRun, String> pair1 = makeCaliperRun(plateBarcode, timestamp,
                messageCollection, !ACCEPT_CALIPER_REDO, PERSIST_VESSELS);

        Assert.assertTrue(StringUtils.isNotBlank(pair1.getRight()));
        Assert.assertFalse(messageCollection.hasErrors());
        Assert.assertTrue(messageCollection.hasWarnings());
        Assert.assertNotNull(pair1.getLeft());
        Assert.assertEquals(pair1.getLeft().getLabMetrics().size(), 96 * 2);
    }

    private Pair<LabMetricRun, String> makeCaliperRun(String plateBarcode, String namePrefix,
                                                        MessageCollection messageCollection, boolean acceptReCaliper,
                                                        boolean persistVessels)
            throws Exception {

        InputStream csvInputStream = VarioskanParserTest.getSpreadsheet(CaliperPlateProcessorTest.CALIPER_OUTPUT_CSV);
        CSVReader reader = new CSVReader(new InputStreamReader(csvInputStream));
        String [] nextLine;
        Date now = new Date();
        DateFormat dateFormat = new SimpleDateFormat(CaliperPlateProcessor.CaliperDataRow.DATE_FORMAT);
        String dateString = dateFormat.format(now);
        File tempFile = File.createTempFile("Caliper", ".csv");
        boolean seenHeader = false;
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile))) {
            while ((nextLine = reader.readNext()) != null) {
                if(!seenHeader) {
                    seenHeader = true;
                } else {
                    nextLine[19] = plateBarcode;
                    nextLine[20] = dateString;
                }
                String line = Joiner.on(",").join(nextLine);
                bw.write(line);
                bw.newLine();

            }
        }

        Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
        Map<VesselPosition, BarcodedTube> mapPositionToTube = CaliperPlateProcessorTest.buildTubesAndTransfers(
                mapBarcodeToPlate, plateBarcode, namePrefix);

        if(persistVessels) {
            labVesselDao.persistAll(mapBarcodeToPlate.values());
            labVesselDao.persistAll(mapPositionToTube.values());
        }

        return vesselEjb.createRNACaliperRun(new FileInputStream(tempFile), LabMetric.MetricType.INITIAL_RNA_CALIPER,
                BSPManagerFactoryStub.QA_DUDE_USER_ID, messageCollection, acceptReCaliper);
    }
}
