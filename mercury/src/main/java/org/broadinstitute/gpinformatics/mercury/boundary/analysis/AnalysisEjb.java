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
     * Add and return true the if a newly created {@link Aligner} was made.
     *
     * @param alignerName The name and business key of the {@link Aligner} to create.
     * @return true if a new {@link Aligner} was created
     */
    public boolean addAligner(@Nonnull String alignerName) {
        boolean wasCreated = false;
        Aligner aligner = alignerDao.findByBusinessKey(alignerName);

        if (aligner == null) {
            alignerDao.persist(new Aligner(alignerName));
            wasCreated = true;
        }

        return wasCreated;
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
            if (aligner != null) {
                alignerDao.remove(aligner);
                deleteCount++;
            }
        }

        return deleteCount;
    }

    /**
     * Add and return true the if a newly created {@link AnalysisType} was made.
     *
     * @param analysisTypeKey the name and business key of the {@link AnalysisType} to create
     * @return true if a new {@link AnalysisType} was created
     */
    public boolean addAnalysisType(@Nonnull String analysisTypeKey) {
        boolean wasCreated = false;
        AnalysisType analysisType = analysisTypeDao.findByBusinessKey(analysisTypeKey);

        if (analysisType == null) {
            analysisTypeDao.persist(new AnalysisType(analysisTypeKey));
            wasCreated = true;
        }

        return wasCreated;
    }

    /**
     * Remove a list of {@link AnalysisType}s from the database.
     *
     * @param analysisTypeKeys a collection of {@link AnalysisType}s to delete
     * @return the number of {@link AnalysisType}s that were deleted
     */
    public int removeAnalysisTypes(@Nonnull String... analysisTypeKeys) {
        int deleteCount = 0;
        for (String analysisTypeKey : analysisTypeKeys) {
            AnalysisType analysisType = analysisTypeDao.findByBusinessKey(analysisTypeKey);
            if (analysisType != null) {
                analysisTypeDao.remove(analysisType);
            }
            deleteCount++;
        }

        return deleteCount;
    }

    /**
     * Add and return true the if a newly created {@link ReferenceSequence} was made.  If the isCurrent flag is
     * true, then it will find and update any existing {@link ReferenceSequence} of the same name that was previously
     * set to current.
     *
     * @param name      the displayed name of the {@link ReferenceSequence}
     * @param version   the version for the {@link ReferenceSequence}
     * @param isCurrent boolean flag to determine if this {@link ReferenceSequence} is the current one
     * @return true if a new {@link ReferenceSequence} was created
     */
    public boolean addReferenceSequence(@Nonnull String name, @Nonnull String version, boolean isCurrent) {
        ReferenceSequence referenceSequence = referenceSequenceDao.findByNameAndVersion(name, version);

        // If there is already a reference sequence, then this exists, so return that nothing was added.
        if (referenceSequence != null) {
            return false;
        }

        if (isCurrent) {
            // We need to change the existing current reference sequence to not be current before saving the new one as
            // current, if there is one set to that
            ReferenceSequence currentReferenceSequence = referenceSequenceDao.findCurrent(name);
            if (currentReferenceSequence != null) {
                currentReferenceSequence.setCurrent(false);
                referenceSequenceDao.persist(currentReferenceSequence);
            }
        }

        referenceSequence = new ReferenceSequence(name, version);
        referenceSequence.setCurrent(isCurrent);
        referenceSequenceDao.persist(referenceSequence);
        return true;
    }


    /**
     * Add and return true the if a newly created {@link ReferenceSequence} was made.  The newly created
     * {@link ReferenceSequence} will be automatically set to the current one and any existing {@link ReferenceSequence}
     * will be no longer be the current one.
     *
     * @param name      the displayed name of the {@link ReferenceSequence}
     * @param version   the version for the {@link ReferenceSequence}
     * @return true if a new {@link ReferenceSequence} was created
     */
    public boolean addReferenceSequence(@Nonnull String name, @Nonnull String version) {
        return addReferenceSequence(name, version, true);
    }

    /**
     * Remove a list of {@link ReferenceSequence}s from the database.
     *
     * @param referenceSequenceKeys a list of {@link ReferenceSequence} business keys to delete
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
     * Add and return true the if a newly created {@link ReagentDesign} was made.
     *
     * @param name the reagent design name to add
     * @param type the reagent type
     * @return true if a new {@link ReagentDesign} was created
     */
    public boolean addReagentDesign(@Nonnull String name, @Nonnull ReagentDesign.ReagentType type) {
        boolean wasCreated = false;
        ReagentDesign reagentDesign = reagentDesignDao.findByBusinessKey(name);

        if (reagentDesign == null) {
            reagentDesign = new ReagentDesign(name, type);
            reagentDesignDao.persist(reagentDesign);
            wasCreated = true;
        }

        return wasCreated;
    }

    /**
     * Remove a list of {@link ReagentDesign}s from the database.
     *
     * @param reagentDesignKeys a list of {@link ReagentDesign} business keys to delete
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
