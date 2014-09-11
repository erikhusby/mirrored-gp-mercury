package org.broadinstitute.gpinformatics.infrastructure.jpa;

import org.broadinstitute.bsp.client.users.BspUser;

import javax.persistence.EntityListeners;
import java.util.Date;

/**
 * interface to define fields for tracking changes to entities
 */
public interface Updatable {
    public void setModifiedDate(Date date);

    Long getCreatedBy();

    Long getModifiedBy();

    public void setModifiedBy(BspUser user);

    Date getModifiedDate();

    Date getCreatedDate();

    void setCreatedBy(BspUser createdBy);

    void setCreatedDate(Date createdDate);

    void setModifiedBy(Long modifiedUserId);
}
