package org.broadinstitute.gpinformatics.mercury.boundary.analysis;

import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AlignerDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.ReferenceSequenceDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;

import javax.annotation.Nonnull;
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
    private ReferenceSequenceDao referenceSequenceDao;

    /**
     * Add and return the newly created {@link Aligner} or have the existing one returned.
     *
     * @param alignerName The name of the {@link Aligner} to create.
     * @return the new or existing {@link Aligner}
     */
    public Aligner addAligner(@Nonnull String alignerName) {
        Aligner aligner = alignerDao.findByBusinessKey(alignerName);
        if (aligner == null) {
            alignerDao.persist(new Aligner(alignerName));
        }

        return aligner;
    }

    /**
     * Remove a {@link Collection} of aligners from the database.
     *
     * @param alignerKeys a collection of aligners to delete
     * @return the number of {@link Aligner}s that were deleted
     */
    public int removeAligners(@Nonnull String... alignerKeys) {
        int deleteCount = 0;
        for (String alignerKey : alignerKeys) {
            Aligner aligner = alignerDao.findByBusinessKey(alignerKey);
            if (alignerDao != null) {
                alignerDao.remove(aligner);
                deleteCount++;
            }
        }

        return deleteCount;
    }

    /**
     * Add and return the newly created {@link AnalysisType} or have the existing one returned.
     *
     * @param analysisTypeKey the name and business key of the {@link AnalysisType} to create
     * @return the new or existing {@link AnalysisType}
     */
    public AnalysisType addAnalysisType(@Nonnull String analysisTypeKey) {
        AnalysisType analysisType = analysisTypeDao.findByBusinessKey(analysisTypeKey);
        if (analysisType == null) {
            analysisTypeDao.persist(new AnalysisType(analysisTypeKey));
        }

        return analysisType;
    }

    /**
     * Remove a {@link Collection} of analysis types from the database.
     *
     * @param analysisTypeKeys a collection of {@link AnalysisType}s to delete
     * @return the number of {@link AnalysisType}s that were deleted
     */
    public int removeAnalysisTypes(@Nonnull String... analysisTypeKeys) {
        int deleteCount = 0;
        for (String analysisTypeKey : analysisTypeKeys) {
            AnalysisType analysisType = analysisTypeDao.findByBusinessKey(analysisTypeKey);
            if (analysisTypeDao != null) {
                analysisTypeDao.remove(analysisType);
            }
            deleteCount++;
        }

        return deleteCount;
    }

    /**
     * Add and return the newly created {@link ReferenceSequence} or have the existing one returned.
     *
     * @param name and business key the displayed name of the {@link ReferenceSequence}
     * @param version the version for the {@link ReferenceSequence}
     * @param isCurrent boolean flag to determine if this {@link ReferenceSequence} is the current one
     * @return the new or existing {@link ReferenceSequence}
     */
    public ReferenceSequence addReferenceSequence(@Nonnull String name, @Nonnull String version, boolean isCurrent) {
        ReferenceSequence referenceSequence = referenceSequenceDao.findByBusinessKey(name);

        if (referenceSequence == null) {
            referenceSequence = new ReferenceSequence(name, version);
            referenceSequence.setCurrent(isCurrent);
            referenceSequenceDao.persist(referenceSequence);
        }

        return referenceSequence;
    }

    /**
     * Remove a list of {@link ReferenceSequence}s from the database.
     *
     * @param referenceSequenceKeys  a list of {@link ReferenceSequence} business keys to delete
     * @return the number of {@link ReferenceSequence}s that were deleted
     */
    public int removeReferenceSequences(@Nonnull String... referenceSequenceKeys) {
        int deleteCount = 0;
        for (String referenceSequenceKey : referenceSequenceKeys) {
            ReferenceSequence referenceSequence = referenceSequenceDao.findByBusinessKey(referenceSequenceKey);
            if (referenceSequence != null) {
                referenceSequenceDao.remove(referenceSequence);
            }

            deleteCount++;
        }

        return deleteCount;
    }

    /**
     * Add and return the newly created {@link ReagentDesign} or have the existing one returned.
     *
     * @param name the reagent design name to add
     * @param type the reagent type
     * @return the new or existing {@link ReagentDesign}
     */
    public ReagentDesign addReagentDesign(@Nonnull String name, @Nonnull ReagentDesign.ReagentType type) {
        ReagentDesign reagentDesign = reagentDesignDao.findByBusinessKey(name);
        if (reagentDesign == null) {
            reagentDesign = new ReagentDesign(name, type);
            reagentDesignDao.persist(reagentDesign);
        }

        return reagentDesign;
    }

    /**
     * Remove a list of {@link ReagentDesign}s from the database.
     *
     * @param reagentDesignKeys  a list of {@link ReagentDesign} business keys to delete
     * @return the number of {@link ReagentDesign}s that were deleted
     */
    public int removeReagentDesigns(@Nonnull String... reagentDesignKeys) {
        int deleteCount = 0;
        for (String reagentDesignKey : reagentDesignKeys) {
            ReagentDesign reagentDesign = reagentDesignDao.findByBusinessKey(reagentDesignKey);
            if (reagentDesign != null) {
                reagentDesignDao.remove(reagentDesign);
            }

            deleteCount++;
        }

        return deleteCount;
    }
}
