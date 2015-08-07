package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
}
