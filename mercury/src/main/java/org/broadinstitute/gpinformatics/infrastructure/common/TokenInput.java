package org.broadinstitute.gpinformatics.infrastructure.common;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class handles the support for autocomplete widgets in Mercury. There is a fair amount of boiler plate code
 * needed to make the tokens work and this class handles that.
 *
 * @author hrafal
 */
public abstract class TokenInput<TOKEN_OBJECT> {

    // The UI will have a comma separated list of keys that needs to be
    // parsed out for turning into real TOKEN_OBJECTs
    private String listOfKeys = "";

    // The UI needs to get at these, so they should be public, but mostly they are protected
    public void setListOfKeys(String listOfKeys) {
        this.listOfKeys = listOfKeys;
    }

    public String getListOfKeys() {
        return listOfKeys;
    }

    public List<TOKEN_OBJECT> getTokenObjects() {
        if (StringUtils.isBlank(listOfKeys)) {
            return Collections.emptyList();
        }

        String[] keys = listOfKeys.split(",");

        List<TOKEN_OBJECT> tokenObjects = new ArrayList<TOKEN_OBJECT>();
        for (String key : keys) {
            tokenObjects.add(getById(key));
        }

        return tokenObjects;
    }

    protected abstract TOKEN_OBJECT getById(String key);
}
