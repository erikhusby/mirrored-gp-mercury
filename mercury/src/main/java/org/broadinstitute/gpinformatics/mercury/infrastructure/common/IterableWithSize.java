package org.broadinstitute.gpinformatics.mercury.infrastructure.common;

import java.util.Iterator;

/**
 * It iterates, it's iterable, and it has size!
 *
 * @param <T>
 */
public interface IterableWithSize<T> extends Iterator<T>, Iterable<T> {
    
    int size();

}
