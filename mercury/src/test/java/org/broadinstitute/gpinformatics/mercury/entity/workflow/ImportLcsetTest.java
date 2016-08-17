package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Imports Squid LCSETs from labopsjira, to allow testing of messages. <br />
 * Wildfly rejects deploying with non-existing persistence unit - uncomment attribute if running
 */
@Test(groups = TestGroups.STANDARD)
public class ImportLcsetTest extends Arquillian {

    //@PersistenceContext(unitName = "squid_pu")
    private EntityManager entityManager;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private BucketDao bucketDao;

    @Inject
    private ProductOrderDao productOrderDao;

    private static final Map<String, String> mapLcsetTypeToWorkflow = new HashMap<>();
    static {
        // Many of these LCSETs don't yet have corresponding workflows in Mercury.  Arbitrarily assign them to
        // Hybrid Selection, so they at least get routed to Mercury.
        mapLcsetTypeToWorkflow.put("Aliquot Only", "Hybrid Selection"); // todo fix
        mapLcsetTypeToWorkflow.put("cDNA TruSeq Non-Strand Specific", "Hybrid Selection"); // todo fix
        mapLcsetTypeToWorkflow.put("cDNA TruSeq Strand Specific Large Insert", "TruSeq Strand Specific CRSP");
        mapLcsetTypeToWorkflow.put("Custom Amplicon", "Hybrid Selection"); // todo fix
        mapLcsetTypeToWorkflow.put("Custom Design (HybSel)", "Hybrid Selection");
        mapLcsetTypeToWorkflow.put("Development LCSET", "Hybrid Selection"); // todo fix
        mapLcsetTypeToWorkflow.put("Exome Express", "ICE Exome Express");
        mapLcsetTypeToWorkflow.put("Fluidigm Custom Amplicon Multiplexed & Seq", "Hybrid Selection"); // todo fix
        mapLcsetTypeToWorkflow.put("Fluidigm Custom Amplicon Standard & Seq", "Hybrid Selection"); // todo fix
        mapLcsetTypeToWorkflow.put("Human PCR-Free", "Whole Genome PCR Free");
        mapLcsetTypeToWorkflow.put("Human PCR-Plus", "Whole Genome PCR Plus");
        mapLcsetTypeToWorkflow.put("Large Genome (WGS)", "Whole Genome");
        mapLcsetTypeToWorkflow.put("Large Insert Nexome", "Nexome");
        mapLcsetTypeToWorkflow.put("Nexome", "Nexome");
        mapLcsetTypeToWorkflow.put("Lasso", "Hybrid Selection"); // todo fix
        mapLcsetTypeToWorkflow.put("MiSeq16s", "Hybrid Selection"); // todo fix
        mapLcsetTypeToWorkflow.put("Standard Non-Assembly LC", "Hybrid Selection"); // todo fix
        mapLcsetTypeToWorkflow.put("TSCA", "Hybrid Selection"); // todo fix
        mapLcsetTypeToWorkflow.put("Whole Exome (HybSel)", "Hybrid Selection");
        mapLcsetTypeToWorkflow.put("Whole Exome (ICE)", "ICE Exome Express");
    }

    @Deployment
    public static WebArchive buildMercuryWar() {
        // change dataSourceEnvironment parameter to "prod" when importing from production.
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "dev");
    }

    private static class Row {
        private String sampleBarcode;
        private String productOrderName;
        private String receptacleBarcode;
        private String individualName;

        private Row(Object[] columns) {
            sampleBarcode = (String) columns[0];
            productOrderName = (String) columns[1];
            receptacleBarcode = (String) columns[2];
            individualName = (String) columns[3];
        }

        public String getSampleBarcode() {
            return sampleBarcode;
        }

        public String getProductOrderName() {
            return productOrderName;
        }

        public String getReceptacleBarcode() {
            return receptacleBarcode;
        }

        public String getIndividualName() {
            return individualName;
        }
    }
    /**
     * This test creates Mercury LCSETs to match those in Squid.  It reads from an XML export from JIRA.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    @Test(enabled = false)
    public void testCreateLcsets() throws Exception {

        // For a given LCSET, go through the work request to get the tubes and samples.
        String coreQuery = "SELECT " +
                "    ls.barcode  AS sample_barcode, " +
                "    wrmd.product_order_name, " +
                "    r.barcode   AS receptacle_barcode, " +
                "    gso.individual_name " +
                "FROM " +
                "    SEQ20.WORK_REQUEST_MAT_SAMPLE wrms " +
                "    INNER JOIN SEQ20.WORK_REQUEST_MATERIAL wrm " +
                "        ON   wrms.WORK_REQUEST_MATERIAL_ID = wrm.WORK_REQUEST_MATERIAL_ID " +
                "    INNER JOIN lc_sample_work_req_checkout lswrc " +
                "        ON   lswrc.work_request_material_id = wrm.work_request_material_id " +
                "    INNER JOIN lc_sample ls " +
                "        ON   ls.lc_sample_id = lswrc.aliquot_sample_id " +
                "    INNER JOIN work_request_material_descr wrmd " +
                "        ON   wrmd.work_request_material_id = wrm.work_request_material_id " +
                "    INNER JOIN genomic_sample_organism gso " +
                "        ON   gso.genomic_sample_organism_id = ls.genomic_sample_organism_id " +
                "    INNER JOIN gssr_pool_descr gpd " +
                "        ON   gpd.lc_sample_id = ls.lc_sample_id " +
                "    INNER JOIN seq_content_descr scd " +
                "        ON   scd.seq_content_descr_id = gpd.seq_content_descr_id " +
                "    INNER JOIN seq_content_descr_set scds " +
                "        ON   scds.seq_content_descr_id = scd.seq_content_descr_id " +
                "    INNER JOIN seq_content sc " +
                "        ON   sc.seq_content_id = scds.seq_content_id " +
                "    INNER JOIN receptacle r " +
                "        ON   r.receptacle_id = sc.receptacle_id ";

        String lcSetWhere =
                "    INNER JOIN work_request wr " +
                "        ON   wr.work_request_id = wrm.work_request_id " +
                "    INNER JOIN lcset l " +
                "        ON   l.lcset_id = wr.lcset_id " +
                "WHERE " +
                "    l.KEY = :lcset";

        String workRequestWhere =
                "WHERE " +
                "    wrm.WORK_REQUEST_ID = :work_request";

        Query workRequestQuery = entityManager.createNativeQuery(coreQuery + workRequestWhere);
        Query lcsetQuery = entityManager.createNativeQuery(coreQuery + lcSetWhere);

        // For a given list of samples, get Dev aliquots, if any.
        Query devAliquotsQuery = entityManager.createNativeQuery("SELECT " +
                "    ls.barcode AS sample_barcode, " +
                "    r.barcode AS receptacle_barcode " +
                "FROM " +
                "    receptacle r " +
                "    INNER JOIN seq_content sc " +
                "        ON   sc.receptacle_id = r.receptacle_id " +
                "    INNER JOIN seq_content_type sct " +
                "        ON   sct.seq_content_type_id = sc.seq_content_type_id " +
                "    INNER JOIN dev_library dl " +
                "        ON   dl.library_id = sc.seq_content_id " +
                "    INNER JOIN seq_content_descr_set scds " +
                "        ON   scds.seq_content_id = sc.seq_content_id " +
                "    INNER JOIN seq_content_descr scd " +
                "        ON   scd.seq_content_descr_id = scds.seq_content_descr_id " +
                "    INNER JOIN seq_content_descr_type scdt " +
                "        ON   scdt.seq_content_descr_type_id = scd.seq_content_descr_type_id " +
                "    INNER JOIN next_generation_library_descr ngld " +
                "        ON   ngld.library_descr_id = scd.seq_content_descr_id " +
                "    INNER JOIN lc_sample ls " +
                "        ON   ls.lc_sample_id = ngld.sample_id " +
                "WHERE " +
                "    ls.barcode IN (:sampleIds) ");

        XPath xPath =  XPathFactory.newInstance().newXPath();
        XPathExpression lcsetKeyExpr = xPath.compile("/rss/channel/item/key");
        XPathExpression lcsetTypeExpr = xPath.compile("../type");
        XPathExpression workRequestExpr = xPath.compile(
                "../customfields/customfield/customfieldname[text()='Work Request ID(s)']/../customfieldvalues/customfieldvalue");

        // Open XML
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
        // LCSET-7432 is for Dev Aliquots that are used in many transfers, so need to go back at least as far
        // as 11/Jun/2015.
        String[] fileNames = {
                "SearchRequest20150501.xml",
                "SearchRequest20150801.xml",
                "SearchRequest20151101.xml",
                "SearchRequest20160201.xml",
        };
        for (String fileName : fileNames) {
            Document document = documentBuilder.parse(new FileInputStream(
                    "C:\\Users\\thompson\\Downloads\\" + fileName));

            NodeList lcsetNodeList = (NodeList) lcsetKeyExpr.evaluate(document,
                    XPathConstants.NODESET);
            // Iterate over LCSETs
            for (int i = 0; i < lcsetNodeList.getLength(); i++) {
                Bucket shearingBucket = bucketDao.findByName("Shearing Bucket");
                Node keyNode = lcsetNodeList.item(i);
                String lcsetId = keyNode.getFirstChild().getNodeValue();
                System.out.println(lcsetId);

                // If LCSET doesn't exist in Mercury
                LabBatch labBatch = labBatchDao.findByBusinessKey(lcsetId);
                if (labBatch != null) {
                    continue;
                }
                Node lcsetTypeNode = (Node) lcsetTypeExpr.evaluate(keyNode, XPathConstants.NODE);
                String lcsetType = lcsetTypeNode.getFirstChild().getNodeValue();

                // Get work request tubes
                lcsetQuery.setParameter("lcset", lcsetId);
                List<?> resultList = lcsetQuery.getResultList();
                System.out.println("Found " + resultList.size() + " Squid tubes by LCSET.");
                if (resultList.isEmpty()) {
                    Node workRequestNode = (Node) workRequestExpr.evaluate(keyNode, XPathConstants.NODE);
                    if (workRequestNode == null) {
                        continue;
                    }
                    String workRequest = workRequestNode.getFirstChild().getNodeValue();
                    if (workRequest != null) {
                        System.out.println("Work request " + workRequest);
                        try {
                            Integer.parseInt(workRequest);
                        } catch (NumberFormatException e) {
                            System.out.println("Failed to convert work request to number");
                            continue;
                        }
                        workRequestQuery.setParameter("work_request", workRequest);
                        resultList = workRequestQuery.getResultList();
                        System.out.println("Found " + resultList.size() + " Squid tubes by work request.");
                    }
                    if (resultList.isEmpty()) {
                        continue;
                    }
                }
                List<String> tubeBarcodes = new ArrayList<>();
                List<String> controlTubeBarcodes = new ArrayList<>();
                List<Row> nonControlTubeBarcodes = new ArrayList<>();
                Set<String> productOrderNames = new HashSet<>();
                Set<String> sampleIds = new HashSet<>();
                for (Object o : resultList) {
                    Row row = new Row((Object[]) o);

                    sampleIds.add(row.getSampleBarcode());
                    if (row.getProductOrderName() != null) {
                        productOrderNames.add(row.getProductOrderName());
                    }
                    tubeBarcodes.add(row.getReceptacleBarcode());

                    // If product order is null, assume it's a control
                    if (row.getProductOrderName() == null) {
                        if (!(row.getIndividualName().equals("NA12878") ||
                                row.getIndividualName().equals("WATER_CONTROL") ||
                                row.getIndividualName().equals("K-562") ||
                                row.getIndividualName().equals("Affi E.coli"))) {
                            System.out.println("Non-control sample with no product order: " + row.getIndividualName());
                        }
                        controlTubeBarcodes.add(row.getReceptacleBarcode());
                    } else {
                        nonControlTubeBarcodes.add(row);
                    }
                }

                // Get dev aliquots.
                Map<String, List<String>> mapSampleToListDevTubes = new HashMap<>();
                devAliquotsQuery.setParameter("sampleIds", sampleIds);
                List<?> devAliquots = devAliquotsQuery.getResultList();
                for (Object row : devAliquots) {
                    Object[] row1 = (Object[]) row;
                    String sampleBarcode = (String) row1[0];
                    String tubeBarcode = (String) row1[1];
                    List<String> devTubeBarcodes = mapSampleToListDevTubes.get(sampleBarcode);
                    if (devTubeBarcodes == null) {
                        devTubeBarcodes = new ArrayList<>();
                        mapSampleToListDevTubes.put(sampleBarcode, devTubeBarcodes);
                    }
                    devTubeBarcodes.add(tubeBarcode);
                }

                // Get existing PDOs and tubes
                if (productOrderNames.size() > 1) {
                    System.out.println("Multiple PDOs " + productOrderNames);
                }
                List<ProductOrder> productOrders = productOrderDao.findListByBusinessKeys(productOrderNames);
                Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(tubeBarcodes);

                // Create bucket entries
                Set<LabVessel> nonControlTubes = new HashSet<>();
                List<BucketEntry> bucketEntries = new ArrayList<>();
                LabEvent labEvent = null;
                for (Row row : nonControlTubeBarcodes) {
                    String nonControlTubeBarcode = row.getReceptacleBarcode();
                    BarcodedTube barcodedTube = mapBarcodeToTube.get(nonControlTubeBarcode);
                    if (barcodedTube == null) {
                        System.out.println("Failed to find " + nonControlTubeBarcode + " in Mercury");
                        continue;
                    }
                    if (barcodedTube.getMercurySamples().isEmpty()) {
                        System.out.println("Failed to find MercurySamples for " + nonControlTubeBarcode);
                        continue;
                    }
                    nonControlTubes.add(barcodedTube);

                    ProductOrder foundProductOrder = null;
                    for (ProductOrder productOrder : productOrders) {
                        if (productOrder.getJiraTicketKey().equals(row.getProductOrderName())) {
                            foundProductOrder = productOrder;
                            break;
                        }
                    }

                    if (foundProductOrder == null) {
                        System.out.println("Failed to find Product Order " + row.getProductOrderName());
                        continue;
                    }

                    bucketEntries.add(new BucketEntry(barcodedTube, foundProductOrder, shearingBucket,
                            BucketEntry.BucketEntryType.PDO_ENTRY, 1));

                    // Create a transfer from each sample to its dev aliquots.
                    List<String> devTubeList = mapSampleToListDevTubes.get(row.getSampleBarcode());
                    if (devTubeList != null && !devTubeList.isEmpty()) {
                        if (labEvent == null) {
                            labEvent = new LabEvent(LabEventType.TRANSFER, new Date(), "ImportLcsetTest", 1L,
                                    101L, "ImportLcsetTest");
                        }
                        for (String devTube : devTubeList) {
                            new VesselToVesselTransfer(barcodedTube, new BarcodedTube(devTube), labEvent);
                        }
                    }
                }

                if (!bucketEntries.isEmpty()) {
                    // Create LCSET
                    labBatch = new LabBatch(lcsetId, nonControlTubes, LabBatch.LabBatchType.WORKFLOW);
                    String workflow = mapLcsetTypeToWorkflow.get(lcsetType);
                    if (workflow == null) {
                        System.out.println("Failed to find workflow mapping for " + lcsetType);
                    } else if (!workflow.isEmpty()) {
                        labBatch.setWorkflowName(workflow);
                    }
                    for (BucketEntry bucketEntry : bucketEntries) {
                        labBatch.addBucketEntry(bucketEntry);
                    }

                    // Register controls
                    for (String controlTubeBarcode : controlTubeBarcodes) {
                        labBatch.addLabVessel(mapBarcodeToTube.get(controlTubeBarcode));
                    }

                    if (labEvent != null) {
                        labBatchDao.persist(labEvent);
                    }
                    labBatchDao.persist(labBatch);
                }
                labBatchDao.flush();
                labBatchDao.clear();
            }
        }
    }

}
