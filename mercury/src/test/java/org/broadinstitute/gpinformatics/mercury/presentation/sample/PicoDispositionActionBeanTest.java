package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests the PicoDispositionActionBean
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class PicoDispositionActionBeanTest extends TestCase {

    @Test
    public void testDisplayList() throws Exception {
        PicoDispositionActionBean pdab = new PicoDispositionActionBean();

        // Makes list of post-pico dispositions from the tube formation label.
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        int incrementer = 0;
        for (int i = 0; i < 8; ++i) {
            for (int col = 1; col <= 12; ++col) {
                ++incrementer;
                String cellname = "ABCDEFGH".substring(i, i + 1) + col;
                VesselPosition vesselPosition = VesselPosition.getByName(cellname);
                // Barcode decreases for increasing position.
                String barcode = "A" + (100000 - incrementer);
                BarcodedTube tube = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube);
                // Bounces the concentration around for successive positions.
                tube.setConcentration(new BigDecimal((col % 2 == 1) ? incrementer : 96 - incrementer));
                mapPositionToTube.put(vesselPosition, tube);
            }
        }
        RackOfTubes rackOfTubes = new RackOfTubes("rackBarcode", RackOfTubes.RackType.Matrix96);
        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        tubeFormation.getContainerRole().setEmbedder(rackOfTubes);

        pdab.setTubeFormation(tubeFormation);

        // Creates the list.
        pdab.displayList();
        Assert.assertTrue(CollectionUtils.isNotEmpty(pdab.getListItems()));

        // Checks that all dispositions are consistent with concentration.
        for (PicoDispositionActionBean.ListItem listItem : pdab.getListItems()) {
            Assert.assertTrue(StringUtils.isNotBlank(listItem.getPosition()));
            Assert.assertTrue(StringUtils.isNotBlank(listItem.getBarcode()));
            Assert.assertNotNull(listItem.getConcentration());
            Assert.assertNotNull(listItem.getDisposition());

            if (listItem.getConcentration().compareTo(LabVessel.FINGERPRINT_CONCENTRATION_LOW_THRESHOLD) < 0) {
                Assert.assertEquals(listItem.getDisposition(), PicoDispositionActionBean.NextStep.EXCLUDE);
            } else {
                if (listItem.getConcentration().compareTo(LabVessel.FINGERPRINT_CONCENTRATION_HIGH_THRESHOLD) > 0) {
                    Assert.assertEquals(listItem.getDisposition(), PicoDispositionActionBean.NextStep.FP_DAUGHTER);
                } else {
                    Assert.assertEquals(listItem.getDisposition(), PicoDispositionActionBean.NextStep.SHEARING_DAUGHTER);
                }
            }
        }

        // Checks sorting by disposition and position.
        for (int i = 0; i < 95; ++i) {
            int compare1 = Integer.compare(pdab.getListItems().get(i).getDisposition().getRangeCompare(),
                    pdab.getListItems().get(i+1).getDisposition().getRangeCompare());
            int compare2 = pdab.getListItems().get(i).getPosition().compareTo(
                    pdab.getListItems().get(i+1).getPosition());
            Assert.assertTrue("at " + i, compare1 < 0 || compare1 == 0 && compare2 < 0);
        }
    }
}
