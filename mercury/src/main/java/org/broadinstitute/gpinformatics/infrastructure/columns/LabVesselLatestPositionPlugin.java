package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.lang3.tuple.Pair;
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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

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
        POSITION("Latest Rack Position");

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
     * Gathers newest rack and position (if applicable) and associates with LabVessel row in search results.
     *
     * @param entityList  List of LabVessel entities for which to return rack and position data
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

        // Populate row plugin columns with any available rack and position data.
        for (LabVessel labVessel : labVesselList) {
            ConfigurableList.Row row = new ConfigurableList.Row(labVessel.getLabel());

            Pair<RackOfTubes, VesselPosition> rackAndPosition = findRackAndPositionFromTube( labVessel );

            if (rackAndPosition == null) {
                // Empty cells
                for (PluginColumn column : PluginColumn.values()) {
                    row.addCell(new ConfigurableList.Cell(column.getHeader(), "", ""));
                }
            } else {
                // Rack
                String cellValue = rackAndPosition.getLeft().getLabel();
                row.addCell(new ConfigurableList.Cell(PluginColumn.RACK_BARCODE.getHeader(), cellValue, cellValue));

                // Position
                cellValue = rackAndPosition.getRight().toString();
                row.addCell(
                        new ConfigurableList.Cell(PluginColumn.POSITION.getHeader(), cellValue, cellValue));
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
     * Try to find rack and position associated with a tube
     * Initially use the newest event associated with the containers (tube formations) a tube is in
     * Fallback to newest rack created for newest tube formation a tube is in
     */
    private Pair<RackOfTubes, VesselPosition> findRackAndPositionFromTube( LabVessel labVessel ) {
        BarcodedTube tube;
        Pair<RackOfTubes, VesselPosition> rackAndPosition = null;
        boolean useTarget;

        // This works on tubes only, ignore all else
        if( !OrmUtil.proxySafeIsInstance( labVessel, BarcodedTube.class )) {
            return null;
        } else {
            tube = OrmUtil.proxySafeCast( labVessel, BarcodedTube.class );
        }

        LabEvent latestEvent;

        // Tube containers(embedder) are always TubeFormations
        Set<VesselContainer<?>> containers = tube.getVesselContainers();
        TreeSet<LabEvent> events = new TreeSet<>(LabEvent.BY_EVENT_DATE);

        for (VesselContainer<?> vesselContainer : containers ) {
            events.addAll(vesselContainer.getTransfersFrom());
        }

        if( !events.isEmpty() ) {
            latestEvent = events.last();
            useTarget = false;
        } else {
            // Try tranfers to
            for (VesselContainer<?> vesselContainer : containers ) {
                events.addAll(vesselContainer.getTransfersToWithRearrays());
            }

            // Give up and die here
            if( events.isEmpty() ) {
                return null;
            } else {
                latestEvent = events.last();
                useTarget = true;
            }
        }

        // Still here so now we need the rack barcode of the tube formation and the tube's position
        rackAndPosition = getRackAndPosition( latestEvent, tube, useTarget );

        // Possibly null due to ancillary vessel logic being added around Aug 2014.
        //  Handle any earlier cases? tube --> containers --> getRacksOfTubes().size() == 1
        //          return tube.getRacksOfTubes().iterator().next().getLabel();
        if( rackAndPosition == null ) {
            rackAndPosition = tryLegacyGetRackAndPosition(tube);
        }

        // Possibly null
        return rackAndPosition;
    }


    /**
     * Logic for events targeting tube formations
     * Similar to LabEventSearchDefinition.getLabelFromTubeFormation()
     */
    private Pair<RackOfTubes, VesselPosition> getRackAndPosition( LabEvent labEvent, BarcodedTube tube, boolean useTarget ){
        LabVessel rack = null;
        VesselContainer vesselContainer = null;

        // Section transfer most likely?
        for( SectionTransfer xfer : labEvent.getSectionTransfers() ) {
            if( useTarget ) {
                rack = xfer.getAncillaryTargetVessel();
                vesselContainer = rack==null?null:xfer.getTargetVesselContainer();
            } else {
                rack = xfer.getAncillarySourceVessel();
                vesselContainer = rack==null?null:xfer.getSourceVesselContainer();
            }
            if( vesselContainer != null ) {
                return Pair.of( OrmUtil.proxySafeCast( rack, RackOfTubes.class ), vesselContainer.getPositionOfVessel(tube) );
            }
        }

        // Still here?  Try CherryPick transfers
        for (CherryPickTransfer xfer : labEvent.getCherryPickTransfers() ) {
            if( useTarget ) {
                rack = xfer.getAncillaryTargetVessel();
                vesselContainer = rack==null?null:xfer.getTargetVesselContainer();
            } else {
                rack = xfer.getAncillarySourceVessel();
                vesselContainer = rack==null?null:xfer.getSourceVesselContainer();
            }
            if( vesselContainer != null ) {
                return Pair.of( OrmUtil.proxySafeCast( rack, RackOfTubes.class ), vesselContainer.getPositionOfVessel(tube) );
            }
        }

        // Still here?  Try VesselToSection transfers
        if( useTarget ) {
            for(VesselToSectionTransfer xfer : labEvent.getVesselToSectionTransfers() ) {
                rack = xfer.getAncillaryTargetVessel();
                vesselContainer = rack==null?null:xfer.getTargetVesselContainer();
            }
            if( vesselContainer != null ) {
                return Pair.of( OrmUtil.proxySafeCast( rack, RackOfTubes.class ), vesselContainer.getPositionOfVessel(tube) );
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
    private Pair<RackOfTubes, VesselPosition> tryLegacyGetRackAndPosition(BarcodedTube tube) {
        TubeFormation latestTubeFormation = null;
        RackOfTubes latestRack = null;
        Date previousCreatedOn = new Date(0L );
        for( LabVessel vesselContainer : tube.getContainers() ) {
            if( vesselContainer.getCreatedOn().compareTo(previousCreatedOn) > 0 ) {
                latestTubeFormation = OrmUtil.proxySafeCast(vesselContainer, TubeFormation.class);
                previousCreatedOn = vesselContainer.getCreatedOn();
            }
        }

        if( latestTubeFormation != null ) {
            previousCreatedOn = new Date(0L );
            for( RackOfTubes rack : latestTubeFormation.getRacksOfTubes()) {
                if( rack.getCreatedOn().compareTo(previousCreatedOn) > 0 ) {
                    latestRack = OrmUtil.proxySafeCast(rack, RackOfTubes.class);
                    previousCreatedOn = rack.getCreatedOn();
                }
            }
        }

        if( latestRack != null ) {
            return Pair.of( latestRack, latestTubeFormation.getContainerRole().getPositionOfVessel(tube) );
        } else {
            return null;
        }
    }

}
