package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Token Input support for Research Projects.
 *
 * @author hrafal
 */
public class OrganismTokenInput extends TokenInput<String> {

    public OrganismTokenInput() {
        super(SINGLE_LINE_FORMAT);
    }

    @Override
    protected String getById(String key) {
        return key;
    }

    /**
     * Get a list of all research projects.
     *
     * @param query the search text string
     * @return list of research projects
     * @throws org.json.JSONException
     */
    public String getJsonString(String query) throws JSONException {
        return getJsonString(query, null);
    }

    /**
     * Get the list of all research projects minus the list of projects to omit.
     *
     * @param query the search text string
     * @param omitProjects list of projects to remove from the returned project list
     * @return list of research projects
     * @throws org.json.JSONException
     */
    public String getJsonString(String query, SampleCollection collection) throws JSONException {
        return createItemListString(new ArrayList<>(collection.getOrganisms()));
    }

    @Override
    protected String getTokenId(String organism) {
        return organism;
    }

    @Override
    protected String getTokenName(String organism) {
        return organism;
    }

    @Override
    protected String formatMessage(String messageString, String organism) {
        return MessageFormat.format(messageString, organism);
    }

    public String getOrganism() {
        List<String> organisms = getTokenObjects();

        if ((organisms == null) || organisms.isEmpty()) {
            return "";
        }

        return organisms.get(0);
    }
}
