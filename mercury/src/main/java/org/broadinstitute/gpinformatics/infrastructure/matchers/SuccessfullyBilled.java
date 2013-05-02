package org.broadinstitute.gpinformatics.infrastructure.matchers;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;


public class SuccessfullyBilled extends TypeSafeMatcher<LedgerEntry> {
    @Override
    public boolean matchesSafely(LedgerEntry ledgerEntry) {
        return BillingSession.SUCCESS.equals(ledgerEntry.getBillingMessage());
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
