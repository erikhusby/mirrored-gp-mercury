package org.broadinstitute.gpinformatics.infrastructure.jpa;

import javax.persistence.Query;
import java.util.Collection;

/**
 * This class is...
 *
 * @author hrafal
 */
public interface CriteriaInClauseCreator<SPLIT_DATA_TYPE> {
    public Query createCriteriaInQuery(Collection<SPLIT_DATA_TYPE> parameterList);
}
