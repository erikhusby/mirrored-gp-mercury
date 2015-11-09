package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchResource;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.TubeBean;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExtractionsBloodJaxbBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test messaging for Extractions
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ExtractionsDbFreeTest {
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");

    public void testMaterialType() {
        final String timestamp = timestampFormat.format(new Date());

        List<TubeBean> tubeBeans = new ArrayList<TubeBean>() {{
            add(new TubeBean("SM-FTAA01" + timestamp, null));
            add(new TubeBean("SM-FTAA02" + timestamp, null));
            add(new TubeBean("SM-FTAA03" + timestamp, null));
        }};

        String batchId = "BP-2";
        Map<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
        Map<MercurySample, MercurySample> mapSampleToSample = new LinkedHashMap<>();
        (new LabBatchResource()).buildLabBatch(
                new LabBatchBean(batchId, "EXB", tubeBeans), mapBarcodeToTube, mapSampleToSample);

        // Sets barcodes for the intermediate plates and destination rack and tubes.
        String deepwell1Barcode = timestamp + "03";
        String deepwell2Barcode = timestamp + "04";
        String matrixRackBarcode = timestamp + "05";
        List<String> matrixTubeBarcodes = new ArrayList<>();
        for (String barcode : mapBarcodeToTube.keySet()) {
            matrixTubeBarcodes.add(barcode + "D");
        }

        BettaLimsMessageTestFactory messageFactory = new BettaLimsMessageTestFactory(false);
        ExtractionsBloodJaxbBuilder jaxbBuilder = new ExtractionsBloodJaxbBuilder(messageFactory, timestamp,
                new ArrayList<>(mapBarcodeToTube.keySet()), deepwell1Barcode, deepwell2Barcode,
                matrixRackBarcode, matrixTubeBarcodes);
        jaxbBuilder.invoke();

        Assert.assertEquals(jaxbBuilder.getMessageList().size(), 3);
        Assert.assertNotNull(jaxbBuilder.getMessageList().get(2).getPlateTransferEvent());

        PlateTransferEventType finalPlate = jaxbBuilder.getMessageList().get(2).getPlateTransferEvent().get(0);
        for (ReceptacleType receptacleType : finalPlate.getPositionMap().getReceptacle()) {
            Assert.assertEquals(receptacleType.getMaterialType(), "DNA:DNA Genomic");
        }
    }
}
