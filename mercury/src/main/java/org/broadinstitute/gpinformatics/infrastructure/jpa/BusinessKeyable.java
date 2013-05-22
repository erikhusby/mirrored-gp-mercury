package org.broadinstitute.gpinformatics.infrastructure.jpa;

/**
 * This interface is for specifying what an object needs when having a business key and name.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public interface BusinessKeyable<T> extends Nameable {
    String getBusinessKey();
}