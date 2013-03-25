package org.broadinstitute.gpinformatics.infrastructure.jpa;

import org.hibernate.envers.query.AuditQuery;

public interface AuditQueryThing<PARAMETER_TYPE> extends Thing<AuditQuery, PARAMETER_TYPE> {
}
