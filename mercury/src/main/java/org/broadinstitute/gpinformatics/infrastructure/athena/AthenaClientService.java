package org.broadinstitute.gpinformatics.infrastructure.athena;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;

/**
 *
 * AthenaClientService provide
 *
 * @author Scott Matthews
 *         Date: 10/24/12
 *         Time: 4:46 PM
 */
public interface AthenaClientService {


    /**
     * Provides the means to find a Product order registered in Athena by the registered Business Key
     *
     * @param poBusinessKey
     * @return
     */
    public ProductOrder retrieveProductOrderDetails(String poBusinessKey);

}
