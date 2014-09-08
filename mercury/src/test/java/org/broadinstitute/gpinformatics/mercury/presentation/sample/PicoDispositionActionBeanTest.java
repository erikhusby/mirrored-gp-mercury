package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests the PicoDispositionActionBean
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class PicoDispositionActionBeanTest extends TestCase {
    private PicoDispositionActionBean picoDispositionActionBean;
    private final long now = System.currentTimeMillis();
    // 3 timestamps having increasing datetime.
    private final Date[] timeSteps = new Date[]{new Date(now - 30000), new Date(now - 20000), new Date(now - 10000)};
    private final int NUMBER_TUBES = 96;
    private final int NUMBER_COLUMNS = 12;
    private final String FIRST_CELLNAME = "A01";
    private final BigDecimal BD_70 = new BigDecimal(70.0);

    // Makes a rack of tubes with initial pico quant metrics.
    private void setUpQuants(int numberTubes) {
        boolean needAtRiskMetric = true;
            Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        for (int incrementer = 0; incrementer < numberTubes; ++incrementer) {
            String barcode = "A" + (100000 - incrementer);
            BarcodedTube tube = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube);

            int row = incrementer / NUMBER_COLUMNS;
            int col = (incrementer % NUMBER_COLUMNS) + 1;
            String cellname = "ABCDEFGH".substring(row, row + 1) + col;
            VesselPosition vesselPosition = VesselPosition.getByName(cellname);
            mapPositionToTube.put(vesselPosition, tube);

            // Concentration bounces around for successive positions (i.e. 96, 1, 94, 3, 92, 5, ...)
            // so code can check sorting.
            BigDecimal concentration = new BigDecimal(incrementer % 2 == 1 ? incrementer : NUMBER_TUBES - incrementer);
            // Puts the quant metric on each tube.
            LabMetric labMetric = new LabMetric(concentration, LabMetric.MetricType.INITIAL_PICO,
                    LabMetric.LabUnit.NG_PER_UL, cellname, timeSteps[0]);
            tube.addMetric(labMetric);
            // Adds the lab metric decision which is used for the Risk Override indication.
            // This decision does NOT determine the next steps -- that is a separate categorization
            // done by the Action Bean.
            boolean isBelowRange = concentration.compareTo(LabMetric.INITIAL_PICO_LOW_THRESHOLD) < 0;
            if (isBelowRange) {
                if (needAtRiskMetric) {
                    needAtRiskMetric = false;
                    labMetric.setLabMetricDecision(new LabMetricDecision(LabMetricDecision.Decision.RISK, timeSteps[1],
                            BSPManagerFactoryStub.QA_DUDE_USER_ID, labMetric));
                } else {
                    labMetric.setLabMetricDecision(new LabMetricDecision(LabMetricDecision.Decision.FAIL, timeSteps[1],
                            BSPManagerFactoryStub.QA_DUDE_USER_ID, labMetric));
                }
            } else {
                labMetric.setLabMetricDecision(new LabMetricDecision(LabMetricDecision.Decision.PASS, timeSteps[1],
                        BSPManagerFactoryStub.QA_DUDE_USER_ID, labMetric));
            }
        }

        // Gives the tube formation to the action bean instead of having the action bean look it up.
        RackOfTubes rackOfTubes = new RackOfTubes("rackBarcode", RackOfTubes.RackType.Matrix96);
        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        tubeFormation.getContainerRole().setEmbedder(rackOfTubes);

        picoDispositionActionBean = new PicoDispositionActionBean();
        picoDispositionActionBean.setTubeFormation(tubeFormation);
    }

    @Test
    public void testDisplayList() throws Exception {
        // Creates the list of sample dispositions.
        setUpQuants(NUMBER_TUBES);
        picoDispositionActionBean.displayList();
        Assert.assertEquals(NUMBER_TUBES, picoDispositionActionBean.getListItems().size());

        // Checks each sample for consistent disposition and concentration.
        boolean found[] = new boolean[] {false, false, false, false};
        for (PicoDispositionActionBean.ListItem listItem : picoDispositionActionBean.getListItems()) {
            Assert.assertTrue(StringUtils.isNotBlank(listItem.getPosition()));
            Assert.assertTrue(StringUtils.isNotBlank(listItem.getBarcode()));
            Assert.assertNotNull(listItem.getConcentration());
            Assert.assertNotNull(listItem.getDisposition());

            if (listItem.getConcentration().compareTo(LabMetric.INITIAL_PICO_HIGH_THRESHOLD) > 0) {
                found[0] = true;
                Assert.assertEquals(listItem.getDisposition(), PicoDispositionActionBean.NextStep.FP_DAUGHTER);
            } else if (listItem.getConcentration().compareTo(LabMetric.INITIAL_PICO_LOW_THRESHOLD) > 0) {
                    found[1] = true;
                    Assert.assertEquals(listItem.getDisposition(),
                            PicoDispositionActionBean.NextStep.SHEARING_DAUGHTER);
            } else {
                if (listItem.hasRiskOverride()) {
                    found[2] = true;
                    Assert.assertEquals(listItem.getDisposition(),
                            PicoDispositionActionBean.NextStep.SHEARING_DAUGHTER_AT_RISK);
                } else {
                    found[3] = true;
                    Assert.assertEquals(listItem.getDisposition(), PicoDispositionActionBean.NextStep.EXCLUDE);
                }
            }
        }
        // Checks that all test cases are found.
        for (int i = 0; i < found.length; ++i) {
            Assert.assertTrue("at " + i, found[i]);
        }

        // Checks sorting by disposition and position.
        for (int i = 0; i < NUMBER_TUBES - 1; ++i) {
            PicoDispositionActionBean.ListItem item = picoDispositionActionBean.getListItems().get(i);
            PicoDispositionActionBean.ListItem nextItem = picoDispositionActionBean.getListItems().get(i + 1);

            int dispositionOrder =
                    Integer.compare(item.getDisposition().getSortOrder(), nextItem.getDisposition().getSortOrder());
            int positionOrder = item.getPosition().compareTo(nextItem.getPosition());
            Assert.assertTrue("at " + i, dispositionOrder < 0 || dispositionOrder == 0 && positionOrder < 0);
        }
    }

    @Test
    public void testMissingQuant() {
        // Removes quants if value is over a fairly arbitrary threshold, just to see the get no disposition.
        int numberAboveThreshold = 0;
        setUpQuants(NUMBER_TUBES);
        for (BarcodedTube barcodedTube : picoDispositionActionBean.getTubeFormation().getContainerRole().
                getMapPositionToVessel().values()) {
            for (LabMetric labMetric : barcodedTube.getMetrics()) {
                if (labMetric.getValue().compareTo(BD_70) > 0) {
                    barcodedTube.getMetrics().clear();
                    ++numberAboveThreshold;
                    break;
                }
            }
        }
        Assert.assertTrue(numberAboveThreshold > 0);

        // Should have a full list of samples but not all have dispositions.
        picoDispositionActionBean.displayList();
        Assert.assertEquals(NUMBER_TUBES, picoDispositionActionBean.getListItems().size());
        int numberWithoutDisposition = 0;
        for (PicoDispositionActionBean.ListItem listItem : picoDispositionActionBean.getListItems()) {
            if (listItem.getDisposition() == null) {
                Assert.assertNull(listItem.getConcentration());
                ++numberWithoutDisposition;
            }
        }
        Assert.assertEquals(numberWithoutDisposition, numberAboveThreshold);
    }

    @Test
    public void testWrongMetric() {
        // Overwrites the existing quant with one that should not be used for pico dispositions.
        setUpQuants(1);
        BarcodedTube barcodedTube = picoDispositionActionBean.getTubeFormation().getContainerRole().
                getMapPositionToVessel().values().iterator().next();

        barcodedTube.getMetrics().clear();
        barcodedTube.addMetric(new LabMetric(new BigDecimal(11.1), LabMetric.MetricType.CATCH_PICO,
                LabMetric.LabUnit.NG_PER_UL, FIRST_CELLNAME, timeSteps[0]));

        // Should have 1 sample in list but having no next step.
        picoDispositionActionBean.displayList();
        Assert.assertEquals(1, picoDispositionActionBean.getListItems().size());
        Assert.assertNull(picoDispositionActionBean.getListItems().get(0).getConcentration());
        Assert.assertNull(picoDispositionActionBean.getListItems().get(0).getDisposition());
    }

    @Test
    public void testGetsLatestMetric() {
        setUpQuants(1);
        BarcodedTube barcodedTube = picoDispositionActionBean.getTubeFormation().getContainerRole().
                getMapPositionToVessel().entrySet().iterator().next().getValue();

        // Adds conflicting metrics that should be resolved by taking only the latest one.
        // Earliest metric has a high concentration and PASS decision.
        // Then a later metric with low concentration and RISK decision.
        // Then the latest metric with low concentration and FAIL decision.
        barcodedTube.getMetrics().clear();

        LabMetric labMetric0 = new LabMetric(new BigDecimal(94.0), LabMetric.MetricType.INITIAL_PICO,
                LabMetric.LabUnit.NG_PER_UL, FIRST_CELLNAME, timeSteps[0]);
        labMetric0.setLabMetricDecision(new LabMetricDecision(LabMetricDecision.Decision.PASS, timeSteps[0],
                BSPManagerFactoryStub.QA_DUDE_USER_ID, labMetric0));
        barcodedTube.addMetric(labMetric0);

        LabMetric labMetric1 = new LabMetric(new BigDecimal(1.1), LabMetric.MetricType.INITIAL_PICO,
                LabMetric.LabUnit.NG_PER_UL, FIRST_CELLNAME, timeSteps[1]);
        labMetric1.setLabMetricDecision(new LabMetricDecision(LabMetricDecision.Decision.RISK, timeSteps[1],
                BSPManagerFactoryStub.QA_DUDE_USER_ID, labMetric1));
        barcodedTube.addMetric(labMetric1);

        LabMetric labMetric2 = new LabMetric(new BigDecimal(1.1), LabMetric.MetricType.INITIAL_PICO,
                LabMetric.LabUnit.NG_PER_UL, FIRST_CELLNAME, timeSteps[2]);
        labMetric2.setLabMetricDecision(new LabMetricDecision(LabMetricDecision.Decision.FAIL, timeSteps[2],
                BSPManagerFactoryStub.QA_DUDE_USER_ID, labMetric2));
        barcodedTube.addMetric(labMetric2);

        // Creates the list of sample dispositions.
        picoDispositionActionBean.displayList();
        Assert.assertEquals(1, picoDispositionActionBean.getListItems().size());
        Assert.assertEquals(PicoDispositionActionBean.NextStep.EXCLUDE,
                picoDispositionActionBean.getListItems().get(0).getDisposition());
    }

    @Test
    public void testGetsNonNullDateMetric() {
        setUpQuants(1);
        BarcodedTube barcodedTube = picoDispositionActionBean.getTubeFormation().getContainerRole().
                getMapPositionToVessel().entrySet().iterator().next().getValue();

        // Adds a conflicting metric that has a null date, and so should be ignored.
        barcodedTube.getMetrics().clear();

        LabMetric labMetric0 = new LabMetric(new BigDecimal(94.0), LabMetric.MetricType.INITIAL_PICO,
                LabMetric.LabUnit.NG_PER_UL, FIRST_CELLNAME, timeSteps[0]);
        labMetric0.setLabMetricDecision(new LabMetricDecision(LabMetricDecision.Decision.PASS, timeSteps[0],
                BSPManagerFactoryStub.QA_DUDE_USER_ID, labMetric0));
        barcodedTube.addMetric(labMetric0);

        LabMetric labMetric1 = new LabMetric(new BigDecimal(1.1), LabMetric.MetricType.INITIAL_PICO,
                LabMetric.LabUnit.NG_PER_UL, FIRST_CELLNAME, null);
        labMetric1.setLabMetricDecision(new LabMetricDecision(LabMetricDecision.Decision.FAIL, null,
                BSPManagerFactoryStub.QA_DUDE_USER_ID, labMetric1));
        barcodedTube.addMetric(labMetric1);

        // Creates the list of sample dispositions.
        picoDispositionActionBean.displayList();
        Assert.assertEquals(1, picoDispositionActionBean.getListItems().size());
        Assert.assertEquals(PicoDispositionActionBean.NextStep.FP_DAUGHTER,
                picoDispositionActionBean.getListItems().get(0).getDisposition());
    }
}
