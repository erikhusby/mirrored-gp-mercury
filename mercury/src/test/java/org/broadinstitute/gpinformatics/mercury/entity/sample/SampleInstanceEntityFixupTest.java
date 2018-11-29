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

    /**
     * Sets SampleInstanceEntity.experiment to null based on input from a csv file, for example:
     *    SUPPORT-4794 experiment should be null when aggregation particle is given
     *    11/8/18 14:59,311427562,96plex_1well_32,DEV-8866,DEV-8868
     *    11/8/18 14:59,311427563,96plex_1well_33,DEV-8866,DEV-8868
     * The first line is the fixup commentary. Subsequent lines are five fields, comma-delimited, unquoted:
     * field[0] - (ignored)
     * field[1] - Tube Barcode
     * field[2] - Sample Library Name from the Dev Tube upload spreadsheet
     * field[3] - Experiment from the Dev Tube upload spreadsheet
     * field[4] - Data Aggregator from the Dev Tube upload spreadsheet
     */
    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void support4794() throws Exception {
        userBean.loginOSUser();
        String filename = "sampleInstanceEntityDevTags.csv";

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource(filename));
        String fixupCommentary = lines.get(0);
        Assert.assertFalse(fixupCommentary.isEmpty());

        Assert.assertFalse(lines.subList(1, lines.size()).stream().
                filter(line -> line.split(",").length != 5).
                findFirst().isPresent(), "Subsequent lines must have five comma delimited, unquoted fields.");

        Set<String> barcodes = lines.subList(1, lines.size()).stream().
                map(line -> line.split(",")[1]).
                filter(barcode -> !barcode.isEmpty()).
                collect(Collectors.toSet());

        List<SampleInstanceEntity> sampleInstanceEntities = sampleInstanceEntityDao.findByBarcodes(barcodes);

        for (String line : lines.subList(1, lines.size())) {
            String[] fields = line.split(",");
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
        sampleInstanceEntityDao.persist(new FixupCommentary(fixupCommentary));
        sampleInstanceEntityDao.flush();
    }
}