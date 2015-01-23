package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Configurable search definitions for various entities.
 */
public class SearchDefinitionFactory {

    // State of ConfigurableSearchDefinition does not change once created.
    private static Map<String, ConfigurableSearchDefinition> MAP_NAME_TO_DEF = new HashMap<>();

    private SearchDefinitionFactory(){}

    static {
        SearchDefinitionFactory fact = new SearchDefinitionFactory();
        fact.buildLabEventSearchDef();
        fact.buildLabVesselSearchDef();
        fact.buildMercurySampleSearchDef();
    }

    public static ConfigurableSearchDefinition getForEntity(String entity) {
        /* **** Change condition to true during development to rebuild for JVM hot-swap changes **** */
        if( true ) {
            SearchDefinitionFactory fact = new SearchDefinitionFactory();
            fact.buildLabEventSearchDef();
            fact.buildLabVesselSearchDef();
            fact.buildMercurySampleSearchDef();
        }

        return MAP_NAME_TO_DEF.get(entity);
    }

    private void buildLabVesselSearchDef() {
        ConfigurableSearchDefinition configurableSearchDefinition
                = new LabVesselSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.LAB_VESSEL.getEntityName(), configurableSearchDefinition);
    }

    private void buildLabEventSearchDef() {
        ConfigurableSearchDefinition configurableSearchDefinition
                = new LabEventSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.LAB_EVENT.getEntityName(), configurableSearchDefinition);
    }

    private void buildMercurySampleSearchDef() {
        ConfigurableSearchDefinition configurableSearchDefinition
                = new MercurySampleSearchDefinition().buildSearchDefinition();
        MAP_NAME_TO_DEF.put(ColumnEntity.MERCURY_SAMPLE.getEntityName(), configurableSearchDefinition);
    }

}
