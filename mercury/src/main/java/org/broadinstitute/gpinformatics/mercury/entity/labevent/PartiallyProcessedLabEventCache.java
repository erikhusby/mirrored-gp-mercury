package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import java.util.Collection;

/**
 * Lab events, whether posted automatically from automation
 * or manually through the UI or web services, can be posted
 * out of order.  In other words, if X happened before Y,
 * we expected that sometimes we might see the event for Y
 * before we see it for X.  Furthermore, we can't necessarily
 * rely on the event time (the time at which the event happened,
 * not the time at which it was received) to order the events
 * because clocks on loosely connected machines are sometimes
 * set incorrectly...NTP doesn't work so well when you have no
 * connection to the time server...
 *
 * Whatever the cause, when things arrive out of order, we
 * can detect this in a few ways:
 * 1. Ideally we'd have a workflow system.  But probably not
 * for a long while.
 * 2. If the the event references a vessel which we should
 * have seen already but haven't (for instance, a source
 * container for a transfer message), we can conclude that
 * it is out of order.
 * 3. If the prior molecular state of the samples in a tube
 * is not what the event expects, we can conclude
 * out of order messaging.  This means that implementations
 * of LabEvent should have some way to express a check
 * on the current state of the molecular envelope,etc.
 *
 * When we detect out of order messaging, we attempt to process
 * the "container motion".  That is, we record the fact that the
 * event happened, but we leave the metadata (molecular state, etc.
 * in a "dirty" state (possibly empty).  All aspects of the application
 * must be able to handle the fact that we might have "seen"
 * a plate and know some source information, but not have
 * complete (or any) sample metadata, molecular envelope, etc. information
 * until after the OOOM (Out Of Order Message) is resolved.
 *
 * When we detect this situation, we put the offending message
 * into some kind of a cache.  This class represents that cache.
 *
 * When new messages come in, we ask the cache "Do you have any
 * messages that have been partially processed that look like
 * they might be resolved by the presence of this new message?"
 */
public interface PartiallyProcessedLabEventCache {

    /**
     * Finds partially processed events in the cache and
     * returns them.
     *
     * Should implementations return things in event time order?
     * Or randomize them, with the hope that LabEvent implementations
     * will reject things until the molecular envelope "looks" right?
     * @param event
     * @return
     */
    public Collection<LabEvent> findRelatedEvents(LabEvent event);

    public void removeEvent(LabEvent labEvent);

    public void addEvent(LabEvent labEvent);
}
