package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.products.ConcentrationRiskCriteria;
import org.broadinstitute.gpinformatics.athena.entity.products.NumericOperator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriteria;
import org.meanbean.lang.EquivalentFactory;
import org.meanbean.lang.Factory;
import org.meanbean.test.*;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 1/22/13
 * Time: 5:00 PM
 */
public class RiskItemTest {

    @Test( enabled = true)
    public void testBeaniness() {
        BeanTester tester = new BeanTester();
        Configuration configuration = new ConfigurationBuilder()
                .ignoreProperty("productOrderSample")
                .build();

        class RiskCriteriaFactory implements Factory<RiskCriteria> {
            @Override public RiskCriteria create() {
                ConcentrationRiskCriteria criterion = new ConcentrationRiskCriteria("test", NumericOperator.LESS_THAN, 100.0);
                return criterion;
            }
        }
        RiskCriteriaFactory riskCriteriaFactory = new RiskCriteriaFactory();
        tester.getFactoryCollection().addFactory(RiskCriteria.class, riskCriteriaFactory);


        class ProductFactory implements Factory<Product> {
            public final long ID = new Random().nextInt(Integer.MAX_VALUE);
            @Override public Product create() {
                Product product = new Product("Exome Express", null, "Exome Express", "P-EX-0002", new Date(), null,
                        1814400, 1814400, 184, null, null, null, true, "Exome Express", false);
                return product;
            }
        }
        ProductFactory productFactory = new ProductFactory();
        tester.getFactoryCollection().addFactory(Product.class, productFactory);
        tester.testBean(RiskItem.class, configuration);

    }

    @Test( enabled = true)
    public void testEquals() throws Exception {
        Configuration configuration = new ConfigurationBuilder()
                .ignoreProperty("productOrderSample")
                .build();

        class RiskCriteriaFactory implements EquivalentFactory<RiskCriteria> {
            @Override public RiskCriteria create() {
                ConcentrationRiskCriteria criterion = new ConcentrationRiskCriteria("test", NumericOperator.LESS_THAN, 100.0);
                return criterion;
            }
        }
        new EqualsMethodTester().testEqualsMethod(new RiskCriteriaFactory(), configuration);

    }

    @Test( enabled = true)
    public void testHashCode() throws Exception {

        class RiskCriteriaFactory implements EquivalentFactory<RiskCriteria> {
            @Override public RiskCriteria create() {
                ConcentrationRiskCriteria criterion = new ConcentrationRiskCriteria("test", NumericOperator.LESS_THAN, 100.0);
                return criterion;
            }
        }
        new HashCodeMethodTester().testHashCodeMethod(new RiskCriteriaFactory());
    }

}
