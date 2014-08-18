package org.broadinstitute.gpinformatics.mercury.presentation.audit;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.EntityField;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.EnversAudit;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.ReflectionUtil;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.hibernate.envers.RevisionType;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@UrlBinding(AuditTrailActionBean.AUDIT_TRAIL_ENTRY_ACTIONBEAN_URL)
public class AuditTrailEntryActionBean extends CoreActionBean {

    @Inject
    private AuditReaderDao auditReaderDao;

    @Validate(required = true, minvalue = 1)
    private long revId;

    @Validate(required = true, on = {AuditTrailActionBean.VIEW_AUDIT_TRAIL_ENTRIES})
    private String displayClassname;

    @Validate(required = true, on = {AuditTrailActionBean.VIEW_ENTITY})
    private String canonicalClassname;

    private Class entityClass;

    /** auditTrailEntries list is only used with the audit trail entry page. */
    private List<AuditTrailEntry> auditTrailEntries = new ArrayList<>();

    /** Entity id and auditEntity are only used with the audited entity page. */
    @Validate(required = true, minvalue = 1, on = {AuditTrailActionBean.VIEW_ENTITY})
    private long entityId;

    private AuditEntity auditEntity;

    public long getRevId() {
        return revId;
    }

    public void setRevId(long revId) {
        this.revId = revId;
    }

    public String getDisplayClassname() {
        return displayClassname;
    }

    public void setDisplayClassname(String displayClassname) {
        this.displayClassname = displayClassname;
        if (StringUtils.isNotBlank(displayClassname)) {
            setCanonicalClassname(AuditTrailActionBean.getDisplayToCanonicalClassname(displayClassname));
        }
    }

    public void setCanonicalClassname(String canonicalClassname) {
        this.canonicalClassname = canonicalClassname;
        if (StringUtils.isNotBlank(canonicalClassname)) {
            if (StringUtils.isBlank(displayClassname)) {
                displayClassname = AuditTrailActionBean.getCanonicalToDisplayClassname(canonicalClassname);
            }
            try {
                entityClass = getClass().getClassLoader().loadClass(canonicalClassname);
            } catch (Exception e) {
                entityClass = null;
            }
        }
    }

    public List<AuditTrailEntry> getAuditTrailEntries() {
        return auditTrailEntries;
    }

    public long getEntityId() {
        return entityId;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    public AuditEntity getAuditEntity() {
        return auditEntity;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    // Default is meaningless, so redirects back to the audit trail list page.
    @DefaultHandler
    public Resolution defaultHandler() {
        return new ForwardResolution(AuditTrailActionBean.AUDIT_TRAIL_LISTING_PAGE);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @HandlesEvent(AuditTrailActionBean.VIEW_AUDIT_TRAIL_ENTRIES)
    public Resolution listEntries() {
        generateAuditTrailEntryList();
        return new ForwardResolution(AuditTrailActionBean.AUDIT_TRAIL_ENTRY_PAGE);
    }

    private void generateAuditTrailEntryList() {
        // For each instance of entity type modified at this revId, generate a previous and current AuditEntity pair
        // showing the differences.
        for (EnversAudit enversAudit : auditReaderDao.fetchEnversAudits(Collections.singleton(revId), entityClass)) {
            Object instanceEntity = enversAudit.getEntity();
            Long instanceEntityId = ReflectionUtil.getEntityId(instanceEntity, entityClass);
            // Previous entity revId will be null for an entity addition.
            Long prevEntityRevId = auditReaderDao.getPreviousVersionRevId(instanceEntityId, entityClass, revId);
            Object prevEntity = (prevEntityRevId != null) ?
                    auditReaderDao.getEntityAtVersion(instanceEntityId, entityClass, prevEntityRevId) : null;

            // Generates a pair of AuditEntity that represent the difference between the two entity versions.
            // RevId, entityId, and the differences are the only fields in the result.

            List<EntityField> prevFields = (enversAudit.getRevType() != RevisionType.ADD) ?
                    ReflectionUtil.formatFields(prevEntity, entityClass) : null;

            List<EntityField> curFields = (enversAudit.getRevType() != RevisionType.DEL) ?
                    ReflectionUtil.formatFields(instanceEntity, entityClass) : null;

            List<String> fieldNames = getFieldDiffs(prevFields, curFields);

            AuditEntity prev = new AuditEntity(prevEntityRevId, displayClassname, instanceEntityId, prevFields);
            AuditEntity cur = new AuditEntity(revId, displayClassname, instanceEntityId, curFields);

            auditTrailEntries.add(new AuditTrailEntry(fieldNames, prev, cur));
        }
    }

    // Finds fields that are different from previous entity.  If same, removes the field from
    // both prev and cur lists.  Always keeps the entity id.
    // Returns a list of field names that are in the prevFields and/or curFields
    private List<String> getFieldDiffs(List<EntityField> prevFields, List<EntityField> curFields) {
        List<String> names = new ArrayList<>();
        if (prevFields != null && curFields != null) {
            // Since EntityFields were obtained from one audit table they will always have the same
            // column names, and should have been sorted by name, except for entity id which should
            // be in the first field.  Entity id is always equal but we want it to be present in the
            // list of fields, so it is excluded from being tested.
            // Uses reverse iteration so that List.remove(index) works.
            for (int i = prevFields.size() - 1; i > 0; --i) {
                if (equals(prevFields.get(i), curFields.get(i))) {
                    prevFields.remove(i);
                    curFields.remove(i);
                }
            }
            for (EntityField entityField : prevFields) {
                names.add(entityField.getFieldName());
            }
        }
        if (prevFields != null && curFields == null) {
            for (EntityField entityField : prevFields) {
                names.add(entityField.getFieldName());
            }
        }
        if (prevFields == null && curFields != null) {
            for (EntityField entityField : curFields) {
                names.add(entityField.getFieldName());
            }
        }
        return names;
    }

    // Returns true if the two EntityFields have identical content.
    private boolean equals(EntityField f1, EntityField f2) {
        if (f1.getFieldName() != f2.getFieldName()) {
            throw new RuntimeException("Found field " + f2.getFieldName() + " instead of field " + f1.getFieldName() +
                                       " in version " + revId + " of class " + displayClassname);
        }
        // Tests the single reference/value.
        if (f1.getValue() == null && f2.getValue() != null || f1.getValue() != null && f2.getValue() == null) {
            return false;
        }
        if (f1.getValue() != null && !f1.getValue().equals(f2.getValue())) {
            return false;
        }
        // Tests the list.  It is expected to be already sorted.
        String valueList1 = (f1.getEntityFieldList() != null) ? StringUtils.join(f1.getEntityFieldList(), ",") : "";
        String valueList2 = (f2.getEntityFieldList() != null) ? StringUtils.join(f2.getEntityFieldList(), ",") : "";
        if (!valueList1.equals(valueList2)) {
            return false;
        }
        // Tests the map.  It is expected to be already sorted.
        String valueMap1 = (f1.getEntityFieldMap() != null) ?
                StringUtils.join(f1.getEntityFieldMap().entrySet(), ",") : "";
        String valueMap2 = (f2.getEntityFieldMap() != null) ?
                StringUtils.join(f2.getEntityFieldMap().entrySet(), ",") : "";
        if (!valueMap1.equals(valueMap2)) {
            return false;
        }

        return true;
    }

    @ValidationMethod(on = {AuditTrailActionBean.VIEW_AUDIT_TRAIL_ENTRIES, AuditTrailActionBean.VIEW_ENTITY})
    public void validateSelection() {
        if (displayClassname == null) {
            addGlobalValidationError("Display classname is null");
        }
        if (canonicalClassname == null) {
            addGlobalValidationError(MessageFormat.format("Unknown canonical classname for display classname %s",
                    displayClassname));
        }
        if (entityClass == null) {
            addGlobalValidationError(MessageFormat.format("Cannot load classname %s", canonicalClassname));
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @HandlesEvent(AuditTrailActionBean.VIEW_ENTITY)
    public Resolution viewAuditedEntity() {
        generateAuditedEntity();
        return new ForwardResolution(AuditTrailActionBean.AUDITED_ENTITY_PAGE);
    }

    private void generateAuditedEntity() {
        // Gets entity from Envers.
        Object entity = auditReaderDao.getEntityAtVersion(entityId, entityClass, revId);
        List<EntityField> fields = (entity != null) ? ReflectionUtil.formatFields(entity, entityClass) : null;
        auditEntity = new AuditEntity(revId, displayClassname, entityId, fields);
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////
}
