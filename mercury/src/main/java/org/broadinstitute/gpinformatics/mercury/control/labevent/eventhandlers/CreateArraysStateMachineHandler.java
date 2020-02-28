package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.ControlEjb;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Dependent
public class CreateArraysStateMachineHandler extends AbstractEventHandler {

    @Inject
    private FiniteStateMachineFactory finiteStateMachineFactory;

    @Inject
    private ControlEjb controlEjb;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Override
    public void handleEvent(LabEvent targetEvent, StationEventType stationEvent) {
        LabVessel vessel = targetEvent.getInPlaceLabVessel();
        for (SampleInstanceV2 sampleInstanceV2 : vessel.getSampleInstancesV2()) {
            if (sampleInstanceV2.getRootOrEarliestMercurySample().getMetadataSource() ==
                MercurySample.MetadataSource.BSP) {
                return;
            }
        }

        StaticPlate chip = OrmUtil.proxySafeCast(vessel, StaticPlate.class);
        boolean atLeastOne = false;
        Map<VesselPosition, SampleInstanceV2> mapPositionToSample = new HashMap<>();
        for (VesselPosition vesselPosition: chip.getVesselGeometry().getVesselPositions()) {
            Set<SampleInstanceV2> sampleInstancesAtPositionV2 =
                    vessel.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition);
            if (sampleInstancesAtPositionV2 != null && sampleInstancesAtPositionV2.size() == 1) {
                SampleInstanceV2 sampleInstanceV2 = sampleInstancesAtPositionV2.iterator().next();
                ProductOrderSample productOrderSample = sampleInstanceV2.getProductOrderSampleForSingleBucket();
                if (productOrderSample != null) {
                    String productType = productOrderSample.getMercurySample()
                            .getMetadataValueForKey(Metadata.Key.PRODUCT_TYPE);
                    if (productType != null && productType.equals(MayoManifestEjb.AOU_ARRAY)) {
                        atLeastOne = true;
                        finiteStateMachineFactory.findOrCreateArraysStateMachine(chip, vesselPosition);
                    }
                } else {
                    mapPositionToSample.put(vesselPosition, sampleInstanceV2);
                }
            }
        }

        // If at least one AoU sample then include the controls
        if (atLeastOne) {
            for (Map.Entry<VesselPosition, SampleInstanceV2> entry: mapPositionToSample.entrySet()) {
                SampleData sampleData = sampleDataFetcher.fetchSampleData(
                        entry.getValue().getNearestMercurySampleName());
                Control processControl = controlEjb.evaluateAsControl(sampleData);
                if (processControl != null) {
                    finiteStateMachineFactory.findOrCreateArraysStateMachine(chip, entry.getKey());
                }
            }
        }
    }

    // For Testing
    public void setFiniteStateMachineFactory(FiniteStateMachineFactory finiteStateMachineFactory) {
        this.finiteStateMachineFactory = finiteStateMachineFactory;
    }

    public FiniteStateMachineFactory getFiniteStateMachineFactory() {
        return finiteStateMachineFactory;
    }
}
