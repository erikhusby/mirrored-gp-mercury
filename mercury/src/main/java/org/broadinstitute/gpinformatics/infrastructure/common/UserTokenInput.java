package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

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
}
