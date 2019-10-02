/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.integration.sap;

import org.broadinstitute.gpinformatics.infrastructure.ExternalServiceRuntimeException;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.integration.quotes.CacheRefresher;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.validation.constraints.Null;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Arquillian tests for PriceListCache.
 */
@Test(groups = TestGroups.STANDARD)
@Dependent
public class SapPriceListCacheContainerTest extends Arquillian {

    public SapPriceListCacheContainerTest(){}

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar();
    }

    @Test(dataProvider = "exceptionDataProvider", expectedExceptions = ExternalServiceRuntimeException.class)
    public void testRefreshCache(Class<RuntimeException> throwable) throws Exception {
        SapIntegrationService sapIntegrationService = Mockito.mock(SapIntegrationService.class);
        Mockito.when(sapIntegrationService.findProductsInSap()).thenThrow(throwable);

        new SAPProductPriceCache(sapIntegrationService).refreshCache();
    }

    @DataProvider(name = "exceptionDataProvider")
    public Iterator<Object[]> exceptionDataProvider() {
        List<Object[]> testCases = new ArrayList<>();
        testCases.add(new Object[]{RuntimeException.class});
        testCases.add(new Object[]{NullPointerException.class});

        return testCases.iterator();
    }
}
