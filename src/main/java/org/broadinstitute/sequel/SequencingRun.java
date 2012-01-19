package org.broadinstitute.sequel;

public interface SequencingRun {

    public String getRunName();

    public String getRunBarcode();

    public String getMachineName();

    public Person getOperator();

    /**
     * Critical attribute for bass, submissions,
     * and billing.  We know which runs are test
     * runs, and we wish to exclude them from
     * activities like billing or aggregation.
     * @return
     */
    public boolean isTestRun();

    /**
     * Flowcell, PTP, Ion chip, 384 well plate for pacbio/sanger,etc.
     * @return
     */
    public Iterable<RunCartridge> getSampleCartridge();
}
