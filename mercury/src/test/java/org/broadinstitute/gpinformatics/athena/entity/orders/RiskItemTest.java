package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.PipelineDataType;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.meanbean.lang.Factory;
import org.meanbean.test.BeanTester;
import org.meanbean.test.Configuration;
import org.meanbean.test.ConfigurationBuilder;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.Random;

@Test(groups = TestGroups.DATABASE_FREE)
public class RiskItemTest {

    @Test( enabled = true)
    public void testBeaniness() {
        BeanTester tester = new BeanTester();
        Configuration configuration = new ConfigurationBuilder()
                .ignoreProperty("productOrderSample")
                .build();

        class RiskCriteriaFactory implements Factory<RiskCriterion> {
            @Override public RiskCriterion create() {
                return new RiskCriterion(RiskCriterion.RiskCriteriaType.CONCENTRATION, Operator.LESS_THAN, "100.0");
            }
        }
        RiskCriteriaFactory riskCriteriaFactory = new RiskCriteriaFactory();
        tester.getFactoryCollection().addFactory(RiskCriterion.class, riskCriteriaFactory);


        class ProductFactory implements Factory<Product> {
            public final long ID = new Random().nextInt(Integer.MAX_VALUE);
            @Override public Product create() {
                Product product = new Product("Exome Express", null, "Exome Express", "P-EX-0002", new Date(), null,
                        1814400, 1814400, 184, null, null, null, true, Workflow.AGILENT_EXOME_EXPRESS, false,
                    new PipelineDataType(Aggregation.DATA_TYPE_EXOME, true));
                return product;
            }
        }
        ProductFactory productFactory = new ProductFactory();
        tester.getFactoryCollection().addFactory(Product.class, productFactory);
        tester.testBean(RiskItem.class, configuration);

    }
}
