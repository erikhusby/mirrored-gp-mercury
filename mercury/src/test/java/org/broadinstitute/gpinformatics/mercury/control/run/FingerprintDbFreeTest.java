package org.broadinstitute.gpinformatics.mercury.control.run;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.SnpList;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Test(groups = TestGroups.DATABASE_FREE)
public class FingerprintDbFreeTest {

    private MercurySampleDao mockMercurySampleDao;
    private Map<String, MercurySample> mapSmidToMercurySample = new HashMap<>();
    private List<Fingerprint> fingerprints = new ArrayList<>();

    @BeforeMethod
    public void setUp() {
        mockMercurySampleDao = Mockito.mock(MercurySampleDao.class);
    }

    @Test
    public void testFindAnchor() throws ParseException {
        Map<Fingerprint, String> lodScoreMap = new HashMap<>();
        List<Fingerprint> expected = new ArrayList<>();
        List<Fingerprint> observed = new ArrayList<>();
        FingerprintEjb fingerprintEjb = new FingerprintEjb();
        MercurySample mercurySample = new MercurySample("SM-IJ2BQ", MercurySample.MetadataSource.MERCURY);
        String sampleId = "SM-IJ2BQ";
        Set<Metadata> metadata = new HashSet<>();
        Metadata metadata1 = new Metadata(Metadata.Key.PATIENT_ID, "CN");
        metadata.add(metadata1);
        MercurySampleData mercurySampleData = new MercurySampleData("SM-IJ2BQ", metadata);
        mercurySample.setSampleData(mercurySampleData);

        Fingerprint fingerprint1 = new Fingerprint(mercurySample,
                Fingerprint.Disposition.FAIL,
                Fingerprint.Platform.GENERAL_ARRAY,
                Fingerprint.GenomeBuild.HG19,
                DateUtils.parseDate("01/28/2019"),
                new SnpList("arrays1"),
                Fingerprint.Gender.MALE,
                true);
        Fingerprint fingerprint2 = new Fingerprint(mercurySample,
                Fingerprint.Disposition.PASS,
                Fingerprint.Platform.FLUIDIGM,
                Fingerprint.GenomeBuild.HG19,
                DateUtils.parseDate("05/28/2020"),
                new SnpList("arrays1"),
                Fingerprint.Gender.MALE,
                true);
        Fingerprint fingerprint3 = new Fingerprint(mercurySample,
                Fingerprint.Disposition.PASS,
                Fingerprint.Platform.GENERAL_ARRAY,
                Fingerprint.GenomeBuild.HG19,
                DateUtils.parseDate("03/28/2019"),
                new SnpList("arrays1"),
                Fingerprint.Gender.MALE,
                true);
        Fingerprint fingerprint4 = new Fingerprint(mercurySample,
                Fingerprint.Disposition.PASS,
                Fingerprint.Platform.GENERAL_ARRAY,
                Fingerprint.GenomeBuild.HG19,
                DateUtils.parseDate("03/28/2017"),
                new SnpList("arrays1"),
                Fingerprint.Gender.MALE,
                true);

        fingerprints.add(fingerprint1);
        fingerprints.add(fingerprint3);
        fingerprints.add(fingerprint4);

        mapSmidToMercurySample =
                mockMercurySampleDao.findMapIdToMercurySample(Arrays.asList(sampleId.split("\\s+")));
        fingerprintEjb.findAnchor(fingerprints, lodScoreMap, expected, observed);
        Assert.assertEquals("Anchor FP", lodScoreMap.get(fingerprint4));
        Assert.assertEquals("N/A", lodScoreMap.get(fingerprint1));
        Assert.assertEquals(true, !lodScoreMap.containsKey(fingerprint3));

        mapSmidToMercurySample.clear();
        lodScoreMap.clear();

        fingerprints.add(fingerprint2);
        mapSmidToMercurySample =
                mockMercurySampleDao.findMapIdToMercurySample(Arrays.asList(sampleId.split("\\s+")));
        fingerprintEjb.findAnchor(fingerprints, lodScoreMap, expected, observed);
        Assert.assertEquals("Anchor FP", lodScoreMap.get(fingerprint2));
        Assert.assertEquals("N/A", lodScoreMap.get(fingerprint1));
        Assert.assertEquals(true, !lodScoreMap.containsKey(fingerprint3));
        Assert.assertEquals(true, !lodScoreMap.containsKey(fingerprint4));
    }
}