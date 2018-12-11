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
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel_;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch_;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.UserTransaction;
import javax.validation.constraints.AssertTrue;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.DENATURE_TO_DILUTION_TRANSFER;
import static org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType.DILUTION_TO_FLOWCELL_TRANSFER;

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


    /**
     * GPLIM-5508 DW getting wrong flowcell lane for loading tube
     * In Mercury, a flowcell loading vessel (strip tube, dilution tube) is never assumed to be re-used. <br/>
     * This NovaSeq project was done manually and the vessel transfer messages were also created and submitted manually: <br/>
     * 4 denature tubes transferred to 4 dilution tubes, then the same dilution tubes were used to load 4 flowcells. <br/>
     * The source of this problem is that on flowcell H3K5CDSXX, 3 of the dilution tubes went to different lanes than
     * the other 3 flowcells causing the mixup. <br/>
     * We could manually fix the data warehouse, but a refresh would overwrite the fix. <br/>
     * Will create distinct dilution tubes and adjust the transfers to 'special' flowcell H3K5CDSXX.
     */
    @Test(enabled = false)
    public void gplim5508() throws Exception {

        long startTs;
        String eventLocation = "Simulated";
        utx.begin();
        userBean.loginOSUser();
        Long operator = userBean.getBspUser().getUserId();

        // Delete denature to dilution transfer event and transfer
        LabEvent badXfer = labVesselDao.findSingle(LabEvent.class, LabEvent_.labEventId,Long.valueOf(2707463L) );
        startTs = badXfer.getEventDate().getTime();
        labVesselDao.remove(badXfer);

        // Delete all the flowcell transfer events and transfers
        badXfer = labVesselDao.findSingle(LabEvent.class, LabEvent_.labEventId,Long.valueOf(2707467L) );
        labVesselDao.remove(badXfer);
        badXfer = labVesselDao.findSingle(LabEvent.class, LabEvent_.labEventId,Long.valueOf(2707469L) );
        labVesselDao.remove(badXfer);
        badXfer = labVesselDao.findSingle(LabEvent.class, LabEvent_.labEventId,Long.valueOf(2707476L) );
        labVesselDao.remove(badXfer);
        badXfer = labVesselDao.findSingle(LabEvent.class, LabEvent_.labEventId,Long.valueOf(2707470L) );
        labVesselDao.remove(badXfer);
        badXfer = labVesselDao.findSingle(LabEvent.class, LabEvent_.labEventId,Long.valueOf(2707477L) );
        labVesselDao.remove(badXfer);
        labVesselDao.flush();

        TubeFormation denatureTubes = OrmUtil.proxySafeCast( labVesselDao.findByIdentifier("e6cc6dc51e2f47238fc35737363c0046"), TubeFormation.class);
        RackOfTubes denatureRack = OrmUtil.proxySafeCast( labVesselDao.findByIdentifier("000006839901"), RackOfTubes.class);

        // ******************************************************************************************************
        // Replace DenatureToFlowcellTransfer	2707467	"000007010501 JwSimsAFlowcell" --> "H3FYJDSXX"  FCT-41412
        BarcodedTube fct12Lane4Dilution = new BarcodedTube("024753262-a", BarcodedTube.BarcodedTubeType.MatrixTube); // A01
        labVesselDao.persist(fct12Lane4Dilution);
        BarcodedTube fct12Lane3Dilution = new BarcodedTube("024753242-a", BarcodedTube.BarcodedTubeType.MatrixTube); // B01
        labVesselDao.persist(fct12Lane3Dilution);
        BarcodedTube fct12Lane2Dilution = new BarcodedTube("024753211-a", BarcodedTube.BarcodedTubeType.MatrixTube); // C01
        labVesselDao.persist(fct12Lane2Dilution);
        BarcodedTube fct12Lane1Dilution = new BarcodedTube("024753210-a", BarcodedTube.BarcodedTubeType.MatrixTube); // D01
        labVesselDao.persist(fct12Lane1Dilution);

        Map<VesselPosition,BarcodedTube> fct12Layout = new HashMap<>();
        fct12Layout.put(VesselPosition.A01,fct12Lane4Dilution );
        fct12Layout.put(VesselPosition.B01,fct12Lane3Dilution );
        fct12Layout.put(VesselPosition.C01,fct12Lane2Dilution );
        fct12Layout.put(VesselPosition.D01,fct12Lane1Dilution );

        TubeFormation fct12Tubes = new TubeFormation(fct12Layout, RackOfTubes.RackType.Matrix96);
        RackOfTubes fct12Rack = new RackOfTubes("JsSimRackFCT-41412", RackOfTubes.RackType.Matrix96);
        labVesselDao.persist(fct12Rack);
        fct12Tubes.addRackOfTubes(fct12Rack);
        labVesselDao.persist(fct12Tubes);

        // Denature to dilution transfer event and transfers
        LabEvent fct12Den2Dil = new LabEvent(DENATURE_TO_DILUTION_TRANSFER, new Date(startTs += 1000), eventLocation, 1L, operator, null );
        labVesselDao.persist(fct12Den2Dil);
        fct12Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.A01, denatureRack, fct12Tubes.getContainerRole(), VesselPosition.A01, fct12Rack, fct12Den2Dil));
        fct12Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.B01, denatureRack, fct12Tubes.getContainerRole(), VesselPosition.B01, fct12Rack, fct12Den2Dil));
        fct12Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.C01, denatureRack, fct12Tubes.getContainerRole(), VesselPosition.C01, fct12Rack, fct12Den2Dil));
        fct12Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.D01, denatureRack, fct12Tubes.getContainerRole(), VesselPosition.D01, fct12Rack, fct12Den2Dil));

        // Dilution to flowcell transfer event and transfers
        LabVessel fct12Flowcell = labVesselDao.findByIdentifier("H3FYJDSXX");
        LabEvent fct12Dil2Fc = new LabEvent(DILUTION_TO_FLOWCELL_TRANSFER, new Date(startTs += 1000), eventLocation, 1L, operator, null );
        labVesselDao.persist(fct12Dil2Fc);
        fct12Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct12Tubes.getContainerRole(), VesselPosition.A01, fct12Rack, fct12Flowcell.getContainerRole(), VesselPosition.LANE4, null, fct12Dil2Fc));
        fct12Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct12Tubes.getContainerRole(), VesselPosition.B01, fct12Rack, fct12Flowcell.getContainerRole(), VesselPosition.LANE3, null, fct12Dil2Fc));
        fct12Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct12Tubes.getContainerRole(), VesselPosition.C01, fct12Rack, fct12Flowcell.getContainerRole(), VesselPosition.LANE2, null, fct12Dil2Fc));
        fct12Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct12Tubes.getContainerRole(), VesselPosition.D01, fct12Rack, fct12Flowcell.getContainerRole(), VesselPosition.LANE1, null, fct12Dil2Fc));

        // ******************************************************************************************************
        // DenatureToFlowcellTransfer	2707469	"000007010501 JwSimsAFlowcell" --> "H3K3HDSXX" FCT-41415
        // Twice?
        // DenatureToFlowcellTransfer	2707476	"000007010501 JwSimsAFlowcell" --> "H3K3HDSXX"
        BarcodedTube fct15Lane4Dilution = new BarcodedTube("024753262-b", BarcodedTube.BarcodedTubeType.MatrixTube); // A01
        labVesselDao.persist(fct15Lane4Dilution);
        BarcodedTube fct15Lane3Dilution = new BarcodedTube("024753242-b", BarcodedTube.BarcodedTubeType.MatrixTube); // B01
        labVesselDao.persist(fct15Lane3Dilution);
        BarcodedTube fct15Lane2Dilution = new BarcodedTube("024753211-b", BarcodedTube.BarcodedTubeType.MatrixTube); // C01
        labVesselDao.persist(fct15Lane2Dilution);
        BarcodedTube fct15Lane1Dilution = new BarcodedTube("024753210-b", BarcodedTube.BarcodedTubeType.MatrixTube); // D01
        labVesselDao.persist(fct15Lane1Dilution);

        Map<VesselPosition,BarcodedTube> fct15Layout = new HashMap<>();
        fct15Layout.put(VesselPosition.A01,fct15Lane4Dilution );
        fct15Layout.put(VesselPosition.B01,fct15Lane3Dilution );
        fct15Layout.put(VesselPosition.C01,fct15Lane2Dilution );
        fct15Layout.put(VesselPosition.D01,fct15Lane1Dilution );

        TubeFormation fct15Tubes = new TubeFormation(fct15Layout, RackOfTubes.RackType.Matrix96);
        RackOfTubes fct15Rack = new RackOfTubes("JsSimRackFCT-41415", RackOfTubes.RackType.Matrix96);
        labVesselDao.persist(fct15Rack);
        fct15Tubes.addRackOfTubes(fct15Rack);
        labVesselDao.persist(fct15Tubes);

        // Denature to dilution transfer event and transfers
        LabEvent fct15Den2Dil = new LabEvent(DENATURE_TO_DILUTION_TRANSFER, new Date(startTs += 1000), eventLocation, 1L, operator, null );
        labVesselDao.persist(fct15Den2Dil);
        fct15Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.A01, denatureRack, fct15Tubes.getContainerRole(), VesselPosition.A01, fct15Rack, fct15Den2Dil));
        fct15Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.B01, denatureRack, fct15Tubes.getContainerRole(), VesselPosition.B01, fct15Rack, fct15Den2Dil));
        fct15Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.C01, denatureRack, fct15Tubes.getContainerRole(), VesselPosition.C01, fct15Rack, fct15Den2Dil));
        fct15Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.D01, denatureRack, fct15Tubes.getContainerRole(), VesselPosition.D01, fct15Rack, fct15Den2Dil));

        // Dilution to flowcell transfer event and transfers
        LabVessel fct15Flowcell = labVesselDao.findByIdentifier("H3K3HDSXX");
        LabEvent fct15Dil2Fc = new LabEvent(DILUTION_TO_FLOWCELL_TRANSFER, new Date(startTs += 1000), eventLocation, 1L, operator, null );
        labVesselDao.persist(fct15Dil2Fc);
        fct15Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct15Tubes.getContainerRole(), VesselPosition.A01, fct15Rack, fct15Flowcell.getContainerRole(), VesselPosition.LANE4, null, fct15Dil2Fc));
        fct15Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct15Tubes.getContainerRole(), VesselPosition.B01, fct15Rack, fct15Flowcell.getContainerRole(), VesselPosition.LANE3, null, fct15Dil2Fc));
        fct15Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct15Tubes.getContainerRole(), VesselPosition.C01, fct15Rack, fct15Flowcell.getContainerRole(), VesselPosition.LANE2, null, fct15Dil2Fc));
        fct15Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct15Tubes.getContainerRole(), VesselPosition.D01, fct15Rack, fct15Flowcell.getContainerRole(), VesselPosition.LANE1, null, fct15Dil2Fc));

        // ******************************************************************************************************
        // DenatureToFlowcellTransfer	2707470	"000007010501 JwSimsAFlowcell" --> "H3K5FDSXX" FCT-41414
        BarcodedTube fct14Lane4Dilution = new BarcodedTube("024753262-c", BarcodedTube.BarcodedTubeType.MatrixTube); // A01
        labVesselDao.persist(fct14Lane4Dilution);
        BarcodedTube fct14Lane3Dilution = new BarcodedTube("024753242-c", BarcodedTube.BarcodedTubeType.MatrixTube); // B01
        labVesselDao.persist(fct14Lane3Dilution);
        BarcodedTube fct14Lane2Dilution = new BarcodedTube("024753211-c", BarcodedTube.BarcodedTubeType.MatrixTube); // C01
        labVesselDao.persist(fct14Lane2Dilution);
        BarcodedTube fct14Lane1Dilution = new BarcodedTube("024753210-c", BarcodedTube.BarcodedTubeType.MatrixTube); // D01
        labVesselDao.persist(fct14Lane1Dilution);

        Map<VesselPosition,BarcodedTube> fct14Layout = new HashMap<>();
        fct14Layout.put(VesselPosition.A01,fct14Lane4Dilution );
        fct14Layout.put(VesselPosition.B01,fct14Lane3Dilution );
        fct14Layout.put(VesselPosition.C01,fct14Lane2Dilution );
        fct14Layout.put(VesselPosition.D01,fct14Lane1Dilution );

        TubeFormation fct14Tubes = new TubeFormation(fct14Layout, RackOfTubes.RackType.Matrix96);
        RackOfTubes fct14Rack = new RackOfTubes("JsSimRackFCT-41414", RackOfTubes.RackType.Matrix96);
        labVesselDao.persist(fct14Rack);
        fct14Tubes.addRackOfTubes(fct14Rack);
        labVesselDao.persist(fct14Tubes);

        // Denature to dilution transfer event and transfers
        LabEvent fct14Den2Dil = new LabEvent(DENATURE_TO_DILUTION_TRANSFER, new Date(startTs += 1000), eventLocation, 1L, operator, null );
        labVesselDao.persist(fct14Den2Dil);
        fct14Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.A01, denatureRack, fct14Tubes.getContainerRole(), VesselPosition.A01, fct14Rack, fct14Den2Dil));
        fct14Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.B01, denatureRack, fct14Tubes.getContainerRole(), VesselPosition.B01, fct14Rack, fct14Den2Dil));
        fct14Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.C01, denatureRack, fct14Tubes.getContainerRole(), VesselPosition.C01, fct14Rack, fct14Den2Dil));
        fct14Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.D01, denatureRack, fct14Tubes.getContainerRole(), VesselPosition.D01, fct14Rack, fct14Den2Dil));

        // Dilution to flowcell transfer event and transfers
        LabVessel fct14Flowcell = labVesselDao.findByIdentifier("H3K5FDSXX");
        LabEvent fct14Dil2Fc = new LabEvent(DILUTION_TO_FLOWCELL_TRANSFER, new Date(startTs += 1000), eventLocation, 1L, operator, null );
        labVesselDao.persist(fct14Dil2Fc);
        fct14Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct14Tubes.getContainerRole(), VesselPosition.A01, fct14Rack, fct14Flowcell.getContainerRole(), VesselPosition.LANE4, null, fct14Dil2Fc));
        fct14Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct14Tubes.getContainerRole(), VesselPosition.B01, fct14Rack, fct14Flowcell.getContainerRole(), VesselPosition.LANE3, null, fct14Dil2Fc));
        fct14Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct14Tubes.getContainerRole(), VesselPosition.C01, fct14Rack, fct14Flowcell.getContainerRole(), VesselPosition.LANE2, null, fct14Dil2Fc));
        fct14Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct14Tubes.getContainerRole(), VesselPosition.D01, fct14Rack, fct14Flowcell.getContainerRole(), VesselPosition.LANE1, null, fct14Dil2Fc));

        // ******************************************************************************************************
        // (the odd transfer) DenatureToFlowcellTransfer   2707477	"000007010501 JwSimsAFlowcell" --> "H3K5CDSXX" FCT-41413
        BarcodedTube fct13Lane4Dilution = new BarcodedTube("024753262-d", BarcodedTube.BarcodedTubeType.MatrixTube); // A01
        labVesselDao.persist(fct13Lane4Dilution);
        BarcodedTube fct13Lane2Dilution = new BarcodedTube("024753242-d", BarcodedTube.BarcodedTubeType.MatrixTube); // B01
        labVesselDao.persist(fct13Lane2Dilution);
        BarcodedTube fct13Lane1Dilution = new BarcodedTube("024753211-d", BarcodedTube.BarcodedTubeType.MatrixTube); // C01
        labVesselDao.persist(fct13Lane1Dilution);
        BarcodedTube fct13Lane3Dilution = new BarcodedTube("024753210-d", BarcodedTube.BarcodedTubeType.MatrixTube); // D01
        labVesselDao.persist(fct13Lane3Dilution);

        Map<VesselPosition,BarcodedTube> fct13Layout = new HashMap<>();
        fct13Layout.put(VesselPosition.A01,fct13Lane4Dilution );
        fct13Layout.put(VesselPosition.B01,fct13Lane3Dilution );
        fct13Layout.put(VesselPosition.C01,fct13Lane2Dilution );
        fct13Layout.put(VesselPosition.D01,fct13Lane1Dilution );

        TubeFormation fct13Tubes = new TubeFormation(fct13Layout, RackOfTubes.RackType.Matrix96);
        RackOfTubes fct13Rack = new RackOfTubes("JsSimRackFCT-41413", RackOfTubes.RackType.Matrix96);
        labVesselDao.persist(fct13Rack);
        fct13Tubes.addRackOfTubes(fct13Rack);
        labVesselDao.persist(fct13Tubes);

        // Denature to dilution transfer event and transfers
        LabEvent fct13Den2Dil = new LabEvent(DENATURE_TO_DILUTION_TRANSFER, new Date(startTs += 1000), eventLocation, 1L, operator, null );
        labVesselDao.persist(fct13Den2Dil);
        fct13Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.A01, denatureRack, fct13Tubes.getContainerRole(), VesselPosition.A01, fct13Rack, fct13Den2Dil));
        fct13Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.B01, denatureRack, fct13Tubes.getContainerRole(), VesselPosition.B01, fct13Rack, fct13Den2Dil));
        fct13Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.C01, denatureRack, fct13Tubes.getContainerRole(), VesselPosition.C01, fct13Rack, fct13Den2Dil));
        fct13Den2Dil.getCherryPickTransfers().add(new CherryPickTransfer(denatureTubes.getContainerRole(), VesselPosition.D01, denatureRack, fct13Tubes.getContainerRole(), VesselPosition.D01, fct13Rack, fct13Den2Dil));

        // Dilution to flowcell transfer event and transfers
        LabVessel fct13Flowcell = labVesselDao.findByIdentifier("H3K5CDSXX");
        LabEvent fct13Dil2Fc = new LabEvent(DILUTION_TO_FLOWCELL_TRANSFER, new Date(startTs += 1000), eventLocation, 1L, operator, null );
        labVesselDao.persist(fct13Dil2Fc);
        fct13Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct13Tubes.getContainerRole(), VesselPosition.A01, fct13Rack, fct13Flowcell.getContainerRole(), VesselPosition.LANE4, null, fct13Dil2Fc));
        fct13Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct13Tubes.getContainerRole(), VesselPosition.B01, fct13Rack, fct13Flowcell.getContainerRole(), VesselPosition.LANE3, null, fct13Dil2Fc));
        fct13Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct13Tubes.getContainerRole(), VesselPosition.C01, fct13Rack, fct13Flowcell.getContainerRole(), VesselPosition.LANE2, null, fct13Dil2Fc));
        fct13Dil2Fc.getCherryPickTransfers().add(new CherryPickTransfer(fct13Tubes.getContainerRole(), VesselPosition.D01, fct13Rack, fct13Flowcell.getContainerRole(), VesselPosition.LANE1, null, fct13Dil2Fc));

        // Deal with batch starting vessels
        LabBatch fct12Batch = labVesselDao.findSingle(LabBatch.class, LabBatch_.batchName,"FCT-41412" );
        for( LabBatchStartingVessel batchStartingVessel : fct12Batch.getLabBatchStartingVessels()) {
            switch (batchStartingVessel.getVesselPosition()) {
                case LANE1:
                    batchStartingVessel.setDilutionVessel(fct12Lane1Dilution);
                    batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103207L) ));
                case LANE2:
                    batchStartingVessel.setDilutionVessel(fct12Lane2Dilution);
                    batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103208L) ));
                case LANE3:
                    batchStartingVessel.setDilutionVessel(fct12Lane3Dilution);
                    batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103209L) ));
                case LANE4:
                    batchStartingVessel.setDilutionVessel(fct12Lane4Dilution);
                    batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103210L) ));
            }
        }

        LabBatch fct15Batch = labVesselDao.findSingle(LabBatch.class, LabBatch_.batchName,"FCT-41415" );
        for( LabBatchStartingVessel batchStartingVessel : fct15Batch.getLabBatchStartingVessels()) {
            switch (batchStartingVessel.getVesselPosition()) {
            case LANE1:
                batchStartingVessel.setDilutionVessel(fct15Lane1Dilution);
                batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103213L) ));
            case LANE2:
                batchStartingVessel.setDilutionVessel(fct15Lane2Dilution);
                batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103211L) ));
            case LANE3:
                batchStartingVessel.setDilutionVessel(fct15Lane3Dilution);
                batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103212L) ));
            case LANE4:
                batchStartingVessel.setDilutionVessel(fct15Lane4Dilution);
                batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103214L) ));
            }
        }

        LabBatch fct14Batch = labVesselDao.findSingle(LabBatch.class, LabBatch_.batchName,"FCT-41414" );
        for( LabBatchStartingVessel batchStartingVessel : fct14Batch.getLabBatchStartingVessels()) {
            switch (batchStartingVessel.getVesselPosition()) {
            case LANE1:
                batchStartingVessel.setDilutionVessel(fct14Lane1Dilution);
                batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103215L) ));
            case LANE2:
                batchStartingVessel.setDilutionVessel(fct14Lane2Dilution);
                batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103216L) ));
            case LANE3:
                batchStartingVessel.setDilutionVessel(fct14Lane3Dilution);
                batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103217L) ));
            case LANE4:
                batchStartingVessel.setDilutionVessel(fct14Lane4Dilution);
                batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103218L) ));
            }
        }

        LabBatch fct13Batch = labVesselDao.findSingle(LabBatch.class, LabBatch_.batchName,"FCT-41413" );
        for( LabBatchStartingVessel batchStartingVessel : fct13Batch.getLabBatchStartingVessels()) {
            switch (batchStartingVessel.getVesselPosition()) {
            case LANE1:
                batchStartingVessel.setDilutionVessel(fct13Lane1Dilution);
                batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103220L) ));
            case LANE2:
                batchStartingVessel.setDilutionVessel(fct13Lane2Dilution);
                batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103221L) ));
            case LANE3:
                batchStartingVessel.setDilutionVessel(fct13Lane3Dilution);
                batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103219L) ));
            case LANE4:
                batchStartingVessel.setDilutionVessel(fct13Lane4Dilution);
                batchStartingVessel.setFlowcellDesignation(labVesselDao.findSingle(FlowcellDesignation.class, FlowcellDesignation_.designationId, Long.valueOf(103222L) ));
            }
        }

        FixupCommentary fixupCommentary = new FixupCommentary("GPLIM-5508 Resolve NovaSeq flowcell designations created by hand built messaging");
        labVesselDao.persist(fixupCommentary);
        labVesselDao.flush();
        utx.commit();

    }

    /**
     * GPLIM-5508 above fixup had error on FCT-41413 - Lane transfers are different for flowcell H3K5CDSXX.
     */
    @Test(enabled = false)
    public void gplim5508Fct41413() throws Exception {
        utx.begin();
        userBean.loginOSUser();
        Long operator = userBean.getBspUser().getUserId();

        // Change barcodes on dilution vessels to match positions/barcodes on FCT-41412, 41414, and 41415
        Field labelField = LabVessel.class.getDeclaredField("label");
        labelField.setAccessible(true);
        // A01 vessel prefix doesn't change
        LabVessel dilutionA01 = labVesselDao.findByIdentifier("024753262-d" );
        labelField.set(dilutionA01, "024753262-e" );
        LabVessel dilutionB01 = labVesselDao.findByIdentifier("024753210-d" );
        labelField.set(dilutionB01, "024753242-e" );
        LabVessel dilutionC01 = labVesselDao.findByIdentifier("024753242-d" );
        labelField.set(dilutionC01, "024753211-e" );
        LabVessel dilutionD01 = labVesselDao.findByIdentifier("024753211-d" );
        labelField.set(dilutionD01, "024753210-e" );

        // Change flowcell transfer target lanes
        LabVessel flowcell = labVesselDao.findByIdentifier("H3K5CDSXX" );
        LabEvent flowcellXfer = flowcell.getTransfersTo().iterator().next();
        Field targetField = CherryPickTransfer.class.getDeclaredField("targetPosition");
        targetField.setAccessible(true);
        for( CherryPickTransfer laneXfer : flowcellXfer.getCherryPickTransfers()){
            if( laneXfer.getTargetPosition() == VesselPosition.LANE1 ) {
                targetField.set( laneXfer, VesselPosition.LANE3 );
            } else if( laneXfer.getTargetPosition() == VesselPosition.LANE2) {
                targetField.set( laneXfer, VesselPosition.LANE1 );
            } else if( laneXfer.getTargetPosition() == VesselPosition.LANE3) {
                targetField.set( laneXfer, VesselPosition.LANE2 );
            }
        }

        // ********************** Use ETL service to manually refresh the DilutionToFlowcellTransfer event ***

        // Flowcell Designation OK
        FixupCommentary fixupCommentary = new FixupCommentary("GPLIM-5508 FCT-41413 correct lanes");
        labVesselDao.persist(fixupCommentary);
        labVesselDao.flush();
        utx.commit();
    }

    // Change read length.
    @Test(enabled = false)
    public void support4584() throws Exception {
        utx.begin();
        userBean.loginOSUser();
        for (long id : new long[]{122305, 122306, 122308, 122309, 122310}) {
            FlowcellDesignation flowcellDesignation  = illuminaFlowcellDao.findById(FlowcellDesignation.class, id);
            Assert.assertNotNull(flowcellDesignation);
            System.out.println("Change read length from 101 to 76 on flowcell designation " + id);
            Assert.assertEquals(flowcellDesignation.getReadLength().intValue(), 101);
            flowcellDesignation.setReadLength(76);
        }
        FixupCommentary fixupCommentary = new FixupCommentary("SUPPORT-4584 fix incorrectly entered read length.");
        labVesselDao.persist(fixupCommentary);
        labVesselDao.flush();
        utx.commit();
    }


    /**
     * Unassigned strip tube dilution references
     *
     * Batch Vessel ID  Striptube     Flowcell         Denature    FCT
     * ---------------  ------------- ----------       ---------   ---------
     * 4657828          000015206411  HMJN3BCX2 LANE1  0311172858  FCT-44494
     * 4657827          000015206411  HMJN3BCX2 LANE2  0311180313  FCT-44494
     *
     * 4657829          000015204811  HMJYFBCX2 LANE1  0311180458  FCT-44495
     * 4657830          000015204811  HMJYFBCX2 LANE2  0311180372  FCT-44495
     */
    @Test(enabled=false)
    public void addMissingDilutionRefsGplim5675() throws Exception {
        utx.begin();
        userBean.loginOSUser();

        LabBatchStartingVessel batchStartingVessel;

        // FCT-44494
        LabVessel stripTubeFct44494 = labVesselDao.findByIdentifier("000015206411");
        batchStartingVessel = labVesselDao.findSingle(LabBatchStartingVessel.class,
                    LabBatchStartingVessel_.batchStartingVesselId, 4657828L );
        batchStartingVessel.setDilutionVessel(stripTubeFct44494);

        batchStartingVessel = labVesselDao.findSingle(LabBatchStartingVessel.class,
                LabBatchStartingVessel_.batchStartingVesselId, 4657827L );
        batchStartingVessel.setDilutionVessel(stripTubeFct44494);

        // FCT-44495
        LabVessel stripTubeFct44495 = labVesselDao.findByIdentifier("000015204811");
        batchStartingVessel = labVesselDao.findSingle(LabBatchStartingVessel.class,
                LabBatchStartingVessel_.batchStartingVesselId, 4657829L );
        batchStartingVessel.setDilutionVessel(stripTubeFct44495);

        batchStartingVessel = labVesselDao.findSingle(LabBatchStartingVessel.class,
                LabBatchStartingVessel_.batchStartingVesselId, 4657830L );
        batchStartingVessel.setDilutionVessel(stripTubeFct44495);

        FixupCommentary fixupCommentary = new FixupCommentary("GPLIM-5675 HiSeq 2500 Rapid Run batches missing dilution references.");
        labVesselDao.persist(fixupCommentary);
        labVesselDao.flush();
        utx.commit();
    }

}
