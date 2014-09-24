/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2014) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import javax.annotation.Nonnull;

/**
 * Hamcrest matcher that replaces all '%s' format string directives with regular expression wildcards for comparison
 * with a query string.
 */
public class FormatStringMatcher extends TypeSafeMatcher<String> {

    private final String formatString;

    public FormatStringMatcher(@Nonnull String formatString) {
        this.formatString = formatString;
    }

    @Override
    public boolean matchesSafely(String text) {
        // Replace occurrences of the '%s' in the template with '.*' and attempt a regular
        // expression match between the template and the actual message.
        return text.matches(formatString.replaceAll("%s", ".*"));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("matches format string = ").appendText(formatString);
    }

    public static FormatStringMatcher matchesFormatString(String string) {
        return new FormatStringMatcher(string);
    }
}
