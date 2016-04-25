package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.run.InfiniumRunResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.run.GenotypingChipTypeActionBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Date;
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

    @Inject
    private ProductEjb productEjb;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    // Populates the initial genotyping chip types.
    @Test(enabled = false)
    public void gplim4023PopulateGenoChipTypes() throws Exception {
        String[] initialAttributes = {
                "pool_name", "Broad_GWAS_supplemental_15061359_A1",
                "norm_manifest_unix", "/humgen/illumina_data/Broad_GWAS_supplemental_15061359_A1.bpm.csv",
                "manifest_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/Broad_GWAS_supplemental/Broad_GWAS_supplemental_15061359_A1.bpm",
                "cluster_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/Broad_GWAS_supplemental/Broad_GWAS_supplemental_15061359_A1.egt",
                "zcall_threshold_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/Broad_GWAS_supplemental/thresholds.7.txt",

                "pool_name", "HumanCoreExome-24v1-0_A",
                "norm_manifest_unix", "/humgen/illumina_data/HumanCoreExome-24v1-0_A.bpm.csv",
                "manifest_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanCoreExome-24v1-0_A/HumanCoreExome-24v1-0_A.bpm",
                "cluster_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanCoreExome-24v1-0_A/HumanCoreExome-24v1-0_A.egt",
                "zcall_threshold_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanCoreExome-24v1-0_A/HumanCoreExome-24v1-0_A.thresholds.7.txt",

                "pool_name", "HumanExome-12v1-2_A",
                "norm_manifest_unix", "/humgen/illumina_data/HumanExome-12v1-2_A.bpm.csv",
                "manifest_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanExome-12v1-2_A/HumanExome-12v1-2_A.bpm",
                "cluster_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanExome-12v1-2_A/HumanExome-12v1-2_Illumina-HapMap.egt",
                "zcall_threshold_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanExome-12v1-2_A/HumanExome-12v1-2_A.thresholds.7.txt",

                "pool_name", "HumanOmni2.5-8v1_A",
                "norm_manifest_unix", "/humgen/illumina_data/HumanOmni2.5-8v1_A.bpm.csv",
                "manifest_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmni2.5-8v1_A/HumanOmni2.5-8v1_A.bpm",
                "cluster_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmni2.5-8v1_A/HumanOmni2.5-8v1_A.egt",
                "zcall_threshold_unix", "",

                "pool_name", "HumanOmniExpressExome-8v1_B",
                "norm_manifest_unix", "/humgen/illumina_data/HumanOmniExpressExome-8v1_B.bpm.csv",
                "manifest_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmniExpressExome-8v1_B/HumanOmniExpressExome-8v1_B.bpm",
                "cluster_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmniExpressExome-8v1_B/HumanOMXEX_CEPH_B.egt",
                "zcall_threshold_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmniExpressExome-8v1_B/Combo_OmniExpress_Exome_All.egt.thresholds.txt",

                "pool_name", "HumanOmniExpressExome-8v1-3_A",
                "norm_manifest_unix", "/humgen/illumina_data/InfiniumOmniExpressExome-8v1-3_A.csv",
                "manifest_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/InfiniumOmniExpressExome-8v1-3_A/InfiniumOmniExpressExome-8v1-3_A.bpm",
                "cluster_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/InfiniumOmniExpressExome-8v1-3_A/InfiniumOmniExpressExome-8v1-3_A_ClusterFile.egt",
                "zcall_threshold_unix", "",

                "pool_name", "HumanOmniExpress-24v1-1_A",
                "norm_manifest_unix", "/humgen/illumina_data/HumanOmniExpress-24v1.1A.bpm.csv",
                "manifest_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmniExpress-24v1-1_A/HumanOmniExpress-24v1.1A.bpm",
                "cluster_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/HumanOmniExpress-24v1.1_A/HumanOmniExpress-24v1.1A.egt",
                "zcall_threshold_unix", "",

                "pool_name", "PsychChip_15048346_B",
                "norm_manifest_unix", "/humgen/illumina_data/PsychChip_15048346_B.bpm.csv",
                "manifest_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15048346_B/PsychChip_15048346_B.bpm",
                "cluster_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15048346_B/PsychChip_B_1000samples_no-filters.egt",
                "zcall_threshold_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15048346_B/PsychChip_15048346_B.thresholds.6.1000.txt",

                "pool_name", "PsychChip_v1-1_15073391_A1",
                "norm_manifest_unix", "/humgen/illumina_data/PsychChip_v1-1_15073391_A1.bpm.csv",
                "manifest_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15073391_v1-1_A/PsychChip_v1-1_15073391_A1.bpm",
                "cluster_location_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15073391_v1-1_A/PsychChip_v1-1_15073391_A1_ClusterFile.egt",
                "zcall_threshold_unix",
                "/gap/illumina/beadstudio/Autocall/ChipInfo/PsychChip_15073391_v1-1_A/thresholds.7.txt",
        };
        final int fieldCount = 2;
        utx.begin();
        userBean.loginOSUser();
        // The collection of new entities to persist.
        List<Object> entities = new ArrayList<>();

        // The data design:
        // - The different genotyping chip technologies (vendors) are represented as different
        //   archetype groups, all having the Genotyping Chip namespace.  The group here is Infinium.
        // - The different Infinium chips each are represented by an Archetype.
        // - Each Infinium archetype has the same attributes with different values for each chip.
        //   The attributes are the four pathnames, plus a "last modified date".
        // - These attributes are defined with AttributeDefinitions.
        // - There is a group attribute in AttributeDefinitions for the one data_path that
        //   applies to all Infinium chips.

        String namespace = GenotypingChipTypeActionBean.class.getCanonicalName();

        // Collects the attribute definitions from the array.
        Set<String> attributeNames = new HashSet<>();
        for (int i = 0; i < initialAttributes.length; i += fieldCount) {
            String attributeName = initialAttributes[i];
            if (!attributeName.equals("pool_name") && attributeNames.add(attributeName)) {
                entities.add(new AttributeDefinition(namespace, InfiniumRunResource.INFINIUM_GROUP,
                        attributeName, null, true, false));
            }
        }

        // Adds the "last modified" attribute definition.
        entities.add(new AttributeDefinition(namespace, InfiniumRunResource.INFINIUM_GROUP,
                GenotypingChipTypeActionBean.LAST_MODIFIED, null, false, false));

        // Adds the group attribute for data_path.
        entities.add(new AttributeDefinition(namespace, InfiniumRunResource.INFINIUM_GROUP,
                "data_path", "/humgen/illumina_data", false, true));

        // Adds each chip type. When iterating, "pool_name" marks the start of a new chip type.
        AttributeArchetype attributeArchetype = null;
        for (int i = 0; i < initialAttributes.length; i += fieldCount) {
            String key = initialAttributes[i];
            String value = initialAttributes[i + 1];

            if (key.equals("pool_name")) {
                // Creates the new chip type.
                attributeArchetype = new AttributeArchetype(namespace, InfiniumRunResource.INFINIUM_GROUP, value);
                entities.add(attributeArchetype);

                // Adds "last modified date" attribute.
                ArchetypeAttribute dateAttribute = new ArchetypeAttribute(attributeArchetype,
                        GenotypingChipTypeActionBean.LAST_MODIFIED, DateUtils.getYYYYMMMDDTime(new Date()));
                attributeArchetype.getAttributes().add(dateAttribute);
            } else {
                // Adds attribute to the currently referenced chip type.
                ArchetypeAttribute attribute = new ArchetypeAttribute(attributeArchetype, key, value);
                System.out.println("Adding attribute (" + attribute.getAttributeName() + ", " +
                                   attribute.getAttributeValue() + ") to " +
                                   attributeArchetype.getArchetypeName());
                attributeArchetype.getAttributes().add(attribute);
            }
        }
        entities.add(new FixupCommentary("GPLIM-4023 add the initial Infinium genotyping chip types from GAP."));
        attributeArchetypeDao.persistAll(entities);
        attributeArchetypeDao.flush();
        utx.commit();
    }

    public static final Map<String, String> INITIAL_PRODUCT_PART_TO_GENO_CHIP = new HashMap<String, String>() {{
        put("P-EX-0017", "Broad_GWAS_supplemental_15061359_A1");
        put("P-WG-0022", "HumanOmni2.5-8v1_A");
        put("P-WG-0023", "HumanOmniExpressExome-8v1-3_A");
        put("P-WG-0025", "HumanExome-12v1-2_A");
        put("P-WG-0028", "HumanOmniExpress-24v1-1_A");
        put("P-WG-0029", "HumanExome-12v1-2_A");
        put("P-WG-0031", "HumanCoreExome-24v1-0_A");
        put("P-WG-0036", "PsychChip_15048346_B");
        put("P-WG-0036" + ProductEjb.DELIMITER + "Danish", "DBS_Wave_Psych");
        put("P-WG-0053", "Broad_GWAS_supplemental_15061359_A1");
        put("P-WG-0055", "PsychChip_v1-1_15073391_A1");
        put("P-WG-0058", "Multi-EthnicGlobal-8_A1");
    }};

    // Populates the initial mapping of product to genotyping chip types.
    @Test(enabled = false)
    public void gplim4023PopulateProductToGenoChipTypes() throws Exception {
        utx.begin();
        userBean.loginOSUser();

        // The data design:
        // - Each product that uses a genotyping chip gets an archetype named with product part number.
        // - Archetypes have a namespace indicating Product, and a group indicating genotyping chip mapping.
        // - One attribute is for genotyping chip technology (Infinium), and one for genotyping chip name.
        // - Over time the genotyping chip name may be changed by the user. Envers is used to find old versions
        //   of attributes belonging to this mapping archetype in order to retrieve the original mapping.
        String namespace = Product.class.getCanonicalName();
        String group = Product.GENOTYPING_CHIP_CONFIG;

        attributeArchetypeDao.persist(
                new AttributeDefinition(namespace, group, Product.GENOTYPING_CHIP_TECHNOLOGY, null, false, false));
        attributeArchetypeDao.persist(
                new AttributeDefinition(namespace, group, Product.GENOTYPING_CHIP_NAME, null, false, false));

        for (String productPartNumber : INITIAL_PRODUCT_PART_TO_GENO_CHIP.keySet()) {
            AttributeArchetype attributeArchetype = new AttributeArchetype(namespace, group, productPartNumber);
            attributeArchetype.getAttributes().add(new ArchetypeAttribute(attributeArchetype,
                    Product.GENOTYPING_CHIP_TECHNOLOGY, InfiniumRunResource.INFINIUM_GROUP));
            ArchetypeAttribute attribute = new ArchetypeAttribute(attributeArchetype, Product.GENOTYPING_CHIP_NAME,
                    INITIAL_PRODUCT_PART_TO_GENO_CHIP.get(productPartNumber));
            attributeArchetype.getAttributes().add(attribute);
            System.out.println("Adding " + attribute.getAttributeName() + " " + attribute.getAttributeValue() +
                               " to the configuration for product " + attributeArchetype.getArchetypeName());
            attributeArchetypeDao.persist(attributeArchetype);
        }
        attributeArchetypeDao.persist(new FixupCommentary(
                "GPLIM-4023 add the mapping from product to Infinium genotyping chip types."));
        attributeArchetypeDao.flush();
        utx.commit();
    }

}