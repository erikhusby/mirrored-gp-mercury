package org.broadinstitute.gpinformatics.mercury.presentation;

import org.broadinstitute.bsp.client.users.BspUser;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.io.Serializable;

/**
 * @author breilly
 */
@Named
@SessionScoped
public class UserBean implements Serializable {

    private BspUser bspUser;

    public BspUser getBspUser() {
        return bspUser;
    }

    public void setBspUser(BspUser bspUser) {
        this.bspUser = bspUser;
    }
}
