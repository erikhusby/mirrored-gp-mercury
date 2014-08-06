package org.broadinstitute.gpinformatics.mercury.presentation.audit;

public class AuditTrailEntityTypeDto {
    private Long revId;
    private String entityClassname;
    private String entityDisplayName;

    public AuditTrailEntityTypeDto(Long revId, String entityClassname) {
        this.revId = revId;
        this.entityClassname = entityClassname;
        // Display name is what is after the last '.' in the classname.
        this.entityDisplayName = entityClassname.substring(entityClassname.lastIndexOf('.'));
    }

    public Long getRevId() {
        return revId;
    }

    public String getEntityClassname() {
        return entityClassname;
    }

    public String getEntityDisplayName() {
        return entityDisplayName;
    }
}
