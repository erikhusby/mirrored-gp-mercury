package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel_;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Tied to LabBatchStartingVessel entity
 * Only interested in ETL of flowcell tickets and vessels as created for MISEQ and FCT batch types.
 */
@Stateful
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

        // Gets pool test from the designation if it exists. Otherwise assumes MiSeq runs are pool tests.
        List<FlowcellDesignation> flowcellDesignations = flowcellDesignationEjb.getFlowcellDesignations(
                Collections.singleton(labBatchStartingVessel.getLabVessel()));
        boolean poolTest = CollectionUtils.isNotEmpty(flowcellDesignations) ?
                flowcellDesignations.iterator().next().isPoolTest() :
                labBatch.getLabBatchType() == LabBatch.LabBatchType.MISEQ;

        return genericRecord(etlDateStr, isDelete,
                labBatchStartingVessel.getBatchStartingVesselId(),
                labBatch.getLabBatchId(),
                format(labBatch.getBatchName()),
                format(labBatch.getLabBatchType().toString()),
                format(labBatchStartingVessel.getLabVessel().getLabel()),
                format(labBatch.getCreatedOn()),
                format(labBatch.getFlowcellType()!=null?labBatch.getFlowcellType().getDisplayName():""),
                format(lane),
                format(labBatchStartingVessel.getConcentration()),
                poolTest ? "Y":"N"
        );
    }
}
