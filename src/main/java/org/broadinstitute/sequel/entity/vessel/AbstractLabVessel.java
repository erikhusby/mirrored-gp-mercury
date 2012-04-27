package org.broadinstitute.sequel.entity.vessel;


import org.broadinstitute.sequel.entity.analysis.ReadBucket;
import org.broadinstitute.sequel.entity.labevent.Failure;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.notice.Stalker;
import org.broadinstitute.sequel.entity.notice.UserRemarks;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;

import javax.persistence.Embedded;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractLabVessel implements LabVessel {

    private String label;

    private final Collection<JiraTicket> ticketsCreated = new HashSet<JiraTicket>();
    
    private final Collection<SampleSheet> sampleSheets = new HashSet<SampleSheet>();

    /** SampleInstances in this vessel.  If null, follow {@link #sampleSheetAuthorities}*/
//    private Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
    /** Ancestor vessel that has the nearest change to sampleInstances, e.g. starting vessel or pooling */
    private Set<LabVessel> sampleSheetAuthorities = new HashSet<LabVessel>();
    
    /** The molecular envelope delta applied to this vessel. If null, follow {@link #molecularEnvelopeDeltaAuthority}*/
    private MolecularEnvelope molecularEnvelopeDelta;
    /** Ancestor vessel that applied the nearest change to molecular envelope */
    private LabVessel molecularEnvelopeDeltaAuthority;
    
    private MolecularState molecularState;
    // todo jmt molecularStateAuthority?
    // need a list of molecular states, and a map from event type to state(s?)
    // a vessel might undergo more than one state change, do we need to

    private Project project;
    private LabVessel projectAuthority;
    
    private ReadBucket readBucket;
    private LabVessel readBucketAuthority;
    
    private Set<LabEvent> transfersFrom = new HashSet<LabEvent>();
    private Set<LabEvent> transfersTo = new HashSet<LabEvent>();

    private final Collection<Stalker> stalkers = new HashSet<Stalker>();
    
    private Set<Reagent> reagentContents = new HashSet<Reagent>();

    private Set<Reagent> appliedReagents = new HashSet<Reagent>();

    private Set<VesselContainer> containers = new HashSet<VesselContainer>();

    @Embedded
    private UserRemarks userRemarks;

    protected AbstractLabVessel(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void addMetric(LabMetric m) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabMetric> getMetrics() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addFailure(Failure failureMode) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<Failure> getFailures() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<Reagent> getReagentContents() {
        return reagentContents;
    }

    @Override
    public void addReagent(Reagent reagent) {
        reagentContents.add(reagent);
    }

    @Override
    public LabMetric getMetric(LabMetric.MetricName metricName, MetricSearchMode searchMode, SampleInstance sampleInstance) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean isProgeny(LabVessel ancestor) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean isAncestor(LabVessel progeny) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addToContainer(VesselContainer vesselContainer) {
        this.containers.add(vesselContainer);
    }

    @Override
    public Set<VesselContainer> getContainers() {
        return containers;
    }

    @Override
    public String getLabCentricName() {
        // todo jmt what should this do?
        return label;
    }

    @Override
    public Collection<SampleSheet> getSampleSheets() {
        return sampleSheets;
    }

    @Override
    public void addSampleSheet(SampleSheet sampleSheet) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public Set<LabVessel> getSampleSheetAuthorities() {
        return sampleSheetAuthorities;
    }

    public void setSampleSheetAuthorities(Set<LabVessel> sampleSheetAuthorities) {
        this.sampleSheetAuthorities = sampleSheetAuthorities;
    }

    @Override
    public Collection<LabEvent> getTransfersFrom() {
        return transfersFrom;
    }

    @Override
    public Collection<LabEvent> getTransfersTo() {
        return transfersTo;
    }

    @Override
    public void addNoteToProjects(String message) {
        Collection<Project> ticketsToNotify = new HashSet<Project>();
        for (SampleInstance sampleInstance : getSampleInstances()) {
            if (sampleInstance.getAllProjectPlans() != null) {
                for (ProjectPlan projectPlan : sampleInstance.getAllProjectPlans()) {
                    ticketsToNotify.add(projectPlan.getProject());
                }
            }
        }
        for (Project project : ticketsToNotify) {
            project.addJiraComment(message);
        }
    }

    @Override
    public void addJiraTicket(JiraTicket jiraTicket) {
        if (jiraTicket != null) {
            ticketsCreated.add(jiraTicket);
        }
    }

    @Override
    public Collection<JiraTicket> getJiraTickets() {
        return ticketsCreated;
    }

    public UserRemarks getUserRemarks() {
        return userRemarks;
    }

    @Override
    public void applyReagent(Reagent reagent) {
        this.appliedReagents.add(reagent);
    }

    @Override
    public Collection<Reagent> getAppliedReagents() {
        return this.appliedReagents;
    }
}
