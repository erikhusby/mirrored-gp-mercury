package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateRangeSelector;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationDto;
import org.hibernate.SQLQuery;
import org.hibernate.type.LongType;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Data Access Object for designation tubes.
 */
@Stateful
@RequestScoped
public class FlowcellDesignationEjb {
    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private LabBatchDao labBatchDao;

    /**
     * Returns barcoded normalization tubes that were not previously designated, or
     * only designated for pool test, and that don't have a downstream denature tube
     * that was designated. Also excludes norm tubes followed by another norm transfer.
     */
    public Collection<BarcodedTube> eligibleDesignations(DateRangeSelector dateRange) {
        List<Long> tubeIds = eligibleDesignationTubeIds(dateRange);
        return barcodedTubeDao.findListByList(BarcodedTube.class, BarcodedTube_.labVesselId, tubeIds);
    }

    /** Returns tube ids of eligible designation tubes. */
    private List<Long> eligibleDesignationTubeIds(DateRangeSelector dateRange) {
        Query normTubeQuery = barcodedTubeDao.getEntityManager().createNativeQuery(
                "select distinct normTube.lab_vessel_id " +
                "from lab_vessel_containers normRack " +
                "join vessel_transfer normXfer on normXfer.target_vessel = normRack.containers " +
                "join lv_map_position_to_vessel normPv on " +
                "  normPv.lab_vessel = normXfer.target_vessel " +
                "  and normPv.mapkey = normXfer.target_position " +
                "join lab_vessel normTube on normTube.lab_vessel_id = normPv.map_position_to_vessel " +
                "join lab_event normEvent on " +
                "  normEvent.lab_event_id = normXfer.lab_event " +
                "  and normEvent.lab_event_type = 'NORMALIZATION_TRANSFER' " +
                "  and normEvent.event_date >= :startDate " +
                "  and normEvent.event_date <= :endDate " +
                "and not exists (select 1 from flowcell_designation dsg " +
                "  where dsg.loading_tube = normTube.lab_vessel_id and pool_test = 0) " +
                "and not exists (select 1 from batch_starting_vessels normFctTubes " +
                "  join lab_batch normFct on normFctTubes.lab_batch = normFct.lab_batch_id " +
                "  where normFctTubes.lab_vessel = normTube.lab_vessel_id " +
                "  and normFct.lab_batch_type in ('MISEQ', 'FCT')) " +
                "and not exists (select 1 from vessel_transfer followingXfer " +
                "  join lab_event followingEvent on followingXfer.lab_event = followingEvent.lab_event_id " +
                "  where followingXfer.source_vessel = normRack.containers " +
                "  and  followingEvent.lab_event_type = 'NORMALIZATION_TRANSFER') " +
                "and not exists (select 1 from lv_map_position_to_vessel denatureSourcePv " +
                "  join vessel_transfer denatureXfer on " +
                "    denatureSourcePv.lab_vessel = denatureXfer.source_vessel " +
                "    and denatureSourcePv.mapkey = denatureXfer.source_position " +
                "  join lv_map_position_to_vessel denaturePv on " +
                "    denaturePv.lab_vessel = denatureXfer.target_vessel " +
                "    and denaturePv.mapkey = denatureXfer.target_position " +
                "  join lab_event denatureEvent on " +
                "    denatureEvent.lab_event_id = denatureXfer.lab_event " +
                "    and lab_event_type = 'DENATURE_TRANSFER' " +
                "  join batch_starting_vessels denatureFctTubes " +
                "    on denatureFctTubes.lab_vessel = denaturePv.map_position_to_vessel " +
                "  join lab_batch denatureFct on denatureFctTubes.lab_batch = denatureFct.lab_batch_id " +
                "    and denatureFct.lab_batch_type in ('MISEQ', 'FCT') " +
                "  where denatureSourcePv.map_position_to_vessel = normTube.lab_vessel_id) ");

        normTubeQuery.setParameter("startDate", dateRange.getStart(), TemporalType.DATE);
        normTubeQuery.setParameter("endDate", dateRange.getEnd(), TemporalType.DATE);

        normTubeQuery.unwrap(SQLQuery.class).addScalar("lab_vessel_id", LongType.INSTANCE);

        try {
            return normTubeQuery.getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }

    /**
     * Persists each dto provided it is selected and has status that is targetable.
     * If it has a null designation id it is persisted as a new entity.
     *
     * @param dtos
     * @param targetableStatuses
     * @return
     */
    public List<Pair<DesignationDto, FlowcellDesignation>> update(Collection<DesignationDto> dtos,
            EnumSet<FlowcellDesignation.Status> targetableStatuses) {
        List<Pair<DesignationDto, FlowcellDesignation>> dtoAndTube = new ArrayList<>();
        for (DesignationDto dto : dtos) {
            if (dto.isSelected() && targetableStatuses.contains(dto.getStatus())) {
                if (dto.getDesignationId() == null) {
                    LabVessel loadingTube = barcodedTubeDao.findByBarcode(dto.getBarcode());
                    LabBatch chosenLcset = StringUtils.isBlank(dto.getChosenLcset()) ?
                            null : labBatchDao.findByName(dto.getChosenLcset());
                    FlowcellDesignation designation = new FlowcellDesignation(loadingTube, chosenLcset,
                            dto.getIndexType(), dto.getPoolTest(), dto.getSequencerModel(),
                            dto.getNumberLanes(), dto.getReadLength(), dto.getLoadingConc(), dto.getPairedEndRead(),
                            dto.getStatus(), dto.getPriority());
                    labBatchDao.persist(designation);
                    dtoAndTube.add(Pair.of(dto, designation));
                } else {
                    FlowcellDesignation designation = barcodedTubeDao.findById(FlowcellDesignation.class,
                            dto.getDesignationId());
                    designation.setPriority(dto.getPriority());
                    designation.setSequencerModel(dto.getSequencerModel());
                    designation.setNumberLanes(dto.getNumberLanes());
                    designation.setLoadingConc(dto.getLoadingConc());
                    designation.setReadLength(dto.getReadLength());
                    designation.setIndexType(dto.getIndexType());
                    designation.setPoolTest(dto.getPoolTest());
                    designation.setStatus(dto.getStatus());
                    dtoAndTube.add(Pair.of(dto, designation));
                }
            }
        }
        return dtoAndTube;
    }

    public Collection<FlowcellDesignation> updateChosenLcset(Collection<Pair<Long, String>> designationIdLcsets) {
        List<FlowcellDesignation> updates = new ArrayList<>();
        for (Pair<Long, String> designationIdLcset : designationIdLcsets) {
            FlowcellDesignation designation = barcodedTubeDao.findById(FlowcellDesignation.class,
                    designationIdLcset.getLeft());
            LabBatch lcset = labBatchDao.findByBusinessKey(designationIdLcset.getRight());
            if (designation != null) {
                designation.setChosenLcset(lcset);
                updates.add(designation);
            }
        }
        return updates;
    }

    public Collection<FlowcellDesignation> existingDesignations(List<FlowcellDesignation.Status> statuses) {
        return labBatchDao.findListByList(FlowcellDesignation.class, FlowcellDesignation_.status, statuses);
    }

    /** Returns the flowcell designations used in the FCT or MISEQ batch sorted by descending create date. */
    public List<FlowcellDesignation> getFlowcellDesignations(LabBatch fct) {
        List<LabVessel> loadingTubes = new ArrayList<>();
        if (fct != null && CollectionUtils.isNotEmpty(fct.getLabBatchStartingVessels())) {
            for (LabBatchStartingVessel labBatchStartingVessel : fct.getLabBatchStartingVessels()) {
                loadingTubes.add(labBatchStartingVessel.getLabVessel());
            }
        }
        return getFlowcellDesignations(loadingTubes);
    }

    /** Returns the flowcell designations for loading tubes sorted by descending create date. */
    public List<FlowcellDesignation> getFlowcellDesignations(Collection<LabVessel> loadingTubes) {
        List<FlowcellDesignation> list = labBatchDao.findListByList(FlowcellDesignation.class,
                FlowcellDesignation_.loadingTube, loadingTubes);
        Collections.sort(list, FlowcellDesignation.BY_DATE_DESC);
        return list;
    }

}
