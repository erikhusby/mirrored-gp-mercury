package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch_;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.UserTransaction;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixups to the FlowcellDesignation entity.
 */
@Test(groups = TestGroups.FIXUP)
public class FlowcellDesignationFixupTest extends Arquillian {
    private static final boolean IS_POOL_TEST = true;
    private static final boolean IS_PAIRED_END_READ = true;

    @Inject
    private IlluminaFlowcellDao illuminaFlowcellDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private FlowcellDesignationEjb flowcellDesignationEjb;

    @Inject
    private UserBean userBean;

    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    // Retrofit some designations in order to set Pool Test for the flowcell in Mercury ETL.
    @Test(enabled = false)
    public void gplim4581() throws Exception {
        userBean.loginOSUser();

        // FCT-34666 shows:
        // lane    loading tube loading conc
        // LANE1 	0214585496 	180
        // LANE2 	0214585496 	180
        // LANE3 	0214585496 	180
        // LANE4 	0214585496 	180
        // LANE5 	0214585496 	180
        // LANE6 	0214585496 	180
        // LANE7 	0214585488 	180
        // LANE8 	0214585488 	180

        List<LabVessel> loadingTubes = labVesselDao.findByListIdentifiers(Arrays.asList("0214585496", "0214585488"));
        Assert.assertEquals(loadingTubes.size(), 2);

        String flowcellBarcode = "H7JYKALXX";
        IlluminaFlowcell flowcell = illuminaFlowcellDao.findByBarcode(flowcellBarcode);
        Assert.assertNotNull(flowcell);
        FlowcellDesignation.IndexType indexType = (flowcell.getUniqueIndexes().size() > 1) ?
                FlowcellDesignation.IndexType.DUAL : (flowcell.getUniqueIndexes().size() > 0) ?
                FlowcellDesignation.IndexType.SINGLE : FlowcellDesignation.IndexType.NONE;

        List<Object> entities = new ArrayList<>();
        for (LabVessel loadingTube : loadingTubes) {
            int numberLanes = loadingTube.getLabel().equals("0214585496") ? 6 : 2;
            for (LabEvent loadingEvent : loadingTube.getEvents()) {
                if (loadingEvent.getLabEventType() == LabEventType.DENATURE_TRANSFER) {
                    System.out.println("Creating flowcell designation for loading tube " + loadingTube);
                    entities.add(new FlowcellDesignation(loadingTube, null, indexType, IS_POOL_TEST,
                            flowcell.getFlowcellType(), numberLanes, 151, new BigDecimal(180), IS_PAIRED_END_READ,
                            FlowcellDesignation.Status.IN_FCT, FlowcellDesignation.Priority.NORMAL));
                }
            }
        }
        Assert.assertFalse(entities.isEmpty());

        entities.add(new FixupCommentary("GPLIM-4581 retrofit pool test designations"));
        illuminaFlowcellDao.persistAll(entities);
        illuminaFlowcellDao.flush();
    }

    /**
     * Backfill map designations to batch_starting_vessels so the designation to flowcell relationship is deterministic
     *    when multiple designations are created for the same loading vessel(s)
     */
    @Test(enabled = false)
    public void gplim4582() throws Exception {

        String logDirName = System.getProperty("jboss.server.log.dir");
        Writer processLogWriter = new FileWriter(logDirName + File.separator + "gplim4582_fixup.log", true);
        ToStringBuilder.setDefaultStyle(ToStringStyle.SHORT_PREFIX_STYLE);
        processLogWriter.write("======== STARTING GPLIM-4582 FIXUP =============\n");
        processLogWriter.write(SimpleDateFormat.getDateTimeInstance().format(new Date()));
        processLogWriter.write( "\n");

        userBean.loginOSUser();

        // Allocate a half hour to do this (reality is 10 minutes)
        utx.setTransactionTimeout(30*60);
        utx.begin();

        processLogWriter.write(" ## ********* Begin gathering FCT and Jira data \n");

        EntityManager em = illuminaFlowcellDao.getEntityManager();
        em.joinTransaction();
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // Get designations - order by loading tube and date  (~700 as of 05/01/2017)
        CriteriaQuery<LabBatch> labBatchCriteriaQuery = cb.createQuery(LabBatch.class);
        Root<LabBatch> labBatchRoot = labBatchCriteriaQuery.from(LabBatch.class);

        // Runtime criteria
        labBatchCriteriaQuery.where( cb.equal(labBatchRoot.get(LabBatch_.labBatchType), LabBatch.LabBatchType.FCT) );

        // (no designations for MISEQ as of 05/16/2017)
        //.where( labBatchRoot.get(LabBatch_.labBatchType).in(LabBatch.LabBatchType.FCT ,LabBatch.LabBatchType.MISEQ ) );

        // Some Test/Development criteria (FCT-35124 and FCT-35198 are good, FCT-35209 is cancelled)
//        labBatchCriteriaQuery.where(
//                labBatchRoot.get(LabBatch_.batchName).in( "FCT-35124", "FCT-35198", "FCT-35209") );

        labBatchCriteriaQuery.orderBy(cb.asc(labBatchRoot.get(LabBatch_.batchName)));
        labBatchCriteriaQuery.select(labBatchRoot);

        TypedQuery<LabBatch> q = em.createQuery(labBatchCriteriaQuery);
        List<LabBatch> fcts = q.getResultList();

        JiraService jiraService = ServiceAccessUtility.getBean(JiraService.class);

        // Lane info is stored in a custom field - get its name
        Map<String, CustomFieldDefinition> fieldDefs = jiraService.getCustomFields(LabBatch.TicketFields.LANE_INFO.getName());
        String laneFieldName = fieldDefs.get(LabBatch.TicketFields.LANE_INFO.getName()).getJiraCustomFieldId();

        JiraIssue jiraFct;
        Map<VesselPosition,String> laneInfo;

        // Get the jira ticket for each FCT batch  [batch - flowcell label -  [ loading tube label - lane ] ]
        List<Pair<LabBatch, Map<VesselPosition,String>>> fctDataList = new ArrayList<>();
        for( LabBatch fct : fcts ) {
            processLogWriter.write(" ## Processing " );
            processLogWriter.write( fct.getBatchName());
            processLogWriter.write( "\n");
            jiraFct = jiraService.getIssueInfo( fct.getBatchName(), laneFieldName );
            if( jiraFct == null || jiraFct.getSummary() == null ) {
                processLogWriter.write("    No Jira ticket available, skipping designation assignment.\n");
                continue;
            }

            // Pull the lane - loading tube info out of the Jira FCT table
            laneInfo = new HashMap<>();
            try {
                String laneInfoLines = (String) jiraFct.getFieldValue(laneFieldName);
                if (laneInfoLines != null) {
                    String[] fctLaneLines = laneInfoLines.split("\n");
                    // Data starts on second line
                    for (int i = 1; i < fctLaneLines.length; i++) {
                        String[] fctLaneLineData = fctLaneLines[i].substring(1).split("\\|");
                        if( fctLaneLineData.length >= 2 ) {
                            laneInfo.put(VesselPosition.getByName(fctLaneLineData[0]), fctLaneLineData[1]);
                        }
                    }
                }
            } catch ( Exception ex ) {
                // Death prevention safety
                processLogWriter.write("    Exception parsing Jira FCT flowcell lane info: ");
                processLogWriter.write(ex.getMessage());
                processLogWriter.write("\n");
                continue;
            }
            if( laneInfo.isEmpty() ) {
                processLogWriter.write("    No Jira FCT flowcell lane info available\n");
                continue;
            }
            fctDataList.add(Pair.of( fct, laneInfo));
        }

        processLogWriter.write(" ## ********* Finished Gathering FCT Jira data, " );
        processLogWriter.write( String.valueOf( fctDataList.size() ) );
        processLogWriter.write(" FCT tickets available\n\n\n");

        processLogWriter.write(" ## ********* Begin to assign designations. \n" );

        // Try to map Jira FCT data to designation
        for( Pair<LabBatch, Map<VesselPosition,String>> fctData :  fctDataList) {

            LabBatch fctBatch = fctData.getLeft();

            // Get all flowcell designation candidates assigned to batch starting vessels sorted by descending date
            List<FlowcellDesignation> flowcellDesignationList = flowcellDesignationEjb.getFlowcellDesignations(fctBatch);
            if( flowcellDesignationList == null || flowcellDesignationList.isEmpty() ) {
                processLogWriter.write("    No flowcell designations for batch vessels in ");
                processLogWriter.write(fctBatch.getBatchName());
                processLogWriter.write("\n");
                continue;
            } else {
                processLogWriter.write("    FCT Batch  ");
                processLogWriter.write(fctBatch.getBatchName());
                processLogWriter.write(" has ");
                processLogWriter.write( String.valueOf( flowcellDesignationList.size() ) );
                processLogWriter.write(" potential designation candidates.");
                processLogWriter.write("\n");
            }

            // Loop through FCT jira lane/loading tube data
            for( Map.Entry<VesselPosition,String> jiraData : fctData.getRight().entrySet()) {
                boolean foundMatch = false;

                // Try to find the batch starting vessel associated with the lane/loading tube jira data
                for( LabBatchStartingVessel batchStartingVessel : fctBatch.getLabBatchStartingVessels() ) {

                    // Jira lane/vessel and starting vessel lane/vessel match
                    if (batchStartingVessel.getVesselPosition() == jiraData.getKey()) {

                        // Find the latest flowcell designation prior to batch creation
                        for (FlowcellDesignation flowcellDesignation : flowcellDesignationList) {
                            if (flowcellDesignation.getStatus() == FlowcellDesignation.Status.IN_FCT
                                    && flowcellDesignation.getLoadingTube().getLabel().equals(jiraData.getValue())
                                    && flowcellDesignation.getCreatedOn().before(fctBatch.getCreatedOn())) {
                                foundMatch = true;
                                // ******************* THIS IS IT!  Assign designation to batch starting vessel.
                                batchStartingVessel.setFlowcellDesignation(flowcellDesignation);
                                break;
                            }
                        }
                        if( !foundMatch ) {
                            processLogWriter.write("    No designation found for ");
                            processLogWriter.write(new ToStringBuilder(batchStartingVessel).
                                    append("batchStartingVesselId", batchStartingVessel.getBatchStartingVesselId()).
                                    append("batchVesselId", batchStartingVessel.getLabVessel().getLabVesselId()).
                                    append("batchVesselBarcode", batchStartingVessel.getLabVessel().getLabel()).
                                    toString());
                            processLogWriter.write("\n");
                        }
                    }
                } // End of jira FCT batch starting vessel loop
            } // End of jira data flowcell lane loop
        } // End of jira data loop

        processLogWriter.write("======== FINISHED GPLIM-4582 FIXUP =============\n");
        processLogWriter.write(SimpleDateFormat.getDateTimeInstance().format(new Date()));
        processLogWriter.flush();
        processLogWriter.close();
        em.flush();
        utx.commit();
    }

}
