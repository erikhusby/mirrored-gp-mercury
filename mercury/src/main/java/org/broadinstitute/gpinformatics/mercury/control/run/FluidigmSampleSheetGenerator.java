package org.broadinstitute.gpinformatics.mercury.control.run;

import org.apache.commons.lang3.ObjectUtils;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Generates sample sheet used by Fluidigm software to convert bml to metrics csv.
 */
public class FluidigmSampleSheetGenerator {

    public static final String NO_TEMPLATE_CONTROL = "NTC";

    /**
     * Writes a Fluidigm Sample Sheet.
     */
    public void writeSheet(StaticPlate fluidigmChip, PrintWriter printWriter) {
        printWriter.println("File Format,BioMark Sample Format V1.0");
        printWriter.println("Sample Plate Name," + ObjectUtils.defaultIfNull(fluidigmChip.getName(),
                fluidigmChip.getLabel()));
        printWriter.println("Barcode ID," + fluidigmChip.getLabel());
        printWriter.println("Description,");
        printWriter.println("Plate Type,SBS96");
        printWriter.println();

        if (fluidigmChip.getTransfersTo().size() != 1) {
            throw new RuntimeException("Expected one transfer to " + fluidigmChip.getLabel() + ", found " +
                    fluidigmChip.getTransfersTo().size());
        }
        LabEvent labEvent = fluidigmChip.getTransfersTo().iterator().next();
        SBSSection sourceSection = labEvent.getSectionTransfers().iterator().next().getSourceSection();

        printWriter.println("Well Location,Sample Name,Sample Concentration,Sample Type");
        VesselPosition[] vesselPositions = fluidigmChip.getVesselGeometry().getVesselPositions();
        for (int i = 0; i < vesselPositions.length; i++) {
            VesselPosition vesselPosition = vesselPositions[i];
            LabVessel vesselAtPosition = fluidigmChip.getContainerRole().getImmutableVesselAtPosition(vesselPosition);
            // Empty positions are "no template controls"
            boolean ntc = false;
            SampleInstanceV2 sampleInstance = null;
            BigDecimal value = null;
            if (vesselAtPosition == null) {
                ntc = true;
            } else {
                Set<SampleInstanceV2> sampleInstances = vesselAtPosition.getSampleInstancesV2();
                if (sampleInstances.isEmpty()) {
                    ntc = true;
                } else {
                    if (sampleInstances.size() != 1) {
                        throw new RuntimeException("Expected one sample in " + fluidigmChip.getLabel() + " " +
                                vesselPosition.name() + ", found " + sampleInstances.size());
                    }
                    sampleInstance = sampleInstances.iterator().next();
                    List<LabMetric> metrics = vesselAtPosition.getNearestMetricsOfType(LabMetric.MetricType.INITIAL_PICO);
                    if (!metrics.isEmpty()) {
                        LabMetric labMetric = metrics.get(metrics.size() - 1);
                        value = labMetric.getValue();
                    }
                }
            }

            printWriter.print(sourceSection.getWells().get(i) + ",");
            printWriter.print((ntc ? NO_TEMPLATE_CONTROL : sampleInstance.getRootOrEarliestMercurySampleName()) + ",");
            printWriter.print(ObjectUtils.defaultIfNull(value, "") + ",");
            printWriter.println(ntc ? NO_TEMPLATE_CONTROL : "Unknown");
        }
    }
}
