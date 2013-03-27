package org.broadinstitute.gpinformatics.infrastructure.jpa;

import javax.persistence.Query;

public interface QueryThing<PARAMETER_TYPE> extends Thing<Query, PARAMETER_TYPE> {
}
