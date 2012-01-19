package org.broadinstitute.sequel;

/**
 * Thrown when we think the molecular state
 * of some stuff in a container isn't what
 * the lab event expects as input.
 */
public class InvalidMolecularStateException extends Exception {

    public InvalidMolecularStateException(String message) {
        super(message);
    }

}
