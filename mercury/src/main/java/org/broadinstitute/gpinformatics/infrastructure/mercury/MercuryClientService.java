package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.presentation.DisplayableItem;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;

/**
 * Interface of Mercury Services available to Athena.
 *
 * @author epolk
 */
public interface MercuryClientService extends Serializable {
    /**
     * Adds the specified product order samples to the pico bucket.
     *
     * @param pdo        the PDO that the samples belong to
     * @param samples    the (possibly subset of) PDO samples to process
     * @return the ProductOrderSamples that were successfully added to the pico bucket
     */
    Collection<ProductOrderSample> addSampleToPicoBucket(@Nonnull ProductOrder pdo,
                                                         @Nonnull Collection<ProductOrderSample> samples);

    /**
     * Get all the reference sequences.
     *
     * @return a collection of {@link DisplayableItem} objects modeling a reference sequence
     */
    public Collection<DisplayableItem> getReferenceSequences();

    /**
     * Get all the sequence aligners.
     *
     * @return a collection of {@link DisplayableItem} objects modeling a sequence aligner
     */
    public Collection<DisplayableItem> getSequenceAligners();
    /**
     * Get all the analysis types.
     *
     * @return a collection of {@link DisplayableItem} objects modeling an analysis type
     */
    public Collection<DisplayableItem> getAnalysisTypes();

    /**
     * Get all the reagent designs (i.e. baits).
     *
     * @return a collection of {@link DisplayableItem} objects modeling a reagent design
     */
    public Collection<DisplayableItem> getReagentDesigns();

    /**
     * Get a specific reagent design by its ID.
     *
     * @param businessKey the business key
     * @return a UI helper object {@link DisplayableItem} to display the reagent design information
     */
    public DisplayableItem getReagentDesign(String businessKey);

    /**
     * Get a specific sequence aligner by its ID.
     *
     * @param businessKey the business key
     * @return a UI helper object {@link DisplayableItem} to display the sequence aligner information
     */
    public DisplayableItem getSequenceAligner(String businessKey);

    /**
     * Get a specific reference sequence by its ID.
     *
     * @param businessKey the business key
     * @return a UI helper object {@link DisplayableItem} to display the reference sequence information
     */
    public DisplayableItem getReferenceSequence(String businessKey);

    /**
     * Get a specific analysis type by its ID.
     *
     * @param businessKey the business key
     * @return a UI helper object {@link DisplayableItem} to display the analysis type information
     */
    public DisplayableItem getAnalysisType(String businessKey);

}
