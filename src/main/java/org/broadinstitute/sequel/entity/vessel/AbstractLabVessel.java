package org.broadinstitute.sequel.entity.vessel;


import org.broadinstitute.sequel.entity.notice.UserRemarkable;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.notice.Stalker;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.analysis.ReadBucket;
import org.broadinstitute.sequel.entity.labevent.Failure;
import org.broadinstitute.sequel.entity.labevent.LabEvent;

import java.util.*;

public abstract class AbstractLabVessel implements LabVessel, UserRemarkable {

    private String label;

    private final Collection<SampleSheet> sampleSheets = new HashSet<SampleSheet>();

    /** SampleInstances in this vessel.  If null, follow {@link #sampleSheetReferences}*/
//    private Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
    /** Ancestor vessel that has the nearest change to sampleInstances, e.g. starting vessel or pooling */
    private Set<LabVessel> sampleSheetReferences = new HashSet<LabVessel>();
    /** The molecular envelope delta applied to this vessel (is this redundant wrt reagent). If null, follow {@link #molecularEnvelopeDeltaReference}*/
    private MolecularEnvelope molecularEnvelopeDelta;
    /** Ancestor vessel that applied the nearest change to molecular envelope */
    private LabVessel molecularEnvelopeDeltaReference;
    
    private Project project;
    private LabVessel projectReference;
    
    private ReadBucket readBucket;
    private LabVessel readBucketReference;
    
    private Set<LabEvent> transfersFrom = new HashSet<LabEvent>();
    private Set<LabEvent> transfersTo = new HashSet<LabEvent>();

    private final Collection<Stalker> stalkers = new HashSet<Stalker>();

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
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addReagent(Reagent r) {
        throw new RuntimeException("I haven't been written yet.");
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

    public Set<LabVessel> getSampleSheetReferences() {
        return sampleSheetReferences;
    }

    public void setSampleSheetReferences(Set<LabVessel> sampleSheetReferences) {
        this.sampleSheetReferences = sampleSheetReferences;
    }

    @Override
    public Collection<LabEvent> getTransfersFrom() {
        return transfersFrom;
    }

    @Override
    public Collection<LabEvent> getTransfersTo() {
        return transfersTo;
    }


}
