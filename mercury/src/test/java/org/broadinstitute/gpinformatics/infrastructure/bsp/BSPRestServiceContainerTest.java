package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class BSPRestServiceContainerTest extends Arquillian {

    @Inject
    private BSPSampleSearchService bspSampleSearchService;

    @Inject
    private BSPRestService bspRestService;

    @Deployment
    public static WebArchive getDeployment() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testGetSampleDetails() {
        List<String> barcodes = Arrays.asList("0156343673");
        bspRestService.fetchSampleDetailsByMatrixBarcodes(barcodes);
//        bspSampleSearchService.runSampleSearch(null, BSPSampleSearchColumn.PDO_SEARCH_COLUMNS);
    }
}
