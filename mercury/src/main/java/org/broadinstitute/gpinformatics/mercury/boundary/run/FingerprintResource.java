package org.broadinstitute.gpinformatics.mercury.boundary.run;

import clover.org.apache.commons.lang3.tuple.ImmutablePair;
import clover.org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.jetbrains.annotations.NotNull;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JAX-RS web service for fingerprints.
 */
@Path("/fingerprint")
@Stateful
@RequestScoped
public class FingerprintResource {

    @Inject
    private MercurySampleDao mercurySampleDao;

    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    public FingerprintsBean get(@QueryParam("lsids")List<String> lsids) {
        List<String> sampleIds = new ArrayList<>();
        for (String lsid : lsids) {
            sampleIds.add("SM-" + lsid.substring(lsid.lastIndexOf(':')));
        }
        Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(sampleIds);

        return buildFingerprintsBean(mapIdToMercurySample);
    }

    private static final String[] rsIds = {
            "rs10037858",
            "rs532905",
            "rs2229857",
            "rs6104310",
            "rs9304229",
            "rs2273827",
            "rs2036902",
            "rs6679393",
            "rs2369754",
            "rs1052053",
            "rs6726639",
            "rs2512276",
            "rs6565604",
            "rs6972020",
            "rs13269287",
            "rs10888734",
            "rs6966770",
            "rs2639",
            "rs10186291",
            "rs7598922",
            "rs2709828",
            "rs1131171",
            "rs7664169",
            "rs1437808",
            "rs11917105",
            "rs10876820",
            "rs2910006",
            "AMG_3b",
            "rs8015958",
            "rs3105047",
            "rs5009801",
            "rs9277471",
            "rs3744877",
            "rs1549314",
            "rs9369842",
            "rs390299",
            "rs1734422",
            "rs9466",
            "rs4517902",
            "rs6563098",
            "rs965897",
            "rs4580999",
            "rs1998603",
            "rs2840240",
            "rs4146473",
            "rs2070132",
            "rs2587507",
            "rs827113",
            "rs213199",
            "rs1028564",
            "rs1634997",
            "rs2241759",
            "rs10435864",
            "rs6140742",
            "rs2302768",
            "rs1051374",
            "rs6714975",
            "rs238148",
            "rs1075622",
            "rs6874609",
            "rs2549797",
            "rs4608",
            "rs7017199",
            "rs2590339",
            "rs10943605",
            "rs3824253",
            "rs2640464",
            "rs11204697",
            "rs2649123",
            "rs27141",
            "rs1158448",
            "rs7679911",
            "rs1045738",
            "rs12057639",
            "rs7949313",
            "rs3094698",
            "rs13030",
            "rs8500",
            "rs3732083",
            "rs1406957",
            "rs935765",
            "rs6484648",
            "rs1584717",
            "rs9374227",
            "rs11652797",
            "rs7152601",
            "rs2452785",
            "rs4572767",
            "rs2737706",
            "rs9809404",
            "rs1595271",
            "rs2049330",
            "rs1077393",
            "rs520806",
            "rs2108978",
            "rs753307"
    };
    private static final Map<String, Integer> mapRsIdToIndex = new LinkedHashMap<>();

    static {
        for (int i = 0; i < rsIds.length; i++) {
            mapRsIdToIndex.put(rsIds[i], i);
        }
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
                        fingerprintEntities.add(new ImmutablePair<>(
                                stringMercurySampleEntry.getKey(), fingerprint));
                    }
                } else {
                    throw new ResourceException("Expected 1 root sample for " + stringMercurySampleEntry.getKey() +
                            ", found " + rootSample.getRootSamples().size(), Response.Status.BAD_REQUEST);
                }
            } else {
                throw new ResourceException("Expected 1 vessel for " + stringMercurySampleEntry.getKey() +
                        ", found " + labVessels.size(), Response.Status.BAD_REQUEST);
            }
        }

        List<FingerprintBean> fingerprints = new ArrayList<>();
        for (Pair<String, Fingerprint> stringFingerprintPair : fingerprintEntities) {
            List<FingerprintCallsBean> calls = new ArrayList<>();
            Fingerprint fingerprint = stringFingerprintPair.getRight();
            for (int i = 0; i < rsIds.length; i++) {
                calls.add(new FingerprintCallsBean(rsIds[i], fingerprint.getGenotypes().substring(i * 2, i * 2 + 1),
                        fingerprint.getCallConfidences().substring(i * 4, i * 4 + 4)));
            }
            fingerprints.add(new FingerprintBean(stringFingerprintPair.getLeft(),
                    fingerprint.getDisposition().getAbbreviation(), fingerprint.getMercurySample().getSampleKey(),
                    fingerprint.getPlatform().name(), fingerprint.getGenomeBuild().name(),
                    fingerprint.getDateGenerated(), fingerprint.getGender().getAbbreviation(), calls));
        }

        return new FingerprintsBean(fingerprints);
    }

    @POST
    public void post(FingerprintBean fingerprintBean) {
        String sampleKey = fingerprintBean.getAliquotLsid().substring(fingerprintBean.getAliquotLsid().lastIndexOf(':'));
        MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleKey);
        if (mercurySample == null) {
            mercurySample = new MercurySample(sampleKey, MercurySample.MetadataSource.BSP);
            mercurySampleDao.persist(mercurySample);
        }

        String[] genotypes = new String[rsIds.length];
        String[] callConfidences = new String[rsIds.length];
        for (FingerprintCallsBean fingerprintCallsBean : fingerprintBean.getCalls()) {
            Integer index = mapRsIdToIndex.get(fingerprintCallsBean.getRsid());
            genotypes[index] = fingerprintCallsBean.getGenotype();
            callConfidences[index] = fingerprintCallsBean.getCallConfidence();
        }
        StringBuilder genotypesBuilder = new StringBuilder();
        StringBuilder callConfidencesBuilder = new StringBuilder();
        for (int i = 0; i < rsIds.length; i++) {
            genotypesBuilder.append(genotypes[i]);
            callConfidencesBuilder.append(callConfidences[i]);
        }

        new Fingerprint(mercurySample,
                Fingerprint.Disposition.byAbbreviation(fingerprintBean.getDisposition()),
                Fingerprint.Platform.valueOf(fingerprintBean.getPlatform()),
                Fingerprint.GenomeBuild.valueOf(fingerprintBean.getGenomeBuild()),
                fingerprintBean.getDateGenerated(),
                genotypesBuilder.toString(),
                callConfidencesBuilder.toString(),
                Fingerprint.Gender.byAbbreviation(fingerprintBean.getGender()),
                true);
        mercurySampleDao.flush();
    }
}
