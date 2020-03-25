package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingChipMapping;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingProductOrderMapping;
import org.broadinstitute.gpinformatics.athena.entity.project.BillingTrigger;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.run.InfiniumRunResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.KeyValueMapping;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
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
@RequestScoped
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
            EARLIEST_XSTAIN_DATE = DateUtils.yyyymmmdddDateTimeFormat.parse("2015-Oct-09 11:13 AM");
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

    @Test(enabled = false)
    public void support2099AddNewChipAttributes() throws Exception {
        utx.begin();
        userBean.loginOSUser();

        final String chipFamily = InfiniumRunResource.INFINIUM_GROUP;

        List<AttributeDefinition> definitions = new ArrayList<AttributeDefinition>() {{
            add(new AttributeDefinition(AttributeDefinition.DefinitionType.GENOTYPING_CHIP,
                    chipFamily, "call_rate_threshold", true));
            add(new AttributeDefinition(AttributeDefinition.DefinitionType.GENOTYPING_CHIP,
                    chipFamily, "gender_cluster_file", true));
        }};

        String megaGenderClusterFile =
                "/gap/illumina/beadstudio/Autocall/ChipInfo/Multi-EthnicGlobal-8_A1/"
                + "Multi-EthnicGlobal_A1_Gentrain_Genderest_ClusterFile_highmafX.egt";
        Set<GenotypingChip> genotypingChips = attributeArchetypeDao.findGenotypingChips(chipFamily);
        for (GenotypingChip genotypingChip: genotypingChips) {
            genotypingChip.addOrSetAttribute("call_rate_threshold", "98");
            if (genotypingChip.getChipName().equals("Multi-EthnicGlobal-8_A1")) {
                genotypingChip.addOrSetAttribute("gender_cluster_file", megaGenderClusterFile);
            } else {
                genotypingChip.addOrSetAttribute("gender_cluster_file", null);
            }
        }

        String fixupReason = "SUPPORT-2099 add call rate threshold and optional gender cluster file.";
        attributeArchetypeDao.persist(new FixupCommentary(fixupReason));
        attributeArchetypeDao.persistAll(definitions);
        attributeArchetypeDao.persistAll(genotypingChips);
        attributeArchetypeDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void gplim4320PdoOverrides() throws Exception {
        utx.begin();
        userBean.loginOSUser();

        final String chipFamily = InfiniumRunResource.INFINIUM_GROUP;

        List<AttributeDefinition> definitions = new ArrayList<AttributeDefinition>() {{
            add(new AttributeDefinition(AttributeDefinition.DefinitionType.GENOTYPING_PRODUCT_ORDER,
                    chipFamily, "call_rate_threshold", true));
            add(new AttributeDefinition(AttributeDefinition.DefinitionType.GENOTYPING_PRODUCT_ORDER,
                    chipFamily, "cluster_location_unix", true));
        }};


        String fixupReason = "GPLIM-4320 add pdo specific overrides.";
        attributeArchetypeDao.persist(new FixupCommentary(fixupReason));
        attributeArchetypeDao.persistAll(definitions);
        attributeArchetypeDao.flush();
        utx.commit();
    }

    /**
     * GPLIM-4320 add pdo specific overrides used wrong AttributeDefinition group name
     */
    @Test(enabled = false)
    public void gplim4949PdoOverrides() throws Exception {
        utx.begin();
        userBean.loginOSUser();

        final String attributeGroup = GenotypingProductOrderMapping.ATTRIBUTES_GROUP;

        AttributeDefinition attribDef = attributeArchetypeDao.findById(AttributeDefinition.class, 2951L);
        attribDef.setGroup(attributeGroup);
        attribDef = attributeArchetypeDao.findById(AttributeDefinition.class, 2952L);
        attribDef.setGroup(attributeGroup);

        String fixupReason = "GPLIM-4949 fix AttributeDefinition group value for Infinium pdo specific overrides.";
        attributeArchetypeDao.persist(new FixupCommentary(fixupReason));
        attributeArchetypeDao.flush();
        utx.commit();
    }


    @Test(enabled = false)
    public void gplim4350IlluminaManifestAttribute() throws Exception {
        utx.begin();
        userBean.loginOSUser();

        final String chipFamily = InfiniumRunResource.INFINIUM_GROUP;

        List<AttributeDefinition> definitions = new ArrayList<AttributeDefinition>() {{
            add(new AttributeDefinition(AttributeDefinition.DefinitionType.GENOTYPING_CHIP,
                    chipFamily, "illumina_manifest_unix", true));
        }};

        Map<String, String> chipToIlluminaManifest = new HashMap<String, String>() {{
            put("PsychChip_v1-1_15073391_A1",
                "/humgen/affy_info/GAPProduction/prod/PsychChip_v1-1_15073391_A1/PsychChip_v1-1_15073391_A1.csv");
            put("PsychChip_15048346_B",
                "/humgen/affy_info/GAPProduction/prod/PsychChip_15048346_B/PsychChip_15048346_B.csv");
            put("HumanOmniExpressExome-8v1_B",
                "/humgen/affy_info/GAPProduction/prod/HumanOmniExpressExome-8v1_B/HumanOmniExpressExome-8v1_B.csv");
            put("HumanCoreExome-24v1-0_A",
                "/humgen/affy_info/GAPProduction/prod/HumanCoreExome-24v1-0_A/HumanCoreExome-24v1-0_A.csv");
            put("Broad_GWAS_supplemental_15061359_A1",
                "/humgen/affy_info/GAPProduction/prod/Broad_GWAS_supplemental_15061359_A1/Broad_GWAS_supplemental_15061359_A1.csv");
            put("DBS_Wave_Psych",
                "/humgen/affy_info/GAPProduction/prod/DBS_Wave_Psych/DBS_Wave_Psych.csv");
            put("HumanExome-12v1-2_A",
                "/humgen/affy_info/GAPProduction/prod/HumanExome-12v1-2_A/HumanExome-12v1-2_A.csv");
            put("HumanOmni2.5-8v1_A",
                "/humgen/affy_info/GAPProduction/prod/HumanOmni2.5-8v1_A/HumanOmni2.5-8v1_A.csv ");
            put("HumanOmniExpress-24v1-1_A",
                "/humgen/affy_info/GAPProduction/prod/HumanOmniExpress-24v1-1_A/HumanOmniExpress-24v1-1_A.csv");
            put("HumanOmniExpressExome-8v1-3_A",
                "/humgen/illumina/Illumina_BeadLab/Infinium_WholeGenome/InfiniumOmniExpressExome-8v1-3_A/InfiniumOmniExpressExome-8v1-3_A.csv");
            put("Multi-EthnicGlobal-8_A1",
                "/humgen/affy_info/GAPProduction/prod/Multi-EthnicGlobal-8_A1/Multi-EthnicGlobal-8_A1.csv");
        }};

        Set<GenotypingChip> genotypingChips = attributeArchetypeDao.findGenotypingChips(chipFamily);
        for (GenotypingChip genotypingChip: genotypingChips) {
            if (chipToIlluminaManifest.containsKey(genotypingChip.getChipName())) {
                String manifest = chipToIlluminaManifest.get(genotypingChip.getChipName());
                genotypingChip.addOrSetAttribute("illumina_manifest_unix", manifest);
                System.out.println("For chip: " + genotypingChip.getChipName() +
                                   " set archetypeAttribute: illumina_manifest_unix to " + manifest);
            } else {
                throw new RuntimeException("Illumina manifest not found in map for chip " + genotypingChip.getChipName());
            }
        }

        String fixupReason = "GPLIM-4350 add illumina manifest attribute.";
        attributeArchetypeDao.persist(new FixupCommentary(fixupReason));
        attributeArchetypeDao.persistAll(definitions);
        attributeArchetypeDao.persistAll(genotypingChips);
        attributeArchetypeDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void Gplim4397AddForwardToGap() throws Exception {
        utx.begin();
        userBean.loginOSUser();

        final String chipFamily = InfiniumRunResource.INFINIUM_GROUP;

        List<AttributeDefinition> definitions = new ArrayList<AttributeDefinition>() {{
            add(new AttributeDefinition(AttributeDefinition.DefinitionType.GENOTYPING_CHIP,
                    chipFamily, "forward_to_gap", true));
        }};

        Set<GenotypingChip> genotypingChips = attributeArchetypeDao.findGenotypingChips(chipFamily);
        for (GenotypingChip genotypingChip: genotypingChips) {
            genotypingChip.addOrSetAttribute("forward_to_gap", "Y");
        }

        String fixupReason = "GPLIM-4397 add forward_to_gap";
        attributeArchetypeDao.persist(new FixupCommentary(fixupReason));
        attributeArchetypeDao.persistAll(definitions);
        attributeArchetypeDao.persistAll(genotypingChips);
        attributeArchetypeDao.flush();
        utx.commit();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/FixupProductChipActiveDate.txt,
     * so it can be used for other similar fixups, without writing a new test.  The format of the file is:
     * one line with the ticket in the FixupCommentary
     * followed by multiple lines with the archetypeId, the product part number, the active date, and optional
     * inactive date
     * For example:
     * SUPPORT-1877
     * 1952,P-WG-0023,2016-09-01 00:00:00
     * 21,P-WG-0023,2015-10-09 11:13:00,2016-09-01 00:00:00
     */
    @Test(enabled = false)
    public void fixupSupport2223() {
        try {
            userBean.loginOSUser();
            List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("FixupProductChipActiveDate.txt"));
            String ticketId = lines.get(0);
            for (int i = 1; i < lines.size(); i++) {
                String[] fields = lines.get(i).split(",");
                long archetypeId = Long.parseLong(fields[0]);
                String productPartNumber = fields[1];
                String activeDate = fields[2];
                String inactiveDate = null;
                if (fields.length == 4) {
                    inactiveDate = fields[3];
                }

                GenotypingChipMapping genotypingChipMapping = (GenotypingChipMapping) attributeArchetypeDao.findById(
                        AttributeArchetype.class, archetypeId);
                Assert.assertEquals(genotypingChipMapping.getArchetypeName(), productPartNumber);
                genotypingChipMapping.setActiveDate(ArchetypeAttribute.dateFormat.parse(activeDate));
                System.out.println("Changing date for " + genotypingChipMapping.getArchetypeId() + " to " +
                        genotypingChipMapping.getActiveDate());
                if (inactiveDate != null) {
                    genotypingChipMapping.setInactiveDate(ArchetypeAttribute.dateFormat.parse(inactiveDate));
                }
            }
            attributeArchetypeDao.persist(new FixupCommentary(ticketId + " adjust Active date for chip mapping"));
            attributeArchetypeDao.flush();
        } catch (ParseException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This test deletes attribute archetypes and/or archetype attributes. It reads its parameters
     * from the file mercury/src/test/resources/testdata/FixupAttributeArchetype.txt
     * so that it can be used for other similar fixups without writing a new test.
     * The format of the file is:
     *   one line with the ticket for the FixupCommentary
     *   one or more lines with either "archetypeId" and the archetypeId, or "attributeId" and the attributeId
     *
     * For example:
     * SUPPORT-3620
     * attributeId,25318
     * archetypeId,29233
     *
     */
    @Test(enabled = false)
    public void fixupSupport3620() throws Exception {
        utx.begin();
        userBean.loginOSUser();
        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("FixupAttributeArchetype.txt"));
        String ticketId = lines.get(0);
        for (int i = 1; i < lines.size(); i++) {
            String[] fields = lines.get(i).split(",");
            Assert.assertEquals(fields.length, 2);
            Long id = Long.parseLong(fields[1]);
            if (fields[0].equalsIgnoreCase("archetypeId")) {
                AttributeArchetype archetype = attributeArchetypeDao.findById(AttributeArchetype.class, id);
                Assert.assertNotNull(archetype);
                System.out.println("Deleting " + archetype.getGroup() + " archetype for " +
                        archetype.getArchetypeName() + " (id " + archetype.getArchetypeId() + ").");
                // Hibernate orphan removal should cause any archetype attributes to be deleted also.
                archetype.getAttributes().clear();
                attributeArchetypeDao.remove(archetype);
            } else {
                Assert.assertTrue(fields[0].equalsIgnoreCase("attributeId"), "unknown param " + fields[0]);
                ArchetypeAttribute attribute = attributeArchetypeDao.findById(ArchetypeAttribute.class, id);
                Assert.assertNotNull(attribute);
                System.out.println("Deleting " + attribute.getAttributeName() + " attribute (id " +
                        attribute.getAttributeId() + ").");
                attributeArchetypeDao.remove(attribute);
            }
        }
        attributeArchetypeDao.persist(new FixupCommentary(ticketId + " fixup Attribute Archetypes"));
        attributeArchetypeDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void fixupGplim5268GdcMetadata() throws Exception {
        utx.begin();
        userBean.loginOSUser();

        final String attributeDefinitionGroup = "workflowMetadata";
        Collection<AttributeDefinition> definitions = new ArrayList<AttributeDefinition>() {{
            add(new AttributeDefinition(AttributeDefinition.DefinitionType.WORKFLOW_METADATA,
                    attributeDefinitionGroup, "library_preparation_kit_catalog_number", true));

            add(new AttributeDefinition(AttributeDefinition.DefinitionType.WORKFLOW_METADATA,
                    attributeDefinitionGroup, "library_preparation_kit_name", true));

            add(new AttributeDefinition(AttributeDefinition.DefinitionType.WORKFLOW_METADATA,
                    attributeDefinitionGroup, "library_preparation_kit_vendor", true));

            add(new AttributeDefinition(AttributeDefinition.DefinitionType.WORKFLOW_METADATA,
                    attributeDefinitionGroup, "library_preparation_kit_version", true));

            add(new AttributeDefinition(AttributeDefinition.DefinitionType.WORKFLOW_METADATA,
                    attributeDefinitionGroup, "target_capture_kit_catalog_number", true));

            add(new AttributeDefinition(AttributeDefinition.DefinitionType.WORKFLOW_METADATA,
                    attributeDefinitionGroup, "target_capture_kit_name", true));

            add(new AttributeDefinition(AttributeDefinition.DefinitionType.WORKFLOW_METADATA,
                    attributeDefinitionGroup, "target_capture_kit_target_region", true));

            add(new AttributeDefinition(AttributeDefinition.DefinitionType.WORKFLOW_METADATA,
                    attributeDefinitionGroup, "target_capture_kit_vendor", true));

            add(new AttributeDefinition(AttributeDefinition.DefinitionType.WORKFLOW_METADATA,
                    attributeDefinitionGroup, "target_capture_kit_version", true));
        }};

        String[] initialAttributes = {
                "workflow_name", "Hyper Prep ICE Exome Express",
                "library_preparation_kit_catalog_number", "KK8504",
                "library_preparation_kit_name", "KAPA Hyper Prep Kit with KAPA Library Amplification Primer Mix (10X).",
                "library_preparation_kit_vendor", "Kapa BioSystems",
                "library_preparation_kit_version", "v1.1",
                "target_capture_kit_catalog_number", "FC-144-1004",
                "target_capture_kit_name", "Illumina TruSeq Rapid Exome Library Prep kit",
                "target_capture_kit_target_region", "http://support.illumina.com/content/dam/illumina-support/documents/documentation/chemistry_documentation/samplepreps_nextera/nexterarapidcapture/nexterarapidcapture_exome_targetedregions_v1.2.bed",
                "target_capture_kit_vendor", "Illumina",
                "target_capture_kit_version", "v1.2",

                "workflow_name", "Whole Genome PCR Free HyperPrep",
                "library_preparation_kit_catalog_number", "KK8505",
                "library_preparation_kit_name", "KAPA HyperPrep Kit (no amp)",
                "library_preparation_kit_vendor", "Kapa BioSystems",
                "library_preparation_kit_version", "v1.1",

                "workflow_name", "Whole Genome PCR Plus HyperPrep",
                "library_preparation_kit_catalog_number", "KK8504",
                "library_preparation_kit_name", "KAPA Hyper Prep Kit with KAPA Library Amplification Primer Mix (10X)",
                "library_preparation_kit_vendor", "Kapa BioSystems",
                "library_preparation_kit_version", "v1.1",

                "workflow_name", "Cell Free HyperPrep",
                "library_preparation_kit_catalog_number", "KK8504",
                "library_preparation_kit_name", "KAPA Hyper Prep Kit with KAPA Library Amplification Primer Mix (10X)",
                "library_preparation_kit_vendor", "Kapa BioSystems",
                "library_preparation_kit_version", "v1.1",
        };
        final int fieldCount = 2;

        List<WorkflowMetadata> workflowMetadataList = new ArrayList<>();
        WorkflowMetadata workflowMetadata = null;
        for (int i = 0; i < initialAttributes.length; i += fieldCount) {
            String key = initialAttributes[i];
            String value = initialAttributes[i + 1];

            if (key.equals("workflow_name")) {
                workflowMetadata = new WorkflowMetadata(value, definitions);
                workflowMetadataList.add(workflowMetadata);
            } else {
                workflowMetadata.addOrSetAttribute(key, value);
                System.out.println("Adding attribute (" + key + ", " + value + ") to " + workflowMetadata.getWorkflowName());
            }
        }

        attributeArchetypeDao.persist(new FixupCommentary("GPLIM-5268 GDC Metadata fixup Attribute Archetypes"));
        attributeArchetypeDao.persistAll(definitions);
        attributeArchetypeDao.persistAll(workflowMetadataList);
        attributeArchetypeDao.flush();
        utx.commit();

    }

    /**
     * Adds key-value archetypes, attributes, and attribute definitions. The key-values must have the same
     * key-value mapping name (a.k.a. the archetype group). An input file is used, and needs this content:
     *   The first line has the Fixup Commentary.
     *   The second line starts with "mappingName" followed by the mapping name
     *   Attribute definitions are in lines that start with "definition" followed by the attribute name.
     *   Archetypes are in lines that starts with "archetype" followed by the key (a.k.a. archetype name).
     *   Attribute lines starts with "attribute" followed by the key, '|', the attribute name, '|', the attribute value.
     *
     * For example:
     *   GPLIM-4205 add initial bait to product part number mapping
     *   mappingName BaitToProductMapping
     *   definition theValue
     *   archetype Buick_v6_0_2014
     *   attribute Buick_v6_0_2014|theValue|P-EX-0011
     *
     * This method can also change an existing attribute value to a new value. The definition lines can be omitted.
     */
    @Test(enabled = false)
    public void addKeyValueMappings() throws Exception {
        utx.begin();
        userBean.loginOSUser();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("keyValueArchetypes.txt"));
        Assert.assertTrue(lines.size() >= 3);

        String fixupCommentary = lines.get(0);
        String mappingName = StringUtils.substringAfter(lines.get(1), "mappingName ");

        List<AttributeArchetype> archetypes = new ArrayList<>();
        List<AttributeDefinition> definitions = new ArrayList<>();

        // Parses input for definitions first, then archetypes, then attributes.
        for (int rowIndex = 2; rowIndex < lines.size(); ++rowIndex) {
            String line = lines.get(rowIndex);
            if (line.startsWith("definition")) {
                String attributeName = StringUtils.substringAfter(line, " ");
                final boolean IS_DISPLAYABLE = true;
                definitions.add(new AttributeDefinition(AttributeDefinition.DefinitionType.KEY_VALUE_MAPPING,
                        mappingName, attributeName, IS_DISPLAYABLE));
            }
        }

        for (int rowIndex = 2; rowIndex < lines.size(); ++rowIndex) {
            String line = lines.get(rowIndex);
            if (line.startsWith("archetype")) {
                String key = StringUtils.substringAfter(line, " ");
                AttributeArchetype archetype = attributeArchetypeDao.findKeyValueByKeyAndMappingName(key, mappingName);
                if (archetype == null) {
                    System.out.println("Adding new " + mappingName + " key " + key);
                    archetype = new KeyValueMapping(mappingName, key, definitions);
                }
                archetypes.add(archetype);
            }
        }

        for (int rowIndex = 2; rowIndex < lines.size(); ++rowIndex) {
            String line = lines.get(rowIndex);
            if (line.startsWith("attribute")) {
                String tokens[] = StringUtils.substringAfter(line, " ").split("\\|");
                Assert.assertEquals(tokens.length, 3,  "at line " + rowIndex);
                String key = tokens[0];
                String attributeName = tokens[1];
                String attributeValue = tokens[2];
                boolean found = false;
                for (AttributeArchetype archetype : archetypes) {
                    if (archetype.getGroup().equals(mappingName) && archetype.getArchetypeName().equals(key)) {
                        found = true;
                        System.out.println("Adding " + mappingName + " key-value (" + key + ", " + attributeValue + ")");
                        archetype.addOrSetAttribute(attributeName, attributeValue);
                    }
                }
                Assert.assertTrue(found, "No archetype found at line " + rowIndex);
            }
        }
        attributeArchetypeDao.persist(new FixupCommentary(fixupCommentary));
        attributeArchetypeDao.persistAll(definitions);
        attributeArchetypeDao.persistAll(archetypes);
        attributeArchetypeDao.flush();
        utx.commit();
    }

    @Test(enabled = false)
    public void gplim6698_billingTriggerValueMappings() throws Exception {
        utx.begin();
        userBean.loginOSUser();
        String fixupCommentary = "GPLIM-6698: All of Us Billing Triggers";
        List<AttributeArchetype> archetypes = new ArrayList<>();

        Arrays.asList(KeyValueMapping.AOU_PDO_WGS, KeyValueMapping.AOU_PDO_ARRAY).forEach(groupName -> {
            AttributeDefinition attributeDefinition = attributeArchetypeDao
                .findSingle(AttributeDefinition.class, AttributeDefinition_.group, groupName);
            AttributeArchetype archetype = attributeArchetypeDao
                .findKeyValueByKeyAndMappingName(MayoManifestEjb.BILLING_TRIGGERS, groupName);

            if (archetype == null) {
                System.out.println("Adding new " + groupName + " key " + MayoManifestEjb.BILLING_TRIGGERS);
                archetype = new KeyValueMapping(groupName, MayoManifestEjb.BILLING_TRIGGERS,
                    Collections.singleton(attributeDefinition));
                String attributeValue = StringUtils
                    .join(Arrays.asList(BillingTrigger.ADDONS_ON_RECEIPT.name(), BillingTrigger.DATA_REVIEW.name()), ",");
                archetype.addOrSetAttribute("theValue", attributeValue);
            }
            archetypes.add(archetype);
        });
        attributeArchetypeDao.persistAll(archetypes);
        attributeArchetypeDao.persist(new FixupCommentary(fixupCommentary));

        utx.commit();
    }

    /**
     * Deletes key-value archetypes and attributes. All must have the same key-value mapping name.
     * Uses an input file that has the same format shown above for adding key-value mappings.
     * The attribute value need not be given in the file.
     */
    @Test(enabled = false)
    public void deleteKeyValueMappings() throws Exception {
        utx.begin();
        userBean.loginOSUser();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("keyValueArchetypeDeletes.txt"));
        Assert.assertTrue(lines.size() >= 3);
        String fixupCommentary = lines.get(0);
        String mappingName = StringUtils.substringAfter(lines.get(1), "mappingName ");

        // Parses the file for attributes first, then definitions, then archetypes.
        for (int rowIndex = 2; rowIndex < lines.size(); ++rowIndex) {
            String line = lines.get(rowIndex);
            if (line.startsWith("attribute")) {
                String tokens[] = StringUtils.substringAfter(line, " ").split("\\|");
                Assert.assertTrue(tokens.length >= 2, " at line " + rowIndex);
                String key = tokens[0];
                String attributeName = tokens[1];
                AttributeArchetype archetype = attributeArchetypeDao.findKeyValueByKeyAndMappingName(key, mappingName);
                Assert.assertNotNull(archetype, "Cannot find archetype at line " + rowIndex);
                ArchetypeAttribute attribute = archetype.getAttribute(attributeName);
                Assert.assertNotNull(attribute, "Cannot find attribute at line " + rowIndex);
                System.out.println("Removing " + attributeName + " from " + key);
                archetype.getAttributes().remove(attribute);
                attributeArchetypeDao.remove(attribute);
            }
        }

        for (int rowIndex = 2; rowIndex < lines.size(); ++rowIndex) {
            String line = lines.get(rowIndex);
            if (line.startsWith("definition")) {
                String attributeName = StringUtils.substringAfter(line, " ");
                Map<String, AttributeDefinition> nameAndDefinition =
                        attributeArchetypeDao.findAttributeNamesByTypeAndGroup(
                                AttributeDefinition.DefinitionType.KEY_VALUE_MAPPING, mappingName);
                Assert.assertTrue(nameAndDefinition != null && !nameAndDefinition.isEmpty(), " at line " + rowIndex);
                AttributeDefinition definition = nameAndDefinition.get(attributeName);
                Assert.assertNotNull(definition, " at line rowIndex");
                attributeArchetypeDao.remove(definition);
            }
        }

        for (int rowIndex = 2; rowIndex < lines.size(); ++rowIndex) {
            String line = lines.get(rowIndex);
            if (line.startsWith("archetype")) {
                String key = StringUtils.substringAfter(line, " ");
                AttributeArchetype archetype = attributeArchetypeDao.findKeyValueByKeyAndMappingName(key, mappingName);
                Assert.assertNotNull(archetype, " at line " + rowIndex);
                System.out.println("Deleting " + mappingName + " key " + key);
                attributeArchetypeDao.remove(archetype);
            }
        }
        attributeArchetypeDao.persist(new FixupCommentary(fixupCommentary));
        attributeArchetypeDao.flush();
        utx.commit();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/keyValueArchetypeUpdates.txt,
     * so it can be used for other similar fixups, without writing a new test.  This is used to update key-value
     * ArchetypeAttribute's.  Example contents of the file are (first line is the fixup commentary,
     * subsequent lines are comma separated archetype id, old value, new value):
     * SUPPORT-4632 update hyperprep capture kit
     * 57760,Illumina TruSeq Rapid Exome Library Prep kit,Nextera Exome Kit (96 Spl)
     * 57754,FC-144-1004,20020617
     */
    @Test(enabled = false)
    public void fixupSupport4632() throws IOException {
        userBean.loginOSUser();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("keyValueArchetypeUpdates.txt"));
        for (int i = 1; i < lines.size(); i++) {
            String[] fields = lines.get(i).split(",");
            if (fields.length != 3) {
                throw new RuntimeException("Expected three comma separated fields in " + lines.get(i));
            }
            long attributeId = Long.parseLong(fields[0]);
            ArchetypeAttribute archetypeAttribute = attributeArchetypeDao.findById(ArchetypeAttribute.class, attributeId);
            Assert.assertNotNull(archetypeAttribute, attributeId + " not found");
            String oldValue = fields[1].trim();
            String newValue = fields[2].trim();
            Assert.assertEquals(oldValue, archetypeAttribute.getAttributeValue());
            System.out.println("Updating " + attributeId + " from " + oldValue + " to " + newValue);
            archetypeAttribute.setAttributeValue(newValue);
        }

        attributeArchetypeDao.persist(new FixupCommentary(lines.get(0)));
        attributeArchetypeDao.flush();
    }
}
