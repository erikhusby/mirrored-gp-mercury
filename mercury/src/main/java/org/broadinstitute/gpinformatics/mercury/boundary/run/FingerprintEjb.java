package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.mercury.control.dao.run.FingerprintDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.SnpListDao;
import org.broadinstitute.gpinformatics.mercury.control.run.ConcordanceCalculator;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.FpGenotype;
import org.broadinstitute.gpinformatics.mercury.entity.run.Snp;
import org.broadinstitute.gpinformatics.mercury.entity.run.SnpList;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Stateful
@RequestScoped
public class FingerprintEjb {

    @Inject
    private SnpListDao snpListDao;

    @Inject
    private FingerprintDao fingerprintDao;

    public Double handleNewFingerprint(FingerprintBean fingerprintBean, MercurySample mercurySample, Fingerprint fluidigmFingerprint) {
        if (fingerprintBean.getSnpListName() == null) {
            throw new RuntimeException("snpListName is required");
        }
        SnpList snpList = snpListDao.findByName(fingerprintBean.getSnpListName());
        if (snpList == null) {
            throw new RuntimeException("snpListName not found");
        }

        Fingerprint fingerprint = fingerprintDao.findBySampleAndDateGenerated(mercurySample,
                fingerprintBean.getDateGenerated());
        if (fingerprint == null) {
            fingerprint = new Fingerprint(mercurySample,
                    Fingerprint.Disposition.byAbbreviation(fingerprintBean.getDisposition()),
                    Fingerprint.Platform.valueOf(fingerprintBean.getPlatform()),
                    Fingerprint.GenomeBuild.valueOf(fingerprintBean.getGenomeBuild()),
                    fingerprintBean.getDateGenerated(),
                    snpList,
                    Fingerprint.Gender.byAbbreviation(fingerprintBean.getGender()),
                    true);

        } else {
            fingerprint.setDisposition(Fingerprint.Disposition.byAbbreviation(fingerprintBean.getDisposition()));
            fingerprint.setPlatform(Fingerprint.Platform.valueOf(fingerprintBean.getPlatform()));
            fingerprint.setGenomeBuild(Fingerprint.GenomeBuild.valueOf(fingerprintBean.getGenomeBuild()));
            fingerprint.setGender(Fingerprint.Gender.byAbbreviation(fingerprintBean.getGender()));
            fingerprint.getFpGenotypes().clear();
        }
        if (fingerprintBean.getCalls() != null) {
            for (FingerprintCallsBean fingerprintCallsBean : fingerprintBean.getCalls()) {
                if (fingerprintCallsBean != null) {
                    Snp snp = snpList.getMapRsIdToSnp().get(fingerprintCallsBean.getRsid());
                    if (snp == null) {
                        throw new RuntimeException("Snp not found: " + fingerprintCallsBean.getRsid());
                    }
                    BigDecimal callConfidence = (fingerprintCallsBean.getCallConfidence() == null) ? null :
                            new BigDecimal(fingerprintCallsBean.getCallConfidence());
                    fingerprint.addFpGenotype(new FpGenotype(fingerprint, snp, fingerprintCallsBean.getGenotype(),
                            callConfidence));
                }
            }
        }

        Double lodScore = null;
        if (fluidigmFingerprint != null) {
            ConcordanceCalculator concordanceCalculator = new ConcordanceCalculator();
            lodScore = concordanceCalculator.calculateLodScore(fingerprint, fluidigmFingerprint);
            concordanceCalculator.done();
        }

        // TODO Don't store for now. Also, don't store on the stock?
        if (!mercurySample.getFingerprints().remove(fingerprint)) {
            throw new RuntimeException("Failed to delete fingerprint from mercury sample " + mercurySample.getSampleKey());
        }

        return lodScore;
    }
}
