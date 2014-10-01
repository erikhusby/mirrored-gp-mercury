package org.broadinstitute.gpinformatics.infrastructure.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher for an exception message
 */
public class ExceptionMessageMatcher <T extends Exception> extends TypeSafeMatcher<T> {

    private final String messageToMatch;

    private ExceptionMessageMatcher(String messageToMatch) {
        this.messageToMatch = messageToMatch;
    }

    public static ExceptionMessageMatcher containsMessage(String message) {
        return new ExceptionMessageMatcher(message);
    }

    @Override
    public void describeTo(Description description) {
           description.appendText("Exception contains string");
    }

    @Override
    protected boolean matchesSafely(T item) {
        return item.getMessage().contains(messageToMatch);
    }
}
