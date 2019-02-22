package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.TubeBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselMetricBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselMetricRunBean;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * In preparation for testing Mercury by sending it BettaLIMS production messages, import data from BSP.
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class ImportFromBspTest extends StubbyContainerTest {

    public ImportFromBspTest(){}

    //    @PersistenceContext(unitName = "gap_pu")
    private EntityManager entityManager;

    private final SimpleDateFormat testPrefixDateFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled = false, groups = TestGroups.STUBBY)
    public void testImportExportedTubes() {
        String testSuffix = testPrefixDateFormat.format(new Date());

        // todo jmt incorporate query to fetch these
        List<String> sampleIds = new ArrayList<>();
        sampleIds.add("3TJ4Y");
        sampleIds.add("3TJ4T");
        sampleIds.add("3TJ44");
        sampleIds.add("3TJ52");
        sampleIds.add("3TJ4E");
        sampleIds.add("3TJ43");
        sampleIds.add("3TJ51");
        sampleIds.add("3TJ4S");
        sampleIds.add("3TJ4B");
        sampleIds.add("3TJ4O");
        sampleIds.add("3TJ4I");
        sampleIds.add("3TJ4R");
        sampleIds.add("3TJ4G");
        sampleIds.add("3TJ4L");
        sampleIds.add("3TJ4A");
        sampleIds.add("3TJ4P");
        sampleIds.add("3TJ4Z");
        sampleIds.add("3TJ49");
        sampleIds.add("3TJ46");
        sampleIds.add("3TJ4W");
        sampleIds.add("3TJ48");
        sampleIds.add("3TJ47");
        sampleIds.add("3TJ45");
        sampleIds.add("3TJ4K");
        sampleIds.add("3TJ4J");
        sampleIds.add("3TJ4Q");
        sampleIds.add("3TJ53");
        sampleIds.add("3TJ4M");
        sampleIds.add("3TJ4H");
        sampleIds.add("3TJ4N");
        sampleIds.add("3TJ4V");
        sampleIds.add("3TJ4U");
        sampleIds.add("3TJ4D");
        sampleIds.add("3TJ4X");
        sampleIds.add("3TJ4C");

        // bsp_batch_sample.product_order_id doesn't seem to be populated reliably, so have to use a list of samples in
        // where clause
        // todo jmt, what about controls?
        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                                                            "     root_sample.sample_id AS root_sample_id, " +
                                                            "     root_receptacle.external_id AS root_barcode, " +
                                                            "     pico_sample.sample_id AS pico_sample_id, " +
                                                            "     pico_receptacle.external_id AS pico_barcode, " +
                                                            "     bsp_sample_export.sample_id AS extract_sample_id, " +
                                                            "     bsp_receptacle.EXTERNAL_ID AS extract_barcode, " +
                                                            "     export_attributes.concentration " +
                                                            "FROM " +
                                                            "     bsp_sample root_sample " +
                                                            "     INNER JOIN bsp_receptacle root_receptacle " +
                                                            "          ON   root_receptacle.receptacle_id = root_sample.receptacle_id "
                                                            +
                                                            "     INNER JOIN bsp_parent_sample bsp_parent_sample1 " +
                                                            "          ON   bsp_parent_sample1.parent_sample_id = root_sample.sample_id "
                                                            +
                                                            "     INNER JOIN bsp_sample pico_sample " +
                                                            "          ON   pico_sample.sample_id = bsp_parent_sample1.sample_id "
                                                            +
                                                            "     INNER JOIN bsp_receptacle pico_receptacle " +
                                                            "          ON   pico_sample.receptacle_id = pico_receptacle.receptacle_id "
                                                            +
                                                            "     INNER JOIN bsp_parent_sample bsp_parent_sample2 " +
                                                            "          ON   bsp_parent_sample2.parent_sample_id = bsp_parent_sample1.sample_id "
                                                            +
                                                            "     INNER JOIN bsp_sample_export " +
                                                            "          ON   bsp_sample_export.sample_id = bsp_parent_sample2.sample_id "
                                                            +
                                                            "     INNER JOIN bsp_sample export_sample " +
                                                            "          ON   export_sample.sample_id = bsp_parent_sample2.sample_id "
                                                            +
                                                            "     INNER JOIN bsp_sample_attributes export_attributes" +
                                                            "          ON   export_attributes.sample_attributes_id = export_sample.sample_attributes_id"
                                                            +
                                                            "     INNER JOIN bsp_receptacle " +
                                                            "          ON   bsp_receptacle.receptacle_id = export_sample.receptacle_id "
                                                            +
                                                            "WHERE " +
                                                            "     bsp_sample_export.destination = 'Sequencing' " +
                                                            "     AND bsp_sample_export.sample_id IN (:sampleList)");
        nativeQuery.setParameter("sampleList", sampleIds);
        List<?> resultList = nativeQuery.getResultList();
        List<TubeBean> rootTubeBeans = new ArrayList<>();
        List<TubeBean> exportTubeBeans = new ArrayList<>();
        List<String> normSourceBarcodes = new ArrayList<>();
        List<String> normTargetBarcodes = new ArrayList<>();
        List<String> platingTargetBarcodes = new ArrayList<>();
        ArrayList<VesselMetricBean> vesselMetricBeans = new ArrayList<>();

        for (Object o : resultList) {
            Object[] columns = (Object[]) o;
            String rootSample = (String) columns[0];
            String rootBarcode = (String) columns[1];
            String picoSample = (String) columns[2];
            String picoBarcode = (String) columns[3];
            String exportedSample = (String) columns[4];
            String exportedBarcode = (String) columns[5];
            BigDecimal exportConcentration = (BigDecimal) columns[6];
            rootTubeBeans.add(new TubeBean(rootBarcode, "SM-" + rootSample));
            exportTubeBeans.add(new TubeBean(exportedBarcode, "SM-" + exportedSample));

            normSourceBarcodes.add(rootBarcode);
            normTargetBarcodes.add(picoBarcode);
            platingTargetBarcodes.add(exportedBarcode);
            vesselMetricBeans.add(new VesselMetricBean(exportedBarcode, exportConcentration.toString(), "ng/uL"));
        }
        LabBatchBean labBatchBean = new LabBatchBean("BP-ROOT-" + testSuffix, null, rootTubeBeans);
        createBatch(labBatchBean);
        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);

        PlateTransferEventType plateTransferEventType = bettaLimsMessageTestFactory.buildRackToRack(
                LabEventType.SAMPLES_NORMALIZATION_TRANSFER.getName(), "ROOT" + testSuffix, normSourceBarcodes,
                "NORM" + testSuffix, normTargetBarcodes);
        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateTransferEvent().add(plateTransferEventType);
        sendMessage(bettaLIMSMessage);

        bettaLimsMessageTestFactory.advanceTime();
        plateTransferEventType = bettaLimsMessageTestFactory.buildRackToRack(
                LabEventType.SAMPLES_PLATING_TO_COVARIS.getName(), "NORM" + testSuffix, normTargetBarcodes,
                "EXPORT" + testSuffix, platingTargetBarcodes);
        bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateTransferEvent().add(plateTransferEventType);
        sendMessage(bettaLIMSMessage);

        labBatchBean = new LabBatchBean("BP-EXPORT-" + testSuffix, null, exportTubeBeans);
        createBatch(labBatchBean);
        // Identify candidate samples: Flowcell -> samples -> LCSET -> PDO

        VesselMetricRunBean vesselMetricRunBean =
                new VesselMetricRunBean("BSP-PICO" + testSuffix, new Date(), "BSP Pico",
                        vesselMetricBeans);
        recordMetrics(vesselMetricRunBean);
    }

    private void createBatch(LabBatchBean labBatchBean) {
        ClientBuilder clientBuilder = JerseyUtils.getClientBuilderAcceptCertificate();

        String response = clientBuilder.newClient().target(ImportFromSquidTest.TEST_MERCURY_URL + "/rest/labbatch")
                .request(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .post(Entity.xml(labBatchBean), String.class);
        System.out.println(response);
    }

    public static String recordMetrics(VesselMetricRunBean vesselMetricRunBean) {
        ClientBuilder clientBuilder = JerseyUtils.getClientBuilderAcceptCertificate();

        String response =
                clientBuilder.newClient().target(ImportFromSquidTest.TEST_MERCURY_URL + "/rest/vesselmetric")
                        .request(MediaType.APPLICATION_XML_TYPE)
                        .accept(MediaType.APPLICATION_XML)
                        .post(Entity.xml(vesselMetricRunBean), String.class);
        System.out.println(response);
        return response;
    }

    private void sendMessage(BettaLIMSMessage bettaLIMSMessage) {
        ClientBuilder clientBuilder = JerseyUtils.getClientBuilderAcceptCertificate();

        String response =
                clientBuilder.newClient().target(ImportFromSquidTest.TEST_MERCURY_URL + "/rest/bettalimsmessage")
                        .request(MediaType.APPLICATION_XML_TYPE)
                        .accept(MediaType.APPLICATION_XML)
                        .post(Entity.xml(bettaLIMSMessage), String.class);
        System.out.println(response);
    }
}
