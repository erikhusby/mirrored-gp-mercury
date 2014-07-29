package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Set;

/**
 * This class is created (temporarily) for plates that don't have persistent wells.
 */
public class PlateWellTransient extends PlateWell {

    public PlateWellTransient(StaticPlate staticPlate,
            VesselPosition vesselPosition) {
        super(staticPlate, vesselPosition);
    }

    @Override
    public Date getCreatedOn() {
        return getPlate().getCreatedOn();
    }

    @Override
    public Set<SampleInstance> getSampleInstances(SampleType sampleType,
            @Nullable LabBatch.LabBatchType labBatchType) {
        return getPlate().getContainerRole().getSampleInstancesAtPosition(getVesselPosition(), sampleType,
                labBatchType);
    }
}
