package org.broadinstitute.gpinformatics.infrastructure.jpa;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
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

    /**
     * Update the created/last updated data within the {@code UpdateData} of an object implementing
     * {@code HasUpdateData} and annotated with {@code @EntityListeners(UpdatedEntityInterceptor.class)}.
     */
    @PreUpdate
    @PrePersist
    public void preUpdateOrPersist(Object object) {
        if (OrmUtil.proxySafeIsInstance(object, Updatable.class)) {
            Updatable updatable = OrmUtil.proxySafeCast(object, Updatable.class);
            UpdateData updateData = updatable.getUpdateData();

            Date now = new Date();
            updateData.setModifiedDate(now);
            if (updateData.getCreatedDate() == null) {
                updateData.setCreatedDate(now);
            }

            UserBean userBean = ServiceAccessUtility.getBean(UserBean.class);

            if (userBean == null) {
                logger.error("Unable to determine the current user because User bean is null");
                throw new InformaticsServiceException("Unable to determine an existing user to record who " +
                                                      "is modifying data");
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
