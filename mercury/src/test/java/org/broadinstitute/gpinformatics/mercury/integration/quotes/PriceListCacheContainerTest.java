package org.broadinstitute.gpinformatics.mercury.integration.quotes;

import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collection;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class PriceListCacheContainerTest extends Arquillian {

    @Inject
    private PriceListCache priceListCache;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testSanity() {

        Assert.assertNotNull(priceListCache);

        Collection<PriceItem> priceItems = priceListCache.getPriceItems();
        Assert.assertNotNull(priceItems);
        Assert.assertTrue(priceItems.size() > 10);

        for (PriceItem priceItem : priceItems) {
            Assert.assertNotNull(priceItem.getPlatformName(), priceItem.toString());
            // category actually can be null
            // Assert.assertNotNull(priceItem.getCategoryName(), priceItem.toString());
            Assert.assertNotNull(priceItem.getName(), priceItem.toString());
            Assert.assertNotNull(priceItem.getSubmittedDate(), priceItem.toString());
            Assert.assertNotNull(priceItem.getEffectiveDate(), priceItem.toString());
        }

    }

}
