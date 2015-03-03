package org.broadinstitute.gpinformatics.infrastructure.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher for an exception message
 */
public class ExceptionMessageMatcher <T extends Exception> extends TypeSafeMatcher<T> {

    private final String messageToMatch;
    private final boolean ignoreCase;

    private ExceptionMessageMatcher(String messageToMatch) {
        this(messageToMatch, false);
    }

    public ExceptionMessageMatcher(String messageToMatch, boolean ignoreCase) {
        this.messageToMatch = messageToMatch;
        this.ignoreCase = ignoreCase;
    }

    public static ExceptionMessageMatcher containsMessage(String message) {
        return new ExceptionMessageMatcher(message);
    }

    public static ExceptionMessageMatcher containsMessageIgnoringCase(String message) {
        return new ExceptionMessageMatcher(message, true);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("Exception with message containing string (case %s), \"%s\"",
                ignoreCase ? "insensitive" : "sensitive", messageToMatch));
    }

    @Override
    protected boolean matchesSafely(T item) {
        return ignoreCase ? item.getMessage().toLowerCase().contains(messageToMatch.toLowerCase()) :
                item.getMessage().contains(messageToMatch);
    }
}
