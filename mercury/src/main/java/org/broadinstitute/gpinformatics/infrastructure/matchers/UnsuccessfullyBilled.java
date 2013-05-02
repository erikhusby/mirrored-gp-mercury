package org.broadinstitute.gpinformatics.infrastructure.matchers;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;


/**
 * This class tests for a non-null and non-successful billing message on a ledger entry.
 */
public class UnsuccessfullyBilled extends TypeSafeMatcher<LedgerEntry> {
    @Override
    public boolean matchesSafely(LedgerEntry ledgerEntry) {
        return ledgerEntry.getBillingMessage() != null && !BillingSession.SUCCESS.equals(ledgerEntry.getBillingMessage());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("unsuccessfully billed");
    }

    @Factory
    public static Matcher<LedgerEntry> unsuccessfullyBilled() {
        return new UnsuccessfullyBilled();
    }
}
