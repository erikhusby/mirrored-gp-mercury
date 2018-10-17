package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.jpa.CriteriaInClauseCreator;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.jpa.JPASplitter;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Stateful
@RequestScoped
public class SampleInstanceEntityDao extends GenericDao {

    public SampleInstanceEntity findByName(String sampleLibraryName) {
        return findSingle(SampleInstanceEntity.class, SampleInstanceEntity_.sampleLibraryName, sampleLibraryName );
    }

    /** Returns all sample instance entities having a matching mercury sample name. */
    public List<SampleInstanceEntity> findBySampleNames(Collection<String> sampleNames) {
        if (CollectionUtils.isEmpty(sampleNames)) {
            return Collections.emptyList();
        }
        return JPASplitter.runCriteriaQuery(sampleNames, new CriteriaInClauseCreator<String>() {
            @Override
            public Query createCriteriaInQuery(Collection<String> parameterList) {
                CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
                CriteriaQuery<SampleInstanceEntity> criteria = cb.createQuery(SampleInstanceEntity.class);
                Root<SampleInstanceEntity> root = criteria.from(SampleInstanceEntity.class);
                Join<SampleInstanceEntity, MercurySample> samples = root.join(SampleInstanceEntity_.mercurySample);
                criteria.select(root).where(samples.get(MercurySample_.sampleKey).in(parameterList));
                return getEntityManager().createQuery(criteria);
            }
        });
    }

    /** Returns all sample instance entities having a matching tube barcode. */
    public List<SampleInstanceEntity> findByBarcodes(Collection<String> tubeBarcodes) {
        if (CollectionUtils.isEmpty(tubeBarcodes)) {
            return Collections.emptyList();
        }
        return JPASplitter.runCriteriaQuery(tubeBarcodes, new CriteriaInClauseCreator<String>() {
            @Override
            public Query createCriteriaInQuery(Collection<String> parameterList) {
                CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
                CriteriaQuery<SampleInstanceEntity> criteria = cb.createQuery(SampleInstanceEntity.class);
                Root<SampleInstanceEntity> root = criteria.from(SampleInstanceEntity.class);
                Join<SampleInstanceEntity, LabVessel> labVessels = root.join(SampleInstanceEntity_.labVessel);
                criteria.select(root).where(labVessels.get(LabVessel_.label).in(parameterList));
                return getEntityManager().createQuery(criteria);
            }
        });
    }

    /**
     * Returns a map of identifier to mercury sample for the sample instance entities identified either
     * by sample name or by lab vessel barcode. There may be multiple MercurySamples for one barcode.
     */
    public Multimap<String, MercurySample> lookupSamplesByIdentifiers(Collection<String> identifiers) {
        Multimap<String, MercurySample> map = HashMultimap.create();
        for (SampleInstanceEntity sampleInstanceEntity : findBySampleNames(identifiers)) {
            MercurySample mercurySample = sampleInstanceEntity.getMercurySample();
            map.put(mercurySample.getSampleKey(), mercurySample);
        }
        for (SampleInstanceEntity sampleInstanceEntity : findByBarcodes(identifiers)) {
            LabVessel labVessel = sampleInstanceEntity.getLabVessel();
            map.put(labVessel.getLabel(), sampleInstanceEntity.getMercurySample());
        }
        return map;
    }
}
