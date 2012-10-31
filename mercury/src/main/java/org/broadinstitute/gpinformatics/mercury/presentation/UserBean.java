package org.broadinstitute.gpinformatics.mercury.presentation;

import org.broadinstitute.bsp.client.users.BspUser;

import javax.annotation.Nullable;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.io.Serializable;

/**
 * @author breilly
 */
@Named
@SessionScoped
public class UserBean implements Serializable {

    @Nullable
    private BspUser bspUser;

    @Nullable
    public BspUser getBspUser() {
        return bspUser;
    }

    public void setBspUser(@Nullable BspUser bspUser) {
        this.bspUser = bspUser;
    }

    public String toString() {
        if (bspUser == null) {
            return "<unknown>";
        }
        return    bspUser.getUsername();
    }
}
