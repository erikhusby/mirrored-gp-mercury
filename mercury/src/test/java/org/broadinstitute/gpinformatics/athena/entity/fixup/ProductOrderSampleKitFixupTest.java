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

import clover.com.google.common.base.Function;
import clover.com.google.common.collect.Lists;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderKitDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
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

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private Log log;

    // When you run this on prod, change to PROD and prod.
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }
    // GPLIM-2427
    @Test(enabled=false)
    public void backPopulateKitRequestIsExomeExpress(){
        List<ProductOrderKit> nullExomeExpress = productOrderKitDao.findList(ProductOrderKit.class, ProductOrderKit_.exomeExpress, null);
        for (ProductOrderKit pdoKit : nullExomeExpress) {
            pdoKit.setExomeExpress(false);
        }
        productOrderKitDao.persistAll(nullExomeExpress);
        productOrderKitDao.flush();

    }

    public void gplim3153_associate_mercurySamples_to_productOrderSamples() {

        int referencePage = 1;

        int samplesPerPage = 5000;
        List<ProductOrderSample> allSamplesToModify =
                productOrderSampleDao.findSamplesWithoutMercurySample(referencePage++, samplesPerPage);

        List<String> sampleKeys = Lists.transform(allSamplesToModify, new Function<ProductOrderSample, String>() {
            @Override
            public String apply(ProductOrderSample o) {
                return o.getSampleKey();
            }
        });

        while(!allSamplesToModify.isEmpty()) {
            log.info(String.format("Working on page %d with a page size of %d", referencePage, samplesPerPage));
            log.info("About to process samples beginning with " +allSamplesToModify.iterator().next().getSampleKey());

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

            allSamplesToModify =
                    productOrderSampleDao.findSamplesWithoutMercurySample(referencePage++, samplesPerPage);
            sampleKeys = Lists.transform(allSamplesToModify, new Function<ProductOrderSample, String>() {
                @Override
                public String apply(ProductOrderSample o) {
                    return o.getSampleKey();
                }
            });
        }
    }
}
