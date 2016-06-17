package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;

import java.io.IOException;

/**
 * TODO scottmat fill in javadoc!!!
 */
public interface SapIntegrationService {
    String submitAge(String age) throws IOException;

    String createOrder(ProductOrder placedOrder) throws SAPInterfaceException;

    String updateOrder(ProductOrder placedOrder) throws SAPInterfaceException;

    String findCustomer(Quote foundQuote) throws SAPInterfaceException;
}
