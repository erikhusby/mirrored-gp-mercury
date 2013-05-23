package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.DENATURE_TO_FLOWCELL_TRANSFER;
import static org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.ALL2;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Database-free test for SequencingTemplateFactory.
 */
@Test(groups = DATABASE_FREE)
public class SequencingTemplateFactoryTest {

    private SequencingTemplateFactory factory = new SequencingTemplateFactory();

    public void testGetSequencingTemplate() {
        TwoDBarcodedTube denatureTube = new TwoDBarcodedTube("denature_tube_barcode");
        IlluminaFlowcell flowcell = new IlluminaFlowcell(HiSeq2500Flowcell, "flowcell_barcode");
        VesselToSectionTransfer denatureToFlowcellTransfer =
                new VesselToSectionTransfer(denatureTube, ALL2, flowcell.getContainerRole(),
                        new LabEvent(DENATURE_TO_FLOWCELL_TRANSFER, new Date(),
                                "SequencingTemplateFactoryTest#testGetSequencingTemplate", 1L, 1L));

        Map<VesselPosition, LabVessel> labVessels = new HashMap<VesselPosition, LabVessel>();
        for (VesselPosition vesselPosition : ALL2.getWells()) {
            labVessels.put(vesselPosition, denatureTube);
        }

        SequencingTemplateType template = factory.getSequencingTemplate(flowcell, labVessels);
        assertThat(template.getBarcode(), equalTo("flowcell_barcode"));
        assertThat(template.getLanes().size(), is(2));
        Set<String> allLanes = new HashSet<String>();
        for (SequencingTemplateLaneType lane : template.getLanes()) {
            assertThat(lane.getLoadingVesselLabel(), equalTo("denature_tube_barcode"));
            allLanes.add(lane.getLaneName());
        }
        assertThat(allLanes, hasItem("LANE1"));
        assertThat(allLanes, hasItem("LANE2"));
    }
}
