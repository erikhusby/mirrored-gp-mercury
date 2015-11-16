/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import java.io.InputStream;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.FIXUP)
public class WorkflowPrefXmlReloadFixupTest extends Arquillian {

    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private UserBean userBean;


    // Use (RC, "rc"), (PROD, "prod") to push the backfill to RC and production respectively.
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * Used to (initially) load workflow configuration from a file to a global preference
     */
    @Test(enabled = false)
    public void gplim3557LoadWorkflowConfigPrefsFromFile() throws Exception {

        userBean.loginOSUser();

        // Do an unmarshall for a validation sanity check
        // As of 05/2015, cannot marshall back to XML
        WorkflowConfig workflowConfig = null;

        try {
            JAXBContext jc = JAXBContext.newInstance(WorkflowConfig.class, WorkflowBucketDef.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            // Make sure XML is valid to begin with!
            workflowConfig = (WorkflowConfig) unmarshaller.unmarshal(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("WorkflowConfig.xml"));
        } catch (JAXBException e) {
            Assert.fail("JAXB unmarshall failure", e );
        }

        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("WorkflowConfig.xml");
        String xml = IOUtils.toString( in, null );
        IOUtils.closeQuietly(in);

        Preference workflowConfigPref = preferenceDao.getGlobalPreference(PreferenceType.WORKFLOW_CONFIGURATION);

        if( workflowConfigPref == null ) {
            workflowConfigPref = new Preference( userBean.getBspUser().getUserId(),
                    PreferenceType.WORKFLOW_CONFIGURATION, xml );
            preferenceDao.persist(workflowConfigPref);
        } else {
            workflowConfigPref.setData(xml);
            workflowConfigPref.setModifiedBy(userBean.getBspUser().getUserId());
            workflowConfigPref.setModifiedDate(new Date());
        }

        preferenceDao.persist(new FixupCommentary("GPLIM-3557 Load Workflow Config From File to Global Preference"));
        preferenceDao.flush();
    }

}
