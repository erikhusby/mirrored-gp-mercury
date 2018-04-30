package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.SnpDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.SnpListDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixups for SNP Lists.
 */
@Test(groups = TestGroups.FIXUP)
public class SnpListFixupTest extends Arquillian {

    @Inject
    private SnpListDao snpListDao;

    @Inject
    private SnpDao snpDao;

    @Inject
    private UserBean userBean;

    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/SnpList.txt, so it
     * can be used for other similar fixups, without writing a new test.  Example contents of the file are (first line
     * is the fixup commentary; second line is the SNP list name, third and successive lines are
     * RSID TAB F for failed TAB G for gender):
     * GPLIM-5122 setup SNP lists
     * FluidigmFPv5
     * rs10037858\t\t
     * rs1052053\tF\t
     * AMG_3b\t\tG
     * 
     * The GAP query to get this list is:
     * SELECT
     *     substr(pa.POLY_ASSAY_NAME, instr(pa.POLY_ASSAY_NAME, '|') + 1),
     *     decode(pa.IS_FAILED, 1, 'F', null),
     *     decode(pa.IS_GENDER_ASSAY, 1, 'G', null)
     * FROM
     *     FEATUREDB.POLY_ASSAY_LIST pal
     *     INNER JOIN featuredb.poly_assay_list_collection palc
     *         ON   palc.poly_assay_list_id = pal.poly_assay_list_id
     *     INNER JOIN featuredb.poly_assay pa
     *         ON   pa.poly_assay_id = palc.poly_assay_id
     * WHERE
     *     pal.POLY_ASSAY_LIST_NAME = 'FluidigmFPv5'
     * ORDER BY
     *     palc.position;
     * 
     */

    @Test(enabled = false)
    public void fixupGplim5122CreateSnpList() {
        userBean.loginOSUser();
        persistSnpList("SnpListFluidigmFPv4.txt");
        persistSnpList("SnpListFluidigmFPv5.txt");
    }

    private void persistSnpList(String fileName) {
        try {
            List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource(fileName));
            String fixupCommentary = lines.get(0);
            String name = lines.get(1);
            SnpList snpList = new SnpList(name);
            Set<String> rsIds = new HashSet<>();
            for (int i = 2; i < lines.size(); i++) {
                String[] fields = lines.get(i).split("\\t");
                rsIds.add(fields[0]);
            }
            Map<String, Snp> mapRsIdToSnp = snpDao.findByRsIds(rsIds);
            for (int i = 2; i < lines.size(); i++) {
                String[] fields = lines.get(i).split("\\t");
                Snp snp = mapRsIdToSnp.get(fields[0]);
                if (snp == null) {
                    snp = new Snp(fields[0], fields.length > 1 && fields[1].equals("F"),
                            fields.length > 2 && fields[2].equals("G"));
                }
                snpList.getSnps().add(snp);
            }
            utx.begin();
            snpListDao.persist(snpList);
            snpListDao.persist(new FixupCommentary(fixupCommentary));
            utx.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicRollbackException |
                HeuristicMixedException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
