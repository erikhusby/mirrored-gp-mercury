/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.fixup;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderKitDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit_;
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
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This "test" is an example of how to fixup some data.  Each fix method includes the JIRA ticket ID.
 * Set @Test(enabled=false) after running once.
 */
@Test(groups = TestGroups.FIXUP)
public class ProductOrderSampleKitFixupTest extends Arquillian {

    @Inject
    private ProductOrderKitDao productOrderKitDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private UserBean userBean;

    private final Log log = LogFactory.getLog(ProductOrderSampleKitFixupTest.class);

    // When you run this on prod, change to PROD and prod.
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    // GPLIM-2427
    @Test(enabled = false)
    public void backPopulateKitRequestIsExomeExpress() {
        List<ProductOrderKit> nullExomeExpress =
                productOrderKitDao.findList(ProductOrderKit.class, ProductOrderKit_.exomeExpress, null);
        for (ProductOrderKit pdoKit : nullExomeExpress) {
            pdoKit.setExomeExpress(false);
        }
        productOrderKitDao.persistAll(nullExomeExpress);
        productOrderKitDao.flush();

    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void gplim3153_associate_mercurySamples_to_productOrderSamples() {

        int iteration = 1;

        int sampleBlockSize = 5000;
        List<ProductOrderSample> allSamplesToModify;
        List<String> sampleKeys;

        do {
            allSamplesToModify =
                    productOrderSampleDao.findSamplesWithoutMercurySample(0, sampleBlockSize);
            sampleKeys = Lists.transform(allSamplesToModify, new Function<ProductOrderSample, String>() {
                @Override
                public String apply(ProductOrderSample o) {
                    return o.getSampleKey();
                }
            });
            if (!allSamplesToModify.isEmpty()) {
                log.info(String.format("Working on iteration %d with a sample block size of %d", iteration,
                        sampleBlockSize));
                log.info("About to process samples beginning with " + allSamplesToModify.iterator().next().getSampleKey());

                Map<String, MercurySample> sampleByKey = mercurySampleDao.findMapIdToMercurySample(sampleKeys);

                for (ProductOrderSample productOrderSample : allSamplesToModify) {
                    assertThat(productOrderSample.getMercurySample(), is(nullValue()));
                    MercurySample matchingSample = sampleByKey.get(productOrderSample.getSampleKey());
                    if (matchingSample != null) {
                        productOrderSample.setMercurySample(matchingSample);
                    }
                }
                productOrderSampleDao.flush();
                productOrderSampleDao.clear();
                iteration++;
            }
        } while (!allSamplesToModify.isEmpty());
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void gplim4146LinkPdoSamplesToMercurySample() {
        userBean.loginOSUser();
        gplim3153_associate_mercurySamples_to_productOrderSamples();
        // Ideally this would be in each transaction, but this is an infrastructure fixup, so it doesn't seem worthwhile
        // to copy and change the existing, working code.
        productOrderSampleDao.persist(new FixupCommentary("GPLIM-4146 link ProductOrderSamples to MercurySamples"));
        productOrderSampleDao.flush();
    }
}
