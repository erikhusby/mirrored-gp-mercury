package org.broadinstitute.gpinformatics.infrastructure.jpa;

import java.util.Collection;

public interface Thing<QUERY_TYPE, PARAMETER_TYPE> {

    QUERY_TYPE invoke(Collection<PARAMETER_TYPE> parameters);
}
