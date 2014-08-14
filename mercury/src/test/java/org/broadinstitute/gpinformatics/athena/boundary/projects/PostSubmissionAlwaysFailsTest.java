/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDtoFetcher;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsWillAlwaysFailSubmissionsService;
import org.broadinstitute.gpinformatics.infrastructure.test.AbstractContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.ALTERNATIVES)
public class PostSubmissionAlwaysFailsTest extends AbstractContainerTest {
    @Inject
    private ProductOrderDao productOrderDao;
    @Inject
    private ResearchProjectEjb researchProjectEjb;
    @Inject
    private SubmissionDtoFetcher submissionDtoFetcher;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DEV, SubmissionsWillAlwaysFailSubmissionsService.class);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        if (!isRunningInContainer()) {
            return;
        }
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (!isRunningInContainer()) {
            return;
        }
    }


    public void testValidationMessage() throws ValidationException {
        ResearchProject researchProject= ResearchProjectTestFactory.createTestResearchProject("PDO-1438");
        ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(productOrderDao, "SM-4B2JK");
        researchProject.addProductOrder(productOrder);

        List<SubmissionDto> foundDTOs = submissionDtoFetcher.fetch(researchProject);
        researchProjectEjb.processSubmissions(researchProject.getBusinessKey(), new BioProject("PRJNA238373"), foundDTOs);
    }
}
