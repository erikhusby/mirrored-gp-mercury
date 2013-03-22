package org.broadinstitute.gpinformatics.infrastructure.athena;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.infrastructure.test.ProductOrderFactory;

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

    private static final Long   TEST_CREATOR = 1111L;
    public static final  String rpSynopsis   = "Test synopsis";
    public static final String otherRpSynopsis = "To Study Stuff";

    @Override
    public ProductOrder retrieveProductOrderDetails(String poBusinessKey) {

        Map<String, ProductOrder> productOrderByBusinessKeyMap = ProductOrderFactory.buildTestProductOrderMap();

        ProductOrder testOrder1 = productOrderByBusinessKeyMap.get(poBusinessKey);
        if (testOrder1 == null) {
            testOrder1 = ProductOrderFactory.createDummyProductOrder(poBusinessKey);
        }
        productOrderByBusinessKeyMap.put(poBusinessKey, testOrder1);

        if (poBusinessKey == null) {
            testOrder1.getProduct().setWorkflowName(null);
        }

        return testOrder1;
    }

    @Override
    public Map<String, List<ProductOrderSample>> findMapSampleNameToPoSample(List<String> sampleNames) {
        Map<String, List<ProductOrderSample>> mapSampleIdToPdoSample = new HashMap<String, List<ProductOrderSample>>();
        ProductOrder productOrder = ProductOrderFactory.buildExExProductOrder(96);
        List<ProductOrderSample> samples = productOrder.getSamples();
        for (ProductOrderSample productOrderSample : samples) {
            mapSampleIdToPdoSample.put(productOrderSample.getSampleName(),
                    new ArrayList<ProductOrderSample>(Collections.singletonList(productOrderSample)));
        }
        return mapSampleIdToPdoSample;
    }

}
