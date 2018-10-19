package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.IsExported;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test BSP web service.
 */
@Test(groups = TestGroups.STANDARD)
public class BSPGetExportedSamplesFromAliquotsTest extends Arquillian {

    @Inject
    private BSPGetExportedSamplesFromAliquots bspGetExportedSamplesFromAliquots;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    public void testBasics() {
        Collection<String> sampleLsids = new ArrayList<>();
        sampleLsids.add("broadinstitute.org:bsp.prod.sample:GOHM6");
        List<BSPGetExportedSamplesFromAliquots.ExportedSample> exportedSamplesFromAliquots =
                bspGetExportedSamplesFromAliquots.getExportedSamplesFromAliquots(sampleLsids,
                        IsExported.ExternalSystem.GAP);
        Assert.assertEquals(exportedSamplesFromAliquots.size(), 1);
        Assert.assertEquals(exportedSamplesFromAliquots.get(0).getExportedLsid(),
                "broadinstitute.org:bsp.prod.sample:GP3T6");
    }
}
