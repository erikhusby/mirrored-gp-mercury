package org.broadinstitute.gpinformatics.mercury.control.dao.envers;

import org.broadinstitute.gpinformatics.mercury.entity.envers.RevInfo;
import org.hibernate.envers.RevisionType;

/**
 * Represents the "Envers triple" returned by AuditReader.forRevisionsOfEntity().
 *
 * @param <T> the class of the entity in the forRevisionsOfEntity query.
 */
public class EnversAudit<T> {
    private T entity;
    private RevInfo revInfo;
    private RevisionType revType;

    public EnversAudit(Object entity, RevInfo revInfo, RevisionType revType) {
        this.entity = (T) entity;
        this.revInfo = revInfo;
        this.revType = revType;
    }

    public T getEntity() {
        return entity;
    }

    public RevInfo getRevInfo() {
        return revInfo;
    }

    public RevisionType getRevType() {
        return revType;
    }
}
