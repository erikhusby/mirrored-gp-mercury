package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
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
        return MessageFormat.format(
            messageString, bspUser.getFullName(), bspUser.getUsername() + " " + bspUser.getEmail());
    }

    @Override
    protected String getTokenName(BspUser bspUser) {
        return bspUser.getFullName();
    }

    public List<Long> getOwnerIds() {
        List<BspUser> users = getTokenObjects();

        List<Long> businessKeyList = new ArrayList<>();
        for (BspUser user : users) {
            businessKeyList.add(user.getUserId());
        }

        return businessKeyList;
    }

    public List<String> getBusinessKeyList() {
        List<BspUser> users = getTokenObjects();

        List<String> businessKeyList = new ArrayList<>();
        for (BspUser user : users) {
            businessKeyList.add(user.getUserId().toString());
        }

        return businessKeyList;
    }

    // Get comma separated list of e-mails from notificationList.
    public String getEmailList() {
        List<String> notificationList = new ArrayList<>();
        for (BspUser user : getTokenObjects()) {
            notificationList.add(user.getEmail());
        }

        return StringUtils.join(notificationList, ", ");
    }
}
