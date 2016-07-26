package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.sap.services.SAPIntegrationException;

import javax.enterprise.inject.Alternative;

@Stub
@Alternative
public class SapIntegrationServiceStub implements SapIntegrationService {

    @Override
    public String createOrder(ProductOrder placedOrder) throws SAPIntegrationException {
        return null;
    }

    @Override
    public String updateOrder(ProductOrder placedOrder) throws SAPIntegrationException {
        return null;
    }

    @Override
    public String findCustomer(Quote foundQuote, String companyCode) throws SAPIntegrationException {
        return null;
    }

    @Override
    public String billOrder(QuoteImportItem item) throws SAPIntegrationException {
        return null;
    }

    @Override
    public void createProductInSAP(Product product) throws SAPIntegrationException {

    }

    @Override
    public void changeProductInSAP(Product product) throws SAPIntegrationException {

    }
}
