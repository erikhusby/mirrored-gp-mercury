package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.storage.NotInStorageException;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class PickerVesselPlugin implements ListPlugin {

    private enum PickerColumn implements Displayable {
        STORAGE_LOCATION("Storage Location"),
        SOURCE_RACK_BARCODE("Source Rack Barcode"),
        SOURCE_WELL("Source Well"),
        TUBE_BARCODE("Tube Barcode"),
        DESTINATION_RACK_BARCODE("Destination Rack Barcode"),
        DESTINATION_WELL("Destination Well");

        private String displayName;
        private ConfigurableList.Header header;

        PickerColumn( String displayName ){
            this.displayName = displayName;
            this.header = new ConfigurableList.Header(displayName, displayName, "");
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        public ConfigurableList.Header getHeader(){
            return header;
        }

    }

    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup,
                                              @Nonnull SearchContext context) {
        List<LabVessel> labVesselList = (List<LabVessel>) entityList;

        for( PickerColumn valueColumnType : PickerColumn.values() ){
            headerGroup.addHeader(valueColumnType.getHeader());
        }

        List<ConfigurableList.Row> rows = new ArrayList<>();
        MessageCollection messageCollection = new MessageCollection();
        RackOfTubes.RackType rackType = RackOfTubes.RackType.Matrix96;
        int counter = 0;
        int rackCounter = 1;
        String destinationContainer = "DEST";
        List<String> missingLabVessels = new ArrayList<>();
        for (LabVessel labVessel: labVesselList) {
            Triple<RackOfTubes, VesselPosition, String> triple = labVessel.findStorageContainer();
            if (triple == null) {
                messageCollection.addError("Failed to find in storage: " + labVessel.getLabel());
                missingLabVessels.add(labVessel.getLabel());
                continue;
            }
            RackOfTubes rack = triple.getLeft();
            VesselPosition vesselPosition = triple.getMiddle();
            ConfigurableList.Row row = new ConfigurableList.Row(labVessel.getLabel());

            String value = triple.getRight();
            row.addCell(new ConfigurableList.Cell(PickerColumn.STORAGE_LOCATION.getHeader(), value, value));

            value = rack.getLabel();
            row.addCell(new ConfigurableList.Cell(PickerColumn.SOURCE_RACK_BARCODE.getHeader(), value, value));

            value = vesselPosition.name();
            row.addCell(new ConfigurableList.Cell(PickerColumn.SOURCE_WELL.getHeader(), value, value));

            value = labVessel.getLabel();
            row.addCell(new ConfigurableList.Cell(PickerColumn.TUBE_BARCODE.getHeader(), value, value));

            row.addCell(new ConfigurableList.Cell(PickerColumn.DESTINATION_RACK_BARCODE.getHeader(), destinationContainer, destinationContainer));

            VesselPosition destinationPosition = rackType.getVesselGeometry().getVesselPositions()[counter];
            value = destinationPosition.name();
            row.addCell(new ConfigurableList.Cell(PickerColumn.DESTINATION_WELL.getHeader(), value, value));

            rows.add(row);
            counter++;
            if (counter >= rackType.getVesselGeometry().getCapacity()) {
                counter = 0;
                rackCounter++;
                destinationContainer = "DEST" + rackCounter;
            }
        }

        if (messageCollection.hasErrors()) {
            throw new NotInStorageException("Failed to find some lab vessels", missingLabVessels);
        }
        return rows;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        return null;
    }
}
