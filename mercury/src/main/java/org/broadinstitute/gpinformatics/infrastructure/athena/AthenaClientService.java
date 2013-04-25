package org.broadinstitute.gpinformatics.infrastructure.athena;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

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
     * For a list of sample names, return corresponding ProductOrderSamples
     * @param sampleNames list of sample names
     * @return map from sample name to List of ProductOrderSample entity.  The list is empty if none were found for
     * the key.
     */
    Map<String, List<ProductOrderSample>> findMapSampleNameToPoSample(List<String> sampleNames);
}
