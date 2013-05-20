package org.broadinstitute.gpinformatics.infrastructure.jpa;

/**
 * This class is for specifying what an object needs to be identified as having a business key and can be named.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public interface BusinessKeyable<T> extends Nameable {
    String getBusinessKey();
}
