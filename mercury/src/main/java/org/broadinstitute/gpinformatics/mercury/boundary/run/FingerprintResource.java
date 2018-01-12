package org.broadinstitute.gpinformatics.mercury.boundary.run;

import clover.org.apache.commons.lang3.tuple.ImmutablePair;
import clover.org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.SnpListDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.FpGenotype;
import org.broadinstitute.gpinformatics.mercury.entity.run.Snp;
import org.broadinstitute.gpinformatics.mercury.entity.run.SnpList;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.jetbrains.annotations.NotNull;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * JAX-RS web service for fingerprints.
 */
@Path("/fingerprint")
@Stateful
@RequestScoped
public class FingerprintResource {

    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile("\\d{1,2}\\.\\d{1,2}|100.0");

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private SnpListDao snpListDao;

    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    public FingerprintsBean get(@QueryParam("lsids")List<String> lsids) {
        List<String> sampleIds = new ArrayList<>();
        for (String lsid : lsids) {
            sampleIds.add("SM-" + lsid.substring(lsid.lastIndexOf(':') + 1));
        }
        Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(sampleIds);

        return buildFingerprintsBean(mapIdToMercurySample);
    }

    @GET
    @Path("/rsids")
    @Produces(MediaType.APPLICATION_JSON)
    public RsIdsBean get() {
        // todo jmt return multiple lists?
        SnpList snpList = snpListDao.findByName("FluidigmFPv5");

        List<String> rsIds = new ArrayList<>();
        for (Snp snp : snpList.getSnps()) {
            rsIds.add(snp.getRsId());
        }

        return new RsIdsBean(rsIds);
    }

    @NotNull
    @DaoFree
    private FingerprintsBean buildFingerprintsBean(Map<String, MercurySample> mapIdToMercurySample) {
        List<Pair<String, Fingerprint>> fingerprintEntities = new ArrayList<>();
        for (Map.Entry<String, MercurySample> stringMercurySampleEntry : mapIdToMercurySample.entrySet()) {
            if (stringMercurySampleEntry.getValue() == null) {
                throw new ResourceException("Sample not found: " + stringMercurySampleEntry.getKey(),
                        Response.Status.BAD_REQUEST);
            }

            if (stringMercurySampleEntry.getValue().getFingerprints().isEmpty()) {
                // Traverse to (new) root
                Set<LabVessel> labVessels = stringMercurySampleEntry.getValue().getLabVessel();
                if (labVessels.size() == 1) {
                    LabVessel labVessel = labVessels.iterator().next();
                    TransferTraverserCriteria.RootSample rootSample = new TransferTraverserCriteria.RootSample();
                    labVessel.evaluateCriteria(rootSample, TransferTraverserCriteria.TraversalDirection.Ancestors);
                    // Traverse to fingerprints
                    if (rootSample.getRootSamples().size() == 1) {
                        MercurySample mercurySample = rootSample.getRootSamples().iterator().next();
                        TransferTraverserCriteria.Fingerprints fpCriteria = new TransferTraverserCriteria.Fingerprints();
                        // todo jmt check for multiple
                        mercurySample.getLabVessel().iterator().next().evaluateCriteria(
                                fpCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
                        for (Fingerprint fingerprint : fpCriteria.getFingerprints()) {
                            fingerprintEntities.add(new ImmutablePair<>(stringMercurySampleEntry.getKey(), fingerprint));
                        }
                        // todo jmt call BSP
                    } else {
                        throw new ResourceException("Expected 1 root sample for " + stringMercurySampleEntry.getKey() +
                                ", found " + rootSample.getRootSamples().size(), Response.Status.BAD_REQUEST);
                    }
                } else {
                    throw new ResourceException("Expected 1 vessel for " + stringMercurySampleEntry.getKey() +
                            ", found " + labVessels.size(), Response.Status.BAD_REQUEST);
                }
            } else {
                fingerprintEntities.add(new ImmutablePair<>(stringMercurySampleEntry.getKey(),
                        stringMercurySampleEntry.getValue().getFingerprints().iterator().next()));
            }
        }

        List<FingerprintBean> fingerprints = new ArrayList<>();
        for (Pair<String, Fingerprint> stringFingerprintPair : fingerprintEntities) {
            List<FingerprintCallsBean> calls = new ArrayList<>();
            Fingerprint fingerprint = stringFingerprintPair.getRight();
            for (FpGenotype fpGenotype : fingerprint.getFpGenotypesOrdered()) {
                calls.add(new FingerprintCallsBean(fpGenotype.getSnp().getRsId(), fpGenotype.getGenotype(),
                        fpGenotype.getCallConfidence().toString()));
            }

            fingerprints.add(new FingerprintBean(stringFingerprintPair.getLeft(),
                    fingerprint.getDisposition().getAbbreviation(), fingerprint.getMercurySample().getSampleKey(),
                    fingerprint.getPlatform().name(), fingerprint.getGenomeBuild().name(),
                    fingerprint.getSnpList().getName(), fingerprint.getDateGenerated(),
                    fingerprint.getGender().getAbbreviation(), calls));
        }

        return new FingerprintsBean(fingerprints);
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public String post(FingerprintBean fingerprintBean) {

        String sampleKey = "SM-" + fingerprintBean.getAliquotLsid().substring(fingerprintBean.getAliquotLsid().
                lastIndexOf(':') + 1);
        MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleKey);
        if (mercurySample == null) {
            mercurySample = new MercurySample(sampleKey, MercurySample.MetadataSource.BSP);
            mercurySampleDao.persist(mercurySample);
        }

        SnpList snpList = snpListDao.findByName(fingerprintBean.getSnpListName());

        Fingerprint fingerprint = new Fingerprint(mercurySample,
                Fingerprint.Disposition.byAbbreviation(fingerprintBean.getDisposition()),
                Fingerprint.Platform.valueOf(fingerprintBean.getPlatform()),
                Fingerprint.GenomeBuild.valueOf(fingerprintBean.getGenomeBuild()),
                fingerprintBean.getDateGenerated(),
                snpList, Fingerprint.Gender.byAbbreviation(fingerprintBean.getGender()),
                true);
        for (FingerprintCallsBean fingerprintCallsBean : fingerprintBean.getCalls()) {
            String callConfidence = fingerprintCallsBean.getCallConfidence();
            if (!CONFIDENCE_PATTERN.matcher(callConfidence).matches()) {
                throw new ResourceException(callConfidence + " is incorrect format", Response.Status.BAD_REQUEST);
            }
            Snp snp = snpList.getMapRsIdToSnp().get(fingerprintCallsBean.getRsid());
            if (snp == null) {
                throw new ResourceException("Snp not found: " + fingerprintCallsBean.getRsid(),
                        Response.Status.BAD_REQUEST);
            }
            fingerprint.addFpGenotype(new FpGenotype(fingerprint, snp, fingerprintCallsBean.getGenotype(),
                    new BigDecimal(callConfidence)));
        }

        // todo jmt check concordance
//        FingerprintChecker fingerprintChecker = new FingerprintChecker();
//        MatchResults matchResults = fingerprintChecker.calculateMatchResults(new picard.fingerprint.Fingerprint(), );
//        matchResults.getLOD();
        mercurySampleDao.flush();
        return "Stored fingerprint";
    }
}
