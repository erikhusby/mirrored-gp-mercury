package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.DENATURE_TO_FLOWCELL_TRANSFER;
import static org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection.ALL2;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

/**
 * Database-free test for SequencingTemplateFactory.
 */
@Test(groups = DATABASE_FREE)
public class SequencingTemplateFactoryTest {

    private SequencingTemplateFactory factory = new SequencingTemplateFactory();

    public void testGetSequencingTemplate() {
        TwoDBarcodedTube denatureTube = new TwoDBarcodedTube("denature_tube_barcode");
        denatureTube.addSample(new MercurySample("SM-1"));
        IlluminaFlowcell flowcell = new IlluminaFlowcell(HiSeq2500Flowcell, "flowcell_barcode");
        LabEvent event = new LabEvent(DENATURE_TO_FLOWCELL_TRANSFER, new Date(),
                "SequencingTemplateFactoryTest#testGetSequencingTemplate", 1L, 1L);
        event.getVesselToSectionTransfers()
                .add(new VesselToSectionTransfer(denatureTube, ALL2, flowcell.getContainerRole(), event));

        Set<VesselAndPosition> vesselsAndPositions = factory.getLoadingVesselsForFlowcell(flowcell);
        MatcherAssert.assertThat(vesselsAndPositions, not(Matchers.empty()));
        SequencingTemplateType template = factory.getSequencingTemplate(flowcell, vesselsAndPositions);
        assertThat(template.getBarcode(), equalTo("flowcell_barcode"));
        assertThat(template.getLanes().size(), is(2));
        Set<String> allLanes = new HashSet<String>();
        for (SequencingTemplateLaneType lane : template.getLanes()) {
            allLanes.add(lane.getLaneName());
            assertThat(lane.getLoadingVesselLabel(), equalTo("denature_tube_barcode"));
        }
        assertThat(allLanes, hasItem("LANE1"));
        assertThat(allLanes, hasItem("LANE2"));
    }
}
