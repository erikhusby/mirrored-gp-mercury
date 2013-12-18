package org.broadinstitute.gpinformatics.infrastructure.jpa;

import javax.persistence.Query;
import java.util.Collection;

/**
 * The in clause creator is used by the splitter to set up the criteria needed for each IN query used by the
 * JPASplitter. Since criteria need to be set up each time the query is run (unlike SQL and JQL which can
 * bind repeatedly with new data), this is essentially a callback to create the criteria.
 */
public interface CriteriaInClauseCreator<SPLIT_DATA_TYPE> {
    public Query createCriteriaInQuery(Collection<SPLIT_DATA_TYPE> parameterList);
}
