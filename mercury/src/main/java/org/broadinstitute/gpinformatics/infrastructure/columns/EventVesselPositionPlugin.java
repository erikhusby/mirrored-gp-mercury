package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
import org.owasp.encoder.Encode;

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

    /** Holds nested table for each position in the layout */
    private Map<VesselPosition, Pair<String, ConfigurableList.ResultList>> positionTableMap;

    // Matrix of positions
    private VesselPosition[][] resultMatrix;

    /**
     * Not used in nested table plugins
     * Should throw UnsupportedOperationException( "EventVesselPositionPlugin produces nested table data");
     * @param entityList  list of entities for which to return data
     * @param headerGroup list of headers; the plugin must re-use any existing headers
     *                    that are passed in (each cell has a reference to a header), and may add
     *                    new ones
     * @return list of rows
     */
    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup
            , @Nonnull SearchContext context) {
        return null;
    }

    /**
     * Implemented in subclass to call either source or target vessel method
     * Builds a nested table with rows and columns dynamically configured based upon lab event vessel geometry
     * @param entity  A single lab event passed in from parent row
     * @param columnTabulation Nested table column definition
     * @return nested table
     */
    @Override
    public abstract ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation
            , @Nonnull SearchContext context);


    /**
     * Gets position/barcode matrix of event target vessels.
     */
    protected ConfigurableList.ResultList getTargetNestedTableData(LabEvent labEvent
            , @Nonnull SearchContext context) {
        VesselContainer<?> containerVessel = findEventTargetContainer(labEvent);
        if( containerVessel == null ) {
            return null;
        }
        populatePositionLabelMaps( containerVessel, context );
        return buildResultList();
    }

    /**
     * Gets position/barcode matrix of event source vessels.
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

        VesselContainer<?> containerVessel = findEventSourceContainer(labEvent);
        if( containerVessel == null || containerVessel.getMapPositionToVessel().isEmpty() ) {
            return null;
        }
        populatePositionLabelMaps( containerVessel, context );
        return buildResultList();
    }

    /**
     * Builds the nested table presentation data structure.
     */
    ConfigurableList.ResultList buildResultList() {

        // Build headers (blank at first column, column names from geometry)
        List<ConfigurableList.Header> headers = new ArrayList<>();
        headers.add(new ConfigurableList.Header( "", null, null ) );
        for( String val : geometry.getColumnNames() ) {
            headers.add(new ConfigurableList.Header( val, val, null ) );
        }

        // Build rows (first cell is row name from geometry
        List<ConfigurableList.ResultRow> resultRows = new ArrayList<>();
        for( int i = 0; i < geometry.getRowCount(); i++ ) {
            List<String> cells = new ArrayList<>();
            cells.add(geometry.getRowNames()[i]);
            List<ConfigurableList.ResultList> nestedTables = new ArrayList<>();
            // Empty nested table for row name
            nestedTables.add(null);

            for(int j = 0; j < geometry.getColumnCount(); j++ ) {
                cells.add(positionTableMap.get( resultMatrix[i][j] ).getLeft());
                nestedTables.add(positionTableMap.get( resultMatrix[i][j] ).getRight());
            }
            ConfigurableList.ResultRow resultRow = new ConfigurableList.ResultRow(null,cells,String.valueOf(i));
            resultRow.setCellNestedTables(nestedTables);
            resultRows.add(resultRow);
        }

        return new ConfigurableList.ResultList( resultRows, headers, 0, "ASC");
    }

    /**
     * Builds and populates the data structures for the container positions.
     */
    void populatePositionLabelMaps(VesselContainer<?> containerVessel, SearchContext context) {

        // Hold previously set context values
        SearchTerm originalTerm = context.getSearchTerm();
        String delimiter =  context.getMultiValueDelimiter();
        context.setMultiValueDelimiter(", ");

        List<SearchTerm> parentTermsToDisplay = getParentTermsToDisplay( context );

        positionTableMap = new HashMap<>();
        geometry = containerVessel.getEmbedder().getVesselGeometry();

        for (VesselPosition vesselPosition : geometry.getVesselPositions()) {
            positionTableMap.put(vesselPosition, appendDataForParentTerms(containerVessel, vesselPosition,
                    parentTermsToDisplay, context));
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
     * User can select terms which are also displayed in the vessel position cells.
     */
    private List<SearchTerm> getParentTermsToDisplay( SearchContext context ) {
        // Logic is tied to context values ...
        SearchInstance searchInstance = context.getSearchInstance();
        ColumnEntity columnEntity = context.getColumnEntityType();

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
        List<SearchTerm> parentTermsToDisplay = new ArrayList<>();
        for( String columnName : displayColumnNames ){
            SearchTerm selectedParentSearchTerm = configurableSearchDefinition.getSearchTerm(columnName);
            if( selectedParentSearchTerm.getDisplayExpression() != null){
                parentTermsToDisplay.add(selectedParentSearchTerm);
            }
        }

        return parentTermsToDisplay;
    }

    /**
     * Appends parent data handled in nested table to displayed position cell.
     * @param containerVessel Extract vessel/sample data from positions
     * @param vesselPosition The position in the container
     * @param parentTermsToDisplay Collection of parent search result columns which can be displayed in nested table
     * @param context Values passed along call stack
     */
    private Pair<String, ConfigurableList.ResultList> appendDataForParentTerms(VesselContainer<?> containerVessel,
            VesselPosition vesselPosition, List<SearchTerm> parentTermsToDisplay, SearchContext context) {

        List<ConfigurableList.Header> headers = new ArrayList<>();
        List<ConfigurableList.ResultRow> resultRows = new ArrayList<>();
        ConfigurableList.ResultList resultList = new ConfigurableList.ResultList(resultRows, headers, 0, "ASC");

        LabVessel labVessel = containerVessel.getImmutableVesselAtPosition(vesselPosition);

        // Vessel barcodes are always displayed
        // todo jmt only display label for tubes
        String cell = null;
        if( labVessel != null ) {
            cell = labVessel.getLabel();
        }

        List<Comparable<?>> emptySortableCells = new ArrayList<>();

        if( !parentTermsToDisplay.isEmpty() && labVessel != null) {
            int cellIndex = 0;
            // Cache and reuse traversals, to ensure same ordering
            Map<Class<?>, Collection<?>> mapClassToResults = new HashMap<>();
            for( SearchTerm parentTerm : parentTermsToDisplay ) {
                context.setSearchTerm(parentTerm);
                Class<?> expressionClass = parentTerm.getDisplayExpression().getExpressionClass();
                Collection<?> objects = mapClassToResults.get(expressionClass);
                if (objects == null) {
                    objects = DisplayExpression.rowObjectToExpressionObject(
                            labVessel,
                            expressionClass,
                            context);
                    mapClassToResults.put(expressionClass, objects);
                }
                int rowIndex = 0;
                for (Object object : objects) {
                    if (cellIndex == 0) {
                        resultRows.add(new ConfigurableList.ResultRow(emptySortableCells, new ArrayList<>(), null));
                    }
                    String output = parentTerm.evalPlainTextOutputExpression(
                            parentTerm.getDisplayExpression().getEvaluator().evaluate(object, context), context);
                    resultRows.get(rowIndex).getRenderableCells().add(parentTerm.mustEscape() ?
                            Encode.forHtml(output) : output);
                    rowIndex++;
                }
                cellIndex++;
            }
            for( SearchTerm parentTerm : parentTermsToDisplay ) {
                headers.add(new ConfigurableList.Header(parentTerm.getName(), parentTerm.getName(), null));
            }
        }
        // todo jmt sort by first column?

        return new ImmutablePair<>(cell, resultList);
    }


    /**
     * Gets target container for the lab event.
     */
    private VesselContainer<?> findEventTargetContainer( LabEvent labEvent ) {

        VesselContainer<?> vesselContainer = null;

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
     * Gets source container for the lab event.
     */
    private VesselContainer<?> findEventSourceContainer( LabEvent labEvent ) {

        VesselContainer<?> vesselContainer = null;

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
