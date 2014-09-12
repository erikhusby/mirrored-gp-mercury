package org.broadinstitute.gpinformatics.infrastructure.jpa;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.UpdateData;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.util.Date;

/**
 * Interceptor to automatically set/update the createdBy, modifiedBy, createdDate, and modifiedDate properties for
 * entities annotated with @EntityListeners(UpdatedEntityInterceptor.class) and implementing {@code HasUpdateData}.
 */
public class UpdatedEntityInterceptor {

    private static final Log logger = LogFactory.getLog(UpdatedEntityInterceptor.class);

    @PreUpdate
    @PrePersist
    public void preUpdateOrPersist(Object object) {
        if (OrmUtil.proxySafeIsInstance(object, HasUpdateData.class)) {
            HasUpdateData objectWithUpdateData = OrmUtil.proxySafeCast(object, HasUpdateData.class);
            UpdateData updateData = objectWithUpdateData.getUpdateData();

            Date now = new Date();
            updateData.setModifiedDate(now);
            if (updateData.getCreatedDate() == null) {
                updateData.setCreatedDate(now);
            }

            UserBean userBean = ServiceAccessUtility.getBean(UserBean.class);

            if (userBean == null) {
                logger.info("Unable to determine the current user because User bean is null");
                updateData.setModifiedBy(updateData.getCreatedBy());
            } else {
                BspUser bspUser = userBean.getBspUser();
                updateData.setModifiedBy(bspUser);

                if (updateData.getCreatedBy() == null) {
                    updateData.setCreatedBy(bspUser);
                }
            }
        }
    }
}
