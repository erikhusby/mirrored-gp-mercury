package org.broadinstitute.sequel.entity.billing;

import org.broadinstitute.sequel.entity.sample.SampleInstance;

import java.util.Collection;
import java.util.Date;

/**
 * Runs and some events (like adaptor ligation)
 * are billable.  In other words, when we
 * generate > 0 PF bases, we bill someone.
 * When we run an adaptor ligation reaction,
 * we bill someone.
 *
 * Typically the cost is fixed per "container".
 * So, for example, doing adaptor ligation
 * costs $X per plate.  Fill a plate with 10
 * samples, and it costs $ (1/10) * x per
 * sample.  Fill the plate with ten thousand
 * samples and you have an economy of scale.
 *
 * Same goes for runs.  There's a fixed cost to
 * running a flowcell or a lane.
 *
 */
public interface Priceable {

    // suppose I want the ui to show me all priceable things
    // generated in the last month, grouped by
    // price item name.  select * from Priceable where creationDate between...


    /**
     * In the quotes database, what named
     * item list thing is this?
     * @return
     */
    public String getPriceListItemName();

    /**
     * What's the name of this thing in
     * the lab?  For example, "A023ABXX",
     * or "AdaptorLigation for plate 0000021381"
     * @return
     */
    public String getLabNameOfPricedItem();

    /**
     * What's the maximum number of ways this
     * bill could be split?
     *
     * For example, if you have 96 samples
     * on a lane, the maximum split would
     * be 96.  If you have a 384 well
     * plate with 12 samples on it, the
     * maximum split would be 12.
     *
     * If you have a 96 well plate and each
     * well has 100 samples in it, the maximum
     * split factor is 96 * 1000 = 96,000.
     *
     * Implementions probably end up
     * looking at {@link org.broadinstitute.sequel.entity.sample.SampleSheet#getSamples() the
     * aliquots in a sample sheet}, possibly paying
     * attention to molecular indexing.
     * @return
     */
    public int getMaximumSplitFactor();

    /**
     * Why the distinction between {@link org.broadinstitute.sequel.control.billing.InvoiceGenerator#generateInvoice(Priceable) generating an invoice }
     * and getting an invoice?  Because how you make a "draft" invoice
     * is different from just associating
     * an invoice.
     *
     * For example, the invoices we generate
     * are ambiguous initially.  We might
     * have n different quotes for some
     * {@link Priceable priceable } thing.
     * After the PMs review and adjust the
     * invoice, it's ready to be sent.
     *
     * A billing app probably starts by showing
     * the user of billable things for some
     * time period (the billing period--quarterly?
     * biweekly?)  For instance "show me all
     * hiseq 76bp lanes from the last quarter
     * that haven't been billed".  We'd
     * implement that as some DAO lookup...right?
     * @return
     */
    public Invoice getInvoice();

    public Collection<SampleInstance> getSampleInstances();

    /**
     * What was the date at which this priceable
     * came into being?  Note that the actual lab
     * creation date isn't necessarily the one
     * that we will use.  For example, if a run
     * takes 7 days to complete and we do not
     * want to bill until we've seen the PF
     * bases count, then we can't bill it on the
     * day the sequencer is loaded on the instrument.
     * We have to wait until the run is done and
     * we have some run metrics.
     * @return
     */
    public Date getPriceableCreationDate();
}
