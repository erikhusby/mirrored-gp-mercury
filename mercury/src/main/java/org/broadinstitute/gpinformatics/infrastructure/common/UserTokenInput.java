package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.AutoCompleteToken;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

/**
 * This class is the user implementation of the token object
 *
 * @author hrafal
 */
public class UserTokenInput extends TokenInput<BspUser> {

    private BSPUserList bspUserList;

    public UserTokenInput(BSPUserList bspUserList) {
        super();
        this.bspUserList = bspUserList;
    }

    @Override
    protected BspUser getById(String key) {
        long userId = Long.valueOf(key);
        return bspUserList.getById(userId);
    }

    public static String getJsonString(BSPUserList bspUserList, String query) throws JSONException {
        List<BspUser> bspUsers = bspUserList.find(query);

        JSONArray itemList = new JSONArray();
        for (BspUser bspUser : bspUsers) {
            createAutocomplete(itemList, bspUser);
        }

        return itemList.toString();
    }

    public static String getUserCompleteData(BSPUserList bspUserList, Long[] userIds) throws JSONException {

        JSONArray itemList = new JSONArray();
        for (Long userId : userIds) {
            BspUser bspUser = bspUserList.getById(userId);
            createAutocomplete(itemList, bspUser);
        }

        return itemList.toString();
    }

    private static void createAutocomplete(JSONArray itemList, BspUser bspUser) throws JSONException {
        String fullName = bspUser.getFirstName() + " " + bspUser.getLastName();
        itemList.put(new AutoCompleteToken(String.valueOf(bspUser.getUserId()), fullName, false).getJSONObject());
    }
}
