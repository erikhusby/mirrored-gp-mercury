package org.broadinstitute.gpinformatics.athena.entity.preference;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
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
import java.io.IOException;
import java.io.InputStream;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test to load preferences into the database.
 */
@Test(groups = TestGroups.FIXUP)
public class ColumnSetsPreferenceFixupTest extends Arquillian {

    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private UserBean userBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void labMetrics() {
        try {
            userBean.loginOSUser();

            saveColumnSets("GlobalLabMetricColumnSets.xml", PreferenceType.GLOBAL_LAB_METRIC_COLUMN_SETS,
                    "GPLIM-3940 Load Column Sets From File to Global Preference");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void saveColumnSets(String xmlFile, PreferenceType preferenceType, String reason) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(ColumnSetsPreference.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            // Make sure XML is valid to begin with!
            ColumnSetsPreference columnSetsPreference = (ColumnSetsPreference) unmarshaller.unmarshal(
                    VarioskanParserTest.getTestResource(xmlFile));
        } catch (JAXBException e) {
            Assert.fail("JAXB unmarshall failure", e );
        }

        Preference preference = preferenceDao.getGlobalPreference(preferenceType);
        InputStream in = VarioskanParserTest.getTestResource(xmlFile);
        String xml = IOUtils.toString( in, null );
        IOUtils.closeQuietly(in);
        if (preference == null) {
            preference = new Preference(userBean.getBspUser().getUserId(),
                    preferenceType, xml);
            preferenceDao.persist(preference);
        } else {
            preference.setData(xml);
        }
        preferenceDao.persist(new FixupCommentary(reason));
        preferenceDao.flush();
    }
}
