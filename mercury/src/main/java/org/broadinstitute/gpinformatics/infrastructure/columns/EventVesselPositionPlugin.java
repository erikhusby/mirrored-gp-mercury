package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a dynamically sized table of barcodes based upon lab vessel container geometry
 * Superclassed to get either target or source container
 */
public abstract class EventVesselPositionPlugin implements ListPlugin {

    private VesselGeometry geometry;

    // Position-value map
    protected Map<VesselPosition,String> positionLabelMap;

    // Matrix of positions
    private VesselPosition[][] resultMatrix;

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
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup
            , SearchContext context) {
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
            , @Nonnull SearchContext context);


    /**
     * Gets position/barcode matrix of event target vessels
     * @param labEvent
     * @param context
     * @return
     */
    protected ConfigurableList.ResultList getTargetNestedTableData(LabEvent labEvent
            , @Nonnull SearchContext context) {
        VesselContainer containerVessel = findEventTargetContainer(labEvent);
        if( containerVessel == null || containerVessel.getMapPositionToVessel().isEmpty() ) {
            return null;
        }
        populatePositionLabelMaps( containerVessel, context );
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
            , @Nonnull SearchContext context) {

        // If destination layout is selected, ignore source vessels for in-place events
        if( labEvent.getInPlaceLabVessel() != null ) {
            SearchInstance searchInstance = context.getSearchInstance();
            List<String> displayColumnNames = searchInstance.getPredefinedViewColumns();
            if( displayColumnNames.contains("Destination Layout")) {
                return null;
            }
        }

        VesselContainer containerVessel = findEventSourceContainer( labEvent );
        if( containerVessel == null || containerVessel.getMapPositionToVessel().isEmpty() ) {
            return null;
        }
        populatePositionLabelMaps( containerVessel, context );
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
        headers.add(new ConfigurableList.Header( "", null, null ) );
        for( String val : geometry.getColumnNames() ) {
            headers.add(new ConfigurableList.Header( val, null, null ) );
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
    private void populatePositionLabelMaps(VesselContainer containerVessel, SearchContext context) {

        // Hold previously set context values
        SearchTerm originalTerm = context.getSearchTerm();
        String delimiter =  context.getMultiValueDelimiter();
        context.setMultiValueDelimiter(", ");

        List<SearchTerm> parentTermsToDisplay = getParentTermsToDisplay( context );

        positionLabelMap = new HashMap<>();
        geometry = containerVessel.getEmbedder().getVesselGeometry();
        StringBuilder valueHolder = new StringBuilder();

        for (VesselPosition vesselPosition : geometry.getVesselPositions()) {
            LabVessel labVessel = containerVessel.getVesselAtPosition(vesselPosition);
            valueHolder.setLength(0);
            if( labVessel != null ) {
                valueHolder.append("Vessel Barcode: ")
                        .append(labVessel.getLabel());

                appendDataForParentTerms(labVessel, valueHolder, parentTermsToDisplay, context);
            }
            positionLabelMap.put(vesselPosition, valueHolder.toString());
        }

        // Restore previously set context values
        if( delimiter != null ) {
            context.setMultiValueDelimiter(delimiter);
        }
        if( originalTerm != null ){
            context.setSearchTerm(originalTerm);
        }

        resultMatrix = new VesselPosition[geometry.getRowCount()][geometry.getColumnCount()];
        for( VesselPosition position : geometry.getVesselPositions() ) {
            VesselGeometry.RowColumn rowCol = geometry.getRowColumnForVesselPosition(position);
            // Position array locations are 1 based
            resultMatrix[rowCol.getRow()-1][rowCol.getColumn()-1] = position;
        }

    }

    /**
     * User can select terms which are also displayed in the vessel position cells
     * @param context
     * @return
     */
    private List<SearchTerm> getParentTermsToDisplay( SearchContext context ) {
        // Logic is tied to context values ...
        SearchInstance searchInstance = context.getSearchInstance();
        ColumnEntity columnEntity = context.getColumnEntityType();
        SearchTerm thisSearchTerm = context.getSearchTerm();

        List<SearchTerm> parentTermsToDisplay = new ArrayList<>();

        // Build a list of user selected columns
        List<String> displayColumnNames = new ArrayList<>();
        for( String displayColumnName : searchInstance.getPredefinedViewColumns() ) {
            displayColumnNames.add(displayColumnName);
        }
        // Add search values to the list
        for(SearchInstance.SearchValue searchValue : searchInstance.getSearchValues()) {
            if( searchValue.getIncludeInResults()) {
                displayColumnNames.add(searchValue.getName());
            }
        }

        ConfigurableSearchDefinition configurableSearchDefinition = SearchDefinitionFactory.getForEntity( columnEntity.getEntityName() );

        // Find the parent terms this plugin handles
        for( String columnName : displayColumnNames ){
            SearchTerm selectedParentSearchTerm = configurableSearchDefinition.getSearchTerm(columnName);
            if( thisSearchTerm.isParentTermHandledByChild(selectedParentSearchTerm)){
                parentTermsToDisplay.add(selectedParentSearchTerm);
            }
        }

        return parentTermsToDisplay;
    }

    /**
     * Appends parent data handled in nested table to displayed position cell.
     * @param labVessel
     * @param valueHolder
     */
    private void appendDataForParentTerms(LabVessel labVessel, StringBuilder valueHolder,
                                          List<SearchTerm> parentTermsToDisplay, SearchContext context) {
        for( SearchTerm parentTerm : parentTermsToDisplay ) {
            // Need if sample metadata is included in selection
            context.setSearchTerm(parentTerm);
            Object value = parentTerm.getDisplayValueExpression().evaluate(labVessel, context );
            valueHolder.append("\n")
                    .append(parentTerm.getName())
                    .append(": ");
            if( value instanceof Collection ){
                Collection multiVal = (Collection)value;
                if( multiVal.isEmpty() ) {
                    valueHolder.append("[No Data]");
                } else {
                    valueHolder.append("[")
                            .append(parentTerm.evalFormattedExpression(multiVal, context))
                            .append("]");
                }
            } else {
                if (value == null || value.toString().isEmpty()) {
                    valueHolder.append("[No Data]");
                } else {
                    valueHolder.append("[")
                            .append(parentTerm.evalFormattedExpression(value, context))
                            .append("]");
                }
            }
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
