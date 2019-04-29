package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class LabEventEtlTest extends Arquillian {

    @Inject
    private LabEventEtl labEventEtl;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testPdoInference() {
        // Events for 1189237891 from GPLIM-6215
        long[] labEventIds = {
                3481709L,
                3481746L,
                3481747L,
                3481748L,
                3481749L,
                3483088L,
                3482996L,
                3492744L,
                3492766L,
                3492776L};
        for (long labEventId : labEventIds) {
            List<LabEventEtl.EventFactDto> eventFactDtos = labEventEtl.makeEventFacts(labEventId);
            for (LabEventEtl.EventFactDto eventFactDto : eventFactDtos) {
                String pdoName = eventFactDto.getPdoName();
                if (pdoName != null) {
                    Assert.assertEquals(pdoName, "PDO-17756");
                }
            }
        }
    }
}
