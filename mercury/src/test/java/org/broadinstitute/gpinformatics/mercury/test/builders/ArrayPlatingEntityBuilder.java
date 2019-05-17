package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds entity graphs for the Array Plating process.
 */
public class ArrayPlatingEntityBuilder {
    private final Map<String, BarcodedTube> mapBarcodeToTube;
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final String testPrefix;
    private StaticPlate arrayPlatingPlate = null;

    public ArrayPlatingEntityBuilder( Map<String, BarcodedTube> mapBarcodeToTube,
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory, LabEventFactory labEventFactory,
            LabEventHandler labEventHandler, String testPrefix) {
        this.mapBarcodeToTube = mapBarcodeToTube;
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.testPrefix = testPrefix;
    }

    public ArrayPlatingEntityBuilder invoke() {
        ArrayPlatingJaxbBuilder arrayPlatingJaxbBuilder = new ArrayPlatingJaxbBuilder(bettaLimsMessageTestFactory,
                new ArrayList<>(mapBarcodeToTube.keySet()), testPrefix);
        arrayPlatingJaxbBuilder.invoke();

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.putAll(mapBarcodeToTube);
        PlateTransferEventType arrayPlatingJaxb = arrayPlatingJaxbBuilder.getArrayPlatingJaxb();
        LabEvent arrayPlatingEvent = labEventFactory.buildFromBettaLims(
                arrayPlatingJaxb, mapBarcodeToVessel);
        labEventHandler.processEvent(arrayPlatingEvent);
        // asserts
        arrayPlatingPlate = (StaticPlate) arrayPlatingEvent.getTargetLabVessels().iterator().next();
        Assert.assertEquals(arrayPlatingPlate.getSampleInstancesV2().size(), mapBarcodeToTube.size(),
                "Wrong number of sample instances");

        return this;
    }

    public StaticPlate getArrayPlatingPlate() {
        if( arrayPlatingPlate == null ) {
            throw new IllegalStateException("Builder not invoked");
        }
        return arrayPlatingPlate;
    }

    /**
     * Infinium ArrayPlatingDilution is followed up by bucketing of all plate wells in InfiniumBucket events
     * @param pdo The PDO to assign to the bucket
     * @param arrayBatch The batch to assign to the bucket
     */
    public void bucketPlateWells( ProductOrder pdo, LabBatch arrayBatch ) {
        if( arrayPlatingPlate == null ) {
            throw new IllegalStateException("Builder not invoked");
        }

        int count = 0;

        for(VesselPosition position : arrayPlatingPlate.getVesselGeometry().getVesselPositions() ) {
            if( count < mapBarcodeToTube.size() ) {
                PlateWell well = new PlateWell(arrayPlatingPlate, position);

                arrayPlatingPlate.getContainerRole().getMapPositionToVessel().put(position, well);
                well.addToContainer(arrayPlatingPlate.getContainerRole());
                BucketEntry infiniumBucket = new BucketEntry(well, pdo,
                        new Bucket("TestBucket"), BucketEntry.BucketEntryType.PDO_ENTRY);
                infiniumBucket.setLabBatch(arrayBatch);
                well.addBucketEntry(infiniumBucket);

                LabEvent bucketEvent = new LabEvent(LabEventType.INFINIUM_BUCKET, new Date(),
                        "BSP", 1L, 1L, "BSP");
                bucketEvent.setInPlaceLabVessel(well);
                well.addInPlaceEvent(bucketEvent);
                count++;
            } else {
                break;
            }
        }

    }
}
