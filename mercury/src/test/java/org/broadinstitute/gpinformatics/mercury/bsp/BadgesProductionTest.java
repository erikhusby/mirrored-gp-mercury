package org.broadinstitute.gpinformatics.mercury.bsp;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.JaxRsUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STANDARD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Scott Matthews
 *         Date: 3/8/13
 *         Time: 8:14 AM
 */
@Test(groups = STANDARD)
@RequestScoped
public class BadgesProductionTest extends Arquillian {


    private static final String PROD_BADGES_SOURCE = "User_badges_prod.csv";

    AppConfig appConfig;

    @Inject
    UserTransaction utx;

    @Inject
    BSPUserList bspList;


    @Deployment
    public static WebArchive buildMercuryWar() {
        // need TEST here for now because there's no STUBBY version of ThriftConfig
        return DeploymentBuilder.buildMercuryWar(PROD);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        if (utx == null) {
            return;
        }
        utx.begin();

        appConfig = AppConfig.produce(DEV);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (utx == null) {
            return;
        }
        utx.rollback();

    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = false)
    public void validateBadgeIds() throws Exception {

        FileInputStream badgesList = (FileInputStream) Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(PROD_BADGES_SOURCE);

        BufferedReader badgesReader = new BufferedReader(new InputStreamReader(badgesList));

        ClientBuilder clientBuilder = JaxRsUtils.getClientBuilderAcceptCertificate();

        final WebTarget badgeResource =
                clientBuilder.build().target(appConfig.getUrl() + "rest/limsQuery/fetchUserIdForBadgeId");

        while (badgesReader.ready()) {
            String[] columns = badgesReader.readLine().split(",");
            //remove outer ' and trim
            String username = columns[0].replace('\'', ' ').trim();
            String badgeId = columns[1].replace('\'', ' ').trim();
            String termination = columns[2].replace('\'', ' ').trim();


            if (StringUtils.isBlank(termination)) {

                System.out.println("Testing bsp list for user " + username);
                assertThat(badgeId, equalTo(bspList.getByUsername(username).getBadgeNumber()));
                String result =
                        badgeResource.queryParam("badgeId", badgeId)
                                .request(APPLICATION_JSON_TYPE)
                                .get(String.class);

                assertThat(result, equalTo(username));
            }
        }
    }
}
