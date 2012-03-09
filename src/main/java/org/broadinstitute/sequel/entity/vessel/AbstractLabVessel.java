package org.broadinstitute.sequel.entity.vessel;


import org.broadinstitute.sequel.entity.notice.UserRemarkable;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.notice.Stalker;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.analysis.ReadBucket;
import org.broadinstitute.sequel.entity.labevent.Failure;
import org.broadinstitute.sequel.entity.labevent.LabEvent;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractLabVessel implements LabVessel, UserRemarkable {

    private String label;

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
    public boolean isProgeny(LabVessel ancestor) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean isAncestor(LabVessel progeny) {
        throw new RuntimeException("I haven't been written yet.");
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
        Collection<JiraTicket> ticketsToNotify = new HashSet<JiraTicket>();
        for (SampleInstance sampleInstance : getSampleInstances()) {
            if (sampleInstance.getProject() != null) {
                if (sampleInstance.getProject().getJiraTicket() != null) {
                    ticketsToNotify.add(sampleInstance.getProject().getJiraTicket());
                }
            }
        }
        for (JiraTicket jiraTicket : ticketsToNotify) {
            jiraTicket.addComment(message);
        }
    }
}
