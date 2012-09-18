package org.broadinstitute.gpinformatics.mercury.entity.notice;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;

/**
 * Listener callback kind of thing that can translate
 * a lab event into a summary "notice" sent
 * to a project manager.
 *
 * PMs can choose different stalk modes
 * for a project.  If they say "tell me
 * everything", they'll find jira comments
 * for every lab event for their project.
 *
 * Maybe they just want to know about pico
 * quants that were out of range.  That's
 * a different Stalker.
 *
 * POEMS or PM Bridge might want to know
 * about some key events.  so that's
 * another stalker.
 */
public interface Stalker {

    /**
     * Do something in response to this event
     * @param event
     */
    public void stalk(LabEvent event);

    /**
     * Mute this thing so it doesn't
     * spam the PM.
     */
    public void disable();

    /**
     * Start spamming
     */
    public void enable();

    /**
     * Do something.  Probably called from
     * a dispatch thread at some interval.
     */
    public void stalk();

    public boolean isMuted();

}
