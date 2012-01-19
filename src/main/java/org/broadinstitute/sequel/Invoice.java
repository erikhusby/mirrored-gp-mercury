package org.broadinstitute.sequel;

import java.util.Collection;

public interface Invoice {

    public enum Status {
        DRAFT,SENT
    }

    public String getInvoiceName();

    /**
     * Get the line items.
     *
     * Note that we can have an inconsistency in terms
     * of the number of line items and the {@link Priceable#getMaximumSplitFactor()
     * maximum split factor}.  This inconsistency
     * will arise when when we have a lane
     * with a high orphan rate.  In this situation,
     * we might have 96 different indexed samples
     * in a lane, but a sequencing failure might
     * have garbled the index reads for 5 of the
     * samples.
     *
     * Or maybe we're charging for a plate
     * event for a 384 well plate, but we
     * accidentally sneezed on the left
     * half of the plate, rendering the
     * samples useless.  In that case,
     * we'd eat the cost of half the plate.
     *
     * Or maybe the collaborator sneezed
     * on the plate they gave to us, in which
     * case they eat the cost.
     *
     * It's up to humans to make reconcile a mismatch in line
     * items and {@link Priceable#getMaximumSplitFactor()
     * theoretical maximum split factor}
     * @return
     */
    public Collection<InvoiceLineItem> getInvoiceLineItems();

    /**
     * Let's say we run a lane with 93 samples on it,
     * but we ate 5 of the samples.  So we're going
     * to absorb the cost of those 5 samples.  We
     * could bill those 5 samples to ourselves in
     * some way, but why are we going to send ourselves
     * an invoice?  We could do that, or we could just
     * drop the 5 samples from the invoice.
     *
     * One wonders how we would then track the extent
     * to which we're eating the cost of various
     * screw ups.  For that reason, maybe the implementation
     * would actually track the removal of the line
     * item as an event so we could report on it later.
     * @param lineItem
     */
    public void remove(InvoiceLineItem lineItem);

    public void add(InvoiceLineItem lineItem);

    /**
     * Sends the invoice to the appropriate parties.
     * Maybe initially this just sends it to the printer,
     * where a PM picks it up.  Or maybe we "send"
     * it to the quote server and debit each quote
     * accordingly.
     */
    public void send();

    /**
     * Gets the thing that is being priced
     * @return
     */
    public Priceable getPriceable();

}
