package org.broadinstitute.gpinformatics.infrastructure.jpa;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.util.Date;

/**
 * Entity to automatically set/update the createdBy, modifiedBy, createdDate, modifiedDate fields for entities
 * that implement the right interface
 */
public class UpdatedEntityInterceptor {

    private static final Log logger = LogFactory.getLog(UpdatedEntityInterceptor.class);

    @PreUpdate
    @PrePersist
    public void myPreSave(Object obj) {
        if (OrmUtil.proxySafeIsInstance(obj, Updateable.class)) {
            Updateable updateObj = OrmUtil.proxySafeCast(obj, Updateable.class);


            Date trackingDate = new Date();
            updateObj.setModifiedDate(trackingDate);
            if(updateObj.getCreatedDate() == null) {
                updateObj.setCreatedDate(trackingDate);
            }

            UserBean currentBean = ServiceAccessUtility.getBean(UserBean.class);

            if(currentBean == null) {
                logger.info("Unable to determine the current user because User bean is null");
                updateObj.setModifiedBy(updateObj.getCreatedBy());
            } else {
                BspUser bspUser = currentBean.getBspUser();
                updateObj.setModifiedBy(bspUser);

                if(updateObj.getCreatedBy() == null ) {
                    updateObj.setCreatedBy(bspUser);
                }
            }
        }
    }
}
