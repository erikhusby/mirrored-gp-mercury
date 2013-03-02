package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.util.List;

/**
 * Token Input support for users.
 *
 * @author hrafal
 */
public class UserTokenInput extends TokenInput<BspUser> {

    @Inject
    private BSPUserList bspUserList;

    public UserTokenInput() {
    }

    @Override
    protected BspUser getById(String key) {
        long userId = Long.valueOf(key);
        return bspUserList.getById(userId);
    }

    public String getJsonString(String query) throws JSONException {
        List<BspUser> bspUsers = bspUserList.find(query);
        return createItemListString(bspUsers);
    }

    @Override
    protected JSONObject createAutocomplete(JSONArray itemList, BspUser bspUser) throws JSONException {
        String fullName = bspUser.getFirstName() + " " + bspUser.getLastName();
        JSONObject item = getJSONObject(String.valueOf(bspUser.getUserId()), fullName, false);
        item.put("email", bspUser.getEmail());
        item.put("username", bspUser.getUsername());
        itemList.put(item);
        return item;
    }
}
