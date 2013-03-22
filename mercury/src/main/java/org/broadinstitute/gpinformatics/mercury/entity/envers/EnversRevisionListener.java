package org.broadinstitute.gpinformatics.mercury.entity.envers;

import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.hibernate.envers.RevisionListener;

/**
 * Called by Envers when it needs to create a new RevInfo entity
 */
public class EnversRevisionListener implements RevisionListener {

    public EnversRevisionListener() {
    }

    /**
     * Sets the username, if any, in RevInfo
     * @param revisionEntity RevInfo object
     */
    @Override
    public void newRevision(Object revisionEntity) {
        RevInfo revInfo = (RevInfo) revisionEntity;
        UserBean userBean = null;
        try {
            userBean = ServiceAccessUtility.getBean(UserBean.class);
        } catch (Exception e) {
            // An exception can be thrown if a database operation occurs inside a JMS message handler, and
            // Envers is called to write the audit entry.  In this case, the username would be empty anyway.
        }
        if (userBean != null && userBean.getLoginUserName() != null) {
            revInfo.setUsername(userBean.getLoginUserName());
        }
    }
}
