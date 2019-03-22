package org.broadinstitute.gpinformatics.mercury.boundary.queue.validation;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Used for validating enqueue / dequeue for pico
 */
@Stateful
@RequestScoped
public class PicoQueueValidator implements AbstractQueueValidator {

    /**
     * Verify is DNA
     */
    @Override
    public Map<Long, ValidationResult> validatePreEnqueue(Collection<LabVessel> labVessels, MessageCollection messageCollection) {
        Map<Long, String> bspSampleIdsByVesselId = new HashMap<>();

        Map<Long, ValidationResult> validationResultsById = new HashMap<>(labVessels.size());
        for (LabVessel labVessel : labVessels) {

            // Default to pass, change to fail or unknown if needed.
            validationResultsById.put(labVessel.getLabVesselId(), ValidationResult.PASS);

            // Checks to see if a sample is a BSP sample AND checks to see if mercury knows that it is DNA or not.
            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                MaterialType materialType = sampleInstanceV2.getMaterialType();
                if (materialType != null && !materialType.name().toLowerCase().contains("dna")) {
                    validationResultsById.put(labVessel.getLabVesselId(), ValidationResult.FAIL);
                } else if (labVessel.getMercurySamples() != null) {
                    for (MercurySample mercurySample : labVessel.getMercurySamples()) {
                        if (mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP) {
                            bspSampleIdsByVesselId.put(labVessel.getLabVesselId(), mercurySample.getSampleKey());
                        }
                    }
                } else {
                    // Can't determine whether it is DNA or not, therefore set to unknown
                    validationResultsById.put(labVessel.getLabVesselId(), ValidationResult.UNKNOWN);
                }
            }
        }

        // We determined it there are some BSP Samples, therefore we can use BSP to verify whether it is DNA or not.
        if (!bspSampleIdsByVesselId.isEmpty()) {
            BSPSampleDataFetcher sampleDataFetcher = ServiceAccessUtility.getBean(BSPSampleDataFetcher.class);

            Map<String, BspSampleData> sampleIdToData =
                    sampleDataFetcher.fetchSampleData(bspSampleIdsByVesselId.values(), BSPSampleSearchColumn.SAMPLE_ID,
                                                      BSPSampleSearchColumn.MATERIAL_TYPE);

            for (Map.Entry<Long, String> labVesselIdToBspSampleId : bspSampleIdsByVesselId.entrySet()) {
                if (!sampleIdToData.containsKey(labVesselIdToBspSampleId.getValue())) {
                    validationResultsById.put(labVesselIdToBspSampleId.getKey(), ValidationResult.UNKNOWN);
                } else {
                    String materialType = sampleIdToData.get(labVesselIdToBspSampleId.getValue()).getMaterialType();
                    if (materialType == null || !materialType.toLowerCase().contains("dna")) {
                        validationResultsById.put(labVesselIdToBspSampleId.getKey(), ValidationResult.FAIL);
                    }
                }
            }
        }

        return validationResultsById;
    }

    /**
     * NOTE:  For Pico (and possibly only pico) the logic for determining completion  will be located at wherever
     * the dequeue code is called from.
     */
    @Override
    public boolean isComplete(LabVessel labVessel, MessageCollection messageCollection) {
        return true;
    }
}
