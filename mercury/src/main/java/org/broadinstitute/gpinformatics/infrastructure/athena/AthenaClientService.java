package org.broadinstitute.gpinformatics.infrastructure.athena;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;

/**
 * @author Scott Matthews
 *         Date: 10/24/12
 *         Time: 4:46 PM
 */
public interface AthenaClientService {

    public ProductOrder retrieveProductOrderDetails(String poBusinessKey);

}
