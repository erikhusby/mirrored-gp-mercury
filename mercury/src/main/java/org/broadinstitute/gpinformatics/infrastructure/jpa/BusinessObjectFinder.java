package org.broadinstitute.gpinformatics.infrastructure.jpa;

/**
 * This interface is for allowing DAOs to have a common means for finding business objects by specifying the business
 * key lookups.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public interface BusinessObjectFinder<T extends BusinessObject> {
    /**
     * Look up an object by its business key to uniquely identify it.
     *
     * @param businessKey the business key for the object
     * @return object associated by the business key
     */
    public T findByBusinessKey(String businessKey);
}
