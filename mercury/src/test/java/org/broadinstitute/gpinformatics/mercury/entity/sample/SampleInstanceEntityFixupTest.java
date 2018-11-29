package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Test(groups = TestGroups.FIXUP)
public class SampleInstanceEntityFixupTest extends Arquillian {

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private SampleInstanceEntityDao sampleInstanceEntityDao;

    @Inject
    private UserBean userBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "DEV");
    }

    /** Sets SampleInstanceEntity.experiment to null based on input from a csv file. */
    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void support4794() throws Exception {
        userBean.loginOSUser();
        String filename = "sampleInstanceEntityDevTags.csv";

        for (String line : IOUtils.readLines(VarioskanParserTest.getTestResource(filename))) {
            String[] fields = line.split(",");
            Assert.assertEquals(fields.length, 5);
        }

        Set<String> barcodes = IOUtils.readLines(VarioskanParserTest.getTestResource(filename)).stream().
                map(line -> line.split(",")[1]).collect(Collectors.toSet());

        List<SampleInstanceEntity> sampleInstanceEntities = sampleInstanceEntityDao.findByBarcodes(barcodes);

        for (String line : IOUtils.readLines(VarioskanParserTest.getTestResource(filename))) {
            String[] fields = line.split(",");
            Assert.assertEquals(fields.length, 5);
            boolean found = false;
            for (SampleInstanceEntity sampleInstanceEntity : sampleInstanceEntities) {
                if (fields[1].equals(sampleInstanceEntity.getLabVessel().getLabel()) &&
                        fields[2].equals(sampleInstanceEntity.getSampleLibraryName()) &&
                        fields[3].equals(sampleInstanceEntity.getExperiment()) &&
                        fields[4].equals(sampleInstanceEntity.getAggregationParticle())) {
                    Assert.assertFalse(found, "multiple matches for " + line);
                    found = true;
                    System.out.println("Setting experiment to null on SampleInstanceEntity for " + line);
                    sampleInstanceEntity.setExperiment(null);
                }
            }
            Assert.assertTrue(found, "missing " + line);
        }
        sampleInstanceEntityDao.persist(new FixupCommentary(
                "SUPPORT-4794 experiment should be null when aggregation particle is given"));
        sampleInstanceEntityDao.flush();
    }
}
