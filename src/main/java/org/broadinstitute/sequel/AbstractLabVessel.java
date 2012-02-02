package org.broadinstitute.sequel;


import java.util.*;

public abstract class AbstractLabVessel implements LabVessel, UserRemarkable {

    private final Collection<Stalker> stalkers = new HashSet<Stalker>();

    @Override
    public String getLabel() {
        throw new RuntimeException("I haven't been written yet.");
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
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<SampleSheet> getSampleSheets() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addSampleSheet(SampleSheet sampleSheet) {
        throw new RuntimeException("I haven't been written yet.");
    }
}
