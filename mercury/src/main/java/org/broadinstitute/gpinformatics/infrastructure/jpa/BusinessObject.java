package org.broadinstitute.gpinformatics.infrastructure.jpa;

/**
 * Objects that implement this interface have a name and a business key..
 */
public interface BusinessObject extends Nameable {
    String getBusinessKey();
}