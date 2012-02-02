package org.broadinstitute.sequel;


import java.util.*;

public abstract class AbstractProject implements  Project, UserRemarkable {

    private String projectName;
    
    JiraTicket jiraTicket;

    private boolean active;

    private final Collection<BSPPlatingRequest> platingRequests = new HashSet<BSPPlatingRequest>();
    
    private final Map<WorkflowDescription,Collection<LabVessel>> startingStuffByWorkflow =  new HashMap<WorkflowDescription,Collection<LabVessel>>();
    
    private final Collection<LabWorkQueue> availableWorkQueues = new HashSet<LabWorkQueue>();

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
    public GroupOfTangibles makeGroup(Collection<LabTangible> labTangibles) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<GroupOfTangibles> getTangibleGroups() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabVessel> getVessels(WorkflowDescription workflowDescription) {
        return startingStuffByWorkflow.get(workflowDescription);
    }

    @Override
    public Collection<LabVessel> getAllVessels() {
        Collection<LabVessel> allTangibles = new HashSet<LabVessel>();
        for (Map.Entry<WorkflowDescription, Collection<LabVessel>> entry : startingStuffByWorkflow.entrySet()) {
            allTangibles.addAll(entry.getValue());
        }
        return allTangibles;
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
        throw new RuntimeException("I haven't been written yet.");
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
        throw new RuntimeException("I haven't been written yet.");
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
    public void addVessel(LabVessel vessel, WorkflowDescription workflowDescription) {
        if (!startingStuffByWorkflow.containsKey(workflowDescription)) {
            startingStuffByWorkflow.put(workflowDescription,new HashSet<LabVessel>());
        }
        startingStuffByWorkflow.get(workflowDescription).add(vessel);
        /**
         * We don't go into the {@link SampleSheet}s and set the
         * Project at this point because we aren't reserving
         * the tangible for this project exclusively.  The separate gesture
         * for this is {@link ProjectBranchable}.
         */
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
