package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.DENATURE_TO_FLOWCELL_TRANSFER;
import static org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.ALL2;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Database-free test for SequencingTemplateFactory.
 */
@Test(groups = DATABASE_FREE)
public class SequencingTemplateFactoryTest {

    private UnifiedLoader factory = new UnifiedLoader();

    public void testGetSequencingTemplate() {
        TwoDBarcodedTube denatureTube = new TwoDBarcodedTube("denature_tube_barcode");
        IlluminaFlowcell flowcell = new IlluminaFlowcell(HiSeq2500Flowcell, "flowcell_barcode");
        VesselToSectionTransfer denatureToFlowcellTransfer =
                new VesselToSectionTransfer(denatureTube, ALL2, flowcell.getContainerRole(),
                        new LabEvent(DENATURE_TO_FLOWCELL_TRANSFER, new Date(),
                                "SequencingTemplateFactoryTest#testGetSequencingTemplate", 1L, 1L));

        final List<VesselPosition> vesselPositions = SBSSection.ALL2.getWells();
        Map<VesselPosition, LabVessel> labVessels=new HashMap<VesselPosition, LabVessel>();
        for (VesselPosition vesselPosition : vesselPositions) {
            labVessels.put(vesselPosition,denatureTube);
        }

        SequencingTemplateType template = factory.getSequencingTemplate(flowcell, labVessels);
        assertThat(template.getBarcode(), equalTo("flowcell_barcode"));
        assertThat(template.getLanes().size(), is(2));
    }
}
