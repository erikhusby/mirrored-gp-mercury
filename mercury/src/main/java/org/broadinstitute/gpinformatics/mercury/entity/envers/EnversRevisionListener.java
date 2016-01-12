package org.broadinstitute.gpinformatics.mercury.entity.envers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.hibernate.envers.RevisionListener;

/**
 * Called by Envers when it needs to create a new RevInfo entity
 */
public class EnversRevisionListener implements RevisionListener {

    private static final Log log = LogFactory.getLog(EnversRevisionListener.class);

    public EnversRevisionListener() {
    }

    /**
     * Sets the username, if any, in RevInfo
     * @param revisionEntity RevInfo object
     */
    @Override
    public void newRevision(Object revisionEntity) {
        RevInfo revInfo = (RevInfo) revisionEntity;
        try {
            UserBean userBean = ServiceAccessUtility.getBean(UserBean.class);
            if (userBean != null && userBean.getLoginUserName() != null) {
                revInfo.setUsername(userBean.getLoginUserName());
            }
        } catch (Exception e) {
            // An exception can be thrown if a database operation occurs inside a JMS message handler, and
            // Envers is called to write the audit entry.  In this case, the username would be empty anyway.
            // This can also occur if we modify the db inside a scheduled task, since there's no session scope
            // in that case either. For some reason, inside a JMS message handler the JNDI call fails, and inside
            // a scheduled task a UserBean is returned but calling into it results in a WELD exception.
            //
            // The best fix would be to somehow check up front if we're currently in session scope before trying
            // to look up UserBean at all.
            log.debug("Could not determine user for revision: " + revInfo.getRevInfoId(), e);
        }
    }
}
