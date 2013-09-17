package org.broadinstitute.gpinformatics.infrastructure.athena;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * AthenaClientService provide
 *
 * @author Scott Matthews
 *         Date: 10/24/12
 *         Time: 4:46 PM
 */
public interface AthenaClientService extends Serializable {


    /**
     * Provides the means to find a Product order registered in Athena by the registered Business Key
     *
     * @param poBusinessKey
     * @return
     */
    ProductOrder retrieveProductOrderDetails(@Nonnull String poBusinessKey);

    /**
     * Provides the means to find a collection of Product orders registered in Athena by the registered Business Keys
     *
     * @param poBusinessKeys
     * @return
     */
    Collection<ProductOrder> retrieveMultipleProductOrderDetails(@Nonnull Collection<String> poBusinessKeys);

    /**
     * For a list of sample names, return corresponding ProductOrderSamples
     *
     * @param sampleNames list of sample names
     * @return map from sample name to List of ProductOrderSample entity.  The list is empty if none were found for
     * the key.
     */
    Map<String,Set<ProductOrderSample>> findMapSampleNameToPoSample(List<String> sampleNames);
}
