package org.broadinstitute.gpinformatics.infrastructure.athena;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;

import javax.annotation.Nonnull;
import javax.enterprise.inject.Alternative;
import java.util.*;

/**
 * @author Scott Matthews
 *         Date: 10/24/12
 *         Time: 4:47 PM
 */
@Stub
@Alternative
public class AthenaClientServiceStub implements AthenaClientService {

    private static final Long TEST_CREATOR = 1111L;
    public static final String rpSynopsis = "Test synopsis";
    public static final String otherRpSynopsis = "To Study Stuff";
    private static Map<String, ProductOrder> productOrderByBusinessKeyMap = new HashMap<String, ProductOrder>();

    @Override
    public synchronized ProductOrder retrieveProductOrderDetails(@Nonnull String poBusinessKey) {
        if (productOrderByBusinessKeyMap.isEmpty()) {
            productOrderByBusinessKeyMap = ProductOrderTestFactory.buildTestProductOrderMap();
        }

        return pullProductOrder(poBusinessKey);
    }

    private ProductOrder pullProductOrder(String poBusinessKey) {
        ProductOrder order = productOrderByBusinessKeyMap.get(poBusinessKey);
        if (order == null) {
            order = ProductOrderTestFactory.createDummyProductOrder(poBusinessKey);
            productOrderByBusinessKeyMap.put(poBusinessKey, order);
        }
        return order;
    }

    @Override
    public Map<String, List<ProductOrderSample>> findMapSampleNameToPoSample(List<String> sampleNames) {
        Map<String, List<ProductOrderSample>> mapSampleIdToPdoSample = new HashMap<String, List<ProductOrderSample>>();
        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        List<ProductOrderSample> samples = productOrder.getSamples();
        for (ProductOrderSample productOrderSample : samples) {
            mapSampleIdToPdoSample.put(productOrderSample.getSampleName(),
                    new ArrayList<ProductOrderSample>(Collections.singletonList(productOrderSample)));
        }
        return mapSampleIdToPdoSample;
    }

    @Override
    public Collection<ProductOrder> retrieveMultipleProductOrderDetails(@Nonnull Collection<String> poBusinessKeys) {
        if (productOrderByBusinessKeyMap.isEmpty()) {
            productOrderByBusinessKeyMap = ProductOrderTestFactory.buildTestProductOrderMap();
        }

        List<ProductOrder> productOrderList = new ArrayList<ProductOrder>(poBusinessKeys.size());

        for(String poKey:poBusinessKeys) {
            productOrderList.add(pullProductOrder(poKey));
        }

        return productOrderList;
    }

    public static synchronized void addProductOrder(ProductOrder productOrder) {
        productOrderByBusinessKeyMap.put(productOrder.getBusinessKey(), productOrder);
    }
}
