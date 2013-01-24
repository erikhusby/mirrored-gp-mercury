package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import com.sun.jersey.api.client.Client;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.TubeBean;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.test.BettaLimsMessageFactory;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.core.MediaType;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * In preparation for testing Mercury by sending it BettaLIMS production messages, import data from BSP.
 */
public class ImportFromBspTest extends ContainerTest {

    @PersistenceContext(unitName = "gap_pu")
    private EntityManager entityManager;

    private final SimpleDateFormat testPrefixDateFormat=new SimpleDateFormat("MMddHHmmss");

    @Test
    public void testImportExportedTubes() {
        String testSuffix = testPrefixDateFormat.format(new Date());

        // bsp_batch_sample.product_order_id doesn't seem to be populated reliably, so have to use a list of samples in
        // where clause
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "     root_sample.sample_id as root_sample_id, " +
                "     root_receptacle.external_id as root_barcode, " +
                "     pico_sample.sample_id as pico_sample_id, " +
                "     pico_receptacle.external_id as pico_barcode, " +
                "     bsp_sample_export.sample_id as extract_sample_id, " +
                "     bsp_receptacle.EXTERNAL_ID as extract_barcode " +
                "FROM " +
                "     bsp_sample root_sample " +
                "     INNER JOIN bsp_receptacle root_receptacle " +
                "          ON   root_receptacle.receptacle_id = root_sample.receptacle_id " +
                "     INNER JOIN bsp_parent_sample bsp_parent_sample1 " +
                "          ON   bsp_parent_sample1.parent_sample_id = root_sample.sample_id " +
                "     INNER JOIN bsp_sample pico_sample " +
                "          ON   pico_sample.sample_id = bsp_parent_sample1.sample_id " +
                "     INNER JOIN bsp_receptacle pico_receptacle " +
                "          ON   pico_sample.receptacle_id = pico_receptacle.receptacle_id " +
                "     INNER JOIN bsp_parent_sample bsp_parent_sample2 " +
                "          ON   bsp_parent_sample2.parent_sample_id = bsp_parent_sample1.sample_id " +
                "     INNER JOIN bsp_sample_export " +
                "          ON   bsp_sample_export.sample_id = bsp_parent_sample2.sample_id " +
                "     INNER JOIN bsp_sample bsp_sample2 " +
                "          ON   bsp_sample2.sample_id = bsp_parent_sample2.sample_id " +
                "     INNER JOIN bsp_receptacle " +
                "          ON   bsp_receptacle.receptacle_id = bsp_sample2.receptacle_id " +
                "WHERE " +
                "     bsp_sample_export.destination = 'Sequencing' " +
                "     AND bsp_sample_export.sample_id IN (" +
                "'3TJ4Y'," +
                "'3TJ4T'," +
                "'3TJ44'," +
                "'3TJ52'," +
                "'3TJ4E'," +
                "'3TJ43'," +
                "'3TJ51'," +
                "'3TJ4S'," +
                "'3TJ4B'," +
                "'3TJ4O'," +
                "'3TJ4I'," +
                "'3TJ4R'," +
                "'3TJ4G'," +
                "'3TJ4L'," +
                "'3TJ4A'," +
                "'3TJ4P'," +
                "'3TJ4Z'," +
                "'3TJ49'," +
                "'3TJ46'," +
                "'3TJ4W'," +
                "'3TJ48'," +
                "'3TJ47'," +
                "'3TJ45'," +
                "'3TJ4K'," +
                "'3TJ4J'," +
                "'3TJ4Q'," +
                "'3TJ53'," +
                "'3TJ4M'," +
                "'3TJ4H'," +
                "'3TJ4N'," +
                "'3TJ4V'," +
                "'3TJ4U'," +
                "'3TJ4D'," +
                "'3TJ4X'," +
                "'3TJ4C'" +
                "     ) " +
                "ORDER BY " +
                "     3 ");
        List<?> resultList = nativeQuery.getResultList();
        List<TubeBean> tubeBeans = new ArrayList<TubeBean>();
        List<String> normSourceBarcodes = new ArrayList<String>();
        List<String> normTargetBarcodes = new ArrayList<String>();
        List<String> platingTargetBarcodes = new ArrayList<String>();

        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String rootSample = (String) columns[0];
            String rootBarcode = (String) columns[1];
            String picoSample = (String) columns[2];
            String picoBarcode = (String) columns[3];
            String exportedSample = (String) columns[4];
            String exportedBarcode = (String) columns[5];
            tubeBeans.add(new TubeBean(rootBarcode, rootSample, "PDO-183"));

            normSourceBarcodes.add(rootBarcode);
            normTargetBarcodes.add(picoBarcode);
            platingTargetBarcodes.add(exportedBarcode);
        }
        LabBatchBean labBatchBean = new LabBatchBean("BP-JanDemo-" + testSuffix, null, tubeBeans);
        String response = Client.create().resource(ImportFromSquidTest.TEST_MERCURY_URL + "/rest/labbatch")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(labBatchBean)
                .post(String.class);
        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();

        PlateTransferEventType plateTransferEventType = bettaLimsMessageFactory.buildRackToRack(
                LabEventType.SAMPLES_NORMALIZATION_TRANSFER.getName(), "ROOT" + testSuffix, normSourceBarcodes,
                "NORM" + testSuffix, normTargetBarcodes);
        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateTransferEvent().add(plateTransferEventType);
        sendMessage(bettaLIMSMessage);

        bettaLimsMessageFactory.advanceTime();
        plateTransferEventType = bettaLimsMessageFactory.buildRackToRack(
                LabEventType.SAMPLES_PLATING_TO_COVARIS.getName(), "NORM" + testSuffix, normTargetBarcodes,
                "EXPORT" + testSuffix, platingTargetBarcodes);
        bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateTransferEvent().add(plateTransferEventType);
        sendMessage(bettaLIMSMessage);

        // Identify candidate samples: Flowcell -> samples -> LCSET -> PDO

    }

    private void sendMessage(BettaLIMSMessage bettaLIMSMessage) {
        String response = Client.create().resource(ImportFromSquidTest.TEST_MERCURY_URL + "/rest/bettalimsmessage")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(bettaLIMSMessage)
                .post(String.class);
        System.out.println(response);
    }
}
