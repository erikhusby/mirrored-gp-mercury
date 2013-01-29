package org.broadinstitute.gpinformatics.infrastructure.bsp;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class BSPSampleDataFetcherContainerTest extends Arquillian {

    @Deployment
    public static WebArchive getDeployment() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Inject
    private BSPSampleDataFetcher bspSampleDataFetcher;

    @Test
    public void testFFPE() {
        BSPSampleDTO ffpe = bspSampleDataFetcher.fetchSingleSampleFromBSP("SM-16BL4");
        BSPSampleDTO notFFPE = bspSampleDataFetcher.fetchSingleSampleFromBSP("SM-3HM8");

        List<BSPSampleDTO> dtoList = Arrays.asList(ffpe, notFFPE);

        bspSampleDataFetcher.fetchFFPEDerived(dtoList);

        Assert.assertTrue(ffpe.getFfpeDerived());
        Assert.assertFalse(notFFPE.getFfpeDerived());
    }
}
