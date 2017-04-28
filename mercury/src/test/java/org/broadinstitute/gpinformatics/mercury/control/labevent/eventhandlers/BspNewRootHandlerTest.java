package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Created by thompson on 4/21/2017.
 */
public class BspNewRootHandlerTest extends Arquillian {

    @Inject
    private BspNewRootHandler bspNewRootHandler;

    @Inject
    private UserBean userBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testX() {
        userBean.loginOSUser();
        // todo jmt fix
        bspNewRootHandler.handleEvent(null, null);
    }
}