package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;

import javax.annotation.Nonnull;

/**
 * Plugin that displays layout of containers, e.g. flowcells.
 */
public class VesselLayoutPlugin extends EventVesselPositionPlugin {
    /**
     * Builds a nested table with rows and columns dynamically configured based upon lab event vessel geometry
     * @param entity  lab vessel
     * @param columnTabulation Nested table column definition
     * @return nested result list
     */
    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation,
            @Nonnull SearchContext context) {
        LabVessel labVessel = OrmUtil.proxySafeCast(entity, LabVessel.class);
        VesselContainer<?> containerVessel = labVessel.getContainerRole();
        if( containerVessel == null ) {
            return null;
        }
        populatePositionLabelMaps( containerVessel, context );
        return buildResultList();
    }
}
