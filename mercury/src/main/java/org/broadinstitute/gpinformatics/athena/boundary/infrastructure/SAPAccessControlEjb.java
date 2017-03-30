package org.broadinstitute.gpinformatics.athena.boundary.infrastructure;

import org.broadinstitute.gpinformatics.athena.control.dao.infrastructure.SAPAccessControlDao;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessItem;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessStatus;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
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
    public SAPAccessControl resetControlDefinitionItems() {
        SAPAccessControl control = getCurrentControlDefinitions();
        control.setAccessStatus(AccessStatus.ENABLED);
        control.setDisabledItems(Collections.<AccessItem>emptySet());

        return control;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public SAPAccessControl setDefinitionItems(AccessStatus status, Set<String> restrictions) {

        SAPAccessControl accessController = getCurrentControlDefinitions();

        Set<AccessItem> newItems = new HashSet<>();

        for (String restriction : restrictions) {
            newItems.add(new AccessItem(restriction));
        }

        accessController.setAccessStatus(status);
        accessController.setDisabledItems(newItems);

        return accessController;
    }
}
