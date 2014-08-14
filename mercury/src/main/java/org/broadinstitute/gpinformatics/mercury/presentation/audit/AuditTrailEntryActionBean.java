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
    @Validate(required = true)
    private String displayClassname;
    private String canonicalClassname;
    private Class entityClass;

    /** auditTrailEntries list is only used with the audit trail entry page. */
    private List<AuditTrailEntry> auditTrailEntries = new ArrayList<>();

    /** Entity id and auditEntity are only used with the audited entity page. */
    @Validate(required = true, minvalue = 1, on = {AuditTrailActionBean.VIEW_ENTITY})
    private long entityId;
    @Validate(required = true, on = {AuditTrailActionBean.VIEW_ENTITY})
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
            canonicalClassname = AuditTrailActionBean.getDisplayToCanonicalClassname(displayClassname);
            if (StringUtils.isNotBlank(canonicalClassname)) {
                try {
                    entityClass = getClass().getClassLoader().loadClass(canonicalClassname);
                } catch (Exception e) {
                    entityClass = null;
                }
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
            generateAuditTrailEntry(enversAudit);
        }
    }

    private void generateAuditTrailEntry(EnversAudit enversAudit) {
        Object currentEntity = enversAudit.getEntity();
        // There are multiple entityIds processed for this page, so don't use the class variable.
        Long entryEntityId = ReflectionUtil.getEntityId(currentEntity, entityClass);
        // Previous entity revId will be null for an entity addition.
        Long prevEntityRevId = auditReaderDao.getPreviousVersionRevId(entryEntityId, entityClass, revId);
        Object prevEntity = (prevEntityRevId != null) ?
                auditReaderDao.getEntityAtVersion(entryEntityId, entityClass, prevEntityRevId) : null;

        // Generates a pair of AuditEntity that represent the difference between the two entity versions.
        // RevId, entityId, and the differences are the only fields in the result.

        List<EntityField> prevFields = (prevEntity != null) ?
                ReflectionUtil.formatFields(prevEntity, entityClass) : null;
        AuditEntity prev = new AuditEntity(prevEntityRevId, displayClassname, entryEntityId, prevFields);

        List<EntityField> curFields = (enversAudit.getRevType() != RevisionType.DEL) ?
                ReflectionUtil.formatFields(currentEntity, entityClass) : null;
        AuditEntity cur = new AuditEntity(revId, displayClassname, entryEntityId, curFields);

        auditTrailEntries.add(new AuditTrailEntry(fieldNames(prevFields, curFields), prev, cur));
    }

    // Returns a list of field names for each different field.
    private List<String> fieldNames(List<EntityField> prevFields, List<EntityField> curFields) {
        List<String> names = new ArrayList<>();
        if (prevFields != null && curFields != null) {
            for (int i = 0; i < prevFields.size(); ++i) {
                if (!matchFields(prevFields.get(i), curFields.get(i))) {
                    names.add(prevFields.get(i).getFieldName());
                }
            }
        }
        if (prevFields != null && curFields == null) {
            for (int i = 0; i < prevFields.size(); ++i) {
                names.add(prevFields.get(i).getFieldName());
            }
        }
        if (prevFields == null && curFields != null) {
            for (int i = 0; i < curFields.size(); ++i) {
                names.add(curFields.get(i).getFieldName());
            }
        }
        return names;
    }

    private boolean matchFields(EntityField f1, EntityField f2) {
        if (f1.getFieldName() != f2.getFieldName()) {
            throw new RuntimeException("Versioned field name " + f1.getFieldName() +
                                       " doesn't match other versioned field name " + f2.getFieldName());
        }
        if (f1.getValue() != f2.getValue()) {
            return false;
        }
        if (f1.getReferenceClassname() != null && !f1.getReferenceClassname().equals(f2.getReferenceClassname())) {
            return false;
        }
        if (f1.getReferenceClassname() == null && f2.getReferenceClassname() != null) {
            return false;
        }
        String valueList1 = (f1.getValueList() != null) ? StringUtils.join(f1.getValueList(), ",") : "";
        String valueList2 = (f2.getValueList() != null) ? StringUtils.join(f2.getValueList(), ",") : "";
        if (!valueList1.equals(valueList2)) {
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
        List<EntityField> fields = (entity != null) ?
                ReflectionUtil.formatFields(entity, entityClass) : null;
        auditEntity = new AuditEntity(revId, displayClassname, entityId, fields);
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////
}
