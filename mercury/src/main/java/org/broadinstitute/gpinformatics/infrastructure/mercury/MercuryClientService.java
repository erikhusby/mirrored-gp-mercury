package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.athena.presentation.DisplayableItem;

import java.io.Serializable;
import java.util.Collection;

/**
 * Interface of Mercury Services available to Athena.
 *
 * @author epolk
 */
public interface MercuryClientService extends Serializable {

    /**
     * Get all the reference sequences.
     *
     * @return a collection of {@link DisplayableItem} objects modeling a reference sequence
     */
    Collection<DisplayableItem> getReferenceSequences();

    /**
     * Get all the sequence aligners.
     *
     * @return a collection of {@link DisplayableItem} objects modeling a sequence aligner
     */
    Collection<DisplayableItem> getSequenceAligners();
    /**
     * Get all the analysis types.
     *
     * @return a collection of {@link DisplayableItem} objects modeling an analysis type
     */
    Collection<DisplayableItem> getAnalysisTypes();

    /**
     * Get all the reagent designs (i.e. baits).
     *
     * @return a collection of {@link DisplayableItem} objects modeling a reagent design
     */
    Collection<DisplayableItem> getReagentDesigns();

    /**
     * Get a specific reagent design by its ID.
     *
     * @param businessKey the business key
     * @return a UI helper object {@link DisplayableItem} to display the reagent design information
     */
    DisplayableItem getReagentDesign(String businessKey);

    /**
     * Get a specific sequence aligner by its ID.
     *
     * @param businessKey the business key
     * @return a UI helper object {@link DisplayableItem} to display the sequence aligner information
     */
    DisplayableItem getSequenceAligner(String businessKey);

    /**
     * Get a specific reference sequence by its ID.
     *
     * @param businessKey the business key
     * @return a UI helper object {@link DisplayableItem} to display the reference sequence information
     */
    DisplayableItem getReferenceSequence(String businessKey);

    /**
     * Get a specific analysis type by its ID.
     *
     * @param businessKey the business key
     * @return a UI helper object {@link DisplayableItem} to display the analysis type information
     */
    DisplayableItem getAnalysisType(String businessKey);
}
