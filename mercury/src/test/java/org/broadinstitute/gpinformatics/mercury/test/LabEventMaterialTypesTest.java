/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.infrastructure.SampleDataTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVesselTest;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.DATABASE_FREE)
public class LabEventMaterialTypesTest extends BaseEventTest {
    public void testVesselWithDNAMercurySampleIsDNA() throws Exception {
        LabVessel sourceVessel = new BarcodedTube("A_SOURCE_VESSEL", BarcodedTube.BarcodedTubeType.MatrixTube075);
        MercurySample mercurySample = SampleDataTestFactory
                .getTestMercurySample(MaterialType.DNA, MercurySample.MetadataSource.MERCURY);
        mercurySample.addLabVessel(sourceVessel);

        assertThat(sourceVessel.isDNA(), is(true));
    }

    @DataProvider(name = "isDnaScenarios")
    public static Object[][] isDnaScenarios() {
        return new Object[][]{
                {MaterialType.FRESH_BLOOD, LabEventType.EXTRACT_BLOOD_SPIN_TO_MATRIX, true},
                {MaterialType.FRESH_BLOOD, LabEventType.EXTRACT_BLOOD_MICRO_TO_SPIN, false},
                {MaterialType.DNA, LabEventType.EXTRACT_BLOOD_SPIN_TO_MATRIX, true},
                {MaterialType.DNA, LabEventType.EXTRACT_BLOOD_MICRO_TO_SPIN, true}
        };
    }

    @Test(dataProvider = "isDnaScenarios")
    public void testExtractToDna(MaterialType sampleMaterialType, LabEventType labEventType, boolean isDna)
            throws Exception {
        BarcodedTube sourceVessel = new BarcodedTube("A_SOURCE_VESSEL", BarcodedTube.BarcodedTubeType.MatrixTube075);
        MercurySample mercurySample =
                SampleDataTestFactory.getTestMercurySample(sampleMaterialType, MercurySample.MetadataSource.MERCURY);
        mercurySample.addLabVessel(sourceVessel);

        BarcodedTube destinationVessel =
                new BarcodedTube("A_DESTINATION_VESSEL", BarcodedTube.BarcodedTubeType.MatrixTube075);
        MercurySample destSample =
                SampleDataTestFactory.getTestMercurySample(sampleMaterialType, MercurySample.MetadataSource.MERCURY);
        destSample.addLabVessel(destinationVessel);

        LabEvent labEvent = LabVesselTest.doVesselToVesselTransfer(sourceVessel, destinationVessel,
                sampleMaterialType, labEventType, MercurySample.MetadataSource.MERCURY, getLabEventFactory()
        );

        LabVessel targetVessel = labEvent.getTargetVesselTubes().iterator().next();
        assertThat(targetVessel.isDNA(), is(isDna));
    }

}
