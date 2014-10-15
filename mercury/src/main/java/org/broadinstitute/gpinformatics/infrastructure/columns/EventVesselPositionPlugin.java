package org.broadinstitute.gpinformatics.infrastructure.columns;

import oracle.net.aso.f;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a dynamically sized table of barcodes based upon lab vessel container geometry
 * Superclassed to get either target or source container
 */
public abstract class EventVesselPositionPlugin implements ListPlugin {

    VesselGeometry geometry;

    // Position-value map
    protected Map<VesselPosition,String> positionLabelMap;

    // Matrix of positions
    VesselPosition[][] resultMatrix;

    /**
     * Not used in nested table plugins
     * Should throw UnsupportedOperationException( "EventVesselPositionPlugin produces nested table data");
     * @param entityList  list of entities for which to return data
     * @param headerGroup list of headers; the plugin must re-use any existing headers
     *                    that are passed in (each cell has a reference to a header), and may add
     *                    new ones
     * @return
     */
    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup) {
        return null;
    }

    /**
     * Implemented in subclass to call either source or target vessel method
     * Builds a nested table with rows and columns dynamically configured based upon lab event vessel geometry
     * @param entity  A single lab event passed in from parent row
     * @param columnTabulation Nested table column definition
     * @return
     */
    @Override
    public abstract ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation
            , @Nonnull Map<String, Object> context);


    /**
     * Gets position/barcode matrix of event target vessels
     * @param labEvent
     * @param context
     * @return
     */
    protected ConfigurableList.ResultList getTargetNestedTableData(LabEvent labEvent
            , @Nonnull Map<String, Object> context) {
        VesselContainer containerVessel = findEventTargetContainer( labEvent );
        if( containerVessel == null || containerVessel.getMapPositionToVessel().isEmpty() ) {
            return null;
        }
        populatePositionLabelMaps( containerVessel );
        ConfigurableList.ResultList resultList = buildResultList();
        return resultList;
    }

    /**
     * Gets position/barcode matrix of event source vessels
     * @param labEvent
     * @param context
     * @return
     */
    protected ConfigurableList.ResultList getSourceNestedTableData(LabEvent labEvent
            , @Nonnull Map<String, Object> context) {

        // Ignore source vessels for in-place events
        if( labEvent.getInPlaceLabVessel() != null ) {
            return null;
        }

        VesselContainer containerVessel = findEventSourceContainer( labEvent );
        if( containerVessel == null || containerVessel.getMapPositionToVessel().isEmpty() ) {
            return null;
        }
        populatePositionLabelMaps( containerVessel );
        ConfigurableList.ResultList resultList = buildResultList();
        return resultList;
    }

    /**
     * Builds the nested table presentation data structure
     * @return
     */
    private ConfigurableList.ResultList buildResultList() {

        // Build headers (blank at first column, column names from geometry)
        List<ConfigurableList.Header> headers = new ArrayList<>();
        headers.add(new ConfigurableList.Header( "", null, null, null ) );
        for( String val : geometry.getColumnNames() ) {
            headers.add(new ConfigurableList.Header( val, null, null, null ) );
        }

        // Build rows (first cell is row name from geometry
        List<ConfigurableList.ResultRow> resultRows = new ArrayList<>();
        for( int i = 0; i < geometry.getRowCount(); i++ ) {
            List<String> cells = new ArrayList<>();
            cells.add(geometry.getRowNames()[i]);
            for( int j = 0; j < geometry.getColumnCount(); j++ ) {
                cells.add( positionLabelMap.get( resultMatrix[i][j] ) );
            }
            ConfigurableList.ResultRow resultRow = new ConfigurableList.ResultRow(null,cells,String.valueOf(i));
            resultRows.add(resultRow);
        }
        ConfigurableList.ResultList resultList = new ConfigurableList.ResultList( resultRows, headers, 0, "ASC");

        return resultList;
    }

    /**
     * Builds and populates the data structures for the container positions
     * @param containerVessel
     */
    private void populatePositionLabelMaps(VesselContainer containerVessel) {

        positionLabelMap = new HashMap<>();
        geometry = containerVessel.getEmbedder().getVesselGeometry();
        StringBuilder valueHolder = new StringBuilder();

        for (VesselPosition vesselPosition : geometry.getVesselPositions()) {
            LabVessel labVessel = containerVessel.getVesselAtPosition(vesselPosition);
            valueHolder.setLength(0);
            if( labVessel != null ) {
                valueHolder.append("Vessel Barcode: ")
                        .append(labVessel.getLabel());
                appendAncestorSampleData( labVessel, valueHolder );
            }
            positionLabelMap.put(vesselPosition, valueHolder.toString() );
        }

        resultMatrix = new VesselPosition[geometry.getRowCount()][geometry.getColumnCount()];
        for( VesselPosition position : geometry.getVesselPositions() ) {
            VesselGeometry.RowColumn rowCol = geometry.getRowColumnForVesselPosition(position);
            // Position array locations are 1 based
            resultMatrix[rowCol.getRow()-1][rowCol.getColumn()-1] = position;
        }

    }

    /**
     * Appends any available ancestor lab vessel collaborator sample IDs to display.
     * @param labVessel
     * @param valueHolder
     */
    private void appendAncestorSampleData( LabVessel labVessel, StringBuilder valueHolder ) {
        boolean foundOne = false;
        for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
            MercurySample sample = sampleInstanceV2.getRootOrEarliestMercurySample();
            if( sample == null ) {
                return;
            }

            Set<Metadata> metadata = sample.getMetadata();
            if( metadata == null || metadata.isEmpty() ) {
                return;
            }

            // Find collaborator sample
            for( Metadata meta : metadata ) {
                if( meta.getKey() == Metadata.Key.SAMPLE_ID ){
                    valueHolder.append( (foundOne?", ":" \nCollaborator Sample ID(s): [") )
                            .append(meta.getValue());
                    foundOne = true;
                }
            }
        }

        if( foundOne ) {
            valueHolder.append("]");
        }
    }


    /**
     * Gets target container for the lab event
     * @param labEvent
     * @return
     */
    private VesselContainer findEventTargetContainer( LabEvent labEvent ) {

        VesselContainer vesselContainer = null;

        // In place vessel is a container where event has no transfers
        LabVessel vessel = labEvent.getInPlaceLabVessel();
        if( vessel != null ) {
            vesselContainer = vessel.getContainerRole();
        }

        // Transfer
        if( vesselContainer == null ){
            for (LabVessel srcVessel : labEvent.getTargetLabVessels()) {
                vesselContainer = srcVessel.getContainerRole();
                break;
            }
        }

        return vesselContainer;
    }


    /**
     * Gets source container for the lab event
     * @param labEvent
     * @return
     */
    private VesselContainer findEventSourceContainer( LabEvent labEvent ) {

        VesselContainer vesselContainer = null;

        // In place vessel is a container where event has no transfers
        LabVessel vessel = labEvent.getInPlaceLabVessel();
        if( vessel != null ) {
            vesselContainer = vessel.getContainerRole();
        }

        // Transfer
        if( vesselContainer == null ){
            for (LabVessel srcVessel : labEvent.getSourceLabVessels()) {
                vesselContainer = srcVessel.getContainerRole();
                break;
            }
        }

        return vesselContainer;
    }

}
