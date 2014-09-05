package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Builds a dynamically sized table of barcodes based upon event target lab vessel container geometry
 */
public class EventVesselTargetPositionPlugin extends EventVesselPositionPlugin {

    /**
     * Builds a nested table with rows and columns dynamically configured based upon lab event vessel geometry
     * @param entity  A single lab event passed in from parent row
     * @param columnTabulation Nested table column definition
     * @return
     */
    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation
            , @Nonnull Map<String, Object> context) {
        LabEvent labEvent = (LabEvent) entity;
        return getTargetNestedTableData(labEvent, context);
    }

}
