package org.broadinstitute.gpinformatics.mercury.entity.labevent;

/**
 * Let's assume we're going to receive lab events
 * fairly often, and that we want to respond to them
 * very fast (like < 1s).
 *
 * Automation decks "speak" in a variety of terms:
 * 1. SDS sections.  Things like "section A1", "top
 * half", "every other well A1 column", etc.  Event
 * messages reference these sections instead of
 * per well information because this is the natural
 * way to program the robot.  Bravos do this.
 *
 * 2. per-well.  In this scenario, a Janus/Multiprobe
 * is doing well-by-well operations.
 *
 * Manual transfers can be phrased either well by
 * well or section by section.
 *
 * I mention all of this because storing events
 * well level is folly.  It took years to correct
 * this mistake, so we need to continue processing
 * plate/rack events in terms of sections as
 * much as possible.
 *
 * But for simplicity at the higher levels of the app,
 * we still want to talk about wells.  So the way we
 * store these events is very different from how
 * we want to react to them.
 *
 * Therefore we have some class that can translate
 * the two.  Probably this thing should make
 * heavy use of unmodifiable collections so that
 * clients can't do things like remove individual
 * wells from a plate section event.
 */
public interface LabEventMessageTranslator {

    public LabEvent translateEvent(LabEventMessage eventMessage);
}
