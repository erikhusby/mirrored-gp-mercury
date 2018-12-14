package org.broadinstitute.gpinformatics.mercury.boundary.queue.datadump;

import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;

public class PicoDataDumpGenerator extends AbstractDataDumpGenerator {

    @Override
    protected Object[] extractData(SampleData sampleData) {
        int index = 0;
        Object[] objects = new Object[getSearchColumns().length];
        objects[index++] = sampleData.getSampleId();
        objects[index++] = sampleData.getSampleStatus();
        objects[index++] = sampleData.getRootSample();
        objects[index++] = sampleData.getSampleKitId();
        objects[index++] = sampleData.getPatientId();
        objects[index++] = sampleData.getCollection();
        objects[index++] = sampleData.getOriginalMaterialType();
        objects[index++] = sampleData.getMaterialType();
        objects[index++] = sampleData.getVolume();
        objects[index++] = sampleData.getConcentration();
        objects[index++] = sampleData.getManufacturerBarcode();
        objects[index++] = sampleData.getContainerId();
        objects[index++] = sampleData.getPosition();
        objects[index++] = sampleData.getBspStorageLocation();
        objects[index++] = sampleData.containerName();
        objects[index++] = sampleData.getCollaboratorParticipantId();
        //noinspection UnusedAssignment
        objects[index++] = sampleData.getCollaboratorsSampleName();

        return objects;
    }

    @Override
    protected BSPSampleSearchColumn[] getSearchColumns() {
        return BSPSampleSearchColumn.PICO_QUEUE_DATA_DUMP;
    }
}
