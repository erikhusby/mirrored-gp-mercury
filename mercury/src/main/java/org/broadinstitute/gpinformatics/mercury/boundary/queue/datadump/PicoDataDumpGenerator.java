package org.broadinstitute.gpinformatics.mercury.boundary.queue.datadump;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

public class PicoDataDumpGenerator extends AbstractDataDumpGenerator {

    @Override
    protected Object[] generateData(LabVessel labVessel) {

        return null;
    }

    @Override
    protected String[] generateHeaderRow() {
        return new String[]{
                "Sample ID", "Sample Status", "Root Sample(s)", "Sample Kit", "Participant ID(s)", "Collection",
                "Original Material Type", "Material Type", "Volume", "Conc", "Manufacturer Tube Barcode", "Container",
                "Position", "Location", "Container Name", "Collaborator Participant ID", "Collaborator Sample ID"
        };
    }
}
