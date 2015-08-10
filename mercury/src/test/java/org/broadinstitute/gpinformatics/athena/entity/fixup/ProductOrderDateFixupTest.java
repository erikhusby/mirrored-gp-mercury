/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.text.ParseException;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.lessThan;

@Test(groups = TestGroups.FIXUP)
public class ProductOrderDateFixupTest extends Arquillian {
    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private UserBean userBean;

    @Inject
    private BSPUserList bspUserList;

    // When you run this on prod, change to PROD and prod.
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void fixupGplim3690() throws ParseException {
        userBean.loginOSUser();

        String orderKey = "PDO-6790";
        ProductOrder order = productOrderDao.findByBusinessKey(orderKey);

        if (order != null) {
            Date originalPlacedDate = order.getPlacedDate();
            Date earlierDate = DateUtils.parseDate("02/18/2015");

            assertThat(earlierDate, lessThan(originalPlacedDate));

            order.setPlacedDate(earlierDate);
            productOrderDao.persist(
                    new FixupCommentary("See https://gpinfojira.broadinstitute.org:8443/jira/browse/GPLIM-3690"));
        } else {
            Assert.fail("Could not find " + orderKey);
        }
    }
}
