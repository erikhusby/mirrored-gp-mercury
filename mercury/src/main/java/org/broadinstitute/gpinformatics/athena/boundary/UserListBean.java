package org.broadinstitute.gpinformatics.athena.boundary;

import clover.org.apache.commons.lang.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Could not seem to inject the BSPUserList directly into the xhtml file, so this is a wrapper that does
 * the injection and provides the access to the find for anything that wants user names instead of the stored
 * ID.
 */
@Named
@RequestScoped
public class UserListBean {

    @Inject
    private BSPUserList userList;

    public String getUser(Long userId) {
        String fullName = "";
        if (userId != null) {
            BspUser bspUser = userList.getById(userId);
            return bspUser.getFirstName() + " " + bspUser.getLastName();
        }

        return fullName;
    }

    public BspUser[] getUsers(Long[] userIds) {
        if (userIds == null) {
            return new BspUser[0];
        } else {
            BspUser[] users = new BspUser[userIds.length];
            int i=0;
            for (Long userId : userIds) {
                users[i++] = userList.getById(userId);
            }

            return users;
        }
    }

    public String getUserListString(Long[] userIds) {
        String userListString = "";

        if (userIds != null) {
            String[] nameList = new String[userIds.length];
            int i=0;
            for (Long userId : userIds) {
                nameList[i++] = getUser(userId);
            }

            userListString = StringUtils.join(nameList, ", ");
        }

        return userListString;
    }
}

