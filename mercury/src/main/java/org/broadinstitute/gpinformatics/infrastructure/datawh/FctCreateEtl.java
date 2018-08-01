package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel_;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;

/**
 * Tied to LabBatchStartingVessel entity
 * Only interested in ETL of flowcell tickets and vessels as created for MISEQ and FCT batch types.
 * Catches initial FCT creation via LabBatchStartingVessel
 * Then when flowcell loaded, catches update of LabBatchStartingVessel with flowcell designation
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class FctCreateEtl extends GenericEntityEtl<LabBatchStartingVessel,LabBatchStartingVessel> {

    FlowcellDesignationEjb flowcellDesignationEjb;

    public FctCreateEtl() {
    }

    @Inject
    public FctCreateEtl(LabBatchDao dao, FlowcellDesignationEjb flowcellDesignationEjb) {
        super(LabBatchStartingVessel.class, "fct_create", "batch_starting_vessels_aud",
                "batch_starting_vessel_id", dao);
        this.flowcellDesignationEjb = flowcellDesignationEjb;
    }

    @Override
    Long entityId(LabBatchStartingVessel entity) {
        return entity.getBatchStartingVesselId();
    }

    @Override
    Path rootId(Root<LabBatchStartingVessel> root) {
        return root.get(LabBatchStartingVessel_.batchStartingVesselId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId){
        return dataRecords( etlDateStr, isDelete, dao.findById(LabBatchStartingVessel.class, entityId) );
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, LabBatchStartingVessel labBatchStartingVessel) {

        if (labBatchStartingVessel == null ) {
            return null;
        }

        // Ignore all other batch types - this process is only interested in flowcell tickets
        LabBatch labBatch = labBatchStartingVessel.getLabBatch();
        LabBatch.LabBatchType labBatchType = labBatch.getLabBatchType();
        if( labBatchType != LabBatch.LabBatchType.MISEQ && labBatchType != LabBatch.LabBatchType.FCT ) {
            return null;
        }

        String lane = "";
        if( labBatchStartingVessel.getVesselPosition() != null ) {
            lane = labBatchStartingVessel.getVesselPosition().toString().replace("LANE","");
        }

        boolean poolTest = false;
        FlowcellDesignation flowcellDesignation = null;
        LabVessel dilutionVessel = labBatchStartingVessel.getDilutionVessel();

        // Gets pool test from the designation if it exists. Otherwise assumes MiSeq runs are pool tests.
        if( labBatch.getLabBatchType() == LabBatch.LabBatchType.MISEQ ) {
            poolTest = true;
        } else if( labBatchStartingVessel.getFlowcellDesignation() != null ) {
            // May or may not be preempted by flowcell loading events
            flowcellDesignation = labBatchStartingVessel.getFlowcellDesignation();
            poolTest = flowcellDesignation.isPoolTest();
        }

        return genericRecord(etlDateStr, isDelete,
                labBatchStartingVessel.getBatchStartingVesselId(),
                flowcellDesignation == null?"":flowcellDesignation.getDesignationId(),
                labBatch.getLabBatchId(),
                format(labBatch.getBatchName()),
                format(labBatch.getLabBatchType().toString()),
                format(labBatchStartingVessel.getLabVessel().getLabel()),
                dilutionVessel == null?"":dilutionVessel.getLabel(),
                format(labBatch.getCreatedOn()),
                format(labBatch.getFlowcellType()!=null?labBatch.getFlowcellType().getDisplayName():""),
                format(lane),
                format(labBatchStartingVessel.getConcentration()),
                poolTest ? "Y":"N"
        );
    }
}
