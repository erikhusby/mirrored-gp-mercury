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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Tests the PicoDispositionActionBean
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class PicoDispositionActionBeanTest extends TestCase {
    Logger logger = Logger.getLogger(this.getClass().getName());
    PicoDispositionActionBean pdab = new PicoDispositionActionBean();

    @BeforeMethod
    public void setup() {
        pdab.getListItems().clear();
        pdab.setTubeFormationLabel(null);
        pdab.setRackBarcode(null);
        pdab.setReverseNextStepOrder(false);
        pdab.setReversePositionOrder(false);
        pdab.setReverseConcentrationOrder(false);
        pdab.setSortOnNextStep(false);
        pdab.setSortOnPosition(false);
        pdab.setSortOnConcentration(false);
    }

    @Test
    public void testDisplayList() throws Exception {
        // Makes list of post-pico dispositions from the tube formation label.
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        for (int i = 0; i < 8; ++i) {
            for (int col = 1; col <= 12; ++col) {
                String cellname = "ABCDEFGH".substring(i, i + 1) + col;
                VesselPosition vesselPosition = VesselPosition.getByName(cellname);
                String barcode = "At" + vesselPosition.name();
                BarcodedTube tube = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube);
                tube.setConcentration(new BigDecimal((i + 1) * col));
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
        for (PicoDispositionActionBean.ListItem listItem : pdab.getListItems()) {
            Assert.assertTrue(StringUtils.isNotBlank(listItem.getPosition()));
            Assert.assertTrue(StringUtils.isNotBlank(listItem.getBarcode()));
            Assert.assertNotNull(listItem.getConcentration());
            Assert.assertNotNull(listItem.getDisposition());

            if (listItem.getBarcode().equals("AtA01") ||
                listItem.getConcentration().compareTo(LabVessel.FINGERPRINT_CONCENTRATION_LOW_THRESHOLD) < 0) {
                Assert.assertEquals(listItem.getDisposition(), PicoDispositionActionBean.NextStep.EXCLUDE);
            } else {
                if (listItem.getBarcode().equals("AtH12") ||
                    listItem.getConcentration().compareTo(LabVessel.FINGERPRINT_CONCENTRATION_HIGH_THRESHOLD) > 0) {
                    Assert.assertEquals(listItem.getDisposition(), PicoDispositionActionBean.NextStep.FP_DAUGHTER);
                } else {
                    Assert.assertEquals(listItem.getDisposition(), PicoDispositionActionBean.NextStep.SHEARING_DAUGHTER);
                }
            }
        }
    }

    @Test
    public void testScanRack() throws Exception {
        // Makes list of post-pico dispositions from the rack barcode.
    }
}
