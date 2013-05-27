package org.broadinstitute.gpinformatics.infrastructure.jpa;

/**
 * This interface is for specifying what a business object needs when having a business key and name.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public interface BusinessObject<T> extends Nameable {
    String getBusinessKey();
}