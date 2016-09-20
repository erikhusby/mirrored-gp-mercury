package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingChipMapping;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingProductOrderMapping;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.run.InfiniumRunResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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

    public static final Date EARLIEST_XSTAIN_DATE;

    static {
        try {
            EARLIEST_XSTAIN_DATE = DateUtils.yyyymmmdddDateTimeFormat.parse("2015-OCT-09 11:13 AM");
        } catch (ParseException e) {
            throw new RuntimeException("Cannot parse date.");
        }
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

                // These chips are used in product mapping so they need to be defined.
                "pool_name", "DBS_Wave_Psych",
                "norm_manifest_unix", "",
                "manifest_location_unix", "",
                "cluster_location_unix", "",
                "zcall_threshold_unix", "",

                "pool_name", "Multi-EthnicGlobal-8_A1",
                "norm_manifest_unix", "",
                "manifest_location_unix", "",
                "cluster_location_unix", "",
                "zcall_threshold_unix", "",
        };
        final int fieldCount = 2;
        utx.begin();
        userBean.loginOSUser();

        String chipFamily = InfiniumRunResource.INFINIUM_GROUP;

        List<AttributeDefinition> definitions = new ArrayList<>();

        // Collects the attribute definitions from the array.
        Set<String> attributeNames = new HashSet<>();
        for (int i = 0; i < initialAttributes.length; i += fieldCount) {
            String attributeName = initialAttributes[i];
            if (!attributeName.equals("pool_name") && attributeNames.add(attributeName)) {
                definitions.add(new AttributeDefinition(AttributeDefinition.DefinitionType.GENOTYPING_CHIP,
                        chipFamily, attributeName, true));
            }
        }

        // Adds the "last modified" attribute definition.
        definitions.add(new AttributeDefinition(AttributeDefinition.DefinitionType.GENOTYPING_CHIP,
                chipFamily, GenotypingChip.LAST_MODIFIED, false));

        // Adds the group attribute for data_path.
        definitions.add(new AttributeDefinition(AttributeDefinition.DefinitionType.GENOTYPING_CHIP,
                chipFamily, "data_path", "/humgen/illumina_data"));

        // Adds each chip type. When iterating, "pool_name" marks the start of a new chip type.
        List<GenotypingChip> chips = new ArrayList<>();
        GenotypingChip chip = null;
        for (int i = 0; i < initialAttributes.length; i += fieldCount) {
            String key = initialAttributes[i];
            String value = initialAttributes[i + 1];

            if (key.equals("pool_name")) {
                // Creates the new chip type with null valued instance attributes.
                chip = new GenotypingChip(InfiniumRunResource.INFINIUM_GROUP, value, definitions);
                chip.setLastModifiedDate();
                chips.add(chip);
            } else {
                chip.addOrSetAttribute(key, value);
                System.out.println("Adding attribute (" + key + ", " + value + ") to " + chip.getChipName());
            }
        }
        String fixupReason = "GPLIM-4023 add the initial Infinium genotyping chip types from GAP.";
        attributeArchetypeDao.persist(new FixupCommentary(fixupReason));
        attributeArchetypeDao.persistAll(definitions);
        attributeArchetypeDao.persistAll(chips);
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
        put("P-WG-0036" + GenotypingChipMapping.DELIMITER + "Danish", "DBS_Wave_Psych");
        put("P-WG-0053", "Broad_GWAS_supplemental_15061359_A1");
        put("P-WG-0055", "PsychChip_v1-1_15073391_A1");
        put("P-WG-0058", "Multi-EthnicGlobal-8_A1");
    }};

    // Populates the initial mapping of product to genotyping chip types.
    @Test(enabled = false)
    public void gplim4023PopulateProductToGenoChipTypes() throws Exception {
        utx.begin();
        userBean.loginOSUser();

        String chipFamily = InfiniumRunResource.INFINIUM_GROUP;

        List<AttributeDefinition> definitions = new ArrayList<AttributeDefinition>() {{
            add(new AttributeDefinition(AttributeDefinition.DefinitionType.GENOTYPING_CHIP_MAPPING,
                    GenotypingChipMapping.MAPPING_GROUP, GenotypingChipMapping.GENOTYPING_CHIP_TECHNOLOGY, false));
            add(new AttributeDefinition(AttributeDefinition.DefinitionType.GENOTYPING_CHIP_MAPPING,
                    GenotypingChipMapping.MAPPING_GROUP, GenotypingChipMapping.GENOTYPING_CHIP_NAME, false));
            add(new AttributeDefinition(AttributeDefinition.DefinitionType.GENOTYPING_CHIP_MAPPING,
                    GenotypingChipMapping.MAPPING_GROUP, GenotypingChipMapping.ACTIVE_DATE, false));
            add(new AttributeDefinition(AttributeDefinition.DefinitionType.GENOTYPING_CHIP_MAPPING,
                    GenotypingChipMapping.MAPPING_GROUP, GenotypingChipMapping.INACTIVE_DATE, false));
        }};
        attributeArchetypeDao.persistAll(definitions);

        for (Map.Entry<String, String> entry : INITIAL_PRODUCT_PART_TO_GENO_CHIP.entrySet()) {
            GenotypingChipMapping mapping = new GenotypingChipMapping(entry.getKey(), chipFamily, entry.getValue(),
                    EARLIEST_XSTAIN_DATE);
            System.out.println("Adding chip mapping for " + entry.getKey() + " to " + entry.getValue());
            attributeArchetypeDao.persist(mapping);
        }
        String fixupReason = "GPLIM-4023 add the mapping from product to Infinium genotyping chip types.";
        attributeArchetypeDao.persist(new FixupCommentary(fixupReason));
        attributeArchetypeDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupSupport1877() {
        userBean.loginOSUser();
        GenotypingChipMapping genotypingChipMapping = (GenotypingChipMapping) attributeArchetypeDao.findById(
                AttributeArchetype.class, 951L);
        Assert.assertEquals(genotypingChipMapping.getArchetypeName(), "P-EX-0021");
        // match created_date for PDO-8542, earliest PDO for P-EX-0021 Standard Exome Plus GWAS Supplement Array which
        // was made available on 01-JUL-16
        genotypingChipMapping.setActiveDate(new GregorianCalendar(2016, Calendar.APRIL, 5, 0, 0).getTime());
        System.out.println("Changing date for " + genotypingChipMapping.getArchetypeId() + " to " +
                genotypingChipMapping.getActiveDate());
        attributeArchetypeDao.persist(new FixupCommentary("SUPPORT-1877 adjust Active date for chip mapping"));
        attributeArchetypeDao.flush();
    }

    /** Add earlier chip version, to support backfilled messages.  This is for dev only. */
    @Test(enabled = false)
    public void fixupGap1068() {
        userBean.loginOSUser();
        GenotypingChipMapping genotypingChipMapping = new GenotypingChipMapping("P-WG-0036", "Infinium",
                "PsychChip_v1-1_15073391_A1", new GregorianCalendar(2015, Calendar.JANUARY, 1, 0, 0).getTime());
        genotypingChipMapping.setInactiveDate(new GregorianCalendar(2015, Calendar.OCTOBER, 9, 11, 13).getTime());
        attributeArchetypeDao.persist(genotypingChipMapping);

        System.out.println("Backfill " + genotypingChipMapping.getChipName() + " to " +
                genotypingChipMapping.getActiveDate());
        attributeArchetypeDao.persist(new FixupCommentary("GAP-1068 backfill chip type"));
        attributeArchetypeDao.flush();
    }
}