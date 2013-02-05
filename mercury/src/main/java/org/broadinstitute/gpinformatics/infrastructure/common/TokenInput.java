package org.broadinstitute.gpinformatics.infrastructure.common;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

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

    /** A cache of the key list as a list of tokens. */
    private List<TOKEN_OBJECT> tokenObjects = Collections.emptyList();

    /** A cache of the result of calling generateCompleteData. */
    private String completeDataCache;

    // The UI needs to get at these so they must be public.
    public void setListOfKeys(String listOfKeys) {
        this.listOfKeys = listOfKeys;
        if (StringUtils.isBlank(listOfKeys)) {
            tokenObjects = Collections.emptyList();
        } else {
            String[] keys = listOfKeys.split(getTokenSeparator());

            tokenObjects = new ArrayList<TOKEN_OBJECT>(keys.length);
            for (String key : keys) {
                tokenObjects.add(getById(key.trim()));
            }
        }
    }

    protected String getTokenSeparator() {
        return ",";
    }

    protected String getJoinSeparator() {
        return ",";
    }

    public String getListOfKeys() {
        return listOfKeys;
    }

    public List<TOKEN_OBJECT> getTokenObjects() {
        return tokenObjects;
    }

    /**
     * Generate the completion data for this TokenInput.  Only called by TokenInput itself.  Subclasses should return
     * the empty string, not null, for cases where no completions exist.
     * * @return the completion data
     * @throws JSONException if an error occurs
     */
    protected abstract String generateCompleteData() throws JSONException;

    public final String getCompleteData() throws JSONException {
        if (completeDataCache == null) {
            completeDataCache = generateCompleteData();
        }
        return completeDataCache;
    }

    protected abstract TOKEN_OBJECT getById(String key);

    public void setup(Long... longIds) {
        setListOfKeys(StringUtils.join(longIds, getJoinSeparator()));
    }

    public void setup(String... ids) {
        setListOfKeys(StringUtils.join(ids, getJoinSeparator()));
    }

    /**
     * Given the arguments, create a JSON object that represents it.  This is used to create the response for a
     * JQeury Tokeninput auto-complete UI.
     * @param id the ID
     * @param name the name
     * @param readonly true if readonly
     * @return the JSON object that contains these fields.
     * @throws JSONException
     */
    public static JSONObject getJSONObject(String id, String name, boolean readonly) throws JSONException {
        JSONObject item = new JSONObject();
        item.put("id", id);
        item.put("name", name);
        item.put("readonly", readonly);
        return item;
    }
}
