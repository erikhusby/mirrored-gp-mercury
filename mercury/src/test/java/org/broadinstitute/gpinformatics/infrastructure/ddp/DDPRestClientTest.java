package org.broadinstitute.gpinformatics.infrastructure.ddp;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.DDPKitInfo;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class DDPRestClientTest {

    private DDPRestClient ddpRestClient;

    @BeforeMethod
    public void setUp() {
        DDPConfig ddpConfig = DDPConfig.produce(DEV);
        ddpRestClient = new DDPRestClient(ddpConfig);
    }

    @Test
    public void testGetKitInfo() {
        Map<String, Boolean> mapBarcodeToStatus = ddpRestClient.areKitsRegistered(Arrays.asList("testing123"));
        Assert.assertEquals(mapBarcodeToStatus.get("testing123"), Boolean.TRUE);
        Optional<DDPKitInfo> kitInfoOpt = ddpRestClient.getKitInfo("testing123");
        DDPKitInfo kitInfo = kitInfoOpt.get();
        Assert.assertEquals(kitInfo.getCollaboratorParticipantId(), "JJGUNZLLAY6HK9AAFWZQ");
        Assert.assertEquals(kitInfo.getCollaboratorSampleId(), "VTLI45RTA3VGZRJBssfQ7");
        Assert.assertEquals(kitInfo.getSampleCollectionBarcode(), "sc-38925");
        Assert.assertEquals(kitInfo.getMaterialInfo(), "Saliva");
        Assert.assertEquals(kitInfo.getReceptacleName(), "Oragene Kit");
        Assert.assertEquals(kitInfo.getGender(), "U");
    }
}