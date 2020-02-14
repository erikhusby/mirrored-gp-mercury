package org.broadinstitute.gpinformatics.mercury.presentation.hsa;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.TopOffStateMachineDecorator;
import org.broadinstitute.gpinformatics.mercury.presentation.TestCoreActionBeanContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class TopOffActionBeanTest {

    @Test
    public void testValidateIndexes() {
        TopOffActionBean topOffActionBean = new TopOffActionBean();
        TestCoreActionBeanContext testContext = new TestCoreActionBeanContext();
        topOffActionBean.setContext(testContext);

        topOffActionBean.setSequencingType(TopOffStateMachineDecorator.StateNames.HiSeqX.getDisplayName());
        Map<String, List<TopOffActionBean.HoldForTopoffDto>> mapTab = new HashMap<>();
        List<TopOffActionBean.HoldForTopoffDto> topoffDtos = new ArrayList<>();
        mapTab.put(topOffActionBean.getSequencingType(), topoffDtos);
        String sampleA = "sampleA";
        String sampleB = "sampleB";
        List<String> selectedSamples = new ArrayList<>();
        selectedSamples.add(sampleA);
        selectedSamples.add(sampleB);
        topOffActionBean.setSelectedSamples(selectedSamples);
        topoffDtos.add(createDto("IlluminaP7", sampleA));
        topoffDtos.add(createDto("IlluminaP5", sampleB));
        topOffActionBean.setMapTabToDto(mapTab);
        topOffActionBean.validateSelectedIndexes();
        Assert.assertEquals(testContext.getValidationErrors().size(), 0);

        String sampleC = "sampleC";
        topoffDtos.add(createDto("IlluminaP5", sampleC));
        selectedSamples.add(sampleC);
        topOffActionBean.validateSelectedIndexes();
        Assert.assertEquals(testContext.getValidationErrors().size(), 1);
    }

    private TopOffActionBean.HoldForTopoffDto createDto(String index, String sample) {
        TopOffActionBean.HoldForTopoffDto dto = new TopOffActionBean.HoldForTopoffDto();
        dto.setIndex(index);
        dto.setPdoSample(sample);
        return dto;
    }
}