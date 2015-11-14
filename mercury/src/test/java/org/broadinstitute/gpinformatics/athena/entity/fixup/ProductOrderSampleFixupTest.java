package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
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
}
