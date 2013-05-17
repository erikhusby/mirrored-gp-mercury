package org.broadinstitute.gpinformatics.mercury.boundary.analysis;

import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AlignerDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Collection;

/**
 * This provides all the APIs for working with the information that is managed by the pipeline
 * team and then applied to products or projects.
 */
@Stateful
@RequestScoped
public class AnalysisEjb {

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private AnalysisTypeDao analysisTypeDao;

    @Inject
    private AlignerDao alignerDao;

    @Inject
    private ReagentDesignDao referenceSequenceDao;

    /**
     * Add a aligner.
     *
     * @param aligner The aligner to add.
     */
    public void addAligner(Aligner aligner) {
        alignerDao.persist(aligner);
    }

    /**
     * Remove aligner.
     *
     * @param aligners multiple aligners can be removed at once.
     */
    public void removeAligner(Aligner aligners) {
        alignerDao.remove(aligners);
    }

    /**
     * Add a new analysis type.
     *
     * @param analysisType The type to add.
     */
    public void addAnalysisType(AnalysisType analysisType) {
        analysisTypeDao.persist(analysisType);
    }

    /**
     * Remove analysis types.
     *
     * @param analysisTypes multiple analysis types can be removed at once.
     */
    public void removeAnalysisTypes(Collection<AnalysisType> analysisTypes) {
        analysisTypeDao.remove(analysisTypes);
    }

    /**
     * Add a new reference sequence.
     *
     * @param referenceSequence The reference sequence to add.
     */
    public void addReferenceSequence(ReferenceSequence referenceSequence) {
        referenceSequenceDao.persist(referenceSequence);
    }

    /**
     * Remove reference sequence.
     *
     * @param referenceSequences multiple reference sequences can be removed at once.
     */
    public void removeReferenceSequences(Collection<ReferenceSequence> referenceSequences) {
        referenceSequenceDao.remove(referenceSequences);
    }

    /**
     * Add a new reagent design.
     *
     * @param reagentDesign The reagent design to add.
     */
    public void addReagentDesign(ReagentDesign reagentDesign) {
        reagentDesignDao.persist(reagentDesign);
    }

    /**
     * Remove reagent design.
     *
     * @param reagentDesigns multiple reagent designs can be removed at once.
     */
    public void removeReagentDesigns(Collection<ReagentDesign> reagentDesigns) {
        reagentDesignDao.remove(reagentDesigns);
    }
}
