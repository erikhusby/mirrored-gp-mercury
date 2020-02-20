package org.broadinstitute.gpinformatics.infrastructure.matchers;

import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;


/**
 * Hamcrest matcher for a non-null and non-successful billing message on a {@link LedgerEntry}.
 */
public class UnsuccessfullyBilled extends TypeSafeMatcher<LedgerEntry> {
    @Override
    public boolean matchesSafely(LedgerEntry ledgerEntry) {
        return ledgerEntry.isUnsuccessfullyBilled();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("unsuccessfully billed");
    }

    @Override
    protected void describeMismatchSafely(LedgerEntry item, Description mismatchDescription) {
        mismatchDescription.appendValue(String
            .format("Ledger for %s in %s is %ssuccessfully billed", item.getProductOrderSample().getSampleKey(),
                item.getProductOrderSample().getProductOrder().getBusinessKey(),
            (!item.isSuccessfullyBilled()) ?"un": ""));
    }

    @Factory
    public static Matcher<LedgerEntry> unsuccessfullyBilled() {
        return new UnsuccessfullyBilled();
    }
}
