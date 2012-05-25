package org.broadinstitute.pmbridge.infrastructure.gap;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/23/12
 * Time: 5:31 PM
 */
public class ProductNotFoundException extends Exception {
    public ProductNotFoundException() {
    }

    public ProductNotFoundException(final String s) {
        super(s);
    }

    public ProductNotFoundException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public ProductNotFoundException(final Throwable throwable) {
        super(throwable);
    }
}
