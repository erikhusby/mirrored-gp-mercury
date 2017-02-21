package org.broadinstitute.gpinformatics.athena.entity.datadelivery;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.testng.annotations.Test;

import java.util.Collections;

/**
 * Prototype entities for Data Delivery
 */
public class DataDeliveryTest {

    @Test
    public void testX() {
        // Create initial set
        // Search with PDO (UDS? would be valuable to save search values, but traverser would likely be heavily customized ; perhaps not, because of multiple schemas)
        // What are the entities for UDS?  LabVessel?  What if the PDO has just been placed?  What about chip wells, which don't have a LabVessel (only a position)
        // Navigate from PDO to bucket entry, to ponds or chip wells (unlikely to have both for one PDO, but could for RP or SM-ID).  Filter out duplicates as appropriate.
        SearchInstance searchInstance = new SearchInstance();
        String entity = ColumnEntity.LAB_VESSEL.getEntityName();
        ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity(entity);

        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("Data Delivery PDO", configurableSearchDef);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("PDO-9246")); // PDO term alone will give bucket entries, need traverser
        // Yield chip wells and ponds (or specify products in advance?)
        // Filter duplicates
        // Fetch metrics
        // Sort?
        // Page
        // Choose / exclude from delivery (some may not have metrics yet)
        // Refresh to reflect new duplicates / metrics / versions?
    }
}
