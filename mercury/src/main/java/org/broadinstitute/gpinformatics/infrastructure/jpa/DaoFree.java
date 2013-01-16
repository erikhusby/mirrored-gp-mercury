package org.broadinstitute.gpinformatics.infrastructure.jpa;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that a method does not use Data Access Objects.  See the Confluence page
 * "Using Hibernate (JPA) as its author intended" for a description of transparent persistence.  A request (browser or
 * web service) will typically contain some IDs that need to be looked up in the database through DAOs, i.e. converted
 * from IDs to entities.  After these lookups, the remainder of the logic should be able to operate on entity graphs
 * without further DAO calls (traversing these graphs may trigger lazy fetches).  This logic can therefore be in
 * DAO-free methods, which take entities as parameters.  DAO-free methods are easily testable without a database, just
 * pass in a graph of entities constructed with the new operator.  For end-to-end testing, multiple DAO-free methods
 * can be called in sequence, without complex database setup, and without mock DAOs.  This can give high test coverage
 * with very fast test run times.
 * When a developer sees a method with this annotation, he or she must not add DAO calls, as this will decrease
 * testability.
 */
@Documented
@Target(ElementType.METHOD)
public @interface DaoFree {
}
