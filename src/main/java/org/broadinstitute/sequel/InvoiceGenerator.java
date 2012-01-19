package org.broadinstitute.sequel;

/**
 * Why do we have {@link Priceable#getInvoice()}
 * and this generator class?
 *
 * Because generating a "possible" invoice is
 * different from just associating an invoice
 * with some lab thing.
 *
 * Implementations will traipse through the lims,
 * using {@link SampleSheet sample sheets}
 * to guess at which {@link Project}s are relevant,
 * and therefore which quotes might be applied
 * (see {@link Project#getAvailableQuotes()
 * how to get quotes from a project}.
 *
 * This complexity exists because we often
 * need to juggle funding sources.  Why
 * do we juggle funding sources?  Because
 * we can.  When we start some lab work,
 * we earmark funds via the Quote server,
 * but only when the work is done to we
 * actually charge for it.
 *
 * Multiple projects often share funding
 * sources, and when you factor in how
 * much time passes between a kit request
 * from BSP and actual sequence output,
 * you sometimes need to swap quotes
 * or funding sources.
 *
 * None of this means that we're being
 * dishonest about billing.  Funding sources
 * can be used for a variety of activities,
 * and it's up to the PMs to ensure that
 * everything lines up.
 */
public interface InvoiceGenerator {

    /**
     * Generate a draft, expected-to-be ambiguous
     * invoice.
     * @param priceable
     * @return
     */
    public Invoice generateInvoice(Priceable priceable);
}
