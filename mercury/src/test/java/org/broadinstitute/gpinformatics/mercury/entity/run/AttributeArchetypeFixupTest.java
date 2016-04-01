package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.run.InfiniumRunResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Data fixups AttributeArchetype.
 */
@Test(groups = TestGroups.FIXUP)
public class AttributeArchetypeFixupTest extends Arquillian {

    @Inject
    private UserBean userBean;

    @Inject
    private UserTransaction utx;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    // Populates the initial genotyping chip types.
    @Test(enabled = false)
    public void gplim4023PopulateGenoChipTypes() throws Exception {
        String[] initialAttributes = {
                "pool_name", "Broad_GWAS_supplemental_15061359_A1",
                "norm_manifest_unix"	, "/humgen/illumina_data/Broad_GWAS_supplemental_15061359_A1.bpm.csv",
                "manifest_location_unix", "/gap/illumina/beadstudio/Autocall/ChipInfo/Broad_GWAS_supplemental/Broad_GWAS_supplemental_15061359_A1.bpm",
                "cluster_location_unix"	, "/gap/illumina/beadstudio/Autocall/ChipInfo/Broad_GWAS_supplemental/Broad_GWAS_supplemental_15061359_A1.egt",
                "zcall_threshold_unix", "/gap/illumina/beadstudio/Autocall/ChipInfo/Broad_GWAS_supplemental/thresholds.7.txt",

                "pool_name", "HumanCoreExome-24v1-0_A",
                "norm_manifest_unix"	, "/humgen/illumina_data/HumanCoreExome-24v1-0_A.bpm.csv",
                "manifest_location_unix", "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanCoreExome-24v1-0_A/HumanCoreExome-24v1-0_A.bpm",
                "cluster_location_unix"	, "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanCoreExome-24v1-0_A/HumanCoreExome-24v1-0_A.egt",
                "zcall_threshold_unix", "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanCoreExome-24v1-0_A/HumanCoreExome-24v1-0_A.thresholds.7.txt",

                "pool_name", "HumanExome-12v1-2_A",
                "norm_manifest_unix"	, "/humgen/illumina_data/HumanExome-12v1-2_A.bpm.csv",
                "manifest_location_unix", "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanExome-12v1-2_A/HumanExome-12v1-2_A.bpm",
                "cluster_location_unix"	, "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanExome-12v1-2_A/HumanExome-12v1-2_Illumina-HapMap.egt",
                "zcall_threshold_unix", "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanExome-12v1-2_A/HumanExome-12v1-2_A.thresholds.7.txt",

                "pool_name", "HumanOmni2.5-8v1_A",
                "norm_manifest_unix"	, "/humgen/illumina_data/HumanOmni2.5-8v1_A.bpm.csv",
                "manifest_location_unix", "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmni2.5-8v1_A/HumanOmni2.5-8v1_A.bpm",
                "cluster_location_unix"	, "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmni2.5-8v1_A/HumanOmni2.5-8v1_A.egt",
                "zcall_threshold_unix", "",

                "pool_name", "HumanOmniExpressExome-8v1_B",
                "norm_manifest_unix"	, "/humgen/illumina_data/HumanOmniExpressExome-8v1_B.bpm.csv",
                "manifest_location_unix", "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmniExpressExome-8v1_B/HumanOmniExpressExome-8v1_B.bpm",
                "cluster_location_unix"	, "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmniExpressExome-8v1_B/HumanOMXEX_CEPH_B.egt",
                "zcall_threshold_unix", "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmniExpressExome-8v1_B/Combo_OmniExpress_Exome_All.egt.thresholds.txt",

                "pool_name", "HumanOmniExpressExome-8v1-3_A",
                "norm_manifest_unix"	, "/humgen/illumina_data/InfiniumOmniExpressExome-8v1-3_A.csv",
                "manifest_location_unix", "/gap/illumina/beadstudio/Autocall/ChipInfo/InfiniumOmniExpressExome-8v1-3_A/InfiniumOmniExpressExome-8v1-3_A.bpm",
                "cluster_location_unix"	, "/gap/illumina/beadstudio/Autocall/ChipInfo/InfiniumOmniExpressExome-8v1-3_A/InfiniumOmniExpressExome-8v1-3_A_ClusterFile.egt",
                "zcall_threshold_unix", "",

                "pool_name", "HumanOmniExpress-24v1-1_A",
                "norm_manifest_unix"	, "/humgen/illumina_data/HumanOmniExpress-24v1.1A.bpm.csv",
                "manifest_location_unix", "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmniExpress-24v1-1_A/HumanOmniExpress-24v1.1A.bpm",
                "cluster_location_unix"	, "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmniExpress-24v1.1_A/HumanOmniExpress-24v1.1A.egt",
                "zcall_threshold_unix", "",

                "pool_name", "PsychChip_15048346_B",
                "norm_manifest_unix"	, "/humgen/illumina_data/PsychChip_15048346_B.bpm.csv",
                "manifest_location_unix", "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15048346_B/PsychChip_15048346_B.bpm",
                "cluster_location_unix"	, "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15048346_B/PsychChip_B_1000samples_no-filters.egt",
                "zcall_threshold_unix", "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15048346_B/PsychChip_15048346_B.thresholds.6.1000.txt",

                "pool_name", "PsychChip_v1-1_15073391_A1",
                "norm_manifest_unix"	, "/humgen/illumina_data/PsychChip_v1-1_15073391_A1.bpm.csv",
                "manifest_location_unix", "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15073391_v1-1_A/PsychChip_v1-1_15073391_A1.bpm",
                "cluster_location_unix"	, "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15073391_v1-1_A/PsychChip_v1-1_15073391_A1_ClusterFile.egt",
                "zcall_threshold_unix", "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15073391_v1-1_A/thresholds.7.txt",
        };

        utx.begin();
        userBean.loginOSUser();
        // The collection of new entities to persist.
        List<Object> entities = new ArrayList<>();

        // Collects and persists the required attribute names, excluding "pool_name".
        Set<String> attributeNames = new HashSet<>();
        for (int i = 0; i < initialAttributes.length; i += 2) {
            String attributeName = initialAttributes[i];
            if (!attributeName.equals("pool_name") && attributeNames.add(attributeName)) {
                attributeArchetypeDao.persist(
                        new AttributeDefinition(InfiniumRunResource.INFINIUM_FAMILY, attributeName));
            }
        }

        // Adds the data applicable to all INFINIUM_FAMILY chips.
        attributeArchetypeDao.persist(new AttributeDefinition(InfiniumRunResource.INFINIUM_FAMILY, "data_path",
                "/humgen/illumina_data"));

        // Adds the attributes for each chip type. When iterating, "pool_name" marks the start of a new chip type.
        AttributeArchetype attributeArchetype = null;
        for (int i = 0; i < initialAttributes.length; i += 2) {
            String key = initialAttributes[i];
            String value = initialAttributes[i + 1];
            if (key.equals("pool_name")) {
                // Creates the new chip type.
                attributeArchetype = new AttributeArchetype(InfiniumRunResource.INFINIUM_FAMILY, value);
                attributeArchetypeDao.persist(attributeArchetype);
                attributeArchetypeDao.flush();
            } else {
                // Adds attribute to the currently referenced chip type.
                ArchetypeAttribute attribute = new ArchetypeAttribute(attributeArchetype, key, value);
                System.out.println("Adding attribute (" + attribute.getAttributeName() + ", " +
                                   attribute.getAttributeValue() + ") to " +
                                   attributeArchetype.getArchetypeName());
                attributeArchetype.getAttributes().add(attribute);
                attributeArchetypeDao.persist(attribute);
            }
        }
        attributeArchetypeDao.persist(new FixupCommentary(
                "GPLIM-4023 add the initial Infinium genotyping chip types from GAP."));
        attributeArchetypeDao.flush();
        utx.commit();
    }
}
