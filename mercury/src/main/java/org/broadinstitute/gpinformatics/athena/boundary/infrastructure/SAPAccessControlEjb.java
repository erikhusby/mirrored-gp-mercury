package org.broadinstitute.gpinformatics.athena.boundary.infrastructure;

import org.broadinstitute.gpinformatics.athena.control.dao.infrastructure.SAPAccessControlDao;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessStatus;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class SAPAccessControlEjb {
    private SAPAccessControlDao accessControlDao;

    @Inject
    public SAPAccessControlEjb(SAPAccessControlDao accessControlDao) {
        this.accessControlDao = accessControlDao;
    }

    public SAPAccessControlEjb() {
    }

    public SAPAccessControl getCurrentControlDefinitions() {
        SAPAccessControl accessControl = accessControlDao.getAccessControl();
        if(accessControl == null) {
            accessControl = new SAPAccessControl();
            accessControlDao.persist(accessControl);
        }
        return accessControl;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public SAPAccessControl resetControlDefinitions() {
        SAPAccessControl control = getCurrentControlDefinitions();
        control.setAccessStatus(AccessStatus.ENABLED);
        control.setDisabledFeatures(Collections.<String>emptySet());

        return control;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public SAPAccessControl setDefinitions(AccessStatus status, Set<String> restrictions) {

        SAPAccessControl accessController = getCurrentControlDefinitions();

        accessController.setAccessStatus(status);
        accessController.setDisabledFeatures(restrictions);

        return accessController;
    }
}
