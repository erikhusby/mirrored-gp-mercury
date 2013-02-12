package org.broadinstitute.gpinformatics.mercury.test.entity.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.*;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
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

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;


public class BSPPlatingTest extends ContainerTest {

    //@Inject
    BSPPlatingRequestService platingService;

    public BSPPlatingTest() {
    }

    /**
     * Don't want to hammer on BSP all the time, so disabling, but turn on
     *
     * @throws Exception any errors
     */
    @Test(groups = {EXTERNAL_INTEGRATION}, enabled = false)
    public void testIssueBSPPlating() throws Exception {

        platingService = new BSPPlatingRequestServiceImpl();

        Map<MercurySample, AliquotParameters> starterMap = new HashMap<MercurySample, AliquotParameters>();

        MercurySample sample = new MercurySample("PDO-123", "SM-26BPV");
        starterMap.put(sample, new AliquotParameters(0.0f, 1.0f));

        sample = new MercurySample("PDO-123", "SM-26BHJ");
        starterMap.put(sample, new AliquotParameters(0.0f, 1.0f));

        sample = new MercurySample("PDO-123", "SM-26BPU");
        starterMap.put(sample, new AliquotParameters(0.0f, 1.0f));

        sample = new MercurySample("PDO-123", "SM-HOWIE");
        starterMap.put(sample, new AliquotParameters(0.0f, 1.0f));

        sample = new MercurySample("PDO-123", "SM-26BPK");
        starterMap.put(sample, new AliquotParameters(0.0f, 1.0f));


        BSPSampleFactory bspSampleFactory = new BSPSampleFactory();
        List<BSPPlatingRequest> bspRequests = bspSampleFactory.buildBSPPlatingRequests(starterMap);

        List<ControlWell> controls = new ArrayList<ControlWell>();
        BSPPlatingRequestService bspPlatingService = new BSPPlatingRequestServiceStub();
        BSPPlatingRequestOptions options = bspPlatingService.getBSPPlatingRequestDefaultOptions();
        BSPPlatingRequestResult platingResult = bspPlatingService.issueBSPPlatingRequest(options, bspRequests,
                controls, "hrafal", "EE-BSP-PLATING-1", "BSP Plating Exome Express Test", "Solexa", "EE-TEST-1");

        Assert.assertNotNull(platingResult);
    }
}

