package org.broadinstitute.gpinformatics.mercury.control.run;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;


/**
 * Test archiving
 */
@Test(groups = TestGroups.STUBBY)
public class InfiniumArchiverTest extends Arquillian {

    @Inject
    private InfiniumArchiver infiniumArchiver;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    public void testX() {
        List<LabVessel> chipsToArchive = infiniumArchiver.findChipsToArchive();
        for (LabVessel labVessel : chipsToArchive) {
            System.out.println(labVessel.getLabel());
        }
    }
}