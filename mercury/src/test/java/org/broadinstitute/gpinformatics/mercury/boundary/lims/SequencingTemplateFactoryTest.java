package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.IndexPositionType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.IndexingSchemeType;
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
import static org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme.IndexPosition.ILLUMINA_P5;
import static org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme.IndexPosition.ILLUMINA_P7;
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
        denatureTube.addSample(new MercurySample("SM-1"));
        Map<MolecularIndexingScheme.IndexPosition, MolecularIndex> indexPositions = new HashMap<MolecularIndexingScheme.IndexPosition, MolecularIndex>();
        indexPositions.put(ILLUMINA_P7, new MolecularIndex("AAA"));
        indexPositions.put(ILLUMINA_P5, new MolecularIndex("CCC"));
        MolecularIndexingScheme indexingScheme = new MolecularIndexingScheme(indexPositions);
        denatureTube.addReagent(new MolecularIndexReagent(indexingScheme));
        IlluminaFlowcell flowcell = new IlluminaFlowcell(HiSeq2500Flowcell, "flowcell_barcode");
        LabEvent event = new LabEvent(DENATURE_TO_FLOWCELL_TRANSFER, new Date(),
                "SequencingTemplateFactoryTest#testGetSequencingTemplate", 1L, 1L);
        event.getVesselToSectionTransfers()
                .add(new VesselToSectionTransfer(denatureTube, ALL2, flowcell.getContainerRole(), event));

        Set<VesselAndPosition> vesselsAndPositions = new HashSet<VesselAndPosition>();
        for (VesselPosition vesselPosition : ALL2.getWells()) {
            vesselsAndPositions.add(new VesselAndPosition(denatureTube, vesselPosition));
        }

        SequencingTemplateType template = factory.getSequencingTemplate(flowcell, vesselsAndPositions);
        assertThat(template.getBarcode(), equalTo("flowcell_barcode"));
        assertThat(template.getLanes().size(), is(2));
        Set<String> allLanes = new HashSet<String>();
        for (SequencingTemplateLaneType lane : template.getLanes()) {
            allLanes.add(lane.getLaneName());
            assertThat(lane.getLoadingVesselLabel(), equalTo("denature_tube_barcode"));
            assertThat(lane.getIndexingScheme().size(), is(2));
            IndexingSchemeType index1 = new IndexingSchemeType();
            index1.setPosition(IndexPositionType.P_7);
            index1.setSequence("AAA");
            IndexingSchemeType index2 = new IndexingSchemeType();
            index2.setPosition(IndexPositionType.P_5);
            index2.setSequence("CCC");
            // TODO: different asserts; IndexingSchemeType doesn't override equals()
            assertThat(lane.getIndexingScheme(), hasItem(index1));
            assertThat(lane.getIndexingScheme(), hasItem(index2));
        }
        assertThat(allLanes, hasItem("LANE1"));
        assertThat(allLanes, hasItem("LANE2"));
    }
}
