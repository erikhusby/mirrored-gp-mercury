package org.broadinstitute.gpinformatics.infrastructure.jpa;

import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;

/**
 * This interface is for allowing DAOs to have a common means for specifying the business key lookups.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public interface BusinessKeyFinder<T extends BusinessKeyable> {

    /**
     * Look up an object by its business key to uniquely identify it.
     *
     * @param businessKey the business key for the object
     * @return object associated by the business key
     */
    public T findByBusinessKey(String businessKey);
}
