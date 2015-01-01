package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.STANDARD)
public class LabEventResourceTest extends Arquillian {

    @Inject
    private LabEventResource labEventResource;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    private static final Set<SBSSection> SECTION_TRANSFERS_IN_PICO = EnumSet.of(
            SBSSection.P384_96TIP_1INTERVAL_A1, SBSSection.P384_96TIP_1INTERVAL_A2, SBSSection.P384_96TIP_1INTERVAL_B1);

    /**
     * This test uses historical data that should not change for a Pico batch without dilution.
     */
    @Test
    public void testTransfersToFirstAncestorRackBatchlessPicoNoDilutions() {
        final String BLACK_PLATE_BARCODE = "CO-7323348";
        final String MATRIX_RACK_BARCODE = "CO-7322820";

        final LabEventResponseBean responseBean =
                labEventResource.transfersToFirstAncestorRack(Collections.singletonList(BLACK_PLATE_BARCODE));

        Set<SBSSection> seenSections = EnumSet.copyOf(SECTION_TRANSFERS_IN_PICO);

        assertThat(responseBean, is(notNullValue()));
        // There are three section transfers from a 96 well matrix rack to the 384 well black plate
        // at target sections A1, A2, B1.
        assertThat(responseBean.getLabEventBeans(), hasSize(3));

        for (LabEventBean labEventBean : responseBean.getLabEventBeans()) {
            assertThat(labEventBean.getSources(), hasSize(1));
            assertThat(labEventBean.getTargets(), hasSize(1));
            assertThat(labEventBean.getTransfers(), hasSize(1));

            final String sourceBarcode = labEventBean.getSources().get(0).getBarcode();
            final String targetBarcode = labEventBean.getTargets().get(0).getBarcode();
            final TransferBean transferBean = labEventBean.getTransfers().get(0);

            assertThat(sourceBarcode, is(equalTo(MATRIX_RACK_BARCODE)));
            assertThat(targetBarcode, is(equalTo(BLACK_PLATE_BARCODE)));

            SBSSection sbsSection = SBSSection.valueOf(transferBean.getTargetSection());
            // This fails for reasons unknown:
            // assertThat(seenSections, contains(sbsSection));
            Assert.assertTrue(seenSections.contains(sbsSection));
            seenSections.remove(sbsSection);
        }

        assertThat(seenSections, is(empty()));
    }

    /**
     * This test uses historical data that should not change for a Pico batch with dilution.
     */
    @Test
    public void testTransfersToFirstAncestorRackBatchlessPicoWithDilutions() {
        final String BLACK_PLATE_BARCODE = "CO-7330324";
        final String DILUTION_PLATE_BARCODE = "CO-7330317";
        final String MATRIX_RACK_BARCODE = "CO-7317612";

        final LabEventResponseBean responseBean =
                labEventResource.transfersToFirstAncestorRack(Collections.singletonList(BLACK_PLATE_BARCODE));

        assertThat(responseBean, is(notNullValue()));

        // There should be one ALL_384 transfer from the dilution plate to the black plate plus three
        // section transfers from the 96 well Matrix rack to the dilution plate.
        assertThat(responseBean.getLabEventBeans(), hasSize(4));

        LabEventBean blackPlateTargetEvent = null;
        Set<SBSSection> seenSections = EnumSet.copyOf(SECTION_TRANSFERS_IN_PICO);

        // There are three section transfers from a 96 well matrix rack to the 384 well black plate
        // at target sections A1, A2, B1.
        for (LabEventBean labEventBean : responseBean.getLabEventBeans()) {
            assertThat(labEventBean.getSources(), hasSize(1));
            assertThat(labEventBean.getTargets(), hasSize(1));
            assertThat(labEventBean.getTransfers(), hasSize(1));

            final String sourceBarcode = labEventBean.getSources().get(0).getBarcode();
            final String targetBarcode = labEventBean.getTargets().get(0).getBarcode();
            final TransferBean transferBean = labEventBean.getTransfers().get(0);

            switch (targetBarcode) {
            case BLACK_PLATE_BARCODE:
                // Assert this event is seen only once in the response.
                assertThat(blackPlateTargetEvent, is(nullValue()));
                assertThat(sourceBarcode, is(equalTo(DILUTION_PLATE_BARCODE)));
                blackPlateTargetEvent = labEventBean;
                break;

            case DILUTION_PLATE_BARCODE:
                assertThat(sourceBarcode, is(equalTo(MATRIX_RACK_BARCODE)));
                // Assert the observed section transfer was among the expected Set of A1, A2, B1.
                SBSSection sbsSection = SBSSection.valueOf(transferBean.getTargetSection());
                // This fails for reasons unknown:
                // assertThat(seenSections, contains(sbsSection));
                Assert.assertTrue(seenSections.contains(sbsSection));
                seenSections.remove(sbsSection);
                break;

            default:
                Assert.fail("Unexpected barcode seen: " + targetBarcode);
            }
        }

        assertThat(seenSections, is(empty()));
    }
}
