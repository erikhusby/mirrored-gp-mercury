package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.sap.services.SAPIntegrationException;

import java.io.IOException;

/**
 * TODO scottmat fill in javadoc!!!
 */
public interface SapIntegrationService {

    String createOrder(ProductOrder placedOrder) throws SAPIntegrationException;

    String updateOrder(ProductOrder placedOrder) throws SAPIntegrationException;

    String findCustomer(Quote foundQuote, String companyCode) throws SAPIntegrationException;

    String billOrder(BillingSession sessionForBilling) throws SAPIntegrationException;

    void createProductInSAP(Product product) throws SAPIntegrationException;

    void changeProductInSAP(Product product) throws SAPIntegrationException;
}
