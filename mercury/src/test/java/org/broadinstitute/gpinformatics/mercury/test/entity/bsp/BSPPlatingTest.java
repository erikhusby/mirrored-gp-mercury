package org.broadinstitute.gpinformatics.mercury.test.entity.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestOptions;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestResult;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.ControlWell;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.bsp.BSPSampleFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.gpinformatics.mercury.entity.queue.AliquotParameters;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STUBBY;


@Test(groups = TestGroups.STUBBY)
@Dependent
public class BSPPlatingTest  {

    public BSPPlatingTest() {}

    /**
     * Don't want to hammer on BSP all the time, so disabling, but turn on <br />
     * TODO:  Don't turn on until either set to STANDARD and needs to extend Arquillian or STUBBY and needs to extend StubbyContainerTest
     *
     * @throws Exception any errors
     */
    @Test(groups = {STUBBY}, enabled = false)
    @Dependent
    public void testIssueBSPPlating() throws Exception {

        Map<MercurySample, AliquotParameters> starterMap = new HashMap<>();

        MercurySample sample = new MercurySample("SM-26BPV", MercurySample.MetadataSource.BSP);
        starterMap.put(sample, new AliquotParameters(0.0f, 1.0f));

        sample = new MercurySample("SM-26BHJ", MercurySample.MetadataSource.BSP);
        starterMap.put(sample, new AliquotParameters(0.0f, 1.0f));

        sample = new MercurySample("SM-26BPU", MercurySample.MetadataSource.BSP);
        starterMap.put(sample, new AliquotParameters(0.0f, 1.0f));

        sample = new MercurySample("SM-HOWIE", MercurySample.MetadataSource.BSP);
        starterMap.put(sample, new AliquotParameters(0.0f, 1.0f));

        sample = new MercurySample("SM-26BPK", MercurySample.MetadataSource.BSP);
        starterMap.put(sample, new AliquotParameters(0.0f, 1.0f));


        BSPSampleFactory bspSampleFactory = new BSPSampleFactory();
        List<BSPPlatingRequest> bspRequests = bspSampleFactory.buildBSPPlatingRequests(starterMap);

        List<ControlWell> controls = new ArrayList<>();

        // Only works because no CDI dependencies in stub
        BSPPlatingRequestService bspPlatingService = new BSPPlatingRequestServiceStub();

        BSPPlatingRequestOptions options = bspPlatingService.getBSPPlatingRequestDefaultOptions();

        BSPPlatingRequestResult platingResult = bspPlatingService.issueBSPPlatingRequest(options, bspRequests,
                controls, "hrafal", "EE-BSP-PLATING-1", "BSP Plating Exome Express Test", "Solexa", "EE-TEST-1");

        Assert.assertNotNull(platingResult);
        Assert.assertNotNull(platingResult.getPlatingRequestReceipt(), "Should have returned a plating request receipt");
        Assert.assertTrue(((platingResult.getErrors() == null) || platingResult.getErrors().isEmpty()), "Should not have received any errors");
    }
}

