package org.broadinstitute.gpinformatics.infrastructure.jpa;

import org.broadinstitute.gpinformatics.mercury.entity.UpdateData;

/**
 * Marks an entity as having data which the {@code UpdatedEntityInterceptor} can automatically updated on persist
 * or update.  Entities will also have to be annotated as @EntityListeners(UpdatedEntityInterceptor.class).
 */
public interface Updatable {

    /**
     * Return the {@code UpdateData} for this object.
     */
    UpdateData getUpdateData();
}
