package org.broadinstitute.gpinformatics.infrastructure.columns;

import java.util.List;

/**
 * The contents of Configurable Lists are typically driven by object graph navigation
 * expressions. This interface allows existing or complex Java code to be used in place of
 * expressions.
 */
public interface ListPlugin {
    /**
     * Defines a mechanism for adding data to a configurable list
     *
     * @param entityList  list of entities for which to return data
     * @param headerGroup list of headers; the plugin must re-use any existing headers
     *                    that are passed in (each cell has a reference to a header), and may add
     *                    new ones
     * @return list of rows of cells, with each cell having a reference to a header
     */
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup);
}
