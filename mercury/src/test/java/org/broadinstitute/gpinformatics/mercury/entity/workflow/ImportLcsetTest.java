package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Imports Squid LCSETs from labopsjira, to allow testing of messages.
 */
@Test(groups = TestGroups.STANDARD)
public class ImportLcsetTest extends Arquillian {

    @PersistenceContext(unitName = "squid_pu")
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
        mapLcsetTypeToWorkflow.put("Aliquot Only", "");
        mapLcsetTypeToWorkflow.put("cDNA TruSeq Strand Specific Large Insert", "");
        mapLcsetTypeToWorkflow.put("Development LCSET", "");
        mapLcsetTypeToWorkflow.put("Exome Express", "ICE Exome Express");
        mapLcsetTypeToWorkflow.put("Fluidigm Custom Amplicon Standard & Seq", "");
        mapLcsetTypeToWorkflow.put("Human PCR-Free", "Whole Genome PCR Free");
        mapLcsetTypeToWorkflow.put("Human PCR-Plus", "Whole Genome PCR Plus");
        mapLcsetTypeToWorkflow.put("Large Genome (WGS)", "Whole Genome");
        mapLcsetTypeToWorkflow.put("Large Insert Nexome", "Nexome");
        mapLcsetTypeToWorkflow.put("Lasso", "");
        mapLcsetTypeToWorkflow.put("MiSeq16s", "");
        mapLcsetTypeToWorkflow.put("Standard Non-Assembly LC", "");
        mapLcsetTypeToWorkflow.put("Whole Exome (HybSel)", "Hybrid Selection");
    }

    @Deployment
    public static WebArchive buildMercuryWar() {
        // change dataSourceEnvironment parameter to "prod" when importing from production.
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "dev");
    }

    /**
     * This test creates Mercury LCSETs to match those in Squid.  It reads from an XML export from JIRA.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    @Test(enabled = true)
    public void testCreateLcsets() throws Exception {

        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "    ls.barcode  AS sample_barcode,   " +
                "    wrmd.product_order_name,   " +
                "    r.barcode   AS receptacle_barcode,   " +
                "    gso.individual_name   " +
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
                "        ON   r.receptacle_id = sc.receptacle_id " +
                "WHERE " +
                "    wrm.WORK_REQUEST_ID = :work_request");

        XPath xPath =  XPathFactory.newInstance().newXPath();
        XPathExpression lcsetKeyExpr = xPath.compile("/rss/channel/item/key");
        XPathExpression lcsetTypeExpr = xPath.compile("../type");
        XPathExpression workRequestExpr = xPath.compile(
                "../customfields/customfield/customfieldname[text()='Work Request ID(s)']/../customfieldvalues/customfieldvalue");

        // Open XML
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(new FileInputStream(
                "C:\\Users\\thompson\\Downloads\\SearchRequest.xml"));

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
            Node workRequestNode = (Node) workRequestExpr.evaluate(keyNode, XPathConstants.NODE);
            if (workRequestNode == null) {
                continue;
            }

            // Get barcodes
            String workRequest = workRequestNode.getFirstChild().getNodeValue();
            if (workRequest != null) {
                System.out.println("Work request " + workRequest);
                try {
                    Integer.parseInt(workRequest);
                } catch (NumberFormatException e) {
                    System.out.println("Failed to convert work request to number");
                    continue;
                }
                nativeQuery.setParameter("work_request", workRequest);
                List<?> resultList = nativeQuery.getResultList();
                System.out.println("Found " + resultList.size() + " Squid tubes.");
                if (resultList.isEmpty()) {
                    continue;
                }
                List<String> tubeBarcodes = new ArrayList<>();
                List<String> controlTubeBarcodes = new ArrayList<>();
                List<ImmutablePair<String, String>> nonControlTubeBarcodes = new ArrayList<>();
                Set<String> productOrderNames = new HashSet<>();
                for (Object o : resultList) {
                    Object[] columns = (Object[]) o;
//                    String sampleBarcode = (String) columns[0];
                    String productOrderName = (String) columns[1];
                    String receptacleBarcode = (String) columns[2];
                    String individualName = (String) columns[3];

                    productOrderNames.add(productOrderName);
                    tubeBarcodes.add(receptacleBarcode);
                    // Determine control
                    if (individualName.equals("NA12878") || individualName.equals("WATER_CONTROL") ||
                            individualName.equals("Affi E.coli")) {
                        controlTubeBarcodes.add(receptacleBarcode);
                    } else {
                        nonControlTubeBarcodes.add(new ImmutablePair<>(receptacleBarcode, productOrderName));
                    }
                }
                Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(tubeBarcodes);
                if (productOrderNames.size() > 1) {
                    System.out.println("Multiple PDOs " + productOrderNames);
                }
                List<ProductOrder> productOrders = productOrderDao.findListByBusinessKeys(productOrderNames);

                // Create bucket entries
                Set<LabVessel> nonControlTubes = new HashSet<>();
                List<BucketEntry> bucketEntries = new ArrayList<>();
                for (Pair<String, String> tubeBarcodePdoPair : nonControlTubeBarcodes) {
                    String nonControlTubeBarcode = tubeBarcodePdoPair.getLeft();
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
                    String pdo = tubeBarcodePdoPair.getRight();
                    for (ProductOrder productOrder : productOrders) {
                        if (productOrder.getJiraTicketKey().equals(pdo)) {
                            foundProductOrder = productOrder;
                            break;
                        }
                    }

                    if (foundProductOrder == null) {
                        System.out.println("Failed to find Product Order " + pdo);
                        continue;
                    }

                    bucketEntries.add(new BucketEntry(barcodedTube, foundProductOrder, shearingBucket,
                            BucketEntry.BucketEntryType.PDO_ENTRY, 1));
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

                    // Register control
                    for (String controlTubeBarcode : controlTubeBarcodes) {
                        labBatch.addLabVessel(mapBarcodeToTube.get(controlTubeBarcode));
                    }

                    labBatchDao.persist(labBatch);
                }
            }
            labBatchDao.flush();
            labBatchDao.clear();
        }
    }

}
