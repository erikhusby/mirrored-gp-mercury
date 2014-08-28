package org.broadinstitute.gpinformatics.mercury.test.entity.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestOptions;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestResult;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPPlatingRequestServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.ControlWell;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.bsp.BSPSampleFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.gpinformatics.mercury.entity.queue.AliquotParameters;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STUBBY;


@Test(groups = TestGroups.STUBBY)
public class BSPPlatingTest  {

    //@Inject
    BSPPlatingRequestService platingService;

    public BSPPlatingTest() {
    }

    /**
     * Don't want to hammer on BSP all the time, so disabling, but turn on
     *
     * @throws Exception any errors
     */
    @Test(groups = {STUBBY}, enabled = false)
    public void testIssueBSPPlating() throws Exception {
        platingService = new BSPPlatingRequestServiceImpl();

        Map<MercurySample, AliquotParameters> starterMap = new HashMap<>();

        MercurySample sample = new MercurySample("SM-26BPV");
        starterMap.put(sample, new AliquotParameters(0.0f, 1.0f));

        sample = new MercurySample("SM-26BHJ");
        starterMap.put(sample, new AliquotParameters(0.0f, 1.0f));

        sample = new MercurySample("SM-26BPU");
        starterMap.put(sample, new AliquotParameters(0.0f, 1.0f));

        sample = new MercurySample("SM-HOWIE");
        starterMap.put(sample, new AliquotParameters(0.0f, 1.0f));

        sample = new MercurySample("SM-26BPK");
        starterMap.put(sample, new AliquotParameters(0.0f, 1.0f));


        BSPSampleFactory bspSampleFactory = new BSPSampleFactory();
        List<BSPPlatingRequest> bspRequests = bspSampleFactory.buildBSPPlatingRequests(starterMap);

        List<ControlWell> controls = new ArrayList<>();
        BSPPlatingRequestService bspPlatingService = new BSPPlatingRequestServiceStub();
        BSPPlatingRequestOptions options = bspPlatingService.getBSPPlatingRequestDefaultOptions();

        BSPPlatingRequestResult platingResult = bspPlatingService.issueBSPPlatingRequest(options, bspRequests,
                controls, "hrafal", "EE-BSP-PLATING-1", "BSP Plating Exome Express Test", "Solexa", "EE-TEST-1");

        Assert.assertNotNull(platingResult);
        Assert.assertNotNull(platingResult.getPlatingRequestReceipt(), "Should have returned a plating request receipt");
        Assert.assertTrue(((platingResult.getErrors() == null) || platingResult.getErrors().isEmpty()), "Should not have received any errors");
    }
}

