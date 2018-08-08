package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Fetches data for the newest event involving a tube formation containing a barcoded tube
 * Ignores processing any vessel which isn't a barcoded tube
 */
public class LabVesselLatestPositionPlugin implements ListPlugin {

    public LabVesselLatestPositionPlugin() {
    }

    private enum PluginColumn implements Displayable {
        RACK_BARCODE("Latest Rack"),
        POSITION("Latest Rack Position"),
        EVENT("Latest Event on Tube or Rack");

        private String displayName;
        private ConfigurableList.Header header;

        PluginColumn(String displayName) {
            this.displayName = displayName;
            this.header = new ConfigurableList.Header(displayName, displayName, "");
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        public ConfigurableList.Header getHeader() {
            return header;
        }

    }

    /**
     * Gathers newest rack, position, and event (if applicable) and associates with LabVessel row in search results.
     *
     * @param entityList  List of LabVessel entities for which to return rack, position, and event data
     * @param headerGroup List of headers associated with columns selected by user.
     * @param context     SearchContext - Nothing required within, not used
     *
     * @return A list of rows, each mapped to a LabVessel search result row by barcode
     */
    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup
            , @Nonnull SearchContext context) {
        List<LabVessel> labVesselList = (List<LabVessel>) entityList;
        List<ConfigurableList.Row> eventRows = new ArrayList<>();

        // Configure column headers to append to search result row display
        for (PluginColumn column : PluginColumn.values()) {
            headerGroup.addHeader(column.getHeader());
        }

        // Populate row plugin columns with any available rack, position, and event data.
        for (LabVessel labVessel : labVesselList) {
            ConfigurableList.Row row = new ConfigurableList.Row(labVessel.getLabel());

            Triple<RackOfTubes, VesselContainer<BarcodedTube>, LabEvent> rackAndTubesData = findRackAndTubesData( labVessel );

            if (rackAndTubesData == null) {
                // Empty cells
                for (PluginColumn column : PluginColumn.values()) {
                    row.addCell(new ConfigurableList.Cell(column.getHeader(), "", ""));
                }
            } else {
                // Rack
                String cellValue = rackAndTubesData.getLeft().getLabel();
                row.addCell(new ConfigurableList.Cell(PluginColumn.RACK_BARCODE.getHeader(), cellValue, cellValue));

                // Position
                cellValue = rackAndTubesData.getMiddle().getPositionOfVessel(labVessel).toString();
                row.addCell(
                        new ConfigurableList.Cell(PluginColumn.POSITION.getHeader(), cellValue, cellValue));

                // Event
                cellValue = getEventDisplay( rackAndTubesData.getRight(), context );
                row.addCell(
                        new ConfigurableList.Cell(PluginColumn.EVENT.getHeader(), cellValue, cellValue));
            }
            eventRows.add(row);
        }

        return eventRows;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
                                                          @Nonnull SearchContext context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in "
                                                + getClass().getSimpleName());
    }

    /**
     * Try to find rack, tube formation, and newest event associated with a tube
     * Initially use the newest event associated with the tube containers (tube formations)
     * Fallback to newest rack created for tube's newest tube formation
     */
    private Triple<RackOfTubes, VesselContainer<BarcodedTube>, LabEvent> findRackAndTubesData(LabVessel labVessel ) {
        BarcodedTube tube;
        Triple<RackOfTubes, VesselContainer<BarcodedTube>, LabEvent> rackAndTubesData = null;
        boolean useTarget;

        // This works on tubes only, ignore all else
        if( !OrmUtil.proxySafeIsInstance( labVessel, BarcodedTube.class )) {
            return null;
        } else {
            tube = OrmUtil.proxySafeCast( labVessel, BarcodedTube.class );
        }

        LabEvent latestInPlaceEvent = null;
        LabEvent latestXferEvent = null;
        boolean latestIsInPlaceEvent = false;

        // Tube containers(embedder) are always TubeFormations
        Set<VesselContainer<?>> containers = tube.getVesselContainers();
        TreeSet<LabEvent> events = new TreeSet<>(LabEvent.BY_EVENT_DATE);

        // Try transfers from first, should always be latest transfer event
        // In place events will always be before a transfer from so ignore
        for (VesselContainer<?> vesselContainer : containers ) {
            events.addAll(vesselContainer.getTransfersFrom());
        }

        if( !events.isEmpty() ) {
            latestXferEvent = events.last();
            useTarget = false;
        } else {

            // Transfers to may also have later in place events
            events.addAll( tube.getInPlaceEventsWithContainers() );
            if( !events.isEmpty() ) {
                latestInPlaceEvent = events.last();
                events.clear();
            }

            // Try tranfers to
            for (VesselContainer<?> vesselContainer : containers ) {
                events.addAll(vesselContainer.getTransfersToWithRearrays());
            }

            // Give up and die here if no transfers to, tubes are empty so irrelevant
            if( events.isEmpty() ) {
                return null;
            } else {
                latestXferEvent = events.last();
                useTarget = true;
                if( latestInPlaceEvent != null && latestInPlaceEvent.getEventDate().compareTo(latestXferEvent.getEventDate()) > 0 ) {
                    latestIsInPlaceEvent = true;
                }
            }
        }

        // Still here so now we need the rack barcode of the tube formation and the tube's position
        if( !latestIsInPlaceEvent ) {
            rackAndTubesData = getRackAndPosition(latestXferEvent, tube, useTarget);
        }

        // Possibly null due to ancillary vessel logic being added around Aug 2014.
        //  Handle any earlier cases? tube --> containers --> getRacksOfTubes().size() == 1
        //          return tube.getRacksOfTubes().iterator().next().getLabel();
        if( rackAndTubesData != null && rackAndTubesData.getLeft() == null ) {
            RackOfTubes rack = tryLegacyGetRackAndPosition(rackAndTubesData.getMiddle().getEmbedder());
            if( rack != null ) {
                rackAndTubesData = Triple.of(rack, rackAndTubesData.getMiddle(), rackAndTubesData.getRight() );
            }
        }

        if ( rackAndTubesData != null && latestIsInPlaceEvent ) {
            rackAndTubesData = Triple.of( rackAndTubesData.getLeft(), rackAndTubesData.getMiddle(), latestInPlaceEvent );
        }

        // Possibly null
        return rackAndTubesData;
    }


    /**
     * Logic for events targeting tube formations
     * Similar to LabEventSearchDefinition.getLabelFromTubeFormation()
     */
    private Triple<RackOfTubes, VesselContainer<BarcodedTube>, LabEvent> getRackAndPosition( LabEvent labEvent, BarcodedTube tube, boolean useTarget ){
        LabVessel rack = null;
        VesselContainer vesselContainer = null;

        // Section transfer most likely?
        for( SectionTransfer xfer : labEvent.getSectionTransfers() ) {
            if( useTarget ) {
                rack = xfer.getAncillaryTargetVessel();
                vesselContainer = rack==null?null:xfer.getTargetVesselContainer();
            } else {
                rack = xfer.getAncillarySourceVessel();
                vesselContainer = xfer.getSourceVesselContainer();
            }
            if( vesselContainer != null ) {
                return Triple.of( OrmUtil.proxySafeCast( rack, RackOfTubes.class ), vesselContainer, labEvent );
            }
        }

        // Still here?  Try CherryPick transfers
        for (CherryPickTransfer xfer : labEvent.getCherryPickTransfers() ) {
            if( useTarget ) {
                rack = xfer.getAncillaryTargetVessel();
                vesselContainer = xfer.getTargetVesselContainer();
            } else {
                rack = xfer.getAncillarySourceVessel();
                vesselContainer = xfer.getSourceVesselContainer();
            }
            if( vesselContainer != null ) {
                return Triple.of( OrmUtil.proxySafeCast( rack, RackOfTubes.class ), vesselContainer, labEvent );
            }
        }

        // Still here?  Try VesselToSection transfers
        if( useTarget ) {
            for(VesselToSectionTransfer xfer : labEvent.getVesselToSectionTransfers() ) {
                rack = xfer.getAncillaryTargetVessel();
                vesselContainer = xfer.getTargetVesselContainer();
            }
            if( vesselContainer != null ) {
                return Triple.of( OrmUtil.proxySafeCast( rack, RackOfTubes.class ), vesselContainer, labEvent );
            }
        }

        // Still here? Die empty handed
        return null;
    }


    /**
     * The implementation of ancillary vessels on transfers to record the rack that a tubeformation was in
     *  was added around Aug 2014. This method is the last attempt to get a rack barcode from a tube formation by
     *  simply finding the tube formation with the latest creation date and getting its
     *  rack reference with the latest creation date.
     */
    private RackOfTubes tryLegacyGetRackAndPosition(LabVessel latestTubeFormation) {
        RackOfTubes latestRack = null;
        Date previousCreatedOn = new Date(0L );


        if( latestTubeFormation != null ) {
            for( RackOfTubes rack : OrmUtil.proxySafeCast( latestTubeFormation, TubeFormation.class ).getRacksOfTubes()) {
                if( rack.getCreatedOn().compareTo(previousCreatedOn) > 0 ) {
                    latestRack = OrmUtil.proxySafeCast(rack, RackOfTubes.class);
                    previousCreatedOn = rack.getCreatedOn();
                }
            }
        }

        if( latestRack != null ) {
            return latestRack;
        } else {
            return null;
        }
    }

    /**
     * Formats event data for single column output
     * @return Space delimited event type, date, and user
     */
    private String getEventDisplay( LabEvent event, SearchContext context ) {
        if( event == null ) {
            return "";
        }

        Long userId = event.getEventOperator();
        BspUser user = context.getBspUserList().getById(userId);

        if( user != null ) {
            return event.getLabEventType().getName()
                   + ", " + ColumnValueType.DATE.format( event.getEventDate(), "" )
                   + ", " + user.getFullName();
        } else {
            return event.getLabEventType().getName()
                   + ", " + ColumnValueType.DATE.format(event.getEventDate(), "" );
        }
    }

}
