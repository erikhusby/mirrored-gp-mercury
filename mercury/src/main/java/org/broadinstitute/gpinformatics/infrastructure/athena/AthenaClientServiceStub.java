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

    public static final Long TEST_CREATOR = 1111L;
    public static final String pdoTitle= "Test synopsis";
    public static final String rpSynopsis = "Test synopsis";
    public static final String otherRpSynopsis = "To Study Stuff";
    private static Map<String, ProductOrder> productOrderByBusinessKeyMap = new HashMap<>();

}
