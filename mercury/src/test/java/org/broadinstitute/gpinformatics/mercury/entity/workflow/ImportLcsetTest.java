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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Imports Squid LCSETs from labopsjira, to allow testing of messages.
 */
@Test(groups = TestGroups.STANDARD)
public class ImportLcsetTest extends Arquillian {

    private static final Pattern BREAK_PATTERN = Pattern.compile("<br/>\\n");

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

    private static Map<String, String> mapLcsetTypeToWorkflow = new HashMap<>();
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
    @Test(enabled = true)
    public void testCreateLcsets() throws Exception {

        Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                "    ls.barcode  AS sample_barcode, " +
                "    listagg(wrmd.product_order_name, ',') WITHIN GROUP (ORDER BY wrmd.product_order_name), " +
                "    r.barcode   AS receptacle_barcode, " +
                "    gso.individual_name " +
                "FROM " +
                "    lc_sample ls " +
                "    LEFT OUTER JOIN lc_sample ls2 " +
                "        ON   ls.parent_sample_id = ls2.lc_sample_id " +
                "    INNER JOIN work_request_material_descr wrmd " +
                "        ON wrmd.lc_sample_id = ls2.lc_sample_id " +
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
                "    ls.barcode IN (:sample_ids) " +
                "GROUP BY " +
                "    ls.barcode, r.barcode, gso.individual_name ");

        XPath xPath =  XPathFactory.newInstance().newXPath();
        XPathExpression lcsetKeyExpr = xPath.compile("/rss/channel/item/key");
        XPathExpression lcsetTypeExpr = xPath.compile("../type");
        XPathExpression gssrExpr = xPath.compile(
                "../customfields/customfield/customfieldname[text()='GSSR ID(s)']/../customfieldvalues/customfieldvalue");
        XPathExpression pdoExpr = xPath.compile(
                "../issuelinks/issuelinktype/outwardlinks/issuelink/issuekey");

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
            NodeList pdoNodeList = (NodeList) pdoExpr.evaluate(keyNode, XPathConstants.NODESET);
            List<String> lcsetPdos = new ArrayList<>();
            for (int j = 0; j < pdoNodeList.getLength(); j++) {
                Node issueKeyNode = pdoNodeList.item(j);
                lcsetPdos.add(issueKeyNode.getFirstChild().getNodeValue());
            }

            // Get GSSR IDs
            NodeList gssrNodeList = (NodeList) gssrExpr.evaluate(keyNode, XPathConstants.NODESET);
            List<String> sampleIds = new ArrayList<>();
            for (int j = 0; j < gssrNodeList.getLength(); j++) {
                Node customfieldvalueNode = gssrNodeList.item(j);
                String sampleIdString = customfieldvalueNode.getFirstChild().getNodeValue();
                System.out.println(sampleIdString);
                Collections.addAll(sampleIds, BREAK_PATTERN.split(sampleIdString));
            }

            // Get barcodes
            if (!sampleIds.isEmpty()) {
                nativeQuery.setParameter("sample_ids", sampleIds);
                List<?> resultList = nativeQuery.getResultList();
                System.out.println("Found " + resultList.size() + " Squid tubes.");
                if (resultList.isEmpty()) {
                    continue;
                }
                List<String> tubeBarcodes = new ArrayList<>();
                List<String> controlTubeBarcodes = new ArrayList<>();
                List<ImmutablePair<String, String[]>> nonControlTubeBarcodes = new ArrayList<>();
                Set<String> productOrderNames = new HashSet<>();
                for (Object o : resultList) {
                    Object[] columns = (Object[]) o;
//                    String sampleBarcode = (String) columns[0];
                    String productOrderNamesCsv = (String) columns[1];
                    String receptacleBarcode = (String) columns[2];
                    String individualName = (String) columns[3];

                    String[] productOrderNamesArray = productOrderNamesCsv.split(",");
                    Collections.addAll(productOrderNames, productOrderNamesArray);
                    tubeBarcodes.add(receptacleBarcode);
                    // Determine control
                    if (individualName.equals("NA12878") || individualName.equals("WATER_CONTROL") ||
                            individualName.equals("Affi E.coli")) {
                        controlTubeBarcodes.add(receptacleBarcode);
                    } else {
                        nonControlTubeBarcodes.add(new ImmutablePair<>(receptacleBarcode, productOrderNamesArray));
                    }
                }
                Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(tubeBarcodes);
                List<ProductOrder> productOrders = productOrderDao.findListByBusinessKeys(productOrderNames);

                // Create bucket entries
                Set<LabVessel> nonControlTubes = new HashSet<>();
                List<BucketEntry> bucketEntries = new ArrayList<>();
                for (Pair<String, String[]> tubeBarcodePdoPair : nonControlTubeBarcodes) {
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
lcsetPdoLoop:       for (String pdo : tubeBarcodePdoPair.getRight()) {
                        if (lcsetPdos.contains(pdo)) {
                            for (ProductOrder productOrder : productOrders) {
                                if (productOrder.getJiraTicketKey().equals(pdo)) {
                                    foundProductOrder = productOrder;
                                    break lcsetPdoLoop;
                                }
                            }
                        }
                    }

                    if (foundProductOrder == null) {
                        System.out.println("Failed to find Product Order " + Arrays.toString(
                                tubeBarcodePdoPair.getRight()));
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
