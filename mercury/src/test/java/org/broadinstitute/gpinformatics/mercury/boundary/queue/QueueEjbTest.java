package org.broadinstitute.gpinformatics.mercury.boundary.queue;


import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class QueueEjbTest extends Arquillian {

    @Inject
    private QueueEjb queueEjb;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test(groups = TestGroups.STANDARD)
    public void loadPicoQueueTest() {
        GenericQueue picoQueue = queueEjb.findQueueByType(QueueType.PICO);
        Assert.assertNotNull(picoQueue);
    }
}
