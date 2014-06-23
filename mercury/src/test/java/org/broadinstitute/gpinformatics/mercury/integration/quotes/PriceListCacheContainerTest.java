package org.broadinstitute.gpinformatics.mercury.integration.quotes;

import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
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


    @Test(groups = TestGroups.STANDARD)
    public void testSanity() {

        Assert.assertNotNull(priceListCache);

        Collection<QuotePriceItem> quotePriceItems = priceListCache.getQuotePriceItems();
        Assert.assertNotNull(quotePriceItems);
        Assert.assertTrue(quotePriceItems.size() > 10);

        for (QuotePriceItem quotePriceItem : quotePriceItems) {
            Assert.assertNotNull(quotePriceItem.getPlatformName(), quotePriceItem.toString());
            // category actually can be null
            // Assert.assertNotNull(quotePriceItem.getCategoryName(), quotePriceItem.toString());
            Assert.assertNotNull(quotePriceItem.getName(), quotePriceItem.toString());
            Assert.assertNotNull(quotePriceItem.getSubmittedDate(), quotePriceItem.toString());
            Assert.assertNotNull(quotePriceItem.getEffectiveDate(), quotePriceItem.toString());
        }

    }

}
