package org.broadinstitute.sequel.entity.project;


import org.broadinstitute.sequel.entity.OrmUtil;
import org.broadinstitute.sequel.entity.billing.Quote;
import org.broadinstitute.sequel.entity.notice.UserRemarks;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.run.SequenceAccessControlModel;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.queue.LabWorkQueue;
import org.broadinstitute.sequel.entity.analysis.SequenceAnalysisInstructions;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.notice.Stalker;
import org.broadinstitute.sequel.entity.run.CapacityDimension;
import org.broadinstitute.sequel.entity.run.SequencingResult;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.infrastructure.quote.QuotesCache;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Entity
public abstract class Project {

    @Id
    @SequenceGenerator(name = "SEQ_PROJECT", sequenceName = "SEQ_PROJECT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PROJECT")
    private Long projectId;

   public static final String JIRA_PROJECT_PREFIX = "TP";

    @Transient
    private QuotesCache quotesCache;

    private String projectName;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private JiraTicket jiraTicket;

    private boolean active;

//    @OneToMany
    @Transient
    public Collection<Starter> starters = new HashSet<Starter>();

    @OneToMany
    private final Collection<LabWorkQueue> availableWorkQueues = new HashSet<LabWorkQueue>();

    // todo jmt fix this
    @Transient
    private List<LabEventName> checkpointableEvents = new ArrayList<LabEventName>();

    // todo jmt fix this
    @Transient
    private Collection<String> grants = new HashSet<String>();

    @OneToMany
    private Collection<ProjectPlan> projectPlans = new HashSet<ProjectPlan>();

    @OneToMany
    private Collection<Quote> availableQuotes = new HashSet<Quote>();

    @Embedded
    private UserRemarks userRemarks;

    public abstract Person getPlatformOwner();

    public void setQuotesCache(QuotesCache cache) {
        this.quotesCache = cache;
    }

    JiraTicket getJiraTicket() {
        return jiraTicket;
    }

    public void setJiraTicket(JiraTicket jiraTicket) {
        this.jiraTicket = jiraTicket;
    }

    public void addAvailableQuote(Quote quote) {
        this.availableQuotes.add(quote);
    }

    public boolean isDevelopment() {
        throw new RuntimeException("I haven't been written yet.");
    }

    public ProjectPriority getPriority() {
        return ProjectPriority.NORMAL;
    }

    /**
     * Got some insanely high priority samples
     * that came from an outbreak?  Turn this
     * bit on.
     * @return
     */
    public void sendAlert(String alert) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public Collection<LabVessel> getAllVessels() {
        throw new RuntimeException("not implemented");
    }

    public Collection<Person> getProjectOwners() {
        throw new RuntimeException("I haven't been written yet.");
    }

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
    public void addWorkQueue(LabWorkQueue q) {
        availableWorkQueues.add(q);
    }

    /**
     * Because lab work queues are shared across
     * projects, we probably do have to worry about
     * simultaneous updates to queues.
     * @return
     */
    public Collection<LabWorkQueue> getWorkQueues() {
        return availableWorkQueues;
    }

    /**
     * Sometimes PMs go on vacation, so someone else needs
     * to shephard their projects.  sharing the project
     * adds a person to the project ownership list
     * and propagates "watching" information to the
     * jira ticket.
     * @param p
     */
    public void shareWith(Person p) {
        throw new RuntimeException("I haven't been written yet.");
    }

    /**
     * There might be multiple stalkers,
     * like a POEMs stalker, a pm bridge stalker,
     * etc.  All this callback stuff makes me think
     * that perhaps we should start forking
     * a new thread, yielding, and logging
     * callbacks...
     * @return
     */
    public Collection<Stalker> getAvailableStalkers() {
        throw new RuntimeException("I haven't been written yet.");
    }

    public SequenceAccessControlModel getAccessControl() {
        throw new RuntimeException("I haven't been written yet.");
    }

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
    public Collection<Quote> getAvailableQuotes() {
        return availableQuotes;
    }

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
    public boolean ignoreLabThresholds() {
        throw new RuntimeException("I haven't been written yet.");
    }

    public SequencingResult getSequencingResults(StartingSample aliquot) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public CapacityDimension getDefaultCapacityDimension() {
        throw new RuntimeException("I haven't been written yet.");
    }

    /**
     * Project managers want to know when various things
     * happen to their samples.  They don't want to know
     * when anything happens; they just want to know when
     * a subset of events happen.  This list tells you
     * which event types the project wants to get
     * immediate notification about.
     * @return
     */
    public Collection<LabEventName> getCheckpointableEvents() {
        return checkpointableEvents;
    }

    /**
     * Is this project considered
     * "in play"?  This is a slight
     * abstraction around statuses
     * like "Started", "Done",
     * "Revoked", etc.
     * @return
     */
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void addStarter(Starter starter) {
        if (starter == null) {
            throw new NullPointerException("starter cannot be null.");
        }
        starters.add(starter);
    }

    /**
     * It's up to the project to figure out for a given
     * sample aliquot instance how the aggregation
     * should work.
     * @param sampleInstance
     * @return
     */
    public SequenceAnalysisInstructions getSequenceAnalysisInstructions(SampleInstance sampleInstance) {
        throw new RuntimeException("I haven't been written yet.");
    }

    /**
     * What was the expected workflow that this
     * sampleAliquotInstance was supposed to
     * undergo?
     *
     * @param sampleInstance
     * @return
     */
    public WorkflowDescription getWorkflowDescription(SampleInstance sampleInstance) {
        throw new RuntimeException("I haven't been written yet.");
    }


    public Collection<Starter> getAllStarters() {
        return starters;
    }

    public Collection<ProjectPlan> getProjectPlans() {
        return projectPlans;
    }

    public void addProjectPlan(ProjectPlan projectPlan) {
        if (projectPlan == null) {
            throw new NullPointerException("projectPlan cannot be null.");
        }
        projectPlans.add(projectPlan);
    }

    public Collection<LabVessel> getVessels(WorkflowDescription workflowDescription) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public void addGrant(String grantDescription) {
        if (grantDescription == null) {
            throw new NullPointerException("grantDescription cannot be null.");
        }
        grants.add(grantDescription);
    }

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
    public void addJiraComment(String comment) {
        if (jiraTicket != null) {
            jiraTicket.addComment(comment);
        }
        else {
            // todo figure out tool for logging and alerting.
            throw new RuntimeException("There is no jira ticket for " + projectName);
        }
    }

    public SampleAnalysisBuddies getAnalysisBuddies(StartingSample sample) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!OrmUtil.proxySafeIsInstance(o, Project.class)) return false;

        Project that = (Project) o;

        if (projectName != null ? !projectName.equals(that.getProjectName()) : that.getProjectName()!= null) return false;

        return true;
    }

    public Collection<ProjectPlan> getAllPlans() {
        return projectPlans;
    }

    @Override
    public int hashCode() {
        return projectName != null ? projectName.hashCode() : 0;
    }

    public UserRemarks getUserRemarks() {
        return userRemarks;
    }
}
