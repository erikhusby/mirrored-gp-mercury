package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


@Test(groups = EXTERNAL_INTEGRATION)
public class LabEventSampleDataFetcherTest extends ContainerTest {

    @Inject
    LabEventSampleDataFetcher fetcher;

    public void testLabEventSampleDataFetcher() {

        List sampleKeys = Arrays.asList(new String[]{"SM-3X73X", "SM-4CT4Y"});

        Map<String, List<LabVessel>> results = fetcher.findMapBySampleKeys(sampleKeys);

        assertNotNull(results.get("SM-3X73X"));

        LabEventSampleDTO labEventSampleDTO = new LabEventSampleDTO(results.get("SM-3X73X"), "SM-3X73X");

        assertEquals("05/15/2013", labEventSampleDTO.getSampleReceiptDate());

        assertNotNull(results.get("SM-4CT4Y"));

        labEventSampleDTO = new LabEventSampleDTO(results.get("SM-4CT4Y"), "SM-4CT4Y");

        assertEquals("11/20/2013", labEventSampleDTO.getSamplePackagedDate());
    }
}
