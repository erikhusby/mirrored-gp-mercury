package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Token Input support for users.
 */
@Dependent
public class UserTokenInput extends TokenInput<BspUser> {
    private BSPUserList bspUserList;

    @Inject
    public UserTokenInput(BSPUserList bspUserList) {
        this();
        this.bspUserList = bspUserList;
    }

    public UserTokenInput() {
        super(DOUBLE_LINE_FORMAT);
    }

    @Override
    protected BspUser getById(String key) {
        long userId = Long.valueOf(key);
        return bspUserList.getById(userId);
    }

    public String getJsonString(String query) throws JSONException {
        return createItemListString(bspUserList.find(query));
    }

    @Override
    protected String getTokenId(BspUser bspUser) {
        return String.valueOf(bspUser.getUserId());
    }

    @Override
    protected String formatMessage(String messageString, BspUser bspUser) {
        return MessageFormat.format(messageString, bspUser.getFullName(),
                bspUser.getUsername() + " " + bspUser.getEmail());
    }

    @Override
    protected String getTokenName(BspUser bspUser) {
        return bspUser.getFullName();
    }

    public List<Long> getOwnerIds() {
        List<BspUser> users = getTokenObjects();

        List<Long> businessKeyList = new ArrayList<>(users.size());
        for (BspUser user : users) {
            businessKeyList.add(user.getUserId());
        }

        return businessKeyList;
    }
}
