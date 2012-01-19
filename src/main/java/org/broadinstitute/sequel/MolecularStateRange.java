package org.broadinstitute.sequel;

import java.util.Collection;

/**
 * Consider this the input constrains to
 * a lab process.  What are the allowable
 * range of input characteristics for
 * a lab event?
 *
 * Null indicates "any value acceptable".
 */
public interface MolecularStateRange {

    /**
     * What do we call this range?  It maps
     * roughly to our current seq content type,
     * plus some index data, plus the presence
     * of adaptors.  What we call this thing
     * is important because people aren't going
     * to go around thinking "I need to find
     * some samples with a primer/adaptor/index
     * name like so, with a concentration range
     * of 1.5nM to 1.7nM, single stranded,
     * at least 3mL."  They're going to call
     * it something more memorable.
     *
     * We should drive the conversation toward
     * naming this things in terms of their
     * most basic capability, like "Illumina Sequencable",
     * "Technology Agnostic Adaptable", "Attachable to
     * Beads", etc.  It'll be a big change.  But
     * then again, we need some big changes...
     * @return
     */
    public String getRangeName();

    public MolecularEnvelope getMolecularEnvelope();

    public MolecularState.DNA_OR_RNA getNucleicAcidState();

    public MolecularState.STRANDEDNESS getStrand();
    
    public Float getMinConcentration();
    
    public Float getMaxConcentration();
    
    public Float getMinVolume();
    
    public Float getMaxVolume();

    /**
     * Is the given molecular state within
     * the required range?
     * @param molecularState
     * @return
     */
    public boolean isInRange(MolecularState molecularState);

    /**
     * The ideal metric ranges for this step
     * @return
     */
    public Collection<LabMetricRange> getBestMetricRanges();

    /**
     * We can deal with these looser metric ranges,
     * but at the risk of worse sequence quality.
     * If you're outside of these ranges, we're going
     * to spam everybody.
     * or qauntity
     * @return
     */
    public Collection<LabMetricRange> getToleratedMetricRanges();

    /**
     * If you're outside of these ranges, you're totally
     * wasting your money, and we're going to spam everybody
     * with lots of blink tags and exclamation points.
     * @return
     */
    public Collection<LabMetricRange> getDisastrousMetricRanges();

    /**
     * What LabEvents can be applied to molecules
     * that meet the given MolecularStateRange?
     *
     * Useful for search
     * @param molecularStateRange
     * @return
     */
    public Collection<LabEventName> getLabEventCapabilities(MolecularStateRange molecularStateRange);

    /**
     * Into which LabWorkQueues can you queue
     * up things that meet the given MolecularStateRange?
     *
     * Useful for search.
     * @param molecularStateRange
     * @return
     */
    public Collection<LabWorkQueueName> getLabWorkQueueCapabilities(MolecularStateRange molecularStateRange);
}
