package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchResource;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates a LabBatch for a given message.  Initially, creates ARRAY ticket for ArrayPlatingDilution message that is
 * all Mercury samples (BSP creates the ARRAY tickets for samples it owns).
 */
@Dependent
public class CreateLabBatchHandler extends AbstractEventHandler {

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private BucketEjb bucketEjb;

    @Inject
    private BSPUserList bspUserList;

    @Override
    public void handleEvent(LabEvent targetEvent, StationEventType stationEvent) {
        Set<LabVessel> sourceLabVessels = targetEvent.getSourceLabVessels();
        Set<LabVessel> targetLabVessels = targetEvent.getTargetLabVessels();
        if (targetEvent.getSectionTransfers().size() != 1 || sourceLabVessels.size() != 1 ||
                targetLabVessels.size() != 1) {
            throw new RuntimeException("Currently supports single section transfer only.");
        }

        // Create LabBatch only for Mercury (not BSP) samples
        LabVessel sourceLabVessel = sourceLabVessels.iterator().next();
        for (SampleInstanceV2 sampleInstanceV2 : sourceLabVessel.getSampleInstancesV2()) {
            if (sampleInstanceV2.getRootOrEarliestMercurySample().getMetadataSource() ==
                    MercurySample.MetadataSource.BSP) {
                return;
            }
        }

        LabVessel targetLabVessel = targetLabVessels.iterator().next();
        if (targetLabVessel.getType() == LabVessel.ContainerType.STATIC_PLATE &&
                targetLabVessel.getContainerRole().getContainedVessels().isEmpty()) {
            // Create target PlateWells for filled positions in source rack, so there's something to attach a
            // BucketEntry to.
            StaticPlate staticPlate = OrmUtil.proxySafeCast(targetLabVessel, StaticPlate.class);
            SectionTransfer sectionTransfer = targetEvent.getSectionTransfers().iterator().next();
            List<VesselPosition> targetPositions = sectionTransfer.getTargetSection().getWells();
            List<VesselPosition> sourcePositions = sectionTransfer.getSourceSection().getWells();
            for (Map.Entry<VesselPosition, ?> vesselPositionEntry :
                    sourceLabVessel.getContainerRole().getMapPositionToVessel().entrySet()) {
                VesselPosition targetPosition = targetPositions.get(sourcePositions.indexOf(vesselPositionEntry.getKey()));
                staticPlate.getContainerRole().addContainedVessel(new PlateWell(staticPlate, targetPosition), targetPosition);
            }

        }

        String username = bspUserList.getById(targetEvent.getEventOperator()).getUsername();
        Set<LabVessel> labVesselSet = new HashSet<>(targetLabVessel.getContainerRole().getContainedVessels());
        LabBatch labBatch = new LabBatch("dummy" , labVesselSet, LabBatch.LabBatchType.WORKFLOW);
        Set<ProductOrder> productOrders = LabBatchResource.addToBatch(labVesselSet, labBatch,
                ProductFamily.WHOLE_GENOME_GENOTYPING, username, bucketEjb);
        String bucketDefinitionName = labBatch.getBucketEntries().iterator().next().getBucket().getBucketDefinitionName();
        Set<String> pdoKeys = productOrders.stream().map(ProductOrder::getBusinessKey).collect(Collectors.toSet());
        labBatchEjb.createJiraTicket(labBatch.getWorkflowName(), username, bucketDefinitionName, MessageReporter.UNUSED,
                Collections.emptyList(), pdoKeys, labBatch);
    }
}
