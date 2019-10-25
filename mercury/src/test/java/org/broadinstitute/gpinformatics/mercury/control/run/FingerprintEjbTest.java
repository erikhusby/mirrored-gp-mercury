package org.broadinstitute.gpinformatics.mercury.control.run;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class FingerprintEjbTest extends Arquillian {

    private Map<String, MercurySample> mapIdToMercurySample = new HashMap<>();
    private List<Fingerprint> fingerprints;
    private Set<Fingerprint.Platform> platforms;
    private List<String> smid;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private FingerprintEjb fingerprintEjb;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testGetPtIdMercurySamples() {
        mapIdToMercurySample = fingerprintEjb.getPtIdMercurySamples(mapIdToMercurySample, "PT-22SS1", mercurySampleDao);
        smid = mapIdToMercurySample.values().stream()
                .map(MercurySample::getSampleKey)
                .collect(Collectors.toList());

        Assert.assertEquals(true, smid.contains("SM-F29II"));
        Assert.assertEquals(true, smid.contains("SM-J6SSU"));
    }

    @Test
    public void testFindFingerprints() {
        mapIdToMercurySample = fingerprintEjb.getPtIdMercurySamples(mapIdToMercurySample, "PT-22SS1", mercurySampleDao);
        smid = mapIdToMercurySample.values().stream()
                .map(MercurySample::getSampleKey)
                .collect(Collectors.toList());

        platforms = EnumSet.allOf(Fingerprint.Platform.class);
        fingerprints = fingerprintEjb.findFingerprints(mapIdToMercurySample);

        Assert.assertNotNull(fingerprints);
        Assert.assertEquals(true, fingerprints.size() != 0);
    }

    @Test
    public void testMakeMatrix() {
        mapIdToMercurySample = fingerprintEjb.getPtIdMercurySamples(mapIdToMercurySample, "PT-22SS1", mercurySampleDao);
        smid = mapIdToMercurySample.values().stream()
                .map(MercurySample::getSampleKey)
                .collect(Collectors.toList());

        platforms = EnumSet.allOf(Fingerprint.Platform.class);
        fingerprints = fingerprintEjb.findFingerprints(mapIdToMercurySample);
        Workbook workbook = fingerprintEjb.makeMatrix(fingerprints,
                platforms);

        Assert.assertNotNull(workbook);

        Sheet sheet = workbook.getSheet("Fluidigm Matrix");
        Assert.assertNotNull(sheet);
        for (Row row : sheet) {
            for (Cell cell : row) {
                short lastCellNum = row.getLastCellNum();
                System.out.println(lastCellNum);
                // Skip first, assert rest
                if (row.getRowNum() == 0) {
                    if (cell.getColumnIndex() != 0) {
                        Assert.assertEquals(true, smid.contains(cell.getStringCellValue()));
                    }
                } else if (cell.getColumnIndex() != 0) {
                    Assert.assertNotNull(cell.getNumericCellValue());
                }
            }
        }
    }
}