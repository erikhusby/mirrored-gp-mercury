package org.broadinstitute.gpinformatics.athena.boundary.infrastructure;

import org.broadinstitute.gpinformatics.athena.control.dao.infrastructure.SAPAccessControlDao;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessStatus;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.util.Collections;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Singleton
@Startup
public class SAPAccessControlEjb {
    private SAPAccessControlDao accessControlDao;

    @Inject
    public SAPAccessControlEjb(SAPAccessControlDao accessControlDao) {
        this.accessControlDao = accessControlDao;
    }

    public SAPAccessControlEjb() {
    }

    public SAPAccessControl getCurrentControlDefinitions() {
        return accessControlDao.getAccessControl();
    }

    public SAPAccessControl resetControlDefinitions() {
        SAPAccessControl control = accessControlDao.getAccessControl();
        control.setAccessStatus(AccessStatus.ENABLED);
        control.setDisabledFeatures(Collections.<String>emptySet());

        return control;
    }
}
