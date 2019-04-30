package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVesselFixupTest;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.FIXUP)
public class ProductOrderSampleFixupTest extends Arquillian {

    @Inject
    private UserBean userBean;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    /**
     * When applying this to Production, change the input to PROD, "prod"
     */
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void gplim3691fixMercurySampleLinksForLcset7666() {
        userBean.loginOSUser();

        HashSet<String> sampleNames = new HashSet<>(Arrays.asList("SM-A2SIA", "SM-A2SIE", "SM-A2SI8", "SM-A2SI9"));
        List<ProductOrderSample> productOrderSamples = productOrderSampleDao.findByOrderKeyAndSampleNames("PDO-6760",
                sampleNames);
        Map<String, MercurySample> mercurySampleMap = mercurySampleDao.findMapIdToMercurySample(sampleNames);

        for (ProductOrderSample productOrderSample : productOrderSamples) {
            productOrderSample.setMercurySample(mercurySampleMap.get(productOrderSample.getSampleKey()));
        }

        productOrderSampleDao.persist(new FixupCommentary("GPLIM-3691 set MercurySamples on ProductOrderSamples to make rework bucketing work"));
    }

    /**
     * This test reads its parameters from a file, testdata/FixupPdoSamples.txt, so it can be used for other similar fixups,
     * without writing a new test.  Example contents of the file are:
     * SUPPORT-1094
     * PDO-6987
     * SM-A8XL2
     * SM-A8XL3
     */
    @Test(enabled = false)
    public void support1094LinkPdoSample() {
        userBean.loginOSUser();

        try {
            List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("FixupPdoSamples.txt"));
            String jiraTicket = lines.get(0);
            String pdoKey = lines.get(1);

            Set<String> sampleNames = new HashSet<>(lines.subList(2, lines.size()));
            List<ProductOrderSample> productOrderSamples = productOrderSampleDao.findByOrderKeyAndSampleNames(pdoKey,
                    sampleNames);
            Map<String, MercurySample> mercurySampleMap = mercurySampleDao.findMapIdToMercurySample(sampleNames);

            for (ProductOrderSample productOrderSample : productOrderSamples) {
                productOrderSample.setMercurySample(mercurySampleMap.get(productOrderSample.getSampleKey()));
            }

            productOrderSampleDao.persist(new FixupCommentary(jiraTicket +
                    " set MercurySamples on ProductOrderSamples to make rework bucketing work"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void support4060UpdateDeliveryDocument() {

        userBean.loginOSUser();

        List<ProductOrderSample> samples = productOrderSampleDao.findByOrderKeyAndSampleNames("PDO-14794",
                Collections.singleton("SM-GFCXQ"));

        for (ProductOrderSample sample : samples) {
            for (LedgerEntry ledgerEntry : sample.getLedgerItems()) {
                if(ledgerEntry.getWorkItem().equals("278914")) {
                    ledgerEntry.setSapDeliveryDocumentId("0200003045");
                }
            }

        }

        productOrderSampleDao.persist(new FixupCommentary("SUPPORT-4060 Adding Sap Delivery document which did not come back during create.  Will allow billing to complete"));

    }

    /**
     * Until Aggregation Particle is added to the UI, this test is used to set it.  The test reads its parameters from
     * a file, testdata/PdoSampleAggParticle.txt.  Example contents of the file are (third and subsequent rows are
     * sample position (1-based, as displayed on PDO page), sample ID, aggregation particle):
     * GPLIM-5598
     * PDO-6987
     * 1 SM-A8XL2 G1234
     * 2 SM-A8XL3 G3456
     */
    @Test(enabled = false)
    public void fixupGplim5598AggParticle() {
        userBean.loginOSUser();

        try {
            List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("PdoSampleAggParticle.txt"));
            String jiraTicket = lines.get(0);
            String pdoKey = lines.get(1);
            Set<String> sampleNames = new HashSet<>();
            for(int i = 2; i < lines.size(); i++) {
                String[] fields = LabVesselFixupTest.WHITESPACE_PATTERN.split(lines.get(i));
                if (fields.length != 3) {
                    throw new RuntimeException("Expected three white-space separated fields in " + lines.get(i));
                }
                sampleNames.add(fields[1]);
            }

            List<ProductOrderSample> productOrderSamples = productOrderSampleDao.findByOrderKeyAndSampleNames(pdoKey,
                    sampleNames);
            Map<Pair<Integer, String>, ProductOrderSample> mapKeyToPdoSample = new HashMap<>();
            for (ProductOrderSample productOrderSample : productOrderSamples) {
                mapKeyToPdoSample.put(
                        new ImmutablePair<>(productOrderSample.getSamplePosition() + 1, productOrderSample.getSampleKey()),
                        productOrderSample);
            }
            for(int i = 2; i < lines.size(); i++) {
                String[] fields = LabVesselFixupTest.WHITESPACE_PATTERN.split(lines.get(i));
                ProductOrderSample productOrderSample = mapKeyToPdoSample.get(
                        new ImmutablePair<>(Integer.parseInt(fields[0]), fields[1]));
                if (productOrderSample == null) {
                    throw new RuntimeException("Failed to find position " + fields[0] + " sample " + fields[1]);
                }
                System.out.println("Setting " + productOrderSample.getSampleKey() + " to " + fields[2]);
                productOrderSample.setAggregationParticle(fields[2].equals("null") ? null : fields[2]);
            }

            productOrderSampleDao.persist(new FixupCommentary(jiraTicket +
                    " set aggregation particle on ProductOrderSamples"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(enabled = false)
    public void fixupGplim5934() throws Exception {
        userBean.loginOSUser();
        ProductOrderSample productOrderSample =  productOrderSampleDao.findById(ProductOrderSample.class, 2659324L);
        Assert.assertNotNull(productOrderSample);
        Assert.assertEquals(productOrderSample.getMercurySample().getSampleKey(), "SM-HZS72_8812");
        ProductOrder productOrder = productOrderSample.getProductOrder();
        System.out.println("Unlinking mercury sample " + productOrderSample.getMercurySample().getSampleKey() +
                " from pdo sample named " + productOrderSample.getSampleKey() + " and removing it from " +
                productOrder.getBusinessKey());
        productOrderSample.setMercurySample(null);
        productOrder.getSamples().remove(productOrderSample);
        productOrderSampleDao.persistAll(Arrays.asList(productOrder,
                new FixupCommentary("GPLIM-5934 unlink mercury sample and remove pdo sample from pdo")));
    }
}
