package org.broadinstitute.gpinformatics.infrastructure;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This provides data for jquery.tokeninput so we can populate it
 */
public class AutoCompleteToken {
    private String id;
    private String name;
    private boolean readonly;

    public AutoCompleteToken(String id, String name, boolean readonly) {
        this.id = id;
        this.name = name;
        this.readonly = readonly;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public JSONObject getJSONObject() throws JSONException {
        JSONObject item = new JSONObject();
        item.put("id", id);
        item.put("name", name); // product name
        item.put("readonly", Boolean.valueOf(readonly));
        return item;
    }
}
