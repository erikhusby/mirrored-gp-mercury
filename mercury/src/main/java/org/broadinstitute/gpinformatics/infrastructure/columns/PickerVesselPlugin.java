package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.storage.NotInStorageException;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
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
        boolean isLoose = false;
        String destinationContainer = "DEST";
        List<String> missingLabVessels = new ArrayList<>();
        for (LabVessel labVessel: labVesselList) {
            Triple<RackOfTubes, VesselPosition, String> triple = findStorageContainer(labVessel);
            if (triple == null) {
                messageCollection.addError("Failed to find in storage: " + labVessel.getLabel());
                missingLabVessels.add(labVessel.getLabel());
                continue;
            }
            RackOfTubes rack = triple.getLeft();
            VesselPosition vesselPosition = triple.getMiddle();
            ConfigurableList.Row row = new ConfigurableList.Row(labVessel.getLabel());

            String value = triple.getRight();
            row.addCell(new ConfigurableList.Cell(PickerColumn.STORAGE_LOCATION.getHeader(), value, value, false));

            if( rack == null ) {
                isLoose = true;
            } else {
                isLoose = false;
            }
            value = isLoose?"":rack.getLabel();
            row.addCell(new ConfigurableList.Cell(PickerColumn.SOURCE_RACK_BARCODE.getHeader(), value, value));

            value = isLoose?"":vesselPosition.name();
            row.addCell(new ConfigurableList.Cell(PickerColumn.SOURCE_WELL.getHeader(), value, value));

            value = labVessel.getLabel();
            row.addCell(new ConfigurableList.Cell(PickerColumn.TUBE_BARCODE.getHeader(), value, value));

            value = isLoose?"":destinationContainer;
            row.addCell(new ConfigurableList.Cell(PickerColumn.DESTINATION_RACK_BARCODE.getHeader(), value, value));

            if( isLoose ) {
                value = "";
            } else {
                VesselPosition destinationPosition = rackType.getVesselGeometry().getVesselPositions()[counter];
                value = destinationPosition.name();
            }
            row.addCell(new ConfigurableList.Cell(PickerColumn.DESTINATION_WELL.getHeader(), value, value));

            rows.add(row);
            counter++;
            if(isLoose) {
                // Loose vessel rows will be removed at display, this avoids gaps in position sequences
                counter--;
            }
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

    private Triple<RackOfTubes, VesselPosition, String> findStorageContainer(LabVessel labVessel) {
        String locationTrail;
        StorageLocation tubeLocation = labVessel.getStorageLocation();
        if ( tubeLocation != null) {

            // TODO: Is there ever going to be a case where a tube's storage location is not equal to the latest checkin location of any of it's formations ?
            locationTrail = tubeLocation.buildLocationTrail();

            // Slides and cryo straws may be stored without a container
            if(tubeLocation.getLocationType() == StorageLocation.LocationType.LOOSE ) {
                return Triple.of(null, null, locationTrail);
            }

            // If Barcoded Tube, attempt to find its container by grabbing most recent Storage Check-in event.
            if (OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)) {
                LabEvent latestCheckInEvent = findLatestCheckInEvent(labVessel);
                if (latestCheckInEvent != null) {
                    LabVessel tubeFormation = latestCheckInEvent.getInPlaceLabVessel();
                    VesselPosition position = tubeFormation.getContainerRole().getPositionOfVessel(labVessel);
                    RackOfTubes rack =
                            OrmUtil.proxySafeCast(latestCheckInEvent.getAncillaryInPlaceVessel(), RackOfTubes.class);
                    locationTrail = locationTrail + " [" + rack.getLabel() + "]";
                    return Triple.of(rack, position, locationTrail);
                }
            }
        }
        return null;
    }

    /**
     * Find latest check-in event for a lab vessel (barcoded tube only) <br/>
     * If a check-out occurs after a check-in, do not report the check-in event
     */
    private LabEvent findLatestCheckInEvent(LabVessel labVessel ) {
        LabEvent latestCheckInEvent = null;
        List<LabEvent> inPlaceLabEvents;

        // Barocded tube containers are TubeFormation
        for( LabVessel container : labVessel.getContainers() ) {
            for( LabEvent labEvent : container.getInPlaceLabEvents() ) {
                if( labEvent.getLabEventType() == LabEventType.STORAGE_CHECK_IN ) {
                    if (latestCheckInEvent == null) {
                        latestCheckInEvent = labEvent;
                    } else if (labEvent.getEventDate().after(latestCheckInEvent.getEventDate())) {
                        latestCheckInEvent = labEvent;
                    }
                } else if ( labEvent.getLabEventType() == LabEventType.STORAGE_CHECK_OUT ) {
                    if (latestCheckInEvent != null && latestCheckInEvent.getEventDate().before(labEvent.getEventDate())) {
                        latestCheckInEvent = null;
                    }
                }
            }
        }
        return latestCheckInEvent;
    }
}
