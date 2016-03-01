package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A JAX-RS resource for Infinium genotyping runs.
 */
@Path("/infiniumrun")
@Stateful
@RequestScoped
public class InfiniumRunResource {

    private static final Pattern BARCODE_PATTERN = Pattern.compile("(\\d*)_(R\\d*)(C\\d*)");
    public static final String DATA_PATH = "/humgen/illumina_data";

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    public InfiniumRunBean getRun(@QueryParam("chipWellBarcode") String chipWellBarcode) {
        InfiniumRunBean infiniumRunBean;
        Matcher matcher = BARCODE_PATTERN.matcher(chipWellBarcode);
        if (matcher.matches()) {
            String chipBarcode = matcher.group(1);
            String row = matcher.group(2);
            String column = matcher.group(3);
            VesselPosition vesselPosition = VesselPosition.valueOf(row + column);
            LabVessel chip = labVesselDao.findByIdentifier(chipBarcode);
            infiniumRunBean = buildRunBean(vesselPosition, chip);
        } else {
            throw new ResourceException("Barcode is not of expected format", Response.Status.INTERNAL_SERVER_ERROR);
        }
        return infiniumRunBean;
    }

    private InfiniumRunBean buildRunBean(VesselPosition vesselPosition, LabVessel chip) {
        InfiniumRunBean infiniumRunBean;
        Set<SampleInstanceV2> sampleInstancesAtPositionV2 =
                chip.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition);
        if (sampleInstancesAtPositionV2.size() == 1) {
            SampleInstanceV2 sampleInstanceV2 = sampleInstancesAtPositionV2.iterator().next();
            // todo jmt determine why Arrays samples have no connection between MercurySample and ProductOrderSample
            List<ProductOrderSample> productOrderSamples = productOrderSampleDao.findBySamples(Collections.singletonList(
                    sampleInstanceV2.getRootOrEarliestMercurySampleName()));
            // todo jmt what about controls?
            Set<String> chipTypes = new HashSet<>();
            for (ProductOrderSample productOrderSample : productOrderSamples) {
                String chipType = ProductOrder.genoChipTypeForPart(
                        productOrderSample.getProductOrder().getProduct().getPartNumber());
                if (chipType != null) {
                    chipTypes.add(chipType);
                }
            }
            if (chipTypes.isEmpty()) {
                throw new ResourceException("Found no chip types", Response.Status.INTERNAL_SERVER_ERROR);
            }
            if (chipTypes.size() != 1) {
                throw new ResourceException("Found mix of chip types " + chipTypes, Response.Status.INTERNAL_SERVER_ERROR);
            }

            String idatPrefix = DATA_PATH + "/" + chip.getLabel() + "_" + vesselPosition.name();
            String chipType = chipTypes.iterator().next();
            Config config = mapChipTypeToConfig.get(chipType);
            if (config == null) {
                throw new ResourceException("No configuration for " + chipType, Response.Status.INTERNAL_SERVER_ERROR);
            } else {
                SampleData sampleData = sampleDataFetcher.fetchSampleData(
                        sampleInstanceV2.getRootOrEarliestMercurySampleName());
                infiniumRunBean = new InfiniumRunBean(
                        idatPrefix + "_Red.idat",
                        idatPrefix + "_Grn.idat",
                        config.getChipManifestPath(),
                        config.getBeadPoolManifestPath(),
                        config.getClusterFilePath(),
                        config.getzCallThresholdsPath(),
                        sampleData.getCollaboratorsSampleName(),
                        sampleData.getSampleLsid(),
                        sampleData.getGender());
            }
        } else {
            throw new RuntimeException("Expected 1 sample, found " + sampleInstancesAtPositionV2.size());
        }
        return infiniumRunBean;
    }

    private static class Config {
        private String chipManifestPath;
        private String beadPoolManifestPath;
        private String clusterFilePath;
        private String zCallThresholdsPath;

        private Config(String chipManifestPath, String beadPoolManifestPath, String clusterFilePath,
                String zCallThresholdsPath) {
            this.chipManifestPath = chipManifestPath;
            this.beadPoolManifestPath = beadPoolManifestPath;
            this.clusterFilePath = clusterFilePath;
            this.zCallThresholdsPath = zCallThresholdsPath;
        }

        public String getChipManifestPath() {
            return chipManifestPath;
        }

        public String getBeadPoolManifestPath() {
            return beadPoolManifestPath;
        }

        public String getClusterFilePath() {
            return clusterFilePath;
        }

        public String getzCallThresholdsPath() {
            return zCallThresholdsPath;
        }
    }

    /*
SELECT
	p.pool_name,
	ici.norm_manifest_unix,
	ici.manifest_location_unix,
	ici.cluster_location_unix,
	ici.zcall_threshold_unix
FROM
	esp.infinium_chip_info ici
	INNER JOIN esp.pool p
		ON   p.pool_id = ici.pool_id
WHERE
	p.pool_name IN ('Broad_GWAS_supplemental_15061359_A1',
	               'HumanCoreExome-24v1-0_A', 'HumanExome-12v1-2_A',
	               'HumanOmni2.5-8v1_A', 'HumanOmniExpressExome-8v1_B',
	               'HumanOmniExpressExome-8v1-3_A', 'HumanOmniExpress-24v1-1_A',
	               'PsychChip_15048346_B', 'PsychChip_v1-1_15073391_A1');
     */
    private static final Map<String, Config> mapChipTypeToConfig = new HashMap<>();
    static {
        mapChipTypeToConfig.put("Broad_GWAS_supplemental_15061359_A1", new Config(
                "/humgen/illumina_data/Broad_GWAS_supplemental_15061359_A1.bpm.csv",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/Broad_GWAS_supplemental/Broad_GWAS_supplemental_15061359_A1.bpm",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/Broad_GWAS_supplemental/Broad_GWAS_supplemental_15061359_A1.egt",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/Broad_GWAS_supplemental/thresholds.7.txt"));
        mapChipTypeToConfig.put("HumanCoreExome-24v1-0_A", new Config(
                "/humgen/illumina_data/HumanCoreExome-24v1-0_A.bpm.csv",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanCoreExome-24v1-0_A/HumanCoreExome-24v1-0_A.bpm",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanCoreExome-24v1-0_A/HumanCoreExome-24v1-0_A.egt",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanCoreExome-24v1-0_A/HumanCoreExome-24v1-0_A.thresholds.7.txt"));
        mapChipTypeToConfig.put("HumanExome-12v1-2_A", new Config(
                "/humgen/illumina_data/HumanExome-12v1-2_A.bpm.csv",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanExome-12v1-2_A/HumanExome-12v1-2_A.bpm",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanExome-12v1-2_A/HumanExome-12v1-2_Illumina-HapMap.egt",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanExome-12v1-2_A/HumanExome-12v1-2_A.thresholds.7.txt"));
        mapChipTypeToConfig.put("HumanOmni2.5-8v1_A",new Config(
                "/humgen/illumina_data/HumanOmni2.5-8v1_A.bpm.csv",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmni2.5-8v1_A/HumanOmni2.5-8v1_A.bpm",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmni2.5-8v1_A/HumanOmni2.5-8v1_A.egt",
                null));
        mapChipTypeToConfig.put("HumanOmniExpressExome-8v1_B", new Config(
                "/humgen/illumina_data/HumanOmniExpressExome-8v1_B.bpm.csv",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmniExpressExome-8v1_B/HumanOmniExpressExome-8v1_B.bpm",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmniExpressExome-8v1_B/HumanOMXEX_CEPH_B.egt",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmniExpressExome-8v1_B/Combo_OmniExpress_Exome_All.egt.thresholds.txt"));
        mapChipTypeToConfig.put("HumanOmniExpressExome-8v1-3_A", new Config(
                "/humgen/illumina_data/InfiniumOmniExpressExome-8v1-3_A.csv",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/InfiniumOmniExpressExome-8v1-3_A/InfiniumOmniExpressExome-8v1-3_A.bpm",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/InfiniumOmniExpressExome-8v1-3_A/InfiniumOmniExpressExome-8v1-3_A_ClusterFile.egt",
                null));
        mapChipTypeToConfig.put("HumanOmniExpress-24v1-1_A", new Config(
                "/humgen/illumina_data/HumanOmniExpress-24v1.1A.bpm.csv",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmniExpress-24v1-1_A/HumanOmniExpress-24v1.1A.bpm",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmniExpress-24v1.1_A/HumanOmniExpress-24v1.1A.egt",
                null));
        mapChipTypeToConfig.put("PsychChip_15048346_B", new Config(
                "/humgen/illumina_data/PsychChip_15048346_B.bpm.csv",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15048346_B/PsychChip_15048346_B.bpm",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15048346_B/PsychChip_B_1000samples_no-filters.egt",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15048346_B/PsychChip_15048346_B.thresholds.6.1000.txt"));
        mapChipTypeToConfig.put("PsychChip_v1-1_15073391_A1", new Config(
                "/humgen/illumina_data/PsychChip_v1-1_15073391_A1.bpm.csv",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15073391_v1-1_A/PsychChip_v1-1_15073391_A1.bpm",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15073391_v1-1_A/PsychChip_v1-1_15073391_A1_ClusterFile.egt",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15073391_v1-1_A/thresholds.7.txt"));
    }
}
