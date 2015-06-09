package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;

import javax.annotation.Nonnull;

/**
 * Builds a dynamically sized table of barcodes based upon lab vessel Source container geometry
 */
public class EventVesselSourcePositionPlugin extends EventVesselPositionPlugin {

    /**
     * Builds a nested table with rows and columns dynamically configured based upon lab event vessel geometry
     * @param entity  A single lab event passed in from parent row
     * @param columnTabulation Nested table column definition
     * @return
     */
    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation
            , @Nonnull SearchContext context) {
        LabEvent labEvent = (LabEvent) entity;
        return getSourceNestedTableData(labEvent, context);
    }

}
