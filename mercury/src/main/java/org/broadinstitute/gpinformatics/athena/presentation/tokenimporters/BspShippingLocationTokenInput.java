package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// TODO: Integrate with Phis S.'s work to search for shipping locations in BSP.
public class BspShippingLocationTokenInput extends TokenInput<String> {

    public BspShippingLocationTokenInput() {
        super(DOUBLE_LINE_FORMAT);
    }

    public String getJsonString(String query) throws JSONException {
        JSONArray itemList = new JSONArray();
        JSONObject item = getJSONObject(getTokenId(query), getTokenName(query));
        item.put("dropdownItem", query);
        itemList.put(item);
        return itemList.toString();
    }

    @Override
    protected String getTokenId(String s) {
        return s;
    }

    @Override
    protected String getTokenName(String s) {
        return s;
    }

    @Override
    protected String formatMessage(String messageString, String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected String getById(String key) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
