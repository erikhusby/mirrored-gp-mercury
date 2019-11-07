package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.statehandler;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.AlignmentStateDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.io.IOException;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.testng.Assert.*;

@Test(groups = TestGroups.STANDARD)
public class AlignmentStateHandlerTest extends Arquillian {

    @Inject
    private AlignmentStateHandler alignmentStateHandler;

    @Inject
    private AlignmentStateDao alignmentStateDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "prod");
    }

    @Test
    public void testOnEnter() throws IOException {
        AlignmentState state = alignmentStateDao.findById(AlignmentState.class, 8307L);
        alignmentStateHandler.onEnter(state);
    }
}