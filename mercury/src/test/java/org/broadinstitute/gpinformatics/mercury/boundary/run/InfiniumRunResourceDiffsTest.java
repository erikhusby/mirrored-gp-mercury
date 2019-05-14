package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.JaxRsUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ImportFromSquidTest;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * A test to compare InfiniumRunResource output from different deployments, e.g. to verify a change.
 */
@Test(groups = TestGroups.STANDARD)
public class InfiniumRunResourceDiffsTest extends Arquillian {

    @Inject
    private StaticPlateDao staticPlateDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void testCompareAll() {
        List<StaticPlate> chips = staticPlateDao.findByPlateType(StaticPlate.PlateType.InfiniumChip24);
        chips.forEach(this::compareChip);
    }

    private void compareChip(StaticPlate staticPlate) {
        System.out.println("Comparing run " + staticPlate.getLabel());
        try {
            long t1 = System.currentTimeMillis();

            String chipPosition = staticPlate.getLabel() + "_R01C01";
            String newRun = getBuilder(
                    new URL(ImportFromSquidTest.TEST_MERCURY_URL + "/"),
                    chipPosition).get(String.class);
            long t2 = System.currentTimeMillis();
            String referenceRun = getBuilder(
                    new URL("https://mercurydev:8443/Mercury/"),
                    chipPosition).get(String.class);
            long t3 = System.currentTimeMillis();
            System.out.println("New: " + (t2 - t1) + ". Reference " + (t3 - t2));
            JSONCompareResult jsonCompareResult = JSONCompare.compareJSON(referenceRun, newRun,
                    JSONCompareMode.LENIENT);
            if (jsonCompareResult.failed()) {
                System.out.println(jsonCompareResult.getMessage());
            }
        } catch (Throwable e) {
            System.out.println(e);
        }
    }

    private static Invocation.Builder getBuilder(URL baseUrl, String runName) throws Exception {
        String url = RestServiceContainerTest.convertUrlToSecure(baseUrl) + "rest/infiniumrun/query";
        ClientBuilder clientBuilder = JaxRsUtils.getClientBuilderAcceptCertificate();
        return clientBuilder.build().target(url)
                .queryParam("chipWellBarcode", runName)
                .request(MediaType.APPLICATION_JSON);
    }
}
