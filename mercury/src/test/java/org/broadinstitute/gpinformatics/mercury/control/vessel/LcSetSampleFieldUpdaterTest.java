package org.broadinstitute.gpinformatics.mercury.control.vessel;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

@Test(groups = TestGroups.DATABASE_FREE)
public class LcSetSampleFieldUpdaterTest {

    @Test
    public void test_sample_field_text_with_reworks() {
        String expectedText = "000012\n\n000033 (rework)";

        Set<LabVessel> newTubes = new HashSet<LabVessel>();
        Set<LabVessel> reworks = new HashSet<LabVessel>();
        newTubes.add(new TwoDBarcodedTube("000012"));
        reworks.add(new TwoDBarcodedTube("000033"));

        LabBatch batch = new LabBatch("test",newTubes, LabBatch.LabBatchType.WORKFLOW);
        batch.addReworks(reworks);

        String actualText = new LcSetSampleFieldUpdater().buildSamplesListString(batch);

        assertThat(actualText.trim(),equalTo(expectedText.trim()));
    }

    @Test
    public void test_sample_field_text_no_reworks() {
        String expectedText = "000012";

        Set<LabVessel> newTubes = new HashSet<LabVessel>();
        newTubes.add(new TwoDBarcodedTube("000012"));

        LabBatch batch = new LabBatch("test",newTubes, LabBatch.LabBatchType.WORKFLOW);

        String actualText = new LcSetSampleFieldUpdater().buildSamplesListString(batch);

        assertThat(actualText.trim(),equalTo(expectedText.trim()));
    }
}
