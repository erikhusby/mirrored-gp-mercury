package org.broadinstitute.gpinformatics.mercury.boundary.run;

import clover.org.apache.commons.lang3.tuple.ImmutablePair;
import clover.org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGetExportedSamplesFromAliquots;
import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.IsExported;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.security.Role;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.FingerprintDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.SnpListDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.FpGenotype;
import org.broadinstitute.gpinformatics.mercury.entity.run.Snp;
import org.broadinstitute.gpinformatics.mercury.entity.run.SnpList;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jetbrains.annotations.NotNull;

import javax.annotation.security.RolesAllowed;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Comparator.reverseOrder;
import static org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean.CRSP_LSID_PREFIX;
import static org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean.MERCURY_LSID_PREFIX;

/**
 * JAX-RS web service for fingerprints.
 */
@Path("/external/fingerprint")
@RolesAllowed(Role.Constants.FINGERPRINT_WEB_SERVICE)
@Stateful
@RequestScoped
public class FingerprintResource {

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private SnpListDao snpListDao;

    @Inject
    private BSPGetExportedSamplesFromAliquots bspGetExportedSamplesFromAliquots;

    @Inject
    private FingerprintDao fingerprintDao;

    @Inject
    private UserBean userBean;

    /**
     * The lsids from the pipeline are sequencing aliquots, so we need to find the associated fingerprinting aliquot(s).
     * Prior to this code being deployed to production, all Fluidigm fingerprints are backfilled to their associated
     * MercurySamples.  However, the BSP transfers that made these plates are not in Mercury, so we have to call
     * getExportedSamplesFromAliquots.
     */
    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    public FingerprintsBean get(@QueryParam("lsids")List<String> lsids) {
        // Extract SM-IDs from LSIDs
        Set<String> sampleIds = new HashSet<>();
        Map<String, String> mapSmidToQueriedLsid = new HashMap<>();
        for (String lsid : lsids) {
            String smId = getSmIdFromLsid(lsid);
            sampleIds.add(smId);
            mapSmidToQueriedLsid.put(smId, lsid);
        }

        // Make list of samples for which to query BSP
        Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(sampleIds);
        List<String> bspLsids = new ArrayList<>();
        Map<String, String> mapSmidToFpLsid = new HashMap<>();
        for (Map.Entry<String, MercurySample> idMercurySampleEntry : mapIdToMercurySample.entrySet()) {
            MercurySample mercurySample = idMercurySampleEntry.getValue();
            if (mercurySample == null || mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP) {
                bspLsids.add(mapSmidToQueriedLsid.get(idMercurySampleEntry.getKey()));
            } else {
                mapSmidToFpLsid.put(idMercurySampleEntry.getKey(), mapSmidToQueriedLsid.get(idMercurySampleEntry.getKey()));
            }
        }

        // Query BSP for FP aliquots for given (sequencing) aliquots
        Multimap<String, BSPGetExportedSamplesFromAliquots.ExportedSample> mapLsidToExportedSample =
                ArrayListMultimap.create();
        if (!bspLsids.isEmpty()) {
            List<BSPGetExportedSamplesFromAliquots.ExportedSample> samplesExportedFromBsp =
                    bspGetExportedSamplesFromAliquots.getExportedSamplesFromAliquots(bspLsids, IsExported.ExternalSystem.GAP);
            if (samplesExportedFromBsp.size() > 100) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "MM/dd/yyyy" );
                samplesExportedFromBsp.sort((o1, o2) -> LocalDate.parse(o2.getExportDate(), formatter).
                        compareTo(LocalDate.parse(o1.getExportDate(), formatter)));
                samplesExportedFromBsp = samplesExportedFromBsp.subList(0, 100);
            }
            Set<String> bspSampleIds = new HashSet<>();
            for (BSPGetExportedSamplesFromAliquots.ExportedSample exportedSample : samplesExportedFromBsp) {
                mapLsidToExportedSample.put(exportedSample.getLsid(), exportedSample);
                String exportedLsid = exportedSample.getExportedLsid();
                String smId = getSmIdFromLsid(exportedLsid);
                bspSampleIds.add(smId);
                mapSmidToFpLsid.put(smId, exportedLsid);
            }
            mapIdToMercurySample.putAll(mercurySampleDao.findMapIdToMercurySample(bspSampleIds));
        }

        // Map queried LSIDs to MercurySamples that may have fingerprints
        Multimap<String, MercurySample> mapLsidToFpSamples = HashMultimap.create();
        for (String lsid : lsids) {
            MercurySample mercurySample = mapIdToMercurySample.get(getSmIdFromLsid(lsid));
            if (mercurySample == null) {
                throw new ResourceException("Sample not found: " + lsid, Response.Status.BAD_REQUEST);
            }
            mapLsidToFpSamples.put(lsid, mercurySample);
            for (BSPGetExportedSamplesFromAliquots.ExportedSample exportedSample : mapLsidToExportedSample.get(lsid)) {
                mapLsidToFpSamples.put(lsid, mapIdToMercurySample.get(getSmIdFromLsid(exportedSample.getExportedLsid())));
            }
        }

        return buildFingerprintsBean(lsids, mapLsidToFpSamples, mapSmidToFpLsid);
    }

    @NotNull
    public static String getSmIdFromLsid(String lsid) {
        if (lsid.startsWith("broadinstitute.org:bsp") || lsid.startsWith(CRSP_LSID_PREFIX)) {
            return "SM-" + lsid.substring(lsid.lastIndexOf(':') + 1);
        } else if (lsid.startsWith(MERCURY_LSID_PREFIX)) {
            return lsid.substring(lsid.lastIndexOf(':') + 1);
        } else {
            throw new RuntimeException("Failed to convert lsid " + lsid);
        }
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
    private FingerprintsBean buildFingerprintsBean(List<String> lsids,
            Multimap<String, MercurySample> mapLsidToFpSamples, Map<String, String> mapSmidToLsid) {
        List<FingerprintBean> fingerprints = new ArrayList<>();
        for (String lsid : lsids) {
            Collection<MercurySample> mercurySamples = mapLsidToFpSamples.get(lsid);
            boolean foundAtLeastOne = false;
            for (MercurySample mercurySample : mercurySamples) {
                if (mercurySample == null) {
                    continue;
                }
                if (mercurySample.getFingerprints().isEmpty()) {
                    // todo jmt re-enable after Fluidigm moves to Mercury
//                traverseMercuryTransfers(fingerprintEntities, stringMercurySampleEntry);
                } else {
                    for (Fingerprint fingerprint : mercurySample.getFingerprints()) {
                        List<FingerprintCallsBean> calls = new ArrayList<>();
                        if (fingerprint.getDisposition() == Fingerprint.Disposition.PASS) {
                            for (FpGenotype fpGenotype : fingerprint.getFpGenotypesOrdered()) {
                                if (fpGenotype != null) {
                                    calls.add(new FingerprintCallsBean(fpGenotype.getSnp().getRsId(), fpGenotype.getGenotype(),
                                            fpGenotype.getCallConfidence().toString()));
                                }
                            }
                        }

                        String gender = null;
                        if (fingerprint.getDisposition() == Fingerprint.Disposition.PASS) {
                            if (fingerprint.getGender() == null) {
                                gender = Fingerprint.Gender.UNKNOWN.getAbbreviation();
                            } else {
                                gender = fingerprint.getGender().getAbbreviation();
                            }
                        }
                        String aliquotLsid = mapSmidToLsid.get(fingerprint.getMercurySample().getSampleKey());
                        if (aliquotLsid == null) {
                            aliquotLsid = lsid;
                        }
                        fingerprints.add(new FingerprintBean(lsid,
                                fingerprint.getDisposition().getAbbreviation(), aliquotLsid,
                                fingerprint.getPlatform().name(), fingerprint.getGenomeBuild().name(),
                                fingerprint.getSnpList().getName(), fingerprint.getDateGenerated(),
                                gender, calls));
                        foundAtLeastOne = true;
                    }
                }
            }
            if (!foundAtLeastOne) {
                fingerprints.add(new FingerprintBean(lsid, Fingerprint.Disposition.NONE.toString(), null, null,
                        null, null, null, null, Collections.emptyList()));
            }
        }
        fingerprints.sort(Comparator.comparing(FingerprintBean::getDateGenerated, Comparator.nullsLast(reverseOrder())));

        return new FingerprintsBean(fingerprints);
    }

    // todo jmt not currently used
    private void traverseMercuryTransfers(List<Pair<String, Fingerprint>> fingerprintEntities,
            Map.Entry<String, MercurySample> stringMercurySampleEntry) {
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
            } else {
                throw new ResourceException("Expected 1 root sample for " + stringMercurySampleEntry.getKey() +
                        ", found " + rootSample.getRootSamples().size(), Response.Status.BAD_REQUEST);
            }
        } else {
            throw new ResourceException("Expected 1 vessel for " + stringMercurySampleEntry.getKey() +
                    ", found " + labVessels.size(), Response.Status.BAD_REQUEST);
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public String post(FingerprintBean fingerprintBean) {

        userBean.login("seqsystem"); // todo jmt add user to fingerprintBean?

        String sampleKey = getSmIdFromLsid(fingerprintBean.getAliquotLsid());
        MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleKey);
        if (mercurySample == null) {
            mercurySample = new MercurySample(sampleKey, MercurySample.MetadataSource.BSP);
            mercurySampleDao.persist(mercurySample);
        }

        if (fingerprintBean.getSnpListName() == null) {
            throw new ResourceException("snpListName is required", Response.Status.BAD_REQUEST);
        }
        SnpList snpList = snpListDao.findByName(fingerprintBean.getSnpListName());
        if (snpList == null) {
            throw new ResourceException("snpListName not found", Response.Status.BAD_REQUEST);
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
                        throw new ResourceException("Snp not found: " + fingerprintCallsBean.getRsid(),
                                Response.Status.BAD_REQUEST);
                    }
                    fingerprint.addFpGenotype(new FpGenotype(fingerprint, snp, fingerprintCallsBean.getGenotype(),
                            new BigDecimal(fingerprintCallsBean.getCallConfidence())));
                }
            }
        }

        // todo jmt check concordance
/*
        List<DownloadGenotypes.GapGetGenotypesResult> gapResults = new ArrayList<>();
        for (FingerprintCallsBean fingerprintCallsBean : fingerprintBean.getCalls()) {
            gapResults.add(new DownloadGenotypes.GapGetGenotypesResult(fingerprintBean.getAliquotLsid(),
                    sampleKey, fingerprintCallsBean.getRsid(), fingerprintCallsBean.getGenotype(),
                    fingerprintBean.getPlatform()));
        }
        HaplotypeMap haplotypes = new HaplotypeMap(new File(
                "\\\\iodine\\seq_references\\Homo_sapiens_assembly19\\v1\\Homo_sapiens_assembly19.haplotype_database.txt"));
        DownloadGenotypes downloadGenotypes = new DownloadGenotypes();
        List<DownloadGenotypes.SnpGenotype> snpGenotypes = downloadGenotypes.getGenotypesFromGap(gapResults, haplotypes);
        downloadGenotypes.cleanupGenotypes(snpGenotypes, haplotypes);
        File reference = new File(
                "\\\\iodine\\seq_references\\Homo_sapiens_assembly19\\v1\\Homo_sapiens_assembly19.fasta");
        File fpFile;
        try (final ReferenceSequenceFile ref = ReferenceSequenceFileFactory.getReferenceSequenceFile(reference)) {
            SequenceUtil.assertSequenceDictionariesEqual(ref.getSequenceDictionary(),
                    haplotypes.getHeader().getSequenceDictionary());
            SortedSet<VariantContext> variantContexts = downloadGenotypes.makeVariantContexts(snpGenotypes, sampleKey,
                    haplotypes, ref);
            fpFile = File.createTempFile("Fingerprint", ".vcf");
            downloadGenotypes.writeVcf(variantContexts, fpFile, reference, ref.getSequenceDictionary(), sampleKey);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        picard.fingerprint.Fingerprint observedFp = new picard.fingerprint.Fingerprint(sampleKey, fpFile, "");
        picard.fingerprint.Fingerprint expectedFp = new picard.fingerprint.Fingerprint(sampleKey, fpFile, "");
        MatchResults matchResults = FingerprintChecker.calculateMatchResults(observedFp, expectedFp);
        matchResults.getLOD();
*/

        mercurySampleDao.flush();
        return "Stored fingerprint";
    }

}
