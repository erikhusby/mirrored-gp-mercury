package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.AnyOf;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.DENATURE_TO_FLOWCELL_TRANSFER;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER;
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

    private SequencingTemplateFactory factory = null;
    private TwoDBarcodedTube denatureTube = null;
    private IlluminaFlowcell flowcell = null;
    private MiSeqReagentKit reagentKit = null;
    @BeforeTest
    public void setUp() {
        factory = new SequencingTemplateFactory();
        denatureTube = new TwoDBarcodedTube("denature_tube_barcode");
        denatureTube.addSample(new MercurySample("SM-1"));

        flowcell = new IlluminaFlowcell(HiSeq2500Flowcell, "flowcell_barcode");
        LabEvent denatureToFlowcellEvent = new LabEvent(DENATURE_TO_FLOWCELL_TRANSFER, new Date(),
                "SequencingTemplateFactoryTest#testGetSequencingTemplate", 1L, 1L);
        denatureToFlowcellEvent.getVesselToSectionTransfers()
                .add(new VesselToSectionTransfer(denatureTube, ALL2, flowcell.getContainerRole(), denatureToFlowcellEvent));

        reagentKit = new MiSeqReagentKit("reagent_kit_barcode");
        LabEvent denatureToReagentKitEvent = new LabEvent(DENATURE_TO_REAGENT_KIT_TRANSFER, new Date(),
                "ZLAB", 1L, 1L);
        final  VesselToSectionTransfer sectionTransfer =
                new VesselToSectionTransfer(denatureTube, SBSSection.getBySectionName(MiSeqReagentKit.LOADING_WELL.name()),
                        reagentKit.getContainerRole(),denatureToReagentKitEvent);
        denatureToReagentKitEvent.getVesselToSectionTransfers().add(sectionTransfer);
    }

    public void testGetSequencingTemplateFromReagentKit(){
        SequencingTemplateType template = factory.getSequencingTemplate(reagentKit);
        assertThat(template.getBarcode(), Matchers.nullValue());
        assertThat(template.getLanes().size(), is(1));
        assertThat(template.getLanes().get(0).getLaneName(), is("D04"));
        assertThat(template.getLanes().get(0).getLoadingVesselLabel(), is("reagent_kit_barcode"));
    }

    public void testGetSequencingTemplate() {
        Set<VesselAndPosition> vesselsAndPositions = factory.getLoadingVessels(flowcell);
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

    public void testGetLoadingVesselsForFlowcell() {
        Set<VesselAndPosition> vesselsAndPositions = factory.getLoadingVessels(flowcell);
        MatcherAssert.assertThat(vesselsAndPositions, not(Matchers.empty()));
        final List<VesselPosition> vesselPositions = Arrays.asList(VesselPosition.LANE1, VesselPosition.LANE2);

        for (VesselAndPosition vesselsAndPosition : vesselsAndPositions) {
            assertThat(vesselsAndPosition.getPosition(),
                                AnyOf.anyOf(equalTo(VesselPosition.LANE1), equalTo(VesselPosition.LANE2)));
            assertThat(denatureTube, equalTo(vesselsAndPosition.getVessel()));
        }

    }
}
