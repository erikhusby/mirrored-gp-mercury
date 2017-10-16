package org.broadinstitute.gpinformatics.mercury.boundary.run;

import clover.org.apache.commons.lang3.tuple.ImmutablePair;
import clover.org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

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
                        fingerprintEntities.add(new ImmutablePair<String, Fingerprint>(
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
            calls.add(new FingerprintCallsBean(, , ));
            fingerprints.add(new FingerprintBean(stringFingerprintPair.getLeft(), fin, , , , , , calls));
        }

        FingerprintsBean fingerprintsBean = new FingerprintsBean(fingerprints);
        return fingerprintsBean;
    }

    @POST
    public void post(FingerprintBean fingerprintBean) {
        fingerprintBean.getAliquotLsid();
        Fingerprint fingerprint = new Fingerprint(, , , , , , , , );
    }
}
