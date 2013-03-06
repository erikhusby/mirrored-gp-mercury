package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;

import javax.inject.Inject;
import java.text.MessageFormat;
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
    protected boolean isSingleLineMenuEntry() {
        return false;
    }

    @Override
    protected String getTokenId(BspUser bspUser) {
        return String.valueOf(bspUser.getUserId());
    }

    @Override
    protected String formatMessage(String messageString, BspUser bspUser) {
        return MessageFormat.format(
            messageString, bspUser.getFirstName() + " " + bspUser.getLastName(),
                           bspUser.getUsername() + " " + bspUser.getEmail());
    }

    @Override
    protected String getTokenName(BspUser bspUser) {
        return bspUser.getFirstName() + " " + bspUser.getLastName();
    }
}
