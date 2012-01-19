package org.broadinstitute.sequel;

/**
 * Something that contains samples and is
 * loaded onto a sequencing instrument.
 *
 * 454 PTP, Illumina flowcell, Ion chip,
 * pacbio plate
 */
public interface RunCartridge extends LabVessel {

    public Iterable<RunChamber> getChambers();

    public String getCartridgeName();

    public String getCartridgeBarcode();

}
