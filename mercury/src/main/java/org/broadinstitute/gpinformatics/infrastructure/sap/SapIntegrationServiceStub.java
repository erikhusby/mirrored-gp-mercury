package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;

import javax.enterprise.inject.Alternative;
import java.math.BigDecimal;

@Stub
@Alternative
public class SapIntegrationServiceStub implements SapIntegrationService {

    public static final String TEST_SAP_NUMBER = "Test000001";
    public static final String TEST_CUSTOMER_NUMBER = "CUST_000002";
    public static final String TEST_DELIVERY_DOCUMENT_ID = "DD_0000003";

    @Override
    public String createOrder(ProductOrder placedOrder) throws SAPIntegrationException {
        return TEST_SAP_NUMBER;
    }

    @Override
    public void updateOrder(ProductOrder placedOrder) throws SAPIntegrationException {
    }

    @Override
    public String findCustomer(SapIntegrationClientImpl.SAPCompanyConfiguration companyCode, FundingLevel fundingLevel) throws SAPIntegrationException {
        return TEST_CUSTOMER_NUMBER;
    }

    @Override
    public String billOrder(QuoteImportItem item, BigDecimal quantityOverride) throws SAPIntegrationException {
        return TEST_DELIVERY_DOCUMENT_ID;
    }

    @Override
    public void createProductInSAP(Product product) throws SAPIntegrationException {

    }

    @Override
    public void changeProductInSAP(Product product) throws SAPIntegrationException {

    }

    @Override
    public SapIntegrationClientImpl.SAPCompanyConfiguration determineCompanyCode(ProductOrder companyProductOrder) {
        return SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
    }
}
