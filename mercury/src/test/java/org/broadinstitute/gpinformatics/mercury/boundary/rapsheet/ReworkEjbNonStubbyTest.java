package org.broadinstitute.gpinformatics.mercury.boundary.rapsheet;

import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class ReworkEjbNonStubbyTest extends Arquillian {

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private ReworkEjb reworkEjb;

    @Inject
    private BSPUserList bspUserList;
    private ProductOrder exExProductOrder1;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testCryovialNotKnownToMercury() throws InvalidProductException {
        /*
        SELECT
            *
        FROM
            bsp.bsp_batch bb
            INNER JOIN bsp.bsp_batch_derived_sample bbds
                ON   bbds.batch_id = bb.batch_id
            INNER JOIN bsp.bsp_sample bs
                ON   bs.sample_id = bbds.sample_id
        WHERE
            bb.process_type = 'STOOL_DISSECTION'
            AND bb.created_on between SYSDATE - 60 and SYSDATE - 30;
         */
        String cryovialSmId = "SM-IHW5S";
        Assert.assertNull(labVesselDao.findByIdentifier(cryovialSmId), "Tube should not be in Mercury");

        if (productOrderSampleDao.findBySamples(Collections.singletonList(cryovialSmId)).isEmpty()) {
            Product exExProduct = productDao.findByPartNumber("P-ESH-0058");
            ResearchProject researchProject = researchProjectDao.findByBusinessKey("RP-19");

            Date currDate = new Date();
            exExProductOrder1 = new ProductOrder(bspUserList.getByUsername("scottmat").getUserId(),
                    "Rework Integration TestOrder 1" + currDate.getTime(),
                    Collections.singletonList(new ProductOrderSample(cryovialSmId)), "GSP-123", exExProduct,
                    researchProject);
            exExProductOrder1.setProduct(exExProduct);
            exExProductOrder1.prepareToSave(bspUserList.getByUsername("scottmat"));
            String pdo1JiraKey = "PDO-SGM-RWINT_tst" + currDate.getTime() + 1;
            exExProductOrder1.setJiraTicketKey(pdo1JiraKey);
            exExProductOrder1.setOrderStatus(ProductOrder.OrderStatus.Submitted);
            productDao.persist(exExProductOrder1);
        }
        Collection<ReworkEjb.BucketCandidate> bucketCandidates = reworkEjb.findBucketCandidates(
                Collections.singletonList(cryovialSmId));
        Assert.assertEquals(bucketCandidates.size(), 1);

    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (reworkEjb == null) {
            return;
        }
        if (exExProductOrder1 != null) {
            exExProductOrder1.setOrderStatus(ProductOrder.OrderStatus.Completed);
            productDao.flush();
        }
    }

}
