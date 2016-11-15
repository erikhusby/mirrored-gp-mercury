package org.broadinstitute.gpinformatics.athena.control.dao.infrastructure;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.PublicMessage;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import java.util.List;

public class SAPAccessControlDao extends GenericDao {
    public SAPAccessControl getAccessControl() {
        List<SAPAccessControl> accessControls = findAll(SAPAccessControl.class);
        if (accessControls.isEmpty()){
            return null;
        }
        return CollectionUtils.extractSingleton(accessControls);
    }
}
