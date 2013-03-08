package org.broadinstitute.gpinformatics.mercury.bsp;

import com.sun.jersey.api.client.WebResource;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Scott Matthews
 *         Date: 3/8/13
 *         Time: 8:14 AM
 */
public class BadgesProductionTest extends RestServiceContainerTest {


    private static final String PROD_BADGES_SOURCE = "User_badges_prod.csv";

    @Deployment
    public static WebArchive buildMercuryWar() {
        // need TEST here for now because there's no STUBBY version of ThriftConfig
        // see ThriftServiceProducer.produce()
        return DeploymentBuilder.buildMercuryWar(PROD);
    }

    @Override
    protected String getResourcePath() {
        return "limsQuery";
    }


    @Test(groups = EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = false)
    @RunAsClient
    public void validateBadgeIds(@ArquillianResource URL baseUrl) throws IOException {

        WebResource resource;

        FileInputStream badgesList = new FileInputStream("src/test/resources/testdata/" + PROD_BADGES_SOURCE);

        BufferedReader badgesReader = new BufferedReader(new InputStreamReader(badgesList));
        while (badgesReader.ready()) {
            String[] columns = badgesReader.readLine().split(",");
            //remove outer ' and trim
            String username = columns[0].replace('\'', ' ').trim();
            String badgeId = columns[1].replace('\'', ' ').trim();
            String termination = columns[2].replace('\'', ' ').trim();

            if(StringUtils.isBlank(termination)) {
                resource = makeWebResource(baseUrl, "fetchUserIdForBadgeId").queryParam("badgeId", badgeId);

                String result = get(resource);

                assertThat(result, equalTo(username));
            }
        }
    }


}
