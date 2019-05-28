package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class ControlFixupTest extends Arquillian {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "dev");
    }

    @Test
    public void gplim6080ControlFingerprintFixup() {
        /*
        SELECT
            ss.lsid
        FROM
            geno.genotyping_run gr
            INNER JOIN geno.metric_value mv
                ON   mv.genotyping_run_id = gr.genotyping_run_id
            INNER JOIN ppm.sts_sample ss
                ON   ss.sample_id = mv.sample_id
            INNER JOIN ppm.sts_clinical_sample scs
                ON   scs.clinical_sample_id = ss.clinical_sample_id
            INNER JOIN gap.object_alias oa
                ON   oa.lsid = scs.lsid
            INNER JOIN gap.object_alias oa2
                ON   oa2.lsid = ss.lsid
            INNER JOIN geno.metric_type mt
                ON   mt.metric_type_id = mv.metric_type_id
        WHERE
            mt.metric_label = 'FLDM.SAMPLE_HAPMAP_CONCORDANCE'
            AND oa.alias = 'NA12878'
            AND mv.METRIC_VALUE = 100.0;
         */
        Map<String, String> mapCollabIdToFpSample = new HashMap<String, String>() {{
            put("NA12878", "broadinstitute.org:bsp.prod.sample:2522V");
            put("NA12891", "SM-4D7US");
            put("NA12892", "SM-41MLW");
//            put("NA18506", "");
//            put("NA18563", "");
//            put("NA18603", "");
            put("NA18609", "broadinstitute.org:bsp.prod.sample:73DPY");
            put("NA18612", "broadinstitute.org:gap.prod.sample:4745691");
            put("NA18942", "broadinstitute.org:gap.prod.sample:4745639");
            put("NA18947", "broadinstitute.org:gap.prod.sample:4745704");
            put("NA18951", "broadinstitute.org:bsp.prod.sample:73DOK");
            put("NA18956", "broadinstitute.org:gap.prod.sample:4745675");
            put("NA18967", "broadinstitute.org:bsp.prod.sample:73DOM");
            put("NA18969", "broadinstitute.org:gap.prod.sample:4745642");
        }};
    }
}
