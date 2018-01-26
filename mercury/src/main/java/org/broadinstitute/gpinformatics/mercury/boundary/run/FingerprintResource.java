package org.broadinstitute.gpinformatics.mercury.boundary.run;

import clover.org.apache.commons.lang3.tuple.ImmutablePair;
import clover.org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGetExportedSamplesFromAliquots;
import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.IsExported;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JAX-RS web service for fingerprints.
 */
@Path("/external/fingerprint")
@RolesAllowed("Mercury-FingerprintWebService")
@Stateful
@RequestScoped
public class FingerprintResource {

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private SnpListDao snpListDao;

    @Inject
    private BSPGetExportedSamplesFromAliquots bspGetExportedSamplesFromAliquots;

    /*
    The lsids from the pipeline are sequencing aliquots, so we need to find the associated fingerprinting aliquot(s).
    Prior to this code being deployed to production, all Fluidigm fingerprints are backfilled to their associated
    MercurySamples.  However, the BSP transfers that made these plates are not in Mercury, so we have to call
    getExportedSamplesFromAliquots.
     */

    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    public FingerprintsBean get(@QueryParam("lsids")List<String> lsids) {
        Set<String> sampleIds = new HashSet<>();
        for (String lsid : lsids) {
            sampleIds.add(getSmIdFromLsid(lsid));
        }
        List<BSPGetExportedSamplesFromAliquots.ExportedSamples> samplesExportedFromBsp =
                bspGetExportedSamplesFromAliquots.getExportedSamplesFromAliquots(lsids, IsExported.ExternalSystem.GAP);
        for (BSPGetExportedSamplesFromAliquots.ExportedSamples exportedSamples : samplesExportedFromBsp) {
            sampleIds.add(getSmIdFromLsid(exportedSamples.getExportedLsid()));
        }
        Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(sampleIds);

        return buildFingerprintsBean(mapIdToMercurySample);
    }

    @NotNull
    private String getSmIdFromLsid(String lsid) {
        return "SM-" + lsid.substring(lsid.lastIndexOf(':') + 1);
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
                // todo jmt re-enable after Fluidigm moves to Mercury
//                traverseMercuryTransfers(fingerprintEntities, stringMercurySampleEntry);
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

    // todo jmt is this a waste of time?  Will BSP continue to be the source of truth for fingerprinting transfers
    // for the foreseeable future?
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
                // todo jmt call BSP
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

        Fingerprint fingerprint = new Fingerprint(mercurySample,
                Fingerprint.Disposition.byAbbreviation(fingerprintBean.getDisposition()),
                Fingerprint.Platform.valueOf(fingerprintBean.getPlatform()),
                Fingerprint.GenomeBuild.valueOf(fingerprintBean.getGenomeBuild()),
                fingerprintBean.getDateGenerated(),
                snpList, Fingerprint.Gender.byAbbreviation(fingerprintBean.getGender()),
                true);

        for (FingerprintCallsBean fingerprintCallsBean : fingerprintBean.getCalls()) {
            Snp snp = snpList.getMapRsIdToSnp().get(fingerprintCallsBean.getRsid());
            if (snp == null) {
                throw new ResourceException("Snp not found: " + fingerprintCallsBean.getRsid(),
                        Response.Status.BAD_REQUEST);
            }
            fingerprint.addFpGenotype(new FpGenotype(fingerprint, snp, fingerprintCallsBean.getGenotype(),
                    new BigDecimal(fingerprintCallsBean.getCallConfidence())));
        }

        // todo jmt check concordance
/*
        HaplotypeMap haplotypes = new HaplotypeMap(
                new File("/notes/Homo_sapiens_assembly19.haplotype_database.txt"));
        DownloadGenotypes downloadGenotypes = new DownloadGenotypes();
        List<DownloadGenotypes.SnpGenotype> snpGenotypes = downloadGenotypes.getGenotypesFromGap(gapResults, haplotypes);
        downloadGenotypes.cleanupGenotypes(snpGenotypes, haplotypes);
        File reference = new File("c:/temp/Homo_sapiens_assembly19.fasta");
        try (final ReferenceSequenceFile ref = ReferenceSequenceFileFactory.getReferenceSequenceFile(reference)) {
            SequenceUtil.assertSequenceDictionariesEqual(ref.getSequenceDictionary(), haplotypes.getHeader().getSequenceDictionary());
            String sampleName = "";
            SortedSet<VariantContext> variantContexts = downloadGenotypes.makeVariantContexts(snpGenotypes, sampleName, haplotypes, ref);
            downloadGenotypes.writeVcf(variantContexts, File.createTempFile("FP", ".vcf"), reference, ref.getSequenceDictionary(), sampleName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
*/
/*
        FingerprintChecker fingerprintChecker = new FingerprintChecker(haplotypes);
        picard.fingerprint.Fingerprint observedFp = new picard.fingerprint.Fingerprint("", null, "");
        observedFp.add();
        picard.fingerprint.Fingerprint expectedFp = new picard.fingerprint.Fingerprint("", null, "");
        MatchResults matchResults = fingerprintChecker.calculateMatchResults(observedFp, expectedFp);
        matchResults.getLOD();
*/
        mercurySampleDao.flush();
        return "Stored fingerprint";
    }

/*
    private void x(String sample, HaplotypeMap haplotypes) {
        final SortedSet<picard.fingerprint.Snp> snps = new TreeSet<>(haplotypes.getAllSnps());
        for (final picard.fingerprint.Snp snp : snps) {

            final HaplotypeBlock h = haplotypes.getHaplotype(ctx.getContig(), ctx.getStart());
            final picard.fingerprint.Snp snp = haplotypes.getSnp(ctx.getContig(), ctx.getStart());
            if (h == null) return;

            final VariantContext usableSnp = AlleleSubsettingUtils.subsetVCToMatchSnp(ctx, snp);
            if (usableSnp == null) {
                return;
            }
            //PLs are preferred over GTs
            //TODO: this code is replicated in various places (ReconstructTriosFromVCF for example). Needs refactoring.
            //TODO: add a way to force using GTs when both are available (why?)

            // Get the genotype for the sample and check that it is useful
            final Genotype genotype = usableSnp.getGenotype(sample);
            if (genotype == null) {
                throw new IllegalArgumentException("Cannot find sample " + sample + " in provided file. ");
            }
            if (genotype.hasPL()) {

                final HaplotypeProbabilitiesFromGenotypeLikelihoods hFp = new HaplotypeProbabilitiesFromGenotypeLikelihoods(h);
                //do not modify the PL array directly fragile!!!!!
                final int[] pls = genotype.getPL();
                final int[] newPLs = new int[pls.length];
                for (int i = 0; i < pls.length; i++) {
                    newPLs[i] = Math.min(maximalPLDifference, pls[i]);
                }
                hFp.addToLogLikelihoods(snp, usableSnp.getAlleles(), GenotypeLikelihoods.fromPLs(newPLs).getAsVector());
                fp.add(hFp);
            } else {

                if (genotype.isNoCall()) continue;

                // TODO: when multiple genotypes are available for a Haplotype check that they
                // TODO: agree. Not urgent since DownloadGenotypes already does this.
                if (fp.containsKey(h)) continue;

                final boolean hom = genotype.isHom();
                final byte allele = StringUtil.toUpperCase(genotype.getAllele(0).getBases()[0]);

                final double halfError = this.genotypingErrorRate / 2;
                final double accuracy = 1 - this.genotypingErrorRate;
                final double[] probs = new double[]{
                        (hom && allele == snp.getAllele1()) ? accuracy : halfError,
                        (!hom) ? accuracy : halfError,
                        (hom && allele == snp.getAllele2()) ? accuracy : halfError
                };

                fp.add(new HaplotypeProbabilitiesFromGenotype(snp, h, probs[0], probs[1], probs[2]));
            }
        }
    }
*/
}
