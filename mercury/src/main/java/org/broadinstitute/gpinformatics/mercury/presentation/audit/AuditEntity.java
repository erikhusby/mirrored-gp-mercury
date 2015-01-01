package org.broadinstitute.gpinformatics.mercury.presentation.audit;

import org.broadinstitute.gpinformatics.mercury.control.dao.envers.EntityField;

import java.util.List;

public class AuditEntity {
    private Long revId;
    private String displayClassname;
    private Long entityId;
    private List<EntityField> fields;

    public AuditEntity(Long revId, String displayClassname, Long entityId, List<EntityField> fields) {
        this.revId = revId;
        this.displayClassname = displayClassname;
        this.entityId = entityId;
        this.fields = fields;
    }

    public Long getRevId() {
        return revId;
    }

    public String getDisplayClassname() {
        return displayClassname;
    }

    public Long getEntityId() {
        return entityId;
    }

    public List<EntityField> getFields() {
        return fields;
    }
}
