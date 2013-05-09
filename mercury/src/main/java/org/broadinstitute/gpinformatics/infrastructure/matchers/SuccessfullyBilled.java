package org.broadinstitute.gpinformatics.infrastructure.matchers;

import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;


/**
 * Hamcrest Matcher that tests for the presence of a successful billing message on a {@link LedgerEntry}.
 */
public class SuccessfullyBilled extends TypeSafeMatcher<LedgerEntry> {
    @Override
    public boolean matchesSafely(LedgerEntry ledgerEntry) {
        return ledgerEntry.isSuccessfullyBilled();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("successfully billed");
    }

    @Factory
    public static Matcher<LedgerEntry> successfullyBilled() {
        return new SuccessfullyBilled();
    }
}
