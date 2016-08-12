package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateRangeSelector;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.DesignationLoadingTube;
import org.broadinstitute.gpinformatics.mercury.entity.run.DesignationLoadingTube_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch_;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationLoadingTubeActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationRowDto;
import org.hibernate.SQLQuery;
import org.hibernate.type.LongType;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data Access Object for designation tubes.
 */
@Stateful
@RequestScoped
public class DesignationLoadingTubeEjb {
    @Inject
    private BarcodedTubeDao barcodedTubeDao;

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
                "and not exists (select 1 from designation_loading_tube dsg " +
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

    /** Returns true if there was a pool test FCT for this tube and lcset. */
    private boolean hasPoolTest(@Nonnull LabVessel labVessel, @Nonnull String lcsetName) {
        for (DesignationLoadingTube designation :
                barcodedTubeDao.findList(DesignationLoadingTube.class, DesignationLoadingTube_.loadingTube, labVessel)) {
            if (designation.isPoolTest() && designation.getLcsetNames().contains(lcsetName) &&
                designation.getStatus() == DesignationLoadingTube.Status.IN_FCT) {
                return true;
            }
        }
        return false;
    }

    @Inject
    private LabBatchDao labBatchDao;

    public Map<DesignationRowDto, DesignationLoadingTube> update(
            List<DesignationRowDto> rowDtos, EnumSet<DesignationLoadingTube.Status> persistableStatuses) {
        Map<DesignationRowDto, DesignationLoadingTube> dtoAndTube = new HashMap<>();
        for (DesignationRowDto rowDto : rowDtos) {
            if (rowDto.isSelected() && persistableStatuses.contains(rowDto.getStatus())) {
                if (rowDto.getDesignationId() == null) {
                    LabVessel loadingTube = barcodedTubeDao.findByBarcode(rowDto.getBarcode());
                    Set<LabBatch> lcsets = new HashSet<>();
                    lcsets.add(labBatchDao.findByBusinessKey(rowDto.getPrimaryLcset()));
                    lcsets.addAll(labBatchDao.findListByList(LabBatch.class, LabBatch_.batchName,
                            rowDto.getAdditionalLcsets()));
                    Set<Product> products = new HashSet<>(labBatchDao.findListByList(Product.class,
                            Product_.productName, rowDto.getProductNames()));
                    LabEvent loadingTubeEvent = labBatchDao.findById(LabEvent.class, rowDto.getTubeEventId());
                    DesignationLoadingTube designation = new DesignationLoadingTube(loadingTube, lcsets,
                            loadingTubeEvent, products, rowDto.getIndexType(), rowDto.getPoolTest(),
                            rowDto.getSequencerModel(), rowDto.getNumberCycles(),
                            rowDto.getNumberLanes(), rowDto.getReadLength(), rowDto.getLoadingConc(),
                            rowDto.getRegulatoryDesignation().equals(DesignationLoadingTubeActionBean.CLINICAL),
                            rowDto.getNumberSamples(), rowDto.getStatus(), rowDto.getPriority());
                    labBatchDao.persist(designation);
                    dtoAndTube.put(rowDto, designation);
                } else {
                    DesignationLoadingTube designation = barcodedTubeDao.findById(DesignationLoadingTube.class,
                            rowDto.getDesignationId());
                    designation.setPriority(rowDto.getPriority());
                    designation.setSequencerModel(rowDto.getSequencerModel());
                    designation.setNumberLanes(rowDto.getNumberLanes());
                    designation.setLoadingConc(rowDto.getLoadingConc());
                    designation.setReadLength(rowDto.getReadLength());
                    designation.setIndexType(rowDto.getIndexType());
                    designation.setNumberCycles(rowDto.getNumberCycles());
                    designation.setPoolTest(rowDto.getPoolTest());
                    designation.setStatus(rowDto.getStatus());
                    dtoAndTube.put(rowDto, designation);
                }
            }
        }
        return dtoAndTube;
    }

    public Collection<DesignationLoadingTube> existingDesignations(List<DesignationLoadingTube.Status> statuses) {
        return labBatchDao.findListByList(DesignationLoadingTube.class, DesignationLoadingTube_.status, statuses);
    }
}
