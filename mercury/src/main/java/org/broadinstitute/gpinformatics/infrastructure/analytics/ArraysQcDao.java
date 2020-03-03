package org.broadinstitute.gpinformatics.infrastructure.analytics;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcBlacklisting;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcBlacklisting_;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcContamination;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcGtConcordance;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc_;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.infrastructure.jpa.CriteriaInClauseCreator;
import org.broadinstitute.gpinformatics.infrastructure.jpa.JPASplitter;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequestScoped
@Stateful
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ArraysQcDao {

    @PersistenceContext(type = PersistenceContextType.EXTENDED, unitName = "analytics_pu")
    private EntityManager entityManager;

    @Inject
    private Deployment deployment;

    @Inject
    private InfiniumStarterConfig infiniumStarterConfig;

    public List<ArraysQc> findByBarcodes(List<String> chipWellBarcodes) {
        if( chipWellBarcodes == null || chipWellBarcodes.isEmpty() ) {
            return Collections.emptyList();
        }

        List<String> chipWellsInDatasource = new ArrayList<>();
        List<ArraysQc> syntheticData = new ArrayList<>();
        if (deployment != Deployment.PROD) {
            // Check trigger files
            File dataPath = new File(infiniumStarterConfig.getDataPath());
            File triggers = new File(dataPath, "triggers");
            File clinicalTriggers = new File(dataPath, "clinical_triggers");
            Set<String> triggerChipFolders = new HashSet<>();
            for (File folder: Arrays.asList(triggers, clinicalTriggers)) {
                if (folder != null && folder.exists()) {
                    Collections.addAll(triggerChipFolders, folder.list());
                }
            }

            for (String chipWellBarcode: chipWellBarcodes) {
                String chipBarcode = chipWellBarcode.replaceAll("_R\\d\\dC\\d\\d", "");
                if (triggerChipFolders.contains(chipBarcode)) {
                    chipWellsInDatasource.add(chipWellBarcode);
                } else {
                    ArraysQc arraysQc = new ArraysQc();
                    arraysQc.setChipWellBarcode(chipWellBarcode);
                    arraysQc.setCallRate(new BigDecimal(".99461"));
                    ArraysQcContamination contamination = new ArraysQcContamination();
                    contamination.setPctMix(new BigDecimal(".0551"));
                    arraysQc.setArraysQcContamination(Collections.singleton(contamination));
                    arraysQc.setHetPct(new BigDecimal(".11121"));
                    arraysQc.setGenderConcordancePf(true);
                    ArraysQcGtConcordance concordance = new ArraysQcGtConcordance();
                    concordance.setVariantType("SNP");
                    concordance.setGenotypeConcordance(new BigDecimal(".451"));
                    arraysQc.setArraysQcGtConcordances(Collections.singleton(concordance));
                    syntheticData.add(arraysQc);
                }
            }
        }

        List<ArraysQc> entities = JPASplitter.runCriteriaQuery(chipWellsInDatasource, new CriteriaInClauseCreator<String>() {
            @Override
            public Query createCriteriaInQuery(Collection<String> parameterList) {
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<ArraysQc> criteria = cb.createQuery(ArraysQc.class);
                Root<ArraysQc> root = criteria.from(ArraysQc.class);
                criteria.select(root).where(cb.and(
                        root.get(ArraysQc_.chipWellBarcode).in(parameterList)),
                        cb.equal(root.get(ArraysQc_.isLatest), 1));
                return entityManager.createQuery(criteria);
            }
        });

        syntheticData.addAll(entities);
        return syntheticData;
    }

    public Map<String, ArraysQc> findMapByBarcodes(List<String> chipWellBarcodes) {
        List<ArraysQc> arraysQcList = findByBarcodes(chipWellBarcodes);
        Map<String, ArraysQc> mapWellBarcodeToMetric = new HashMap<>();
        for (ArraysQc arraysQc : arraysQcList) {
            mapWellBarcodeToMetric.put(arraysQc.getChipWellBarcode(), arraysQc);
        }
        return mapWellBarcodeToMetric;
    }

    /**
     * Returns a raw list of all ArraysQcBlacklisting entities related to chip well barcode<br/>
     * Note:  Schema design allows more than one blacklist entry per chip well barcode so caller should account
     *    for possibility of multiples if details (e.g reasons, dates) required
     */
    @SuppressWarnings("WeakerAccess")
    public List<ArraysQcBlacklisting> findBlacklistByBarcodes(List<String> chipWellBarcodes) {
        if( chipWellBarcodes == null || chipWellBarcodes.isEmpty() ) {
            return Collections.emptyList();
        }
        return JPASplitter.runCriteriaQuery(chipWellBarcodes, new CriteriaInClauseCreator<String>() {
            @Override
            public Query createCriteriaInQuery(Collection<String> parameterList) {
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<ArraysQcBlacklisting> criteria = cb.createQuery(ArraysQcBlacklisting.class);
                Root<ArraysQcBlacklisting> root = criteria.from(ArraysQcBlacklisting.class);
                criteria.select(root)
                        .where(root.get(ArraysQcBlacklisting_.chipWellBarcode).in(parameterList))
                        .orderBy(cb.asc(root.get(ArraysQcBlacklisting_.chipWellBarcode)), cb.asc(root.get(ArraysQcBlacklisting_.blacklistedOn)));
                return entityManager.createQuery(criteria);
            }
        });
    }

    /**
     * Returns 0:n ArraysQcBlacklisting entities related to each chip well barcode
     */
    public ListValuedMap<String, ArraysQcBlacklisting> findBlacklistMapByBarcodes(List<String> chipWellBarcodes) {
        List<ArraysQcBlacklisting> arraysQcBlacklist = findBlacklistByBarcodes(chipWellBarcodes);
        ListValuedMap<String, ArraysQcBlacklisting> mapWellBarcodeToMetric = new ArrayListValuedHashMap<>();
        for (ArraysQcBlacklisting blacklist : arraysQcBlacklist) {
            mapWellBarcodeToMetric.put(blacklist.getChipWellBarcode(), blacklist);
        }
        return mapWellBarcodeToMetric;
    }
}
