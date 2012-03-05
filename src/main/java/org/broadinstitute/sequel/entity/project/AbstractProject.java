package org.broadinstitute.sequel.entity.project;


import org.broadinstitute.sequel.control.quote.Funding;
import org.broadinstitute.sequel.control.quote.Quote;
import org.broadinstitute.sequel.control.quote.QuotesCache;
import org.broadinstitute.sequel.entity.analysis.SequenceAnalysisInstructions;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.notice.Stalker;
import org.broadinstitute.sequel.entity.notice.UserRemarkable;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.queue.LabWorkQueue;
import org.broadinstitute.sequel.entity.run.CapacityDimension;
import org.broadinstitute.sequel.entity.run.SequenceAccessControlModel;
import org.broadinstitute.sequel.entity.run.SequencingResult;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.workflow.WorkflowDescription;

import javax.inject.Inject;
import java.util.*;

public abstract class AbstractProject implements Project, UserRemarkable {

    private QuotesCache quotesCache;
    
    private String projectName;
    
    JiraTicket jiraTicket;

    private boolean active;

    private final Collection<BSPPlatingRequest> platingRequests = new HashSet<BSPPlatingRequest>();
    
    public Collection<LabVessel> starters = new HashSet<LabVessel>();
    
    private final Collection<LabWorkQueue> availableWorkQueues = new HashSet<LabWorkQueue>();
    
    private List<LabEventName> checkpointableEvents = new ArrayList<LabEventName>();
    
    private Collection<String> grants = new HashSet<String>();
    
    private Collection<ProjectPlan> projectPlans = new HashSet<ProjectPlan>();

    public void setQuotesCache(QuotesCache cache) {
        this.quotesCache = cache;
    }

    @Override
    public JiraTicket getJiraTicket() {
         return jiraTicket;
    }

    public void setJiraTicket(JiraTicket jiraTicket) {
        this.jiraTicket = jiraTicket;
    }

    @Override
    public boolean isDevelopment() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public ProjectPriority getPriority() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void sendAlert(String alert) {
        throw new RuntimeException("I haven't been written yet.");
    }

    

    @Override
    public Collection<LabVessel> getAllVessels() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Collection<Person> getProjectOwners() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addWorkQueue(LabWorkQueue q) {
        availableWorkQueues.add(q);
    }

    @Override
    public Collection<LabWorkQueue> getWorkQueues() {
        return availableWorkQueues;
    }

    @Override
    public void shareWith(Person p) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<Stalker> getAvailableStalkers() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public SequenceAccessControlModel getAccessControl() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<Quote> getAvailableQuotes() {
        Set<Quote> quotes = new HashSet<Quote>();
        if (quotesCache == null) {
            throw new RuntimeException("Cached quote server data is not available");
        }
        for (String grant : grants) {
            quotes.addAll(quotesCache.getQuotesForGrantDescription(grant));
        }
        return quotes;
    }

    @Override
    public boolean ignoreLabThresholds() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public SequencingResult getSequencingResults(StartingSample aliquot) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public CapacityDimension getDefaultCapacityDimension() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void setUserStatus(Person user, String status) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void getUserStatus(Person user) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addUserNote(Person user, String note) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<String> getUserNotes(Person user) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean isUserFlagged(Person user) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void setUserFlag(Person user, boolean isFlagged) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public INTERESTINGNESS getUserInterestLevel(Person user) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void setUserInterestLevel(Person user, INTERESTINGNESS interestLevel) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<String> getAllNotes() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void hasUserUpdate(Person user) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void setUserUpdate(Person user, boolean isNew) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Date getUserCheckbackDate(Person user) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void setUserCheckbackDate(Person user, Date targetDate) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void setUserCategory(Person user, String category) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getUserCategory(Person user) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabEventName> getCheckpointableEvents() {
        return checkpointableEvents;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public void addStarter(LabVessel vessel) {
        if (vessel == null) {
             throw new NullPointerException("labVessel cannot be null."); 
        }
        starters.add(vessel);
    }

    @Override
    public SequenceAnalysisInstructions getSequenceAnalysisInstructions(SampleInstance sampleInstance) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public WorkflowDescription getWorkflowDescription(SampleInstance sampleInstance) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addPlatingRequest(BSPPlatingRequest platingRequest) {
        if (platingRequest == null) {
             throw new IllegalArgumentException("platingRequest must be non-null in AbstractProject.addPlatingRequest");
        }
        platingRequests.add(platingRequest);
    }

    @Override
    public Collection<BSPPlatingRequest> getPendingPlatingRequests() {
        final Collection<BSPPlatingRequest> pendingRequests = new HashSet<BSPPlatingRequest>();
        for (BSPPlatingRequest platingRequest : platingRequests) {
            if (!platingRequest.isFulfilled()) {
                pendingRequests.add(platingRequest);
            }
        }
        return pendingRequests;
    }

    @Override
    public Collection<LabVessel> getAllStarters() {
        return starters;
    }

    @Override
    public void addProjectPlan(ProjectPlan projectPlan) {
        if (projectPlan == null) {
             throw new NullPointerException("projectPlan cannot be null."); 
        }
        projectPlans.add(projectPlan);
    }

    @Override
    public Collection<LabVessel> getVessels(WorkflowDescription workflowDescription) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addGrant(String grantDescription) {
        if (grantDescription == null) {
             throw new NullPointerException("grantDescription cannot be null."); 
        }
        grants.add(grantDescription);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project)) return false;

        Project that = (Project) o;

        if (projectName != null ? !projectName.equals(that.getProjectName()) : that.getProjectName()!= null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return projectName != null ? projectName.hashCode() : 0;
    }
}
