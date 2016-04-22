package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Imports Squid LCSETs from labopsjira, to allow testing of messages.
 */
@Test(groups = TestGroups.STANDARD)
public class ImportLcsetTest extends Arquillian {

    private static final Pattern BREAK_PATTERN = Pattern.compile("<br/>\\n");
    @PersistenceContext(unitName = "squid_pu")
    private EntityManager entityManager;

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
    public void testCreateLcsets() {

        try {
            Query nativeQuery = entityManager.createNativeQuery("SELECT " +
                    "    ls.barcode AS sample_barcode," +
                    "    r.barcode AS receptacle_barcode, " +
                    "    gso.individual_name " +
                    "FROM " +
                    "    lc_sample ls " +
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
                    "    ls.barcode IN (:sample_ids)");

            // Open XML
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(new FileInputStream(
                    "C:\\Users\\thompson\\Downloads\\SearchRequest.xml"));
            XPath xPath =  XPathFactory.newInstance().newXPath();
            XPathExpression lcsetKeyExpr = xPath.compile("/rss/channel/item/key");
            XPathExpression gssrExpr = xPath.compile(
                    "../customfields/customfield/customfieldname[text()='GSSR ID(s)']/../customfieldvalues/customfieldvalue");
            NodeList lcsetNodeList = (NodeList) lcsetKeyExpr.evaluate(document,
                    XPathConstants.NODESET);
            for (int i = 0; i < lcsetNodeList.getLength(); i++) {
                Node keyNode = lcsetNodeList.item(i);
                System.out.println(keyNode.getFirstChild().getNodeValue());
                NodeList gssrNodeList = (NodeList) gssrExpr.evaluate(keyNode, XPathConstants.NODESET);
                List<String> sampleIds = new ArrayList<>();
                for (int j = 0; j < gssrNodeList.getLength(); j++) {
                    Node customfieldvalueNode = gssrNodeList.item(j);
                    String sampleIdString = customfieldvalueNode.getFirstChild().getNodeValue();
                    System.out.println(sampleIdString);
                    Collections.addAll(sampleIds, BREAK_PATTERN.split(sampleIdString));
                }
                if (!sampleIds.isEmpty()) {
                    nativeQuery.setParameter("sample_ids", sampleIds);
                    List resultList = nativeQuery.getResultList();
                    if (!resultList.isEmpty()) {
                        System.out.println("Found " + resultList.size() + " Squid tubes.");
                    }
                }
            }
            // Iterate over LCSETs
            // If LCSET doesn't exist in Mercury
            // Get GSSR IDs
            // Get barcodes
            // Determine control
            // Create bucket entries?
            // Create LCSET
            // Register control
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }
}
