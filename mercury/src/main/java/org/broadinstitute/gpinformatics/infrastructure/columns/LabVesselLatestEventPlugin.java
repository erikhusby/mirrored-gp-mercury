package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches data for the last event of any descendant vessels in a lab vessel search.
 */
public class LabVesselLatestEventPlugin implements ListPlugin {

    public LabVesselLatestEventPlugin() {}

    private enum LatestEventColumn implements Displayable {
        EVENT_NAME( "Latest Event"),
        EVENT_OPERATOR( "Event Operator"),
        EVENT_LOCATION( "Event Location"),
        EVENT_DATE( "Event Date");

        private String displayName;
        private ConfigurableList.Header header;

        LatestEventColumn( String displayName ){
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

    /**
     * Gathers last event data of interest and associates with LabVessel row in search results.
     * @param entityList  List of LabVessel entities for which to return LabMetrics data
     * @param headerGroup List of headers associated with columns selected by user.  This plugin appends column headers
     *                    for LabMetrics and Decisions of interest.
     * @param context At minimum, BSP user required helper object passed in from callers (e.g. ConfigurableListFactory)
     * @return A list of rows, each corresponding to a LabVessel row in search results.
     */
    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup
            , @Nonnull SearchContext context) {
        List<LabVessel> labVesselList = (List<LabVessel>) entityList;
        List<ConfigurableList.Row> eventRows = new ArrayList<>();
        BSPUserList bspUserList = context.getBspUserList();

        // Append headers for event data of interest.
        for( LatestEventColumn column : LatestEventColumn.values() ){
            headerGroup.addHeader(column.getHeader());
        }

        // Populate rows with any available event data.
        for( LabVessel labVessel : labVesselList ) {
            ConfigurableList.Row row = new ConfigurableList.Row( labVessel.getLabel() );

            TransferTraverserCriteria.LabEventDescendantCriteria eventTraversalCriteria =
                    new TransferTraverserCriteria.LabEventDescendantCriteria();
            labVessel.evaluateCriteria(eventTraversalCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);

            LabEvent lastEvent = null;
            for (LabEvent labEvent : eventTraversalCriteria.getAllEvents()) {
                lastEvent = labEvent;
            }

            if( lastEvent == null ) {
                // Empty cells
                for( LatestEventColumn column : LatestEventColumn.values() ){
                    row.addCell(new ConfigurableList.Cell(column.getHeader(), "", ""));
                }
            } else {
                // Event Name
                String cellValue = lastEvent.getLabEventType().getName();
                row.addCell( new ConfigurableList.Cell(LatestEventColumn.EVENT_NAME.getHeader(), cellValue, cellValue ));
                // Event Operator
                Long userId = lastEvent.getEventOperator();
                BspUser bspUser = bspUserList.getById(userId);
                if (bspUser == null) {
                    cellValue = "Unknown user - ID: " + userId;
                } else {
                    cellValue = bspUser.getFullName();
                }
                row.addCell( new ConfigurableList.Cell(LatestEventColumn.EVENT_OPERATOR.getHeader(), cellValue, cellValue ));
                // Event Location
                cellValue = lastEvent.getEventLocation();
                row.addCell( new ConfigurableList.Cell(LatestEventColumn.EVENT_LOCATION.getHeader(), cellValue, cellValue ));
                // Event Date
                cellValue = ColumnValueType.DATE_TIME.format( lastEvent.getEventDate(), null );
                row.addCell( new ConfigurableList.Cell(LatestEventColumn.EVENT_DATE.getHeader(), lastEvent.getEventDate(), cellValue ));
            }
            eventRows.add(row);
        }

        return eventRows;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
            @Nonnull SearchContext context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in "
                                                + getClass().getSimpleName() );
    }
}
