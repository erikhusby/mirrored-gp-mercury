package org.broadinstitute.gpinformatics.mercury.entity.envers;

import org.hibernate.envers.RevisionListener;

/**
 * Called by Envers when it needs to create a new RevInfo entity
 */
public class EnversRevisionListener implements RevisionListener {
    @Override
    public void newRevision(Object revisionEntity) {
        // todo jmt set username, JNDI to get Credentials?
    }
}
