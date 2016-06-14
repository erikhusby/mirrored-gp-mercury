package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;

import java.io.IOException;

/**
 * TODO scottmat fill in javadoc!!!
 */
public interface SapIntegrationService {
    String submitAge(String age) throws IOException;

    String createOrder(ProductOrder placedOrder);

    String updateOrder(ProductOrder placedOrder);

    String findCustomer(String quoteId);
}
