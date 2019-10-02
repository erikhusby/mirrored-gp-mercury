/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.sap.entity.OrderCalculatedValues;
import org.broadinstitute.sap.entity.OrderValue;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SAPServiceFailure;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Dependent
@Default
public class SapIntegrationServiceThrowsExceptions implements SapIntegrationService {
    @Override
    public String createOrder(ProductOrder placedOrder) throws SAPIntegrationException, SAPServiceFailure {
        throw new SAPServiceFailure("No createOrder!!");
    }

    @Override
    public void updateOrder(ProductOrder placedOrder, boolean closingOrder)
        throws SAPIntegrationException, SAPServiceFailure {
        throw new SAPServiceFailure("No updateOrder!!");
    }

    @Override
    public String billOrder(QuoteImportItem item, BigDecimal quantityOverride, Date workCompleteDate)
        throws SAPIntegrationException, SAPServiceFailure {
        throw new SAPServiceFailure("No billOrder!!");
    }

    @Override
    public void publishProductInSAP(Product product) throws SAPIntegrationException, SAPServiceFailure {
        throw new SAPServiceFailure("No publishProductInSAP!!");
    }

    @Override
    public void publishProductInSAP(Product product, boolean extendProductsToOtherPlatforms, PublishType publishType)
        throws SAPIntegrationException, SAPServiceFailure {
        throw new SAPServiceFailure("No publishProductInSAP!!");
    }

    @Override
    public Set<SAPMaterial> findProductsInSap() throws SAPIntegrationException, SAPServiceFailure {
        throw new SAPServiceFailure("No findProductsInSap!!");
    }

    @Override
    public OrderCalculatedValues calculateOpenOrderValues(int addedSampleCount, SapQuote sapQuote,
                                                          ProductOrder productOrder)
        throws SAPIntegrationException, SAPServiceFailure {
        throw new SAPServiceFailure("No calculateOpenOrderValues!!");
    }

    @Override
    public boolean isSapServiceAvailable() {
        return false;
    }

    @Override
    public SapQuote findSapQuote(String sapQuoteId) throws SAPIntegrationException, SAPServiceFailure {
        throw new SAPServiceFailure("No findSapQuote!!");
    }

    @Override
    public String creditDelivery(String deliveryDocumentId, QuoteImportItem quoteItemForBilling)
        throws SAPIntegrationException, SAPServiceFailure {
        throw new SAPServiceFailure("No creditDelivery!!");
    }
}
