package org.broadinstitute.sequel;


import java.util.Collection;

public interface Project {

    /**
     * Is this project considered
     * "in play"?  This is a slight
     * abstraction around statuses
     * like "Started", "Done",
     * "Revoked", etc.
     * @return
     */
    public boolean isActive();

    public void setActive(boolean isActive);
    
    public String getProjectName();

    /**
     * What if all the free-form data for a project
     * was captured in a jira ticket?  We'd pick up
     * lots of attachment, commenting, and communication
     * capabilities.
     *
     * Project managers love jira.  Why not give them
     * the ability to make a jira ticket for a project?
     * They can configure jira to match their workflow.
     * @return
     */
    public JiraTicket getJiraTicket();

    public boolean isDevelopment();

    public ProjectPriority getPriority();

    /**
     * Got some insanely high priority samples
     * that came from an outbreak?  Turn this
     * bit on.
     * @return
     */

    public void sendAlert(String alertText);

    /**
     * Suppose you (as a PM) start a simple project with 10 samples,
     * and then decide you want to do 2 different things to
     * that same set of samples.  You'd want to tell the system
     * "Hey, take these samples and make a group for them."
     * @param labTangibles
     * @return
     */
    public GroupOfTangibles makeGroup(Collection<LabTangible> labTangibles);

    /**
     * A group of tangibles for use in the mind of the
     * PM.  "Here are the libraries I want to top off
     * it the libraries from last week don't make it".
     * @return
     */
    public Collection<GroupOfTangibles> getTangibleGroups();

    public Collection<LabTangible> getLabTangibles(WorkflowDescription workflowDescription);

    public Collection<LabTangible> getAllLabTangibles();

    /**
     * Stateful side effect: when you add a {@link LabTangible}
     * to a {@link Project}, you are implicitly linking
     * every {@link Goop} in {@link LabTangible#getSampleSheets()
     * the sample sheet} to this project.  In other words,
     * if you call Project1.addLabTangible(), you'll
     * find Project1 listed as an active project
     * when you call {@link Goop#getActiveProjects()},
     * as long as Project1 is considered "active"
     * @param labTangible
     * @param workflowDescription the description of the workflow
     *                            that labTangible is expected
     *                            to undergo
     */
    public void addLabTangible(LabTangible labTangible,WorkflowDescription workflowDescription);

    public Collection<Person> getProjectOwners();

    /**
     * Some projects won't use every lab queue.
     * When you want to add a lab queue to the
     * PM dashboard so that they can add
     * containers to the queue on behalf of
     * the project, just add the queue to the
     * project and it will show up as an option.
     *
     * Do we track a LabEvent to look back and
     * see which PM added what things to which
     * queues?  Probably.  That'd be useful
     * information to help a PM see what's
     * going on in a queue.
     * @param q
     */
    public void addWorkQueue(LabWorkQueue q);

    /**
     * Because lab work queues are shared across
     * projects, we probably do have to worry about
     * simultaneous updates to queues.
     * @return
     */
    public Collection<LabWorkQueue> getWorkQueues();

    /**
     * Sometimes PMs go on vacation, so someone else needs
     * to shephard their projects.  sharing the project
     * adds a person to the project ownership list
     * and propagates "watching" information to the
     * jira ticket.
     * @param p
     */
    public void shareWith(Person p);

    /**
     * There might be multiple stalkers,
     * like a POEMs stalker, a pm bridge stalker,
     * etc.  All this callback stuff makes me think
     * that perhaps we should start forking
     * a new thread, yielding, and logging
     * callbacks...
     * @return
     */
    public Collection<Stalker> getAvailableStalkers();

    public SequenceAccessControlModel getAccessControl();

    /**
     * Gets a list of all quotes that have been
     * tenatively earmarked for use in this project.
     *
     * This list does not tell you anything
     * about what quote to charge.  It's just
     * a scratch pad for the PM to keep track
     * of possible quotes to be used
     * in the billing app.
     *
     * Pass through to web service into quote server.
     * Show funding source, current balance, reserved
     * balance for project, etc. by querying
     * quotes server in realtime
     * @return
     */
    public Collection<Quote> getAvailableQuotes();

    /**
     * Some projects might wish to proceed "at risk".  Consider
     * rare samples, or samples which are hard to extract, and
     * a PI with a sack of cash who says "Damn the torpedos; I do
     * not care if I get back low yield, just sequence this
     * as best you can."
     *
     * In that situation, perhaps our automated alerts about concentration
     * being out of range should be silenced.  This bit can be
     * set at the discretion of the project to tell the lab
     * that even if things are out of bounds in terms of
     * thresholds, run it anyway.
     * @return
     */
    public boolean ignoreLabThresholds();

    public SequencingResult getSequencingResults(Goop aliquot);

    public CapacityDimension getDefaultCapacityDimension();

    /**
     * It's up to the project to figure out for a given
     * sample aliquot instance how the aggregation
     * should work.
     * @param sampleInstance
     * @return
     */
    public SequenceAnalysisInstructions getSequenceAnalysisInstructions(SampleInstance sampleInstance);

    /**
     * What was the expected workflow that this
     * sampleAliquotInstance was supposed to
     * undergo?
     *
     * @param sampleInstance
     * @return
     */
    public WorkflowDescription getWorkflowDescription(SampleInstance sampleInstance);

    /**
     * Project managers want to know when various things
     * happen to their samples.  They don't want to know
     * when anything happens; they just want to know when
     * a subset of events happen.  This list tells you
     * which event types the project wants to get
     * immediate notification about.
     * @return
     */
    public Collection<LabEventName> getCheckpointableEvents();

    public void addPlatingRequest(BSPPlatingRequest platingRequest);
    
    public Collection<BSPPlatingRequest> getPendingPlatingRequests();

}
