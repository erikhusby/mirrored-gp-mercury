package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.io.PrintStream;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test generation of the samplesheet.csv
 */
@Test(groups = TestGroups.STANDARD)
public class SampleSheetFactoryTest extends ContainerTest {

    @Inject
    private SampleSheetFactory sampleSheetFactory;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testBasics() {
        MessageCollection messageCollection = new MessageCollection();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        sampleSheetFactory.write(new PrintStream(byteArrayOutputStream), , messageCollection);
    }
}